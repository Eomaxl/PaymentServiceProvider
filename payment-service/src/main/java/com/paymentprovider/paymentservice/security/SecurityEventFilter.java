package com.paymentprovider.paymentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter for monitoring and logging security events.
 */
@Component
public class SecurityEventFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityEventFilter.class);

    private final SecurityAuditService auditService;

    // Sensitive endpoints that require additional monitoring
    private static final List<String> SENSITIVE_ENDPOINTS = Arrays.asList(
            "/api/v1/payments",
            "/api/v1/auth",
            "/api/v1/admin"
    );

    @Autowired
    public SecurityEventFilter(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        // Check for suspicious patterns
        checkForSuspiciousPatterns(request, clientIp);

        // Log access to sensitive endpoints
        if (isSensitiveEndpoint(requestUri)) {
            logSensitiveEndpointAccess(request, clientIp);
        }

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log slow requests (potential DoS attempts)
            if (duration > 5000) { // 5 seconds
                auditService.logSuspiciousActivity(clientIp, "SLOW_REQUEST",
                        String.format("Request took %d ms: %s %s", duration, method, requestUri), request);
            }

            // Log failed authentication/authorization attempts
            if (response.getStatus() == 401) {
                auditService.logAuthenticationFailure(
                        extractUsernameFromRequest(request), clientIp, "HTTP_401", request);
            } else if (response.getStatus() == 403) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String userId = auth != null && auth.getPrincipal() instanceof AuthenticatedUser
                        ? ((AuthenticatedUser) auth.getPrincipal()).getUserId() : "unknown";
                auditService.logAuthorizationFailure(userId, requestUri, method, clientIp, request);
            }
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void checkForSuspiciousPatterns(HttpServletRequest request, String clientIp) {
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();

        // Check for SQL injection patterns
        if (containsSqlInjectionPatterns(requestUri) || containsSqlInjectionPatterns(request.getQueryString())) {
            auditService.logSuspiciousActivity(clientIp, "SQL_INJECTION_ATTEMPT",
                    "Potential SQL injection in request: " + requestUri, request);
        }

        // Check for XSS patterns
        if (containsXssPatterns(requestUri) || containsXssPatterns(request.getQueryString())) {
            auditService.logSuspiciousActivity(clientIp, "XSS_ATTEMPT",
                    "Potential XSS attack in request: " + requestUri, request);
        }

        // Check for suspicious user agents
        if (isSuspiciousUserAgent(userAgent)) {
            auditService.logSuspiciousActivity(clientIp, "SUSPICIOUS_USER_AGENT",
                    "Suspicious user agent: " + userAgent, request);
        }

        // Check for path traversal attempts
        if (containsPathTraversalPatterns(requestUri)) {
            auditService.logSuspiciousActivity(clientIp, "PATH_TRAVERSAL_ATTEMPT",
                    "Potential path traversal in request: " + requestUri, request);
        }
    }

    private boolean containsSqlInjectionPatterns(String input) {
        if (input == null) return false;

        String lowerInput = input.toLowerCase();
        String[] sqlPatterns = {
                "union select", "drop table", "insert into", "delete from",
                "update set", "exec(", "execute(", "sp_", "xp_", "' or '1'='1",
                "' or 1=1", "'; drop", "'; delete", "'; insert"
        };

        return Arrays.stream(sqlPatterns).anyMatch(lowerInput::contains);
    }

    private boolean containsXssPatterns(String input) {
        if (input == null) return false;

        String lowerInput = input.toLowerCase();
        String[] xssPatterns = {
                "<script", "javascript:", "onload=", "onerror=", "onclick=",
                "onmouseover=", "alert(", "document.cookie", "eval("
        };

        return Arrays.stream(xssPatterns).anyMatch(lowerInput::contains);
    }

    private boolean containsPathTraversalPatterns(String input) {
        if (input == null) return false;

        return input.contains("../") || input.contains("..\\") ||
                input.contains("%2e%2e%2f") || input.contains("%2e%2e%5c");
    }

    private boolean isSuspiciousUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return true; // Missing user agent is suspicious
        }

        String lowerUserAgent = userAgent.toLowerCase();
        String[] suspiciousPatterns = {
                "sqlmap", "nikto", "nmap", "masscan", "zap", "burp",
                "wget", "curl", "python-requests", "bot", "crawler", "spider"
        };

        return Arrays.stream(suspiciousPatterns).anyMatch(lowerUserAgent::contains);
    }

    private boolean isSensitiveEndpoint(String requestUri) {
        return SENSITIVE_ENDPOINTS.stream().anyMatch(requestUri::startsWith);
    }

    private void logSensitiveEndpointAccess(HttpServletRequest request, String clientIp) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser) {
            AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
            auditService.logDataAccess(user.getUserId(), "SENSITIVE_ENDPOINT",
                    request.getMethod(), request.getRequestURI(), clientIp, request);
        } else {
            // Unauthenticated access to sensitive endpoint
            auditService.logSuspiciousActivity(clientIp, "UNAUTHENTICATED_SENSITIVE_ACCESS",
                    "Unauthenticated access to: " + request.getRequestURI(), request);
        }
    }

    private String extractUsernameFromRequest(HttpServletRequest request) {
        // Try to extract username from request body or parameters
        // This is a simplified implementation
        String username = request.getParameter("username");
        if (username != null) {
            return username;
        }

        // Could also parse JSON body if needed
        return "unknown";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip for health checks and static resources
        return path.startsWith("/actuator/health") ||
                path.startsWith("/actuator/info") ||
                path.startsWith("/static/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/");
    }
}
