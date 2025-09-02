package com.paymentprovider.paymentservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Stateless payment service designed for horizontal scaling.
 * All operations are stateless and can be distributed across multiple instances.
 */
@Service
public class StatelessPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(StatelessPaymentService.class);

    private final Executor paymentProcessingExecutor;
    private final Executor fraudDetectionExecutor;

    public StatelessPaymentService(
            @Qualifier("paymentProcessingExecutor") Executor paymentProcessingExecutor,
            @Qualifier("fraudDetectionExecutor") Executor fraudDetectionExecutor) {
        this.paymentProcessingExecutor = paymentProcessingExecutor;
        this.fraudDetectionExecutor = fraudDetectionExecutor;
    }

    /**
     * Process payment asynchronously for better scalability.
     * Each request is independent and can be processed on any instance.
     */
    @Async("paymentProcessingExecutor")
    public CompletableFuture<PaymentResult> processPaymentAsync(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing payment asynchronously: {}", request.getPaymentId());

            try {
                // Simulate payment processing
                Thread.sleep(100 + (int)(Math.random() * 200)); // 100-300ms processing time

                // Determine result based on amount (for demo purposes)
                boolean success = request.getAmount().compareTo(new BigDecimal("10000")) <= 0;

                return PaymentResult.builder()
                        .paymentId(request.getPaymentId())
                        .status(success ? "SUCCESS" : "DECLINED")
                        .transactionId("txn_" + System.currentTimeMillis())
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                        .build();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Payment processing interrupted: {}", request.getPaymentId());
                return PaymentResult.builder()
                        .paymentId(request.getPaymentId())
                        .status("ERROR")
                        .errorMessage("Processing interrupted")
                        .build();
            } catch (Exception e) {
                logger.error("Payment processing failed: {}", request.getPaymentId(), e);
                return PaymentResult.builder()
                        .paymentId(request.getPaymentId())
                        .status("ERROR")
                        .errorMessage(e.getMessage())
                        .build();
            }
        }, paymentProcessingExecutor);
    }

    /**
     * Perform fraud check asynchronously.
     * Independent operation that can be scaled separately.
     */
    @Async("fraudDetectionExecutor")
    public CompletableFuture<FraudCheckResult> performFraudCheckAsync(FraudCheckRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Performing fraud check asynchronously: {}", request.getPaymentId());

            try {
                // Simulate fraud detection processing
                Thread.sleep(50 + (int)(Math.random() * 100)); // 50-150ms processing time

                // Simple fraud scoring logic (for demo)
                int riskScore = calculateRiskScore(request);
                String decision = riskScore > 70 ? "DECLINE" : (riskScore > 40 ? "REVIEW" : "APPROVE");

                return FraudCheckResult.builder()
                        .paymentId(request.getPaymentId())
                        .riskScore(riskScore)
                        .decision(decision)
                        .processingTimeMs(System.currentTimeMillis() - request.getTimestamp())
                        .build();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Fraud check interrupted: {}", request.getPaymentId());
                return FraudCheckResult.builder()
                        .paymentId(request.getPaymentId())
                        .riskScore(50)
                        .decision("ERROR")
                        .errorMessage("Processing interrupted")
                        .build();
            } catch (Exception e) {
                logger.error("Fraud check failed: {}", request.getPaymentId(), e);
                return FraudCheckResult.builder()
                        .paymentId(request.getPaymentId())
                        .riskScore(50)
                        .decision("ERROR")
                        .errorMessage(e.getMessage())
                        .build();
            }
        }, fraudDetectionExecutor);
    }

    /**
     * Process multiple payments in batch for better throughput.
     * Optimized for high-volume scenarios.
     */
    public CompletableFuture<BatchProcessingResult> processBatch(BatchPaymentRequest batchRequest) {
        logger.info("Processing payment batch: {} payments", batchRequest.getPayments().size());

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            // Process payments concurrently
            CompletableFuture<PaymentResult>[] futures = batchRequest.getPayments().stream()
                    .map(this::processPaymentAsync)
                    .toArray(CompletableFuture[]::new);

            // Wait for all to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);

            try {
                allOf.get(); // Wait for completion

                // Collect results
                PaymentResult[] results = new PaymentResult[futures.length];
                for (int i = 0; i < futures.length; i++) {
                    results[i] = futures[i].get();
                }

                long processingTime = System.currentTimeMillis() - startTime;

                return BatchProcessingResult.builder()
                        .batchId(batchRequest.getBatchId())
                        .totalPayments(batchRequest.getPayments().size())
                        .successfulPayments((int) java.util.Arrays.stream(results)
                                .filter(r -> "SUCCESS".equals(r.getStatus()))
                                .count())
                        .failedPayments((int) java.util.Arrays.stream(results)
                                .filter(r -> !"SUCCESS".equals(r.getStatus()))
                                .count())
                        .processingTimeMs(processingTime)
                        .results(java.util.Arrays.asList(results))
                        .build();

            } catch (Exception e) {
                logger.error("Batch processing failed: {}", batchRequest.getBatchId(), e);
                return BatchProcessingResult.builder()
                        .batchId(batchRequest.getBatchId())
                        .totalPayments(batchRequest.getPayments().size())
                        .successfulPayments(0)
                        .failedPayments(batchRequest.getPayments().size())
                        .errorMessage(e.getMessage())
                        .build();
            }
        }, paymentProcessingExecutor);
    }

    private int calculateRiskScore(FraudCheckRequest request) {
        // Simple risk scoring logic for demo
        int score = 0;

        // Amount-based scoring
        if (request.getAmount().compareTo(new BigDecimal("1000")) > 0) {
            score += 20;
        }
        if (request.getAmount().compareTo(new BigDecimal("5000")) > 0) {
            score += 30;
        }

        // Add some randomness for demo
        score += (int)(Math.random() * 30);

        return Math.min(score, 100);
    }

    // Inner classes for request/response objects
    public static class PaymentRequest {
        private String paymentId;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private long timestamp;

        public PaymentRequest() {
            this.timestamp = System.currentTimeMillis();
        }

        public PaymentRequest(String paymentId, BigDecimal amount, String currency, String merchantId) {
            this();
            this.paymentId = paymentId;
            this.amount = amount;
            this.currency = currency;
            this.merchantId = merchantId;
        }

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class PaymentResult {
        private String paymentId;
        private String status;
        private String transactionId;
        private BigDecimal amount;
        private String currency;
        private long processingTimeMs;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PaymentResult result = new PaymentResult();

            public Builder paymentId(String paymentId) {
                result.paymentId = paymentId;
                return this;
            }

            public Builder status(String status) {
                result.status = status;
                return this;
            }

            public Builder transactionId(String transactionId) {
                result.transactionId = transactionId;
                return this;
            }

            public Builder amount(BigDecimal amount) {
                result.amount = amount;
                return this;
            }

            public Builder currency(String currency) {
                result.currency = currency;
                return this;
            }

            public Builder processingTimeMs(long processingTimeMs) {
                result.processingTimeMs = processingTimeMs;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }

            public PaymentResult build() {
                return result;
            }
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getStatus() { return status; }
        public String getTransactionId() { return transactionId; }
        public BigDecimal getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class FraudCheckRequest {
        private String paymentId;
        private BigDecimal amount;
        private String merchantId;
        private long timestamp;

        public FraudCheckRequest() {
            this.timestamp = System.currentTimeMillis();
        }

        public FraudCheckRequest(String paymentId, BigDecimal amount, String merchantId) {
            this();
            this.paymentId = paymentId;
            this.amount = amount;
            this.merchantId = merchantId;
        }

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    public static class FraudCheckResult {
        private String paymentId;
        private int riskScore;
        private String decision;
        private long processingTimeMs;
        private String errorMessage;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FraudCheckResult result = new FraudCheckResult();

            public Builder paymentId(String paymentId) {
                result.paymentId = paymentId;
                return this;
            }

            public Builder riskScore(int riskScore) {
                result.riskScore = riskScore;
                return this;
            }

            public Builder decision(String decision) {
                result.decision = decision;
                return this;
            }

            public Builder processingTimeMs(long processingTimeMs) {
                result.processingTimeMs = processingTimeMs;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }

            public FraudCheckResult build() {
                return result;
            }
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public int getRiskScore() { return riskScore; }
        public String getDecision() { return decision; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class BatchPaymentRequest {
        private String batchId;
        private java.util.List<PaymentRequest> payments;

        public BatchPaymentRequest() {}

        public BatchPaymentRequest(String batchId, java.util.List<PaymentRequest> payments) {
            this.batchId = batchId;
            this.payments = payments;
        }

        // Getters and setters
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public java.util.List<PaymentRequest> getPayments() { return payments; }
        public void setPayments(java.util.List<PaymentRequest> payments) { this.payments = payments; }
    }

    public static class BatchProcessingResult {
        private String batchId;
        private int totalPayments;
        private int successfulPayments;
        private int failedPayments;
        private long processingTimeMs;
        private String errorMessage;
        private java.util.List<PaymentResult> results;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private BatchProcessingResult result = new BatchProcessingResult();

            public Builder batchId(String batchId) {
                result.batchId = batchId;
                return this;
            }

            public Builder totalPayments(int totalPayments) {
                result.totalPayments = totalPayments;
                return this;
            }

            public Builder successfulPayments(int successfulPayments) {
                result.successfulPayments = successfulPayments;
                return this;
            }

            public Builder failedPayments(int failedPayments) {
                result.failedPayments = failedPayments;
                return this;
            }

            public Builder processingTimeMs(long processingTimeMs) {
                result.processingTimeMs = processingTimeMs;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                result.errorMessage = errorMessage;
                return this;
            }

            public Builder results(java.util.List<PaymentResult> results) {
                result.results = results;
                return this;
            }

            public BatchProcessingResult build() {
                return result;
            }
        }

        // Getters
        public String getBatchId() { return batchId; }
        public int getTotalPayments() { return totalPayments; }
        public int getSuccessfulPayments() { return successfulPayments; }
        public int getFailedPayments() { return failedPayments; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public String getErrorMessage() { return errorMessage; }
        public java.util.List<PaymentResult> getResults() { return results; }
    }
}