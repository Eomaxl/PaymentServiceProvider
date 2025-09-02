package com.paymentprovider.common.patterns.observer;

/**
 * Payment event data
 */
class PaymentEvent {
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
