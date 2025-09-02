package com.paymentprovider.paymentservice.security;

import java.util.Set;

/**
 * Represents an authenticated user in the system.
 */
public class AuthenticatedUser {
    private final String userId;
    private final String username;
    private final String merchantId;
    private final Set<UserRole> roles;
    private final AuthenticationType authenticationType;

    public AuthenticatedUser(String userId, String username, String merchantId,
                             Set<UserRole> roles, AuthenticationType authenticationType) {
        this.userId = userId;
        this.username = username;
        this.merchantId = merchantId;
        this.roles = roles;
        this.authenticationType = authenticationType;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(UserRole... roles) {
        for (UserRole role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
