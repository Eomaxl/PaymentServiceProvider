package com.paymentprovider.common.cache;

import java.time.Duration;

/**
 * Strategy pattern for different caching strategies.
 * Follows Strategy Pattern and Open/Closed Principle.
 */
public interface CacheStrategy {

    /**
     * Get cache key for the given parameters
     */
    String getCacheKey(Object... params);

    /**
     * Get TTL for this cache strategy
     */
    Duration getTtl();

    /**
     * Determine if the value should be cached
     */
    boolean shouldCache(Object value);

    /**
     * Get cache name/namespace
     */
    String getCacheName();
}

/**
 * Cache strategy for fraud detection results
 */
class FraudCacheStrategy implements CacheStrategy {

    @Override
    public String getCacheKey(Object... params) {
        if (params.length >= 2) {
            return String.format("fraud:%s:%s", params[0], params[1]);
        }
        throw new IllegalArgumentException("Fraud cache requires at least 2 parameters");
    }

    @Override
    public Duration getTtl() {
        return Duration.ofMinutes(15); // Fraud results valid for 15 minutes
    }

    @Override
    public boolean shouldCache(Object value) {
        return value != null; // Cache all non-null fraud results
    }

    @Override
    public String getCacheName() {
        return "fraud-detection";
    }
}

/**
 * Cache strategy for routing decisions
 */
class RoutingCacheStrategy implements CacheStrategy {

    @Override
    public String getCacheKey(Object... params) {
        if (params.length >= 3) {
            return String.format("routing:%s:%s:%s", params[0], params[1], params[2]);
        }
        throw new IllegalArgumentException("Routing cache requires at least 3 parameters");
    }

    @Override
    public Duration getTtl() {
        return Duration.ofMinutes(30); // Routing decisions valid for 30 minutes
    }

    @Override
    public boolean shouldCache(Object value) {
        return value != null;
    }

    @Override
    public String getCacheName() {
        return "payment-routing";
    }
}

/**
 * Cache strategy for merchant configurations
 */
class MerchantConfigCacheStrategy implements CacheStrategy {

    @Override
    public String getCacheKey(Object... params) {
        if (params.length >= 1) {
            return String.format("merchant-config:%s", params[0]);
        }
        throw new IllegalArgumentException("Merchant config cache requires merchant ID");
    }

    @Override
    public Duration getTtl() {
        return Duration.ofHours(1); // Merchant configs valid for 1 hour
    }

    @Override
    public boolean shouldCache(Object value) {
        return value != null;
    }

    @Override
    public String getCacheName() {
        return "merchant-config";
    }
}

/**
 * Cache strategy for exchange rates
 */
class ExchangeRateCacheStrategy implements CacheStrategy {

    @Override
    public String getCacheKey(Object... params) {
        if (params.length >= 2) {
            return String.format("exchange-rate:%s:%s", params[0], params[1]);
        }
        throw new IllegalArgumentException("Exchange rate cache requires from and to currencies");
    }

    @Override
    public Duration getTtl() {
        return Duration.ofMinutes(5); // Exchange rates valid for 5 minutes
    }

    @Override
    public boolean shouldCache(Object value) {
        return value != null;
    }

    @Override
    public String getCacheName() {
        return "exchange-rates";
    }
}
