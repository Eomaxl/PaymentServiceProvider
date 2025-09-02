package com.paymentprovider.paymentservice.services;

import com.paymentprovider.common.async.AsyncMessagePublisher;
import com.paymentprovider.common.patterns.strategy.PaymentStrategy;
import com.paymentprovider.common.patterns.factory.PaymentProcessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

/**
 * Async payment processor implementing Command pattern and async communication.
 * Follows Single Responsibility Principle and Dependency Inversion Principle.
 */
@Service
public class AsyncPaymentProcessor {

    private final PaymentProcessorFactory processorFactory;
    private final AsyncMessagePublisher messagePublisher;

    @Autowired
    public AsyncPaymentProcessor(PaymentProcessorFactory processorFactory,
                                 AsyncMessagePublisher messagePublisher) {
        this.processorFactory = processorFactory;
        this.messagePublisher = messagePublisher;
    }

    /**
     * Process payment asynchronously with event publishing
     */
    public CompletableFuture<PaymentStrategy.PaymentResult> processPaymentAsync(
            PaymentStrategy.PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // Get appropriate processor using Factory pattern
            PaymentStrategy processor = processorFactory.createProcessor(request.getPaymentMethod())
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported payment method"));

            // Process payment
            PaymentStrategy.PaymentResult result = processor.processPayment(request);

            // Publish event asynchronously
            publishPaymentEvent(request, result);

            return result;
        });
    }

    /**
     * Batch payment processing with async communication
     */
    public CompletableFuture<BatchPaymentResult> processBatchAsync(
            BatchPaymentRequest batchRequest) {
        return CompletableFuture.supplyAsync(() -> {
            BatchPaymentResult batchResult = new BatchPaymentResult(batchRequest.getBatchId());

            // Process payments in parallel
            CompletableFuture<PaymentStrategy.PaymentResult>[] futures =
                    batchRequest.getPayments().stream()
                            .map(this::processPaymentAsync)
                            .toArray(CompletableFuture[]::new);

            // Wait for all to complete
            CompletableFuture.allOf(futures).join();

            // Collect results
            for (CompletableFuture<PaymentStrategy.PaymentResult> future : futures) {
                batchResult.addResult(future.join());
            }

            // Publish batch completion event
            publishBatchCompletionEvent(batchRequest, batchResult);

            return batchResult;
        });
    }

    private void publishPaymentEvent(PaymentStrategy.PaymentRequest request,
                                     PaymentStrategy.PaymentResult result) {
        PaymentEvent event = new PaymentEvent(
                request.getPaymentId(),
                result.getStatus().name(),
                result
        );
        messagePublisher.publishAsync("payment-events", request.getPaymentId(), event);
    }

    private void publishBatchCompletionEvent(BatchPaymentRequest request,
                                             BatchPaymentResult result) {
        BatchCompletionEvent event = new BatchCompletionEvent(
                request.getBatchId(),
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailureCount()
        );
        messagePublisher.publishAsync("batch-events", request.getBatchId(), event);
    }

    // Inner classes for DTOs
    public static class BatchPaymentRequest {
        private final String batchId;
        private final java.util.List<PaymentStrategy.PaymentRequest> payments;

        public BatchPaymentRequest(String batchId, java.util.List<PaymentStrategy.PaymentRequest> payments) {
            this.batchId = batchId;
            this.payments = payments;
        }

        public String getBatchId() { return batchId; }
        public java.util.List<PaymentStrategy.PaymentRequest> getPayments() { return payments; }
    }

    public static class BatchPaymentResult {
        private final String batchId;
        private final java.util.List<PaymentStrategy.PaymentResult> results = new java.util.ArrayList<>();

        public BatchPaymentResult(String batchId) {
            this.batchId = batchId;
        }

        public void addResult(PaymentStrategy.PaymentResult result) {
            results.add(result);
        }

        public String getBatchId() { return batchId; }
        public int getTotalCount() { return results.size(); }
        public long getSuccessCount() {
            return results.stream().filter(r -> r.getStatus() == PaymentStrategy.PaymentStatus.SUCCESS).count();
        }
        public long getFailureCount() {
            return results.stream().filter(r -> r.getStatus() != PaymentStrategy.PaymentStatus.SUCCESS).count();
        }
    }

    public static class PaymentEvent {
        private final String paymentId;
        private final String eventType;
        private final Object eventData;
        private final long timestamp;

        public PaymentEvent(String paymentId, String eventType, Object eventData) {
            this.paymentId = paymentId;
            this.eventType = eventType;
            this.eventData = eventData;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getEventType() { return eventType; }
        public Object getEventData() { return eventData; }
        public long getTimestamp() { return timestamp; }
    }

    public static class BatchCompletionEvent {
        private final String batchId;
        private final int totalCount;
        private final long successCount;
        private final long failureCount;
        private final long timestamp;

        public BatchCompletionEvent(String batchId, int totalCount, long successCount, long failureCount) {
            this.batchId = batchId;
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.timestamp = System.currentTimeMillis();
        }

        // Getters
        public String getBatchId() { return batchId; }
        public int getTotalCount() { return totalCount; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public long getTimestamp() { return timestamp; }
    }
}
