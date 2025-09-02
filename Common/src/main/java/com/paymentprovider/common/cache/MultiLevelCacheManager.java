package com.paymentprovider.common.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-level cache manager implementing Cache-Aside pattern.
 * Follows Single Responsibility Principle - handles caching operations only.
 */
@Component
public class MultiLevelCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LocalCacheManager localCache;

    @Autowired
    public MultiLevelCacheManager(RedisTemplate<String, Object> redisTemplate,
                                  LocalCacheManager localCache) {
        this.redisTemplate = redisTemplate;
        this.localCache = localCache;
    }

    /**
     * Get from cache with fallback strategy
     */
    public <T> CompletableFuture<T> getAsync(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            // L1 Cache (Local)
            T value = localCache.get(key, type);
            if (value != null) {
                return value;
            }

            // L2 Cache (Redis)
            value = (T) redisTemplate.opsForValue().get(key);
            if (value != null) {
                localCache.put(key, value, Duration.ofMinutes(5));
                return value;
            }

            return null;
        });
    }

    /**
     * Put with write-through strategy
     */
    public <T> CompletableFuture<Void> putAsync(String key, T value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            // Write to both levels
            localCache.put(key, value, ttl);
            redisTemplate.opsForValue().set(key, value, ttl);
        });
    }

    /**
     * Evict from all cache levels
     */
    public CompletableFuture<Void> evictAsync(String key) {
        return CompletableFuture.runAsync(() -> {
            localCache.evict(key);
            redisTemplate.delete(key);
        });
    }

    /**
     * Get or compute pattern with async computation
     */
    public <T> CompletableFuture<T> getOrComputeAsync(String key, Class<T> type,
                                                      CompletableFuture<T> computation,
                                                      Duration ttl) {
        return getAsync(key, type)
                .thenCompose(cached -> {
                    if (cached != null) {
                        return CompletableFuture.completedFuture(cached);
                    }
                    return computation.thenCompose(computed ->
                            putAsync(key, computed, ttl).thenApply(v -> computed)
                    );
                });
    }
}