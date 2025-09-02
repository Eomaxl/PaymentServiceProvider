package com.paymentprovider.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Local cache manager using Caffeine for L1 caching.
 * Implements Singleton pattern for cache instances.
 */
@Component
public class LocalCacheManager {

    private final ConcurrentMap<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();

    /**
     * Get cache instance using lazy initialization
     */
    private Cache<String, Object> getCache(String cacheName) {
        return caches.computeIfAbsent(cacheName, name ->
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .build()
        );
    }

    public <T> T get(String key, Class<T> type) {
        Cache<String, Object> cache = getCache("default");
        Object value = cache.getIfPresent(key);
        return type.cast(value);
    }

    public <T> void put(String key, T value, Duration ttl) {
        Cache<String, Object> cache = getCache("default");
        cache.put(key, value);
    }

    public void evict(String key) {
        Cache<String, Object> cache = getCache("default");
        cache.invalidate(key);
    }
}
