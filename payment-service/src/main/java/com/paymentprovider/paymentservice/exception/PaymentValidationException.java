package com.paymentprovider.paymentservice.exception;

/**
 * Exception thrown when payment validation fails
 */
public class PaymentValidationException extends RuntimeException {

    private final String field;
    private final Object value;

    public PaymentValidationException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public PaymentValidationException(String field, Object value, String message) {
        super(message);
        this.field = field;
        this.value = value;
    }

    public PaymentValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.value = null;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}
