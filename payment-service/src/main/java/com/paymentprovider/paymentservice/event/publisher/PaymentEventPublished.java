package com.paymentprovider.paymentservice.event.publisher;

import com.paymentprovider.paymentservice.event.PaymentEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event that is published when a payment event is stored.
 * Used for internal event-driven processing and real-time notifications.
 */
public class PaymentEventPublished extends ApplicationEvent {

    private final PaymentEvent paymentEvent;

    public PaymentEventPublished(PaymentEvent paymentEvent) {
        super(paymentEvent);
        this.paymentEvent = paymentEvent;
    }

    public PaymentEvent getPaymentEvent() {
        return paymentEvent;
    }

    public String getPaymentId() {
        return paymentEvent.getPaymentId();
    }

    public String getEventId() {
        return paymentEvent.getEventId();
    }

    @Override
    public String toString() {
        return "PaymentEventPublished{" +
                "paymentId='" + paymentEvent.getPaymentId() + '\'' +
                ", eventType=" + paymentEvent.getEventType() +
                ", eventId='" + paymentEvent.getEventId() + '\'' +
                ", timestamp=" + paymentEvent.getTimestamp() +
                '}';
    }
}
