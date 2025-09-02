package com.paymentprovider.paymentservice.event;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a payment event in the event store.
 * Used for event sourcing to maintain complete audit trail of payment state changes.
 */
@Entity
@Table(name = "payment_events", indexes = {
        @Index(name = "idx_payment_id", columnList = "paymentId"),
        @Index(name = "idx_event_type", columnList = "eventType"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String eventId;

    @Column(nullable = false)
    private String paymentId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String eventData;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String version;

    @Column
    private String correlationId;

    @Column
    private String userId;

    // Default constructor for JPA
    protected PaymentEvent() {}

    public PaymentEvent(String paymentId, PaymentEventType eventType, String eventData, String version) {
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.version = version;
    }

    public PaymentEvent(String paymentId, PaymentEventType eventType, String eventData,
                        String version, String correlationId, String userId) {
        this(paymentId, eventType, eventData, version);
        this.correlationId = correlationId;
        this.userId = userId;
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public PaymentEventType getEventType() {
        return eventType;
    }

    public void setEventType(PaymentEventType eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentEvent that = (PaymentEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "eventId='" + eventId + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", eventType=" + eventType +
                ", timestamp=" + timestamp +
                ", version='" + version + '\'' +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
