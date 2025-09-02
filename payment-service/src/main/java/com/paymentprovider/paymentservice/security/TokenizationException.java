package com.paymentprovider.paymentservice.security;

/**
 * Exception thrown when tokenization or detokenization operations fail.
 */
public class TokenizationException extends RuntimeException {

    public TokenizationException(String message) {
        super(message);
    }

    public TokenizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
