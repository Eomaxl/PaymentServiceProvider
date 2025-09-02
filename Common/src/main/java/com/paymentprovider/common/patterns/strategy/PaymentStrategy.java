package com.paymentprovider.common.patterns.strategy;

import java.math.BigDecimal;

/**
 * Strategy pattern interface for different payment processing strategies.
 * Follows Interface Segregation Principle - focused on payment processing.
 */
public interface PaymentStrategy {
    PaymentResult processPayment(PaymentRequest request);
    boolean supports(String paymentMethod);
    BigDecimal calculateFee(BigDecimal amount);

    /**
     * Payment request DTO
     */
    class PaymentRequest {
        private final String paymentId;
        private final BigDecimal amount;
        private final String currency;
        private final String paymentMethod;

        public PaymentRequest(String paymentId, BigDecimal amount, String currency, String paymentMethod) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.currency = currency;
            this.paymentMethod = paymentMethod;
        }

        // Getters
        public String getPaymentId() { return paymentId; }
        public BigDecimal getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getPaymentMethod() { return paymentMethod; }
    }

    /**
     * Payment result DTO
     */
    class PaymentResult {
        private final String transactionId;
        private final PaymentStatus status;
        private final String message;

        public PaymentResult(String transactionId, PaymentStatus status, String message) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
        }

        // Getters
        public String getTransactionId() { return transactionId; }
        public PaymentStatus getStatus() { return status; }
        public String getMessage() { return message; }
    }

    enum PaymentStatus {
        SUCCESS, FAILED, PENDING, DECLINED
    }
}