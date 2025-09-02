package com.paymentprovider.paymentservice.security;

/**
 * Types of authentication methods supported by the system.
 */
public enum AuthenticationType {
    /**
     * JWT token-based authentication.
     */
    JWT,

    /**
     * API key-based authentication.
     */
    API_KEY,

    /**
     * OAuth 2.0 authentication.
     */
    OAUTH2
}
