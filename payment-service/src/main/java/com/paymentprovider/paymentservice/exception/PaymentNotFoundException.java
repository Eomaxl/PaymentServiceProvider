package com.paymentprovider.paymentservice.exception;

/**
 * Exception thrown when a payment is not found
 */
public class PaymentNotFoundException extends RuntimeException {

    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super("Payment not found: " + paymentId);
        this.paymentId = paymentId;
    }

    public PaymentNotFoundException(String paymentId, String message) {
        super(message);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}
