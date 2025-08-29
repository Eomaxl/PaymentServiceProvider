package com.paymentprovider.paymentservice.domain;

/**
 * Enumeration of payment processing statuses
 */
public enum PaymentStatus {
    PENDING("pending", "Payment is being processed"),
    AUTHORIZED("authorized", "Payment has been authorized"),
    CAPTURED("captured", "Payment has been captured"),
    SETTLED("settled", "Payment has been settled"),
    FAILED("failed", "Payment processing failed"),
    CANCELLED("cancelled", "Payment was cancelled"),
    REFUNDED("refunded", "Payment has been refunded"),
    PARTIALLY_REFUNDED("partially_refunded", "Payment has been partially refunded"),
    DISPUTED("disputed", "Payment is under dispute"),
    EXPIRED("expired", "Payment authorization has expired"),
    DECLINED("declined", "Payment was declined by processor");

    private final String code;
    private final String description;

    PaymentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown payment status code: " + code);
    }

    public boolean isTerminal() {
        return this == CAPTURED || this == SETTLED || this == FAILED ||
                this == CANCELLED || this == REFUNDED || this == DECLINED || this == EXPIRED;
    }

    public boolean isSuccessful() {
        return this == CAPTURED || this == SETTLED;
    }
}
