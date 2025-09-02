package com.paymentprovider.paymentservice.security;

/**
 * Response object for authentication operations.
 */
public class AuthenticationResponse {
    private final String accessToken;
    private final String refreshToken;
    private final AuthenticatedUser user;

    public AuthenticationResponse(String accessToken, String refreshToken, AuthenticatedUser user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public AuthenticatedUser getUser() {
        return user;
    }
}
