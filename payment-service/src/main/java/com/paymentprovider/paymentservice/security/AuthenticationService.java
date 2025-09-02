package com.paymentprovider.paymentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Main authentication service that handles JWT and API key authentication.
 */
@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final JwtService jwtService;
    private final ApiKeyService apiKeyService;

    @Autowired
    public AuthenticationService(JwtService jwtService, ApiKeyService apiKeyService) {
        this.jwtService = jwtService;
        this.apiKeyService = apiKeyService;
    }

    /**
     * Authenticates a user with username/password and returns JWT tokens.
     * In production, this would validate against a user database.
     */
    public AuthenticationResponse authenticateUser(String username, String password, String merchantId) {
        // TODO: In production, validate credentials against user database
        // For now, using mock validation
        if (!isValidCredentials(username, password)) {
            logger.warn("Authentication failed for username: {}", username);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Create authenticated user
        AuthenticatedUser user = new AuthenticatedUser(
                generateUserId(username),
                username,
                merchantId,
                determineUserRoles(username), // Mock role determination
                AuthenticationType.JWT
        );

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        logger.info("User authenticated successfully: {} for merchant: {}", username, merchantId);

        return new AuthenticationResponse(accessToken, refreshToken, user);
    }

    /**
     * Validates a JWT token and returns the authenticated user.
     */
    public AuthenticatedUser validateJwtToken(String token) throws JwtException {
        Claims claims = jwtService.validateToken(token);
        return jwtService.extractUser(claims);
    }

    /**
     * Validates an API key and returns the authenticated user.
     */
    public AuthenticatedUser validateApiKey(String apiKey) throws SecurityException {
        return apiKeyService.validateApiKey(apiKey);
    }

    /**
     * Refreshes an access token using a refresh token.
     */
    public String refreshAccessToken(String refreshToken) throws JwtException {
        Claims claims = jwtService.validateToken(refreshToken);

        // Verify it's a refresh token
        String tokenType = claims.get("tokenType", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new JwtException("Invalid token type for refresh");
        }

        // Create a new access token (would typically fetch fresh user data from database)
        String userId = claims.getSubject();
        AuthenticatedUser user = createUserFromRefreshToken(userId);

        return jwtService.generateAccessToken(user);
    }

    /**
     * Generates an API key for the current authenticated user.
     */
    public String generateApiKey() {
        AuthenticatedUser currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new SecurityException("No authenticated user found");
        }

        return apiKeyService.generateApiKey(
                currentUser.getUserId(),
                currentUser.getMerchantId(),
                currentUser.getRoles()
        );
    }

    /**
     * Gets the currently authenticated user from the security context.
     */
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser) {
            return (AuthenticatedUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Checks if the current user has the specified role.
     */
    public boolean hasRole(UserRole role) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.hasRole(role);
    }

    /**
     * Checks if the current user has any of the specified roles.
     */
    public boolean hasAnyRole(UserRole... roles) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.hasAnyRole(roles);
    }

    // Mock methods - replace with real implementations in production

    private boolean isValidCredentials(String username, String password) {
        // Mock validation - in production, validate against database with hashed passwords
        return username != null && password != null && password.length() >= 8;
    }

    private String generateUserId(String username) {
        // Mock user ID generation - in production, this would come from the database
        return "user_" + username.hashCode();
    }

    private Set<UserRole> determineUserRoles(String username) {
        // Mock role determination - in production, fetch from database
        if (username.startsWith("admin")) {
            return Set.of(UserRole.ADMIN);
        } else if (username.startsWith("merchant")) {
            return Set.of(UserRole.MERCHANT);
        } else if (username.startsWith("support")) {
            return Set.of(UserRole.SUPPORT);
        } else {
            return Set.of(UserRole.API_CLIENT);
        }
    }

    private AuthenticatedUser createUserFromRefreshToken(String userId) {
        // Mock user creation - in production, fetch from database
        return new AuthenticatedUser(
                userId,
                "user_" + userId,
                "merchant_" + userId,
                Set.of(UserRole.MERCHANT),
                AuthenticationType.JWT
        );
    }
}
