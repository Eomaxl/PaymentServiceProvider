package com.paymentprovider.paymentservice.event.publisher;

import com.paymentprovider.paymentservice.event.PaymentEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch container for publishing multiple payment events in a single transaction.
 * Useful for complex payment operations that generate multiple events.
 */
public class PaymentEventBatch {

    private final List<EventData> events = new ArrayList<>();

    public PaymentEventBatch addEvent(String paymentId, PaymentEventType eventType, Object data) {
        events.add(new EventData(paymentId, eventType, data, null, null));
        return this;
    }

    public PaymentEventBatch addEvent(String paymentId, PaymentEventType eventType, Object data,
                                      String correlationId, String userId) {
        events.add(new EventData(paymentId, eventType, data, correlationId, userId));
        return this;
    }

    public List<EventData> getEvents() {
        return events;
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public int size() {
        return events.size();
    }

    /**
     * Data container for individual events in the batch
     */
    public static class EventData {
        private final String paymentId;
        private final PaymentEventType eventType;
        private final Object data;
        private final String correlationId;
        private final String userId;

        public EventData(String paymentId, PaymentEventType eventType, Object data,
                         String correlationId, String userId) {
            this.paymentId = paymentId;
            this.eventType = eventType;
            this.data = data;
            this.correlationId = correlationId;
            this.userId = userId;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public PaymentEventType getEventType() {
            return eventType;
        }

        public Object getData() {
            return data;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public String getUserId() {
            return userId;
        }
    }
}
