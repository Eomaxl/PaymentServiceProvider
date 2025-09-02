package com.paymentprovider.paymentservice.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Configuration for resilience patterns including circuit breakers, retries, bulkheads, and time limiters.
 * This configuration supports high availability and fault tolerance requirements.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit breaker configuration for external payment processors.
     * Implements fail-fast pattern to prevent cascading failures.
     */
    @Bean
    public CircuitBreakerConfig paymentProcessorCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(60.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .recordExceptions(
                        TimeoutException.class,
                        RuntimeException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        IllegalStateException.class
                )
                .build();
    }

    /**
     * Retry configuration for payment processor calls.
     * Implements exponential backoff to handle transient failures.
     */
    @Bean
    public RetryConfig paymentProcessorRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                        TimeoutException.class,
                        RuntimeException.class
                )
                .ignoreExceptions(
                        IllegalArgumentException.class,
                        SecurityException.class
                )
                .build();
    }

    /**
     * Bulkhead configuration for payment processor calls.
     * Implements resource isolation to prevent resource exhaustion.
     */
    @Bean
    public BulkheadConfig paymentProcessorBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(50)
                .maxWaitDuration(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Time limiter configuration for payment processor calls.
     * Prevents long-running operations from blocking resources.
     */
    @Bean
    public TimeLimiterConfig paymentProcessorTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();
    }

    /**
     * Circuit breaker configuration for fraud detection service.
     * More lenient than payment processor due to non-critical nature.
     */
    @Bean
    public CircuitBreakerConfig fraudServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .failureRateThreshold(40.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Bulkhead configuration for fraud detection service.
     * Lower concurrency limit due to CPU-intensive operations.
     */
    @Bean
    public BulkheadConfig fraudServiceBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(30)
                .maxWaitDuration(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Circuit breaker configuration for database operations.
     * Higher failure threshold due to critical nature of database.
     */
    @Bean
    public CircuitBreakerConfig databaseCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .permittedNumberOfCallsInHalfOpenState(4)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .failureRateThreshold(60.0f)
                .build();
    }

    /**
     * Bulkhead configuration for database operations.
     * Higher concurrency limit for database operations.
     */
    @Bean
    public BulkheadConfig databaseBulkheadConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(100)
                .maxWaitDuration(Duration.ofSeconds(2))
                .build();
    }
}
