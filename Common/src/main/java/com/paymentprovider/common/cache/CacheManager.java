package com.paymentprovider.common.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Cache manager implementing multiple caching strategies.
 * Follows Single Responsibility Principle and Strategy Pattern.
 */
@Component
public class CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Executor cacheExecutor;

    public CacheManager(RedisTemplate<String, Object> redisTemplate,
                        Executor cacheExecutor) {
        this.redisTemplate = redisTemplate;
        this.cacheExecutor = cacheExecutor;
    }

    /**
     * Get value from cache with fallback to supplier if not found
     */
    public <T> CompletableFuture<T> getOrCompute(String key, Supplier<T> supplier,
                                                 Duration ttl, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Try to get from cache first
                Object cached = redisTemplate.opsForValue().get(key);
                if (cached != null && type.isInstance(cached)) {
                    logger.debug("Cache hit for key: {}", key);
                    return type.cast(cached);
                }

                // Cache miss - compute value
                logger.debug("Cache miss for key: {}, computing value", key);
                T value = supplier.get();

                if (value != null) {
                    // Store in cache asynchronously
                    CompletableFuture.runAsync(() -> {
                        try {
                            redisTemplate.opsForValue().set(key, value, ttl);
                            logger.debug("Cached value for key: {} with TTL: {}", key, ttl);
                        } catch (Exception e) {
                            logger.warn("Failed to cache value for key: {}", key, e);
                        }
                    }, cacheExecutor);
                }

                return value;
            } catch (Exception e) {
                logger.error("Error accessing cache for key: {}", key, e);
                return supplier.get(); // Fallback to direct computation
            }
        }, cacheExecutor);
    }

    /**
     * Get value from cache
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null && type.isInstance(cached)) {
                return Optional.of(type.cast(cached));
            }
        } catch (Exception e) {
            logger.warn("Error getting value from cache for key: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * Put value in cache
     */
    public CompletableFuture<Void> put(String key, Object value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                logger.debug("Cached value for key: {} with TTL: {}", key, ttl);
            } catch (Exception e) {
                logger.error("Error caching value for key: {}", key, e);
            }
        }, cacheExecutor);
    }

    /**
     * Remove value from cache
     */
    public CompletableFuture<Void> evict(String key) {
        return CompletableFuture.runAsync(() -> {
            try {
                redisTemplate.delete(key);
                logger.debug("Evicted cache key: {}", key);
            } catch (Exception e) {
                logger.error("Error evicting cache key: {}", key, e);
            }
        }, cacheExecutor);
    }

    /**
     * Clear cache by pattern
     */
    public CompletableFuture<Void> evictByPattern(String pattern) {
        return CompletableFuture.runAsync(() -> {
            try {
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    logger.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
                }
            } catch (Exception e) {
                logger.error("Error evicting cache by pattern: {}", pattern, e);
            }
        }, cacheExecutor);
    }

    /**
     * Check if key exists in cache
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.warn("Error checking cache key existence: {}", key, e);
            return false;
        }
    }

    /**
     * Refresh cache entry asynchronously
     */
    public <T> CompletableFuture<Void> refresh(String key, Supplier<T> supplier, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            try {
                T value = supplier.get();
                if (value != null) {
                    redisTemplate.opsForValue().set(key, value, ttl);
                    logger.debug("Refreshed cache for key: {}", key);
                }
            } catch (Exception e) {
                logger.error("Error refreshing cache for key: {}", key, e);
            }
        }, cacheExecutor);
    }
}
