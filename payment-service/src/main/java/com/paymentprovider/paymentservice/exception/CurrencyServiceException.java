package com.paymentprovider.paymentservice.exception;

/**
 * Exception thrown when currency service operations fail
 */
public class CurrencyServiceException extends RuntimeException {

    public CurrencyServiceException(String message) {
        super(message);
    }

    public CurrencyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
