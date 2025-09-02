package com.paymentprovider.paymentservice.exception;

/**
 * Exception thrown when payment processing fails
 */
public class PaymentProcessingException extends RuntimeException {

    private final String paymentId;
    private final String errorCode;

    public PaymentProcessingException(String message) {
        super(message);
        this.paymentId = null;
        this.errorCode = null;
    }

    public PaymentProcessingException(String paymentId, String message) {
        super(message);
        this.paymentId = paymentId;
        this.errorCode = null;
    }

    public PaymentProcessingException(String paymentId, String errorCode, String message) {
        super(message);
        this.paymentId = paymentId;
        this.errorCode = errorCode;
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.paymentId = null;
        this.errorCode = null;
    }

    public PaymentProcessingException(String paymentId, String message, Throwable cause) {
        super(message, cause);
        this.paymentId = paymentId;
        this.errorCode = null;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
