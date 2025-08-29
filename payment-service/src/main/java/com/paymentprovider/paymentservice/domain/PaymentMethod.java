package com.paymentprovider.paymentservice.domain;

/**
 * Enumeration of supported payment methods
 */
public enum PaymentMethod {
    CREDIT_CARD("credit_card", "Credit Card"),
    DEBIT_CARD("debit_card", "Debit Card"),
    PAYPAL("paypal", "PayPal"),
    APPLE_PAY("apple_pay", "Apple Pay"),
    GOOGLE_PAY("google_pay", "Google Pay"),
    BANK_TRANSFER("bank_transfer", "Bank Transfer"),
    SEPA("sepa", "SEPA Direct Debit"),
    ACH("ach", "ACH Transfer"),
    IDEAL("ideal", "iDEAL"),
    SOFORT("sofort", "Sofort"),
    GIROPAY("giropay", "Giropay"),
    ALIPAY("alipay", "Alipay"),
    WECHAT_PAY("wechat_pay", "WeChat Pay"),
    DIGITAL_WALLET("digital_wallet", "Digital Wallet"),
    STORED_PAYMENT("stored_payment", "Stored Payment Method");

    private final String code;
    private final String displayName;

    PaymentMethod(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PaymentMethod fromCode(String code) {
        for (PaymentMethod method : values()) {
            if (method.code.equals(code)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown payment method code: " + code);
    }
}
