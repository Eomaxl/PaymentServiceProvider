package com.paymentprovider.paymentservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for DDoS protection and attack mitigation.
 */
@Service
public class DdosProtectionService {
    private static final Logger logger = LoggerFactory.getLogger(DdosProtectionService.class);

    private static final String DDOS_PATTERN_PREFIX = "ddos_pattern:";
    private static final String BLOCKED_IP_PREFIX = "blocked_ip:";
    private static final String ATTACK_SIGNATURE_PREFIX = "attack_sig:";

    // DDoS detection thresholds
    private static final int BURST_THRESHOLD = 100; // requests per 10 seconds
    private static final int SUSTAINED_THRESHOLD = 500; // requests per minute
    private static final int DISTRIBUTED_THRESHOLD = 50; // IPs making similar requests

    // Block durations
    private static final int SHORT_BLOCK_MINUTES = 5;
    private static final int MEDIUM_BLOCK_MINUTES = 30;
    private static final int LONG_BLOCK_MINUTES = 120;

    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityAuditService auditService;

    @Autowired
    public DdosProtectionService(RedisTemplate<String, String> redisTemplate,
                                 SecurityAuditService auditService) {
        this.redisTemplate = redisTemplate;
        this.auditService = auditService;
    }

    /**
     * Analyzes request patterns to detect potential DDoS attacks.
     */
    public DdosAnalysisResult analyzeRequest(String clientIp, String requestUri, String userAgent) {
        // Check for burst attacks (high frequency from single IP)
        if (detectBurstAttack(clientIp)) {
            blockIp(clientIp, SHORT_BLOCK_MINUTES, "Burst attack detected");
            return new DdosAnalysisResult(true, "BURST_ATTACK", SHORT_BLOCK_MINUTES);
        }

        // Check for sustained attacks (high volume over time)
        if (detectSustainedAttack(clientIp)) {
            blockIp(clientIp, MEDIUM_BLOCK_MINUTES, "Sustained attack detected");
            return new DdosAnalysisResult(true, "SUSTAINED_ATTACK", MEDIUM_BLOCK_MINUTES);
        }

        // Check for distributed attacks (coordinated from multiple IPs)
        if (detectDistributedAttack(requestUri, userAgent)) {
            // For distributed attacks, we might implement more sophisticated blocking
            logger.warn("Distributed attack pattern detected for URI: {} User-Agent: {}", requestUri, userAgent);
            return new DdosAnalysisResult(true, "DISTRIBUTED_ATTACK", 0);
        }

        // Update request patterns for analysis
        updateRequestPatterns(clientIp, requestUri, userAgent);

        return new DdosAnalysisResult(false, null, 0);
    }

    /**
     * Checks if an IP is currently blocked.
     */
    public boolean isIpBlocked(String clientIp) {
        String blockedKey = BLOCKED_IP_PREFIX + clientIp;
        String blockedUntil = redisTemplate.opsForValue().get(blockedKey);

        if (blockedUntil != null) {
            long blockedUntilTime = Long.parseLong(blockedUntil);
            if (System.currentTimeMillis() < blockedUntilTime) {
                return true;
            } else {
                // Block expired, clean up
                redisTemplate.delete(blockedKey);
            }
        }

        return false;
    }

    /**
     * Manually blocks an IP address.
     */
    public void blockIp(String clientIp, int durationMinutes, String reason) {
        long blockUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durationMinutes);
        String blockedKey = BLOCKED_IP_PREFIX + clientIp;

        redisTemplate.opsForValue().set(blockedKey, String.valueOf(blockUntil),
                Duration.ofMinutes(durationMinutes));

        logger.warn("IP {} blocked for {} minutes. Reason: {}", clientIp, durationMinutes, reason);
        auditService.logIpBlocked(clientIp, reason, durationMinutes);
    }

    /**
     * Unblocks an IP address.
     */
    public void unblockIp(String clientIp) {
        String blockedKey = BLOCKED_IP_PREFIX + clientIp;
        redisTemplate.delete(blockedKey);
        logger.info("IP {} unblocked", clientIp);
    }

    /**
     * Gets statistics about blocked IPs and attack patterns.
     */
    public DdosStatistics getStatistics() {
        Set<String> blockedIps = redisTemplate.keys(BLOCKED_IP_PREFIX + "*");
        Set<String> attackPatterns = redisTemplate.keys(ATTACK_SIGNATURE_PREFIX + "*");

        return new DdosStatistics(
                blockedIps != null ? blockedIps.size() : 0,
                attackPatterns != null ? attackPatterns.size() : 0
        );
    }

    private boolean detectBurstAttack(String clientIp) {
        String burstKey = DDOS_PATTERN_PREFIX + "burst:" + clientIp;
        String currentCountStr = redisTemplate.opsForValue().get(burstKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        // Increment counter
        redisTemplate.opsForValue().increment(burstKey);
        if (currentCount == 0) {
            redisTemplate.expire(burstKey, Duration.ofSeconds(10));
        }

        return currentCount >= BURST_THRESHOLD;
    }

    private boolean detectSustainedAttack(String clientIp) {
        String sustainedKey = DDOS_PATTERN_PREFIX + "sustained:" + clientIp;
        String currentCountStr = redisTemplate.opsForValue().get(sustainedKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        // Increment counter
        redisTemplate.opsForValue().increment(sustainedKey);
        if (currentCount == 0) {
            redisTemplate.expire(sustainedKey, Duration.ofMinutes(1));
        }

        return currentCount >= SUSTAINED_THRESHOLD;
    }

    private boolean detectDistributedAttack(String requestUri, String userAgent) {
        // Create attack signature based on URI and User-Agent
        String signature = createAttackSignature(requestUri, userAgent);
        String signatureKey = ATTACK_SIGNATURE_PREFIX + signature;

        String currentCountStr = redisTemplate.opsForValue().get(signatureKey);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        // Increment counter
        redisTemplate.opsForValue().increment(signatureKey);
        if (currentCount == 0) {
            redisTemplate.expire(signatureKey, Duration.ofMinutes(5));
        }

        return currentCount >= DISTRIBUTED_THRESHOLD;
    }

    private void updateRequestPatterns(String clientIp, String requestUri, String userAgent) {
        // Store request patterns for future analysis
        String patternKey = DDOS_PATTERN_PREFIX + "pattern:" + clientIp;
        String pattern = requestUri + "|" + userAgent;

        redisTemplate.opsForList().leftPush(patternKey, pattern);
        redisTemplate.opsForList().trim(patternKey, 0, 99); // Keep last 100 requests
        redisTemplate.expire(patternKey, Duration.ofHours(1));
    }

    private String createAttackSignature(String requestUri, String userAgent) {
        // Create a signature that can identify coordinated attacks
        return String.valueOf((requestUri + "|" + userAgent).hashCode());
    }

    /**
     * Result of DDoS analysis.
     */
    public static class DdosAnalysisResult {
        private final boolean isAttack;
        private final String attackType;
        private final int blockDurationMinutes;

        public DdosAnalysisResult(boolean isAttack, String attackType, int blockDurationMinutes) {
            this.isAttack = isAttack;
            this.attackType = attackType;
            this.blockDurationMinutes = blockDurationMinutes;
        }

        public boolean isAttack() { return isAttack; }
        public String getAttackType() { return attackType; }
        public int getBlockDurationMinutes() { return blockDurationMinutes; }
    }

    /**
     * DDoS protection statistics.
     */
    public static class DdosStatistics {
        private final int blockedIpsCount;
        private final int attackPatternsCount;

        public DdosStatistics(int blockedIpsCount, int attackPatternsCount) {
            this.blockedIpsCount = blockedIpsCount;
            this.attackPatternsCount = attackPatternsCount;
        }

        public int getBlockedIpsCount() { return blockedIpsCount; }
        public int getAttackPatternsCount() { return attackPatternsCount; }
    }
}
