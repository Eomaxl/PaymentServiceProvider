package com.paymentprovider.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * Configuration for application scaling, load balancing, and performance optimization.
 * Implements stateless session management and optimized thread pools for high throughput.
 */
@Configuration
public class ScalingConfig implements WebMvcConfigurer {

    /**
     * Async task executor for handling concurrent requests.
     * Configured for optimal performance under load.
     */
    @Bean(name = "scalingTaskExecutor")
    public Executor scalingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("PaymentAsync-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Payment processing executor for high-throughput operations.
     * Separate thread pool for payment processing to avoid blocking other operations.
     */
    @Bean(name = "paymentProcessingExecutor")
    public Executor paymentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("PaymentProcessor-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Fraud detection executor for CPU-intensive operations.
     * Optimized for fraud analysis workloads.
     */
    @Bean(name = "fraudDetectionExecutor")
    public Executor fraudDetectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("FraudDetection-");
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Configure async support for web layer.
     * Enables non-blocking request processing for better scalability.
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(30000); // 30 seconds
        configurer.setTaskExecutor((AsyncTaskExecutor) scalingTaskExecutor());
    }

    /**
     * Configuration properties for scaling parameters.
     */
    @ConfigurationProperties(prefix = "payment.scaling")
    public static class ScalingProperties {
        private int maxConcurrentPayments = 1000;
        private int maxConcurrentFraudChecks = 500;
        private int requestTimeoutMs = 30000;
        private boolean enableAsyncProcessing = true;
        private int batchSize = 100;

        // Getters and setters
        public int getMaxConcurrentPayments() {
            return maxConcurrentPayments;
        }

        public void setMaxConcurrentPayments(int maxConcurrentPayments) {
            this.maxConcurrentPayments = maxConcurrentPayments;
        }

        public int getMaxConcurrentFraudChecks() {
            return maxConcurrentFraudChecks;
        }

        public void setMaxConcurrentFraudChecks(int maxConcurrentFraudChecks) {
            this.maxConcurrentFraudChecks = maxConcurrentFraudChecks;
        }

        public int getRequestTimeoutMs() {
            return requestTimeoutMs;
        }

        public void setRequestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public boolean isEnableAsyncProcessing() {
            return enableAsyncProcessing;
        }

        public void setEnableAsyncProcessing(boolean enableAsyncProcessing) {
            this.enableAsyncProcessing = enableAsyncProcessing;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}
