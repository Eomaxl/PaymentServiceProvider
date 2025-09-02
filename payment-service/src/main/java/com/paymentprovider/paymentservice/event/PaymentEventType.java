package com.paymentprovider.paymentservice.event;

/**
 * Enumeration of payment event types for event sourcing.
 * Each type represents a significant state change in the payment lifecycle.
 */
public enum PaymentEventType {

    // Payment lifecycle events
    PAYMENT_INITIATED("Payment processing initiated"),
    PAYMENT_VALIDATED("Payment data validated"),
    PAYMENT_AUTHORIZED("Payment authorized by processor"),
    PAYMENT_CAPTURED("Payment captured successfully"),
    PAYMENT_SETTLED("Payment settled"),
    PAYMENT_FAILED("Payment processing failed"),
    PAYMENT_CANCELLED("Payment cancelled"),
    PAYMENT_REFUNDED("Payment refunded"),
    PAYMENT_PARTIALLY_REFUNDED("Payment partially refunded"),

    // Fraud and security events
    FRAUD_CHECK_INITIATED("Fraud check initiated"),
    FRAUD_CHECK_PASSED("Fraud check passed"),
    FRAUD_CHECK_FAILED("Fraud check failed"),
    FRAUD_ALERT_TRIGGERED("Fraud alert triggered"),

    // Routing events
    PROCESSOR_SELECTED("Payment processor selected"),
    PROCESSOR_SWITCHED("Payment processor switched"),
    ROUTING_FAILED("Payment routing failed"),

    // Error and retry events
    RETRY_ATTEMPTED("Payment retry attempted"),
    TIMEOUT_OCCURRED("Payment timeout occurred"),
    ERROR_OCCURRED("Payment error occurred"),

    // Webhook events
    WEBHOOK_SENT("Webhook notification sent"),
    WEBHOOK_FAILED("Webhook delivery failed"),
    WEBHOOK_RETRY("Webhook retry attempted"),

    // Reconciliation events
    RECONCILIATION_MATCHED("Payment reconciliation matched"),
    RECONCILIATION_DISCREPANCY("Reconciliation discrepancy found"),

    // Configuration events
    MERCHANT_CONFIG_UPDATED("Merchant configuration updated"),
    PAYMENT_METHOD_UPDATED("Payment method configuration updated");

    private final String description;

    PaymentEventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this event type represents a terminal state
     */
    public boolean isTerminal() {
        return this == PAYMENT_CAPTURED ||
                this == PAYMENT_SETTLED ||
                this == PAYMENT_FAILED ||
                this == PAYMENT_CANCELLED ||
                this == PAYMENT_REFUNDED;
    }

    /**
     * Check if this event type represents an error state
     */
    public boolean isError() {
        return this == PAYMENT_FAILED ||
                this == FRAUD_CHECK_FAILED ||
                this == ROUTING_FAILED ||
                this == ERROR_OCCURRED ||
                this == TIMEOUT_OCCURRED;
    }

    /**
     * Check if this event type is related to fraud detection
     */
    public boolean isFraudRelated() {
        return this == FRAUD_CHECK_INITIATED ||
                this == FRAUD_CHECK_PASSED ||
                this == FRAUD_CHECK_FAILED ||
                this == FRAUD_ALERT_TRIGGERED;
    }
}
