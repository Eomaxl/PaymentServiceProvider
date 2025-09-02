package com.paymentprovider.paymentservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for custom metrics collection
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter paymentSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment.success.total")
                .description("Total number of successful payments")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment.failure.total")
                .description("Total number of failed payments")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Counter paymentVolumeCounter(MeterRegistry meterRegistry) {
        return Counter.builder("payment.volume.total")
                .description("Total payment volume processed")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Timer paymentProcessingTimer(MeterRegistry meterRegistry) {
        return Timer.builder("payment.processing.duration")
                .description("Time taken to process payments")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Counter fraudDetectionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("fraud.detection.total")
                .description("Total number of fraud detections")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Timer fraudAnalysisTimer(MeterRegistry meterRegistry) {
        return Timer.builder("fraud.analysis.duration")
                .description("Time taken for fraud analysis")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Counter routingDecisionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("routing.decision.total")
                .description("Total number of routing decisions")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Counter processorFailoverCounter(MeterRegistry meterRegistry) {
        return Counter.builder("processor.failover.total")
                .description("Total number of processor failovers")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Counter currencyConversionCounter(MeterRegistry meterRegistry) {
        return Counter.builder("currency.conversion.total")
                .description("Total number of currency conversions")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    @Bean
    public Timer apiResponseTimer(MeterRegistry meterRegistry) {
        return Timer.builder("api.response.duration")
                .description("API response time")
                .tag("service", "payment")
                .register(meterRegistry);
    }
}
