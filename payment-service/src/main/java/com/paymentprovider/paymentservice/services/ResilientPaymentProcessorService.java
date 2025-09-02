package com.paymentprovider.paymentservice.services;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service demonstrating resilience patterns for payment processing.
 * Implements circuit breaker, retry, bulkhead, and time limiter patterns.
 */
@Service
public class ResilientPaymentProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(ResilientPaymentProcessorService.class);

    /**
     * Processes payment with full resilience patterns applied.
     * Demonstrates circuit breaker, retry, bulkhead, and time limiter usage.
     */
    @CircuitBreaker(name = "payment-processor", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "payment-processor")
    @Bulkhead(name = "payment-processor")
    @TimeLimiter(name = "payment-processor")
    public CompletableFuture<PaymentProcessorResponse> processPayment(PaymentProcessorRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Processing payment for amount: {} with processor", request.getAmount());

            // Simulate external payment processor call
            simulateExternalProcessorCall(request);

            return PaymentProcessorResponse.builder()
                    .transactionId(generateTransactionId())
                    .status("SUCCESS")
                    .processorReference("proc_" + System.currentTimeMillis())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();
        });
    }

    /**
     * Fallback method for payment processing failures.
     * Routes to alternative processor or queues for retry.
     */
    public CompletableFuture<PaymentProcessorResponse> fallbackProcessPayment(
            PaymentProcessorRequest request, Exception ex) {
        logger.warn("Payment processing failed, using fallback. Error: {}", ex.getMessage());

        return CompletableFuture.supplyAsync(() -> {
            // In real implementation, this would route to backup processor
            // or queue the payment for later processing
            return PaymentProcessorResponse.builder()
                    .transactionId(generateTransactionId())
                    .status("QUEUED_FOR_RETRY")
                    .processorReference("fallback_" + System.currentTimeMillis())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();
        });
    }

    /**
     * Fraud detection with resilience patterns.
     * Uses separate circuit breaker configuration for fraud service.
     */
    @CircuitBreaker(name = "fraud-service", fallbackMethod = "fallbackFraudCheck")
    @Retry(name = "fraud-service")
    @Bulkhead(name = "fraud-service")
    public CompletableFuture<FraudCheckResponse> checkFraud(FraudCheckRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Performing fraud check for payment: {}", request.getPaymentId());

            // Simulate fraud detection service call
            simulateFraudServiceCall(request);

            return FraudCheckResponse.builder()
                    .paymentId(request.getPaymentId())
                    .riskScore(ThreadLocalRandom.current().nextInt(0, 100))
                    .decision("APPROVE")
                    .build();
        });
    }

    /**
     * Fallback method for fraud detection failures.
     * Applies conservative fraud rules when service is unavailable.
     */
    public CompletableFuture<FraudCheckResponse> fallbackFraudCheck(
            FraudCheckRequest request, Exception ex) {
        logger.warn("Fraud service unavailable, using fallback rules. Error: {}", ex.getMessage());

        return CompletableFuture.supplyAsync(() -> {
            // Conservative fallback - approve small amounts, review large ones
            String decision = request.getAmount().compareTo(new BigDecimal("100.00")) <= 0
                    ? "APPROVE" : "REVIEW";

            return FraudCheckResponse.builder()
                    .paymentId(request.getPaymentId())
                    .riskScore(50) // Neutral score when service unavailable
                    .decision(decision)
                    .build();
        });
    }

    /**
     * Database operations with resilience patterns.
     * Demonstrates database-specific circuit breaker configuration.
     */
    @CircuitBreaker(name = "database", fallbackMethod = "fallbackSavePayment")
    @Retry(name = "database")
    @Bulkhead(name = "database")
    public CompletableFuture<String> savePayment(PaymentEntity payment) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Saving payment to database: {}", payment.getPaymentId());

            // Simulate database operation
            simulateDatabaseOperation();

            return payment.getPaymentId();
        });
    }

    /**
     * Fallback method for database save failures.
     * Queues payment for later persistence.
     */
    public CompletableFuture<String> fallbackSavePayment(PaymentEntity payment, Exception ex) {
        logger.error("Database save failed, queuing payment for retry. Error: {}", ex.getMessage());

        return CompletableFuture.supplyAsync(() -> {
            // In real implementation, this would queue to message broker
            // for later processing when database is available
            logger.info("Payment queued for retry: {}", payment.getPaymentId());
            return payment.getPaymentId();
        });
    }

    private void simulateExternalProcessorCall(PaymentProcessorRequest request) {
        // Simulate network latency
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate occasional failures
        if (ThreadLocalRandom.current().nextInt(100) < 10) {
            throw new RuntimeException("Payment processor timeout");
        }
    }

    private void simulateFraudServiceCall(FraudCheckRequest request) {
        // Simulate processing time
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(50, 500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate occasional failures
        if (ThreadLocalRandom.current().nextInt(100) < 5) {
            throw new RuntimeException("Fraud service unavailable");
        }
    }

    private void simulateDatabaseOperation() {
        // Simulate database latency
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate occasional database issues
        if (ThreadLocalRandom.current().nextInt(100) < 3) {
            throw new RuntimeException("Database connection timeout");
        }
    }

    private String generateTransactionId() {
        return "txn_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    // Inner classes for request/response objects
    public static class PaymentProcessorRequest {
        private String paymentId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;

        // Constructors, getters, setters
        public PaymentProcessorRequest() {}

        public PaymentProcessorRequest(String paymentId, BigDecimal amount, String currency, String paymentMethod) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class PaymentProcessorResponse {
        private String transactionId;
        private String status;
        private String processorReference;
        private BigDecimal amount;
        private String currency;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PaymentProcessorResponse response = new PaymentProcessorResponse();

            public Builder transactionId(String transactionId) {
                response.transactionId = transactionId;
                return this;
            }

            public Builder status(String status) {
                response.status = status;
                return this;
            }

            public Builder processorReference(String processorReference) {
                response.processorReference = processorReference;
                return this;
            }

            public Builder amount(BigDecimal amount) {
                response.amount = amount;
                return this;
            }

            public Builder currency(String currency) {
                response.currency = currency;
                return this;
            }

            public PaymentProcessorResponse build() {
                return response;
            }
        }

        // Getters
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getProcessorReference() { return processorReference; }
        public BigDecimal getAmount() { return amount; }
        public String getCurrency() { return currency; }
    }

    public static class FraudCheckRequest {
        private String paymentId;
        private BigDecimal amount;
        private String merchantId;

        public FraudCheckRequest() {}

        public FraudCheckRequest(String paymentId, BigDecimal amount, String merchantId) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.merchantId = merchantId;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    }

    public static class FraudCheckResponse {
        private String paymentId;
        private int riskScore;
        private String decision;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private FraudCheckResponse response = new FraudCheckResponse();

            public Builder paymentId(String paymentId) {
                response.paymentId = paymentId;
                return this;
            }

            public Builder riskScore(int riskScore) {
                response.riskScore = riskScore;
                return this;
            }

            public Builder decision(String decision) {
                response.decision = decision;
                return this;
            }

            public FraudCheckResponse build() {
                return response;
            }
        }

        public String getPaymentId() { return paymentId; }
        public int getRiskScore() { return riskScore; }
        public String getDecision() { return decision; }
    }

    public static class PaymentEntity {
        private String paymentId;
        private BigDecimal amount;
        private String status;

        public PaymentEntity() {}

        public PaymentEntity(String paymentId, BigDecimal amount, String status) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.status = status;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
