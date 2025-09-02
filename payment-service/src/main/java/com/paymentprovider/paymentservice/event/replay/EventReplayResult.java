package com.paymentprovider.paymentservice.event.replay;

import java.util.Map;

/**
 * Result of system-wide event replay operation.
 * Contains reconstructed payment states and system statistics.
 */
public class EventReplayResult {

    private final Map<String, PaymentStateSnapshot> paymentStates;
    private final long totalPayments;
    private final long successfulPayments;
    private final long failedPayments;

    public EventReplayResult(Map<String, PaymentStateSnapshot> paymentStates,
                             long totalPayments,
                             long successfulPayments,
                             long failedPayments) {
        this.paymentStates = paymentStates;
        this.totalPayments = totalPayments;
        this.successfulPayments = successfulPayments;
        this.failedPayments = failedPayments;
    }

    public Map<String, PaymentStateSnapshot> getPaymentStates() {
        return paymentStates;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public long getSuccessfulPayments() {
        return successfulPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public long getPendingPayments() {
        return totalPayments - successfulPayments - failedPayments;
    }

    public double getSuccessRate() {
        return totalPayments > 0 ? (double) successfulPayments / totalPayments : 0.0;
    }

    @Override
    public String toString() {
        return "EventReplayResult{" +
                "totalPayments=" + totalPayments +
                ", successfulPayments=" + successfulPayments +
                ", failedPayments=" + failedPayments +
                ", pendingPayments=" + getPendingPayments() +
                ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                '}';
    }
}
