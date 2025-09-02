package com.paymentprovider.common.performance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * Performance optimization service for handling high traffic and spikes.
 * Implements rate limiting, circuit breakers, bulkheading, and adaptive scaling.
 */
@Service
public class PerformanceOptimizationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private Executor performanceExecutor;

    private final Semaphore requestSemaphore = new Semaphore(1000); // Max concurrent requests

    /**
     * Rate limiting with sliding window
     */
    public CompletableFuture<Boolean> checkRateLimit(String key, int maxRequests, Duration window) {
        return CompletableFuture.supplyAsync(() -> {
            String redisKey = "rate_limit:" + key;
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - window.toMillis();

            // Remove old entries
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // Count current requests
            Long currentCount = redisTemplate.opsForZSet().count(redisKey, windowStart, currentTime);

            if (currentCount < maxRequests) {
                // Add current request
                redisTemplate.opsForZSet().add(redisKey, currentTime, currentTime);
                redisTemplate.expire(redisKey, window);
                return true;
            }

            return false;
        }, performanceExecutor);
    }

    /**
     * Circuit breaker pattern implementation
     */
    public <T> CompletableFuture<T> executeWithCircuitBreaker(String circuitKey,
                                                              CompletableFuture<T> operation,
                                                              T fallbackValue) {
        return CompletableFuture.supplyAsync(() -> {
            String statusKey = "circuit:" + circuitKey + ":status";
            String failureKey = "circuit:" + circuitKey + ":failures";

            // Check circuit status
            String status = (String) redisTemplate.opsForValue().get(statusKey);
            if ("OPEN".equals(status)) {
                return fallbackValue; // Circuit is open, return fallback
            }

            try {
                T result = operation.join();

                // Reset failure count on success
                redisTemplate.delete(failureKey);
                redisTemplate.opsForValue().set(statusKey, "CLOSED");

                return result;
            } catch (Exception e) {
                // Increment failure count
                Long failures = redisTemplate.opsForValue().increment(failureKey);

                if (failures >= 5) { // Threshold
                    // Open circuit
                    redisTemplate.opsForValue().set(statusKey, "OPEN", Duration.ofMinutes(5));
                }

                return fallbackValue;
            }
        }, performanceExecutor);
    }

    /**
     * Bulkhead pattern - isolate resources
     */
    public <T> CompletableFuture<T> executeWithBulkhead(CompletableFuture<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                requestSemaphore.acquire();
                return operation.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request rejected - bulkhead full", e);
            } finally {
                requestSemaphore.release();
            }
        }, performanceExecutor);
    }

    /**
     * Adaptive batching based on system load
     */
    public <T> CompletableFuture<List<T>> adaptiveBatch(List<CompletableFuture<T>> operations) {
        return CompletableFuture.supplyAsync(() -> {
            int systemLoad = getCurrentSystemLoad();
            int batchSize = calculateOptimalBatchSize(systemLoad, operations.size());

            List<T> results = new java.util.ArrayList<>();

            for (int i = 0; i < operations.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, operations.size());
                List<CompletableFuture<T>> batch = operations.subList(i, endIndex);

                // Process batch
                CompletableFuture<Void> batchFuture = CompletableFuture.allOf(
                        batch.toArray(new CompletableFuture[0]));

                batchFuture.join();

                // Collect results
                batch.forEach(future -> results.add(future.join()));

                // Adaptive delay based on system load
                if (systemLoad > 80) {
                    try {
                        Thread.sleep(100); // Throttle when system is under high load
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            return results;
        }, performanceExecutor);
    }

    /**
     * Reactive stream processing with backpressure
     */
    public <T, R> Flux<R> processStreamWithBackpressure(Flux<T> inputStream,
                                                        java.util.function.Function<T, Mono<R>> processor,
                                                        int concurrency) {
        return inputStream
                .onBackpressureBuffer(10000) // Buffer up to 10k items
                .flatMap(processor, concurrency)
                .onErrorContinue((error, item) -> {
                    // Log error and continue processing
                    System.err.println("Error processing item: " + item + " - " + error.getMessage());
                })
                .subscribeOn(Schedulers.parallel());
    }

    /**
     * Connection pooling optimization
     */
    public WebClient createOptimizedWebClient(String baseUrl) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
                    configurer.defaultCodecs().enableLoggingRequestDetails(false); // Disable for performance
                })
                .build();
    }

    /**
     * Memory-efficient data processing
     */
    public <T> CompletableFuture<Void> processLargeDataset(List<T> dataset,
                                                           java.util.function.Consumer<T> processor,
                                                           int chunkSize) {
        return CompletableFuture.runAsync(() -> {
            for (int i = 0; i < dataset.size(); i += chunkSize) {
                int endIndex = Math.min(i + chunkSize, dataset.size());
                List<T> chunk = dataset.subList(i, endIndex);

                // Process chunk
                chunk.parallelStream().forEach(processor);

                // Force garbage collection of processed chunk
                chunk.clear();

                // Yield to other threads
                Thread.yield();
            }
        }, performanceExecutor);
    }

    /**
     * Cache warming strategy
     */
    public CompletableFuture<Void> warmupCache(List<String> keys,
                                               java.util.function.Function<String, Object> dataLoader) {
        return CompletableFuture.runAsync(() -> {
            keys.parallelStream().forEach(key -> {
                try {
                    Object data = dataLoader.apply(key);
                    redisTemplate.opsForValue().set("warmup:" + key, data, Duration.ofHours(1));
                } catch (Exception e) {
                    System.err.println("Failed to warm cache for key: " + key + " - " + e.getMessage());
                }
            });
        }, performanceExecutor);
    }

    /**
     * Spike handling with queue-based processing
     */
    public <T> CompletableFuture<Void> handleSpike(List<T> spikeData,
                                                   java.util.function.Consumer<T> processor) {
        return CompletableFuture.runAsync(() -> {
            // Use Redis as a queue for spike handling
            String queueKey = "spike_queue:" + System.currentTimeMillis();

            // Add all items to queue
            spikeData.forEach(item ->
                    redisTemplate.opsForList().rightPush(queueKey, item));

            // Process items from queue with controlled rate
            while (redisTemplate.opsForList().size(queueKey) > 0) {
                @SuppressWarnings("unchecked")
                T item = (T) redisTemplate.opsForList().leftPop(queueKey);
                if (item != null) {
                    try {
                        processor.accept(item);
                    } catch (Exception e) {
                        // Re-queue failed items
                        redisTemplate.opsForList().rightPush(queueKey + ":failed", item);
                    }
                }

                // Control processing rate
                try {
                    Thread.sleep(10); // 100 items per second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, performanceExecutor);
    }

    /**
     * Auto-scaling trigger based on metrics
     */
    public CompletableFuture<ScalingDecision> evaluateScaling() {
        return CompletableFuture.supplyAsync(() -> {
            // Collect metrics
            int currentLoad = getCurrentSystemLoad();
            long memoryUsage = getMemoryUsage();
            int activeConnections = getActiveConnections();
            double responseTime = getAverageResponseTime();

            // Scaling decision logic
            if (currentLoad > 80 || memoryUsage > 85 || responseTime > 1000) {
                return new ScalingDecision(ScalingAction.SCALE_UP,
                        "High load detected: CPU=" + currentLoad + "%, Memory=" + memoryUsage + "%");
            } else if (currentLoad < 30 && memoryUsage < 50 && responseTime < 200) {
                return new ScalingDecision(ScalingAction.SCALE_DOWN,
                        "Low load detected: CPU=" + currentLoad + "%, Memory=" + memoryUsage + "%");
            } else {
                return new ScalingDecision(ScalingAction.NO_ACTION, "System load within normal range");
            }
        }, performanceExecutor);
    }

    // Helper methods
    private int getCurrentSystemLoad() {
        // Implementation would get actual system metrics
        return (int) (Math.random() * 100);
    }

    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return ((runtime.totalMemory() - runtime.freeMemory()) * 100) / runtime.maxMemory();
    }

    private int getActiveConnections() {
        // Implementation would get actual connection count
        return (int) (Math.random() * 1000);
    }

    private double getAverageResponseTime() {
        // Implementation would get actual response time metrics
        return Math.random() * 2000;
    }

    private int calculateOptimalBatchSize(int systemLoad, int totalOperations) {
        if (systemLoad > 80) {
            return Math.min(10, totalOperations); // Small batches under high load
        } else if (systemLoad > 50) {
            return Math.min(50, totalOperations); // Medium batches under medium load
        } else {
            return Math.min(100, totalOperations); // Large batches under low load
        }
    }

    // DTOs
    public static class ScalingDecision {
        private final ScalingAction action;
        private final String reason;

        public ScalingDecision(ScalingAction action, String reason) {
            this.action = action;
            this.reason = reason;
        }

        public ScalingAction getAction() { return action; }
        public String getReason() { return reason; }
    }

    public enum ScalingAction {
        SCALE_UP, SCALE_DOWN, NO_ACTION
    }
}