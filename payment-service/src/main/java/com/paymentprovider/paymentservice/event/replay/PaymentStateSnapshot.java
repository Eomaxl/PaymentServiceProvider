package com.paymentprovider.paymentservice.event.replay;

import com.paymentprovider.paymentservice.event.PaymentEventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents a point-in-time snapshot of payment state reconstructed from events.
 * Used for event sourcing and state reconstruction scenarios.
 */
public class PaymentStateSnapshot {

    private final String paymentId;
    private final String currentStatus;
    private final BigDecimal amount;
    private final String currency;
    private final String merchantId;
    private final String paymentMethod;
    private final Instant createdAt;
    private final Instant lastUpdatedAt;
    private final String processorReference;
    private final List<PaymentEventType> eventHistory;
    private final Map<String, Object> metadata;
    private final boolean isTerminal;
    private final String lastEventId;
    private final int eventCount;

    private PaymentStateSnapshot(Builder builder) {
        this.paymentId = builder.paymentId;
        this.currentStatus = builder.currentStatus;
        this.amount = builder.amount;
        this.currency = builder.currency;
        this.merchantId = builder.merchantId;
        this.paymentMethod = builder.paymentMethod;
        this.createdAt = builder.createdAt;
        this.lastUpdatedAt = builder.lastUpdatedAt;
        this.processorReference = builder.processorReference;
        this.eventHistory = builder.eventHistory;
        this.metadata = builder.metadata;
        this.isTerminal = builder.isTerminal;
        this.lastEventId = builder.lastEventId;
        this.eventCount = builder.eventCount;
    }

    public static PaymentStateSnapshot empty(String paymentId) {
        return new Builder(paymentId)
                .withCurrentStatus("UNKNOWN")
                .withEventCount(0)
                .build();
    }

    public static Builder builder(String paymentId) {
        return new Builder(paymentId);
    }

    // Getters
    public String getPaymentId() {
        return paymentId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getProcessorReference() {
        return processorReference;
    }

    public List<PaymentEventType> getEventHistory() {
        return eventHistory;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isTerminal() {
        return isTerminal;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public int getEventCount() {
        return eventCount;
    }

    public boolean hasEvent(PaymentEventType eventType) {
        return eventHistory != null && eventHistory.contains(eventType);
    }

    public boolean isEmpty() {
        return eventCount == 0;
    }

    @Override
    public String toString() {
        return "PaymentStateSnapshot{" +
                "paymentId='" + paymentId + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", isTerminal=" + isTerminal +
                ", eventCount=" + eventCount +
                ", lastUpdatedAt=" + lastUpdatedAt +
                '}';
    }

    /**
     * Builder for PaymentStateSnapshot
     */
    public static class Builder {
        private final String paymentId;
        private String currentStatus;
        private BigDecimal amount;
        private String currency;
        private String merchantId;
        private String paymentMethod;
        private Instant createdAt;
        private Instant lastUpdatedAt;
        private String processorReference;
        private List<PaymentEventType> eventHistory;
        private Map<String, Object> metadata;
        private boolean isTerminal;
        private String lastEventId;
        private int eventCount;

        public Builder(String paymentId) {
            this.paymentId = paymentId;
        }

        public Builder withCurrentStatus(String currentStatus) {
            this.currentStatus = currentStatus;
            return this;
        }

        public Builder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder withCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder withMerchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder withPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder withCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder withLastUpdatedAt(Instant lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
            return this;
        }

        public Builder withProcessorReference(String processorReference) {
            this.processorReference = processorReference;
            return this;
        }

        public Builder withEventHistory(List<PaymentEventType> eventHistory) {
            this.eventHistory = eventHistory;
            return this;
        }

        public Builder withMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder withIsTerminal(boolean isTerminal) {
            this.isTerminal = isTerminal;
            return this;
        }

        public Builder withLastEventId(String lastEventId) {
            this.lastEventId = lastEventId;
            return this;
        }

        public Builder withEventCount(int eventCount) {
            this.eventCount = eventCount;
            return this;
        }

        public PaymentStateSnapshot build() {
            return new PaymentStateSnapshot(this);
        }
    }
}
