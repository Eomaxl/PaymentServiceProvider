package com.paymentprovider.paymentservice.security;

/**
 * Enumeration of user roles for role-based access control.
 */
public enum UserRole {
    /**
     * System administrator with full access to all resources.
     */
    ADMIN,

    /**
     * Merchant user with access to their own payment data and operations.
     */
    MERCHANT,

    /**
     * API client with programmatic access to payment processing.
     */
    API_CLIENT,

    /**
     * Support user with read-only access for customer service operations.
     */
    SUPPORT,

    /**
     * Auditor with read-only access to financial and compliance data.
     */
    AUDITOR
}
