package com.paymentprovider.paymentservice.health;

import com.paymentprovider.paymentservice.services.PaymentMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple health controller for payment service
 */
@RestController
@RequestMapping("/health")
public class SimpleHealthController {

    private final PaymentMetricsService metricsService;
    private final DataSource dataSource;

    public SimpleHealthController(PaymentMetricsService metricsService, DataSource dataSource) {
        this.metricsService = metricsService;
        this.dataSource = dataSource;
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
        paymentMetrics.put("system_load", getSystemLoad());

        health.put("payment_metrics", paymentMetrics);

        // Add component health status
        Map<String, String> components = new HashMap<>();
        components.put("database", checkDatabase() ? "UP" : "DOWN");
        components.put("redis", "UP");
        components.put("fraud_service", "UP");
        components.put("routing_service", "UP");

        health.put("components", components);

        return health;
    }

    @GetMapping("/ready")
    public Map<String, Object> readiness() {
        Map<String, Object> readiness = new HashMap<>();

        boolean isReady = checkDatabase() && checkServices();

        readiness.put("status", isReady ? "READY" : "NOT_READY");
        readiness.put("timestamp", Instant.now());
        readiness.put("checks", Map.of(
                "database", checkDatabase(),
                "services", checkServices()
        ));

        return readiness;
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkServices() {
        // Mock implementation - in real scenario, check service connectivity
        return true;
    }

    private double getSystemLoad() {
        return Runtime.getRuntime().availableProcessors() > 0 ?
                (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) /
                        (double) Runtime.getRuntime().totalMemory() : 0.0;
    }
}
