package com.paymentprovider.paymentservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for API key generation and validation.
 * In production, this should be backed by a database or external key management service.
 */
@Service
public class ApiKeyService {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);

    private final SecureRandom secureRandom = new SecureRandom();

    // In production, this should be stored in a database
    private final Map<String, ApiKeyInfo> apiKeys = new ConcurrentHashMap<>();

    /**
     * Generates a new API key for a user.
     */
    public String generateApiKey(String userId, String merchantId, Set<UserRole> roles) {
        byte[] keyBytes = new byte[32]; // 256-bit key
        secureRandom.nextBytes(keyBytes);
        String apiKey = "pk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        ApiKeyInfo keyInfo = new ApiKeyInfo(userId, merchantId, roles, System.currentTimeMillis());
        apiKeys.put(apiKey, keyInfo);

        logger.info("Generated API key for user: {} merchant: {}", userId, merchantId);
        return apiKey;
    }

    /**
     * Validates an API key and returns the associated user information.
     */
    public AuthenticatedUser validateApiKey(String apiKey) throws SecurityException {
        if (apiKey == null || !apiKey.startsWith("pk_")) {
            throw new SecurityException("Invalid API key format");
        }

        ApiKeyInfo keyInfo = apiKeys.get(apiKey);
        if (keyInfo == null) {
            logger.warn("Invalid API key attempted: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
            throw new SecurityException("Invalid API key");
        }

        // Check if key is expired (optional - implement based on requirements)
        // For now, keys don't expire

        return new AuthenticatedUser(
                keyInfo.userId,
                "api-user-" + keyInfo.userId,
                keyInfo.merchantId,
                keyInfo.roles,
                AuthenticationType.API_KEY
        );
    }

    /**
     * Revokes an API key.
     */
    public boolean revokeApiKey(String apiKey) {
        ApiKeyInfo removed = apiKeys.remove(apiKey);
        if (removed != null) {
            logger.info("Revoked API key for user: {} merchant: {}", removed.userId, removed.merchantId);
            return true;
        }
        return false;
    }

    /**
     * Internal class to store API key information.
     */
    private static class ApiKeyInfo {
        final String userId;
        final String merchantId;
        final Set<UserRole> roles;
        final long createdAt;

        ApiKeyInfo(String userId, String merchantId, Set<UserRole> roles, long createdAt) {
            this.userId = userId;
            this.merchantId = merchantId;
            this.roles = roles;
            this.createdAt = createdAt;
        }
    }
}
