package com.paymentprovider.paymentservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for logging security-related events for audit purposes.
 */
@Service
public class SecurityAuditService {
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final ObjectMapper objectMapper;

    @Autowired
    public SecurityAuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Logs a successful authentication event.
     */
    public void logAuthenticationSuccess(String userId, String username, String clientIp,
                                         AuthenticationType authType, HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("AUTHENTICATION_SUCCESS", clientIp, request);
        auditEvent.put("userId", userId);
        auditEvent.put("username", username);
        auditEvent.put("authenticationType", authType.name());

        logAuditEvent(auditEvent);
    }

    /**
     * Logs a failed authentication event.
     */
    public void logAuthenticationFailure(String username, String clientIp, String reason,
                                         HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("AUTHENTICATION_FAILURE", clientIp, request);
        auditEvent.put("username", username);
        auditEvent.put("reason", reason);

        logAuditEvent(auditEvent);
    }

    /**
     * Logs an authorization failure event.
     */
    public void logAuthorizationFailure(String userId, String resource, String action,
                                        String clientIp, HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("AUTHORIZATION_FAILURE", clientIp, request);
        auditEvent.put("userId", userId);
        auditEvent.put("resource", resource);
        auditEvent.put("action", action);

        logAuditEvent(auditEvent);
    }

    /**
     * Logs a rate limiting event.
     */
    public void logRateLimitExceeded(String clientIp, int requestCount, int limit,
                                     HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("RATE_LIMIT_EXCEEDED", clientIp, request);
        auditEvent.put("requestCount", requestCount);
        auditEvent.put("limit", limit);

        logAuditEvent(auditEvent);
    }

    /**
     * Logs a suspicious activity event.
     */
    public void logSuspiciousActivity(String clientIp, String activityType, String details,
                                      HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("SUSPICIOUS_ACTIVITY", clientIp, request);
        auditEvent.put("activityType", activityType);
        auditEvent.put("details", details);
        auditEvent.put("severity", "HIGH");

        logAuditEvent(auditEvent);
    }

    /**
     * Logs an IP blocking event.
     */
    public void logIpBlocked(String clientIp, String reason, int durationMinutes) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventType", "IP_BLOCKED");
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("clientIp", clientIp);
        auditEvent.put("reason", reason);
        auditEvent.put("blockDurationMinutes", durationMinutes);
        auditEvent.put("severity", "CRITICAL");

        logAuditEvent(auditEvent);
    }

    /**
     * Logs an API key generation event.
     */
    public void logApiKeyGenerated(String userId, String merchantId, String clientIp,
                                   HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("API_KEY_GENERATED", clientIp, request);
        auditEvent.put("userId", userId);
        auditEvent.put("merchantId", merchantId);

        logAuditEvent(auditEvent);
    }

    /**
     * Logs an API key revocation event.
     */
    public void logApiKeyRevoked(String apiKeyPrefix, String userId, String reason) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventType", "API_KEY_REVOKED");
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("apiKeyPrefix", apiKeyPrefix);
        auditEvent.put("userId", userId);
        auditEvent.put("reason", reason);

        logAuditEvent(auditEvent);
    }

    /**
     * Logs a payment processing security event.
     */
    public void logPaymentSecurityEvent(String paymentId, String merchantId, String eventType,
                                        String details, String clientIp, HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("PAYMENT_SECURITY_EVENT", clientIp, request);
        auditEvent.put("paymentId", paymentId);
        auditEvent.put("merchantId", merchantId);
        auditEvent.put("securityEventType", eventType);
        auditEvent.put("details", details);

        logAuditEvent(auditEvent);
    }

    /**
     * Logs a data access event for sensitive operations.
     */
    public void logDataAccess(String userId, String dataType, String operation, String resourceId,
                              String clientIp, HttpServletRequest request) {
        Map<String, Object> auditEvent = createBaseAuditEvent("DATA_ACCESS", clientIp, request);
        auditEvent.put("userId", userId);
        auditEvent.put("dataType", dataType);
        auditEvent.put("operation", operation);
        auditEvent.put("resourceId", resourceId);

        logAuditEvent(auditEvent);
    }

    private Map<String, Object> createBaseAuditEvent(String eventType, String clientIp,
                                                     HttpServletRequest request) {
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventType", eventType);
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("clientIp", clientIp);

        if (request != null) {
            auditEvent.put("method", request.getMethod());
            auditEvent.put("uri", request.getRequestURI());
            auditEvent.put("userAgent", request.getHeader("User-Agent"));
            auditEvent.put("sessionId", request.getSession(false) != null ?
                    request.getSession().getId() : null);
        }

        return auditEvent;
    }

    private void logAuditEvent(Map<String, Object> auditEvent) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(auditEvent);
            auditLogger.info(jsonEvent);
        } catch (Exception e) {
            auditLogger.error("Failed to serialize audit event", e);
            // Fallback to simple logging
            auditLogger.info("AUDIT_EVENT: {}", auditEvent.toString());
        }
    }
}
