package com.paymentprovider.paymentservice.exception;

/**
 * Exception thrown when currency conversion fails
 */
public class CurrencyConversionException extends RuntimeException {

    public CurrencyConversionException(String message) {
        super(message);
    }

    public CurrencyConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
