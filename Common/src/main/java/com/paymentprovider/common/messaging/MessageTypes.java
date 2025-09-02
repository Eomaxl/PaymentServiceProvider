package com.paymentprovider.common.messaging;

/**
 * Message types for async communication between services.
 * Follows Open/Closed Principle - new message types can be added without modifying existing code.
 */
public final class MessageTypes {

    // Payment Events
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_CANCELLED = "payment.cancelled";

    // Fraud Events
    public static final String FRAUD_CHECK_REQUESTED = "fraud.check.requested";
    public static final String FRAUD_DETECTED = "fraud.detected";
    public static final String FRAUD_CLEARED = "fraud.cleared";

    // Routing Events
    public static final String ROUTING_REQUESTED = "routing.requested";
    public static final String ROUTING_COMPLETED = "routing.completed";
    public static final String PROCESSOR_FAILED = "processor.failed";
    public static final String FAILOVER_TRIGGERED = "failover.triggered";

    // Reconciliation Events
    public static final String RECONCILIATION_STARTED = "reconciliation.started";
    public static final String RECONCILIATION_COMPLETED = "reconciliation.completed";
    public static final String DISCREPANCY_FOUND = "discrepancy.found";

    // System Events
    public static final String SYSTEM_HEALTH_CHECK = "system.health.check";
    public static final String CACHE_INVALIDATED = "cache.invalidated";
    public static final String CONFIGURATION_UPDATED = "configuration.updated";

    private MessageTypes() {
        // Utility class
    }
}