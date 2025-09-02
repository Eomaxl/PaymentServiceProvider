package com.paymentprovider.common.performance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load balancing service for distributing requests across multiple instances.
 * Implements various load balancing algorithms and health checking.
 */
@Service
public class LoadBalancingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * Round-robin load balancing
     */
    public String selectServerRoundRobin(List<String> servers) {
        if (servers.isEmpty()) {
            throw new IllegalArgumentException("No servers available");
        }

        int index = roundRobinCounter.getAndIncrement() % servers.size();
        return servers.get(index);
    }

    /**
     * Weighted round-robin load balancing
     */
    public String selectServerWeightedRoundRobin(List<ServerInstance> servers) {
        if (servers.isEmpty()) {
            throw new IllegalArgumentException("No servers available");
        }

        int totalWeight = servers.stream().mapToInt(ServerInstance::getWeight).sum();
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

        int currentWeight = 0;
        for (ServerInstance server : servers) {
            currentWeight += server.getWeight();
            if (randomWeight < currentWeight) {
                return server.getUrl();
            }
        }

        return servers.get(0).getUrl(); // Fallback
    }

    /**
     * Least connections load balancing
     */
    public CompletableFuture<String> selectServerLeastConnections(List<String> servers) {
        return CompletableFuture.supplyAsync(() -> {
            String bestServer = null;
            long minConnections = Long.MAX_VALUE;

            for (String server : servers) {
                String connectionKey = "connections:" + server;
                Long connections = (Long) redisTemplate.opsForValue().get(connectionKey);
                if (connections == null) connections = 0L;

                if (connections < minConnections) {
                    minConnections = connections;
                    bestServer = server;
                }
            }

            // Increment connection count for selected server
            if (bestServer != null) {
                String connectionKey = "connections:" + bestServer;
                redisTemplate.opsForValue().increment(connectionKey);
                redisTemplate.expire(connectionKey, Duration.ofMinutes(5));
            }

            return bestServer;
        });
    }

    /**
     * Response time-based load balancing
     */
    public CompletableFuture<String> selectServerByResponseTime(List<String> servers) {
        return CompletableFuture.supplyAsync(() -> {
            String bestServer = null;
            long minResponseTime = Long.MAX_VALUE;

            for (String server : servers) {
                String responseTimeKey = "response_time:" + server;
                Long responseTime = (Long) redisTemplate.opsForValue().get(responseTimeKey);
                if (responseTime == null) responseTime = 1000L; // Default 1 second

                if (responseTime < minResponseTime) {
                    minResponseTime = responseTime;
                    bestServer = server;
                }
            }

            return bestServer;
        });
    }

    /**
     * Health check for servers
     */
    public CompletableFuture<List<String>> getHealthyServers(List<String> servers) {
        List<CompletableFuture<String>> healthChecks = servers.stream()
                .map(this::checkServerHealth)
                .toList();

        return CompletableFuture.allOf(healthChecks.toArray(new CompletableFuture[0]))
                .thenApply(v -> healthChecks.stream()
                        .map(CompletableFuture::join)
                        .filter(server -> server != null)
                        .toList());
    }

    /**
     * Individual server health check
     */
    private CompletableFuture<String> checkServerHealth(String serverUrl) {
        WebClient webClient = webClientBuilder.build();

        return webClient.get()
                .uri(serverUrl + "/health")
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .map(response -> response.getStatusCode().is2xxSuccessful() ? serverUrl : null)
                .onErrorReturn(null)
                .toFuture();
    }

    /**
     * Update server response time metrics
     */
    public void updateResponseTime(String serverUrl, long responseTimeMs) {
        String responseTimeKey = "response_time:" + serverUrl;

        // Use exponential moving average
        Long currentAvg = (Long) redisTemplate.opsForValue().get(responseTimeKey);
        if (currentAvg == null) {
            currentAvg = responseTimeMs;
        } else {
            // EMA with alpha = 0.3
            currentAvg = (long) (0.3 * responseTimeMs + 0.7 * currentAvg);
        }

        redisTemplate.opsForValue().set(responseTimeKey, currentAvg, Duration.ofMinutes(10));
    }

    /**
     * Circuit breaker for server instances
     */
    public CompletableFuture<Boolean> isServerAvailable(String serverUrl) {
        return CompletableFuture.supplyAsync(() -> {
            String circuitKey = "circuit:" + serverUrl;
            String status = (String) redisTemplate.opsForValue().get(circuitKey);

            if ("OPEN".equals(status)) {
                // Check if circuit should be half-open
                String lastFailureKey = "last_failure:" + serverUrl;
                Long lastFailure = (Long) redisTemplate.opsForValue().get(lastFailureKey);

                if (lastFailure != null &&
                        System.currentTimeMillis() - lastFailure > Duration.ofMinutes(5).toMillis()) {
                    redisTemplate.opsForValue().set(circuitKey, "HALF_OPEN");
                    return true; // Allow one request to test
                }
                return false;
            }

            return true; // CLOSED or HALF_OPEN
        });
    }

    /**
     * Record server failure
     */
    public void recordServerFailure(String serverUrl) {
        String failureKey = "failures:" + serverUrl;
        String circuitKey = "circuit:" + serverUrl;
        String lastFailureKey = "last_failure:" + serverUrl;

        Long failures = redisTemplate.opsForValue().increment(failureKey);
        redisTemplate.expire(failureKey, Duration.ofMinutes(10));
        redisTemplate.opsForValue().set(lastFailureKey, System.currentTimeMillis());

        if (failures >= 5) { // Threshold
            redisTemplate.opsForValue().set(circuitKey, "OPEN", Duration.ofMinutes(5));
        }
    }

    /**
     * Record server success
     */
    public void recordServerSuccess(String serverUrl) {
        String failureKey = "failures:" + serverUrl;
        String circuitKey = "circuit:" + serverUrl;

        redisTemplate.delete(failureKey);
        redisTemplate.opsForValue().set(circuitKey, "CLOSED");
    }

    /**
     * Adaptive load balancing based on current metrics
     */
    public CompletableFuture<String> selectServerAdaptive(List<String> servers) {
        return getHealthyServers(servers)
                .thenCompose(healthyServers -> {
                    if (healthyServers.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    // Get current system load
                    int systemLoad = getCurrentSystemLoad();

                    if (systemLoad > 80) {
                        // High load - use response time based selection
                        return selectServerByResponseTime(healthyServers);
                    } else if (systemLoad > 50) {
                        // Medium load - use least connections
                        return selectServerLeastConnections(healthyServers);
                    } else {
                        // Low load - use round robin
                        return CompletableFuture.completedFuture(
                                selectServerRoundRobin(healthyServers));
                    }
                });
    }

    /**
     * Sticky session support
     */
    public String selectServerSticky(String sessionId, List<String> servers) {
        String stickyKey = "sticky_session:" + sessionId;
        String assignedServer = (String) redisTemplate.opsForValue().get(stickyKey);

        if (assignedServer != null && servers.contains(assignedServer)) {
            return assignedServer;
        }

        // Assign new server
        String newServer = selectServerRoundRobin(servers);
        redisTemplate.opsForValue().set(stickyKey, newServer, Duration.ofHours(1));

        return newServer;
    }

    /**
     * Geographic load balancing
     */
    public String selectServerByGeography(String clientRegion, List<ServerInstance> servers) {
        // First try to find server in same region
        List<ServerInstance> regionalServers = servers.stream()
                .filter(server -> clientRegion.equals(server.getRegion()))
                .toList();

        if (!regionalServers.isEmpty()) {
            return selectServerWeightedRoundRobin(regionalServers);
        }

        // Fallback to any available server
        return selectServerWeightedRoundRobin(servers);
    }

    private int getCurrentSystemLoad() {
        // Implementation would get actual system metrics
        return (int) (Math.random() * 100);
    }

    /**
     * Server instance with metadata
     */
    public static class ServerInstance {
        private final String url;
        private final int weight;
        private final String region;
        private final String datacenter;

        public ServerInstance(String url, int weight, String region, String datacenter) {
            this.url = url;
            this.weight = weight;
            this.region = region;
            this.datacenter = datacenter;
        }

        // Getters
        public String getUrl() { return url; }
        public int getWeight() { return weight; }
        public String getRegion() { return region; }
        public String getDatacenter() { return datacenter; }
    }
}