package com.paymentprovider.paymentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Filter for API rate limiting and DDoS protection.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String SUSPICIOUS_IP_PREFIX = "suspicious_ip:";

    // Rate limiting configuration
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 60;
    private static final int API_KEY_REQUESTS_PER_MINUTE = 300;
    private static final int SUSPICIOUS_THRESHOLD = 1000;
    private static final int BLOCK_DURATION_MINUTES = 15;

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // Check if IP is blocked for suspicious activity
        if (isIpBlocked(clientIp)) {
            logger.warn("Blocked request from suspicious IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"IP temporarily blocked due to suspicious activity\"}");
            return;
        }

        // Determine rate limit based on authentication type
        int rateLimit = determineRateLimit(request);
        String rateLimitKey = RATE_LIMIT_PREFIX + clientIp;

        // Check current request count
        String currentCountStr = redisTemplate.opsForValue().get(rateLimitKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        if (currentCount >= rateLimit) {
            logger.warn("Rate limit exceeded for IP: {} ({})", clientIp, currentCount);

            // Track suspicious activity
            trackSuspiciousActivity(clientIp);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}");
            return;
        }

        // Increment request count
        redisTemplate.opsForValue().increment(rateLimitKey);
        if (currentCount == 0) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(1));
        }

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimit - currentCount - 1));

        // Log request for monitoring
        logRequest(request, clientIp, userAgent);

        filterChain.doFilter(request, response);
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

    private int determineRateLimit(HttpServletRequest request) {
        // Higher rate limit for authenticated API key requests
        String apiKey = request.getHeader("X-API-Key");
        String authHeader = request.getHeader("Authorization");

        if (apiKey != null || (authHeader != null && authHeader.startsWith("ApiKey "))) {
            return API_KEY_REQUESTS_PER_MINUTE;
        }

        return DEFAULT_REQUESTS_PER_MINUTE;
    }

    private boolean isIpBlocked(String clientIp) {
        String suspiciousKey = SUSPICIOUS_IP_PREFIX + clientIp;
        String blockedUntil = redisTemplate.opsForValue().get(suspiciousKey);

        if (blockedUntil != null) {
            long blockedUntilTime = Long.parseLong(blockedUntil);
            return System.currentTimeMillis() < blockedUntilTime;
        }

        return false;
    }

    private void trackSuspiciousActivity(String clientIp) {
        String suspiciousCountKey = "suspicious_count:" + clientIp;
        String currentCountStr = redisTemplate.opsForValue().get(suspiciousCountKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        currentCount++;
        redisTemplate.opsForValue().set(suspiciousCountKey, String.valueOf(currentCount), Duration.ofHours(1));

        // Block IP if suspicious activity threshold is exceeded
        if (currentCount >= SUSPICIOUS_THRESHOLD) {
            long blockUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(BLOCK_DURATION_MINUTES);
            String suspiciousKey = SUSPICIOUS_IP_PREFIX + clientIp;
            redisTemplate.opsForValue().set(suspiciousKey, String.valueOf(blockUntil), Duration.ofMinutes(BLOCK_DURATION_MINUTES));

            logger.error("IP {} blocked for {} minutes due to suspicious activity (count: {})",
                    clientIp, BLOCK_DURATION_MINUTES, currentCount);
        }
    }

    private void logRequest(HttpServletRequest request, String clientIp, String userAgent) {
        if (logger.isDebugEnabled()) {
            logger.debug("Request: {} {} from IP: {} User-Agent: {}",
                    request.getMethod(), request.getRequestURI(), clientIp, userAgent);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for health checks
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }
}
