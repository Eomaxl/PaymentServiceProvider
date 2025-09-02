package com.paymentprovider.paymentservice.health;

import com.paymentprovider.paymentservice.services.PaymentMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom health endpoint with payment-specific metrics
 */
@RestController
@RequestMapping("/actuator/payment-health")
public class CustomHealthEndpoint {

    private final PaymentMetricsService metricsService;

    public CustomHealthEndpoint(PaymentMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("service", "payment-service");

        // Add payment-specific health metrics
        Map<String, Object> paymentMetrics = new HashMap<>();
        paymentMetrics.put("success_rate", metricsService.getPaymentSuccessRate());
        paymentMetrics.put("active_payments", getCurrentActivePayments());
        paymentMetrics.put("system_load", getSystemLoad());

        health.put("payment_metrics", paymentMetrics);

        // Add component health status
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("redis", "UP");
        components.put("fraud_service", "UP");
        components.put("routing_service", "UP");

        health.put("components", components);

        return health;
    }

    private long getCurrentActivePayments() {
        // This would typically query the actual active payments
        // For now, return a mock value
        return 0L;
    }

    private double getSystemLoad() {
        return Runtime.getRuntime().availableProcessors() > 0 ?
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
                        (double) Runtime.getRuntime().totalMemory() : 0.0;
    }
}
