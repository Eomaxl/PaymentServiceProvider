package com.paymentprovider.paymentservice.domain;

/**
 * Enumeration of merchant account statuses
 */
public enum MerchantStatus {
    PENDING("pending", "Merchant account is pending approval"),
    ACTIVE("active", "Merchant account is active and can process payments"),
    SUSPENDED("suspended", "Merchant account is temporarily suspended"),
    INACTIVE("inactive", "Merchant account is inactive"),
    CLOSED("closed", "Merchant account is permanently closed"),
    UNDER_REVIEW("under_review", "Merchant account is under compliance review");

    private final String code;
    private final String description;

    MerchantStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MerchantStatus fromCode(String code) {
        for (MerchantStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown merchant status code: " + code);
    }

    public boolean canProcessPayments() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
