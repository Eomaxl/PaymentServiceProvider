package com.paymentprovider.common.patterns.factory;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory pattern for creating payment processors.
 * Follows Factory Pattern, Single Responsibility Principle, and Open/Closed Principle.
 */
@Component
public class PaymentProcessorFactory {

    private final Map<String, PaymentProcessorCreator> creators = new ConcurrentHashMap<>();

    /**
     * Register a processor creator
     */
    public void registerCreator(String processorType, PaymentProcessorCreator creator) {
        creators.put(processorType.toLowerCase(), creator);
    }

    /**
     * Create a payment processor instance
     */
    public PaymentProcessor createProcessor(String processorType, Map<String, Object> config) {
        PaymentProcessorCreator creator = creators.get(processorType.toLowerCase());
        if (creator == null) {
            throw new UnsupportedOperationException("Unsupported processor type: " + processorType);
        }
        return creator.create(config);
    }

    /**
     * Check if processor type is supported
     */
    public boolean isSupported(String processorType) {
        return creators.containsKey(processorType.toLowerCase());
    }

    /**
     * Get all supported processor types
     */
    public java.util.Set<String> getSupportedTypes() {
        return creators.keySet();
    }
}

/**
 * Interface for payment processor creators
 */
interface PaymentProcessorCreator {
    PaymentProcessor create(Map<String, Object> config);
}

/**
 * Base interface for payment processors
 */
interface PaymentProcessor {
    ProcessorResult processPayment(PaymentRequest request);
    String getProcessorType();
    boolean isHealthy();
}

/**
 * Stripe processor creator
 */
@Component
class StripeProcessorCreator implements PaymentProcessorCreator {

    @Override
    public PaymentProcessor create(Map<String, Object> config) {
        return new StripePaymentProcessor(config);
    }
}

/**
 * Adyen processor creator
 */
@Component
class AdyenProcessorCreator implements PaymentProcessorCreator {

    @Override
    public PaymentProcessor create(Map<String, Object> config) {
        return new AdyenPaymentProcessor(config);
    }
}

/**
 * Stripe payment processor implementation
 */
class StripePaymentProcessor implements PaymentProcessor {

    private final Map<String, Object> config;

    public StripePaymentProcessor(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public ProcessorResult processPayment(PaymentRequest request) {
        // Stripe-specific payment processing logic
        return new ProcessorResult("SUCCESS", "stripe_txn_" + System.currentTimeMillis());
    }

    @Override
    public String getProcessorType() {
        return "stripe";
    }

    @Override
    public boolean isHealthy() {
        return true; // Health check logic
    }
}

/**
 * Adyen payment processor implementation
 */
class AdyenPaymentProcessor implements PaymentProcessor {

    private final Map<String, Object> config;

    public AdyenPaymentProcessor(Map<String, Object> config) {
        this.config = config;
    }

    @Override
    public ProcessorResult processPayment(PaymentRequest request) {
        // Adyen-specific payment processing logic
        return new ProcessorResult("SUCCESS", "adyen_txn_" + System.currentTimeMillis());
    }

    @Override
    public String getProcessorType() {
        return "adyen";
    }

    @Override
    public boolean isHealthy() {
        return true; // Health check logic
    }
}

/**
 * Payment request data
 */
class PaymentRequest {
    private final String paymentId;
    private final java.math.BigDecimal amount;
    private final String currency;

    public PaymentRequest(String paymentId, java.math.BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
    }

    // Getters
    public String getPaymentId() { return paymentId; }
    public java.math.BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
}

/**
 * Processor result data
 */
class ProcessorResult {
    private final String status;
    private final String transactionId;

    public ProcessorResult(String status, String transactionId) {
        this.status = status;
        this.transactionId = transactionId;
    }

    // Getters
    public String getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
}