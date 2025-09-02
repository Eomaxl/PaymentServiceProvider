package com.paymentprovider.paymentservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for load balancer integration.
 * Provides detailed health status for auto-scaling decisions.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint for load balancers.
     * Returns 200 OK if service is healthy, 503 if unhealthy.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "payment-service");
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness check for load balancers.
     * Checks if service is ready to accept traffic.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Check database connectivity
            boolean dbHealthy = checkDatabaseHealth();

            if (dbHealthy) {
                response.put("status", "UP");
                response.put("database", "UP");
                response.put("timestamp", System.currentTimeMillis());
                response.put("service", "payment-service");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "DOWN");
                response.put("database", "DOWN");
                response.put("timestamp", System.currentTimeMillis());
                response.put("service", "payment-service");
                return ResponseEntity.status(503).body(response);
            }
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            response.put("service", "payment-service");
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Detailed health check with metrics for auto-scaling decisions.
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> response = new HashMap<>();

        try {
            // System metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

            // Thread metrics
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            int activeThreads = rootGroup.activeCount();

            // Database health
            boolean dbHealthy = checkDatabaseHealth();

            response.put("status", dbHealthy ? "UP" : "DOWN");
            response.put("timestamp", System.currentTimeMillis());
            response.put("service", "payment-service");

            // System metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("memory", Map.of(
                    "total", totalMemory,
                    "used", usedMemory,
                    "free", freeMemory,
                    "usagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0
            ));
            metrics.put("threads", Map.of(
                    "active", activeThreads
            ));
            metrics.put("processors", runtime.availableProcessors());

            response.put("metrics", metrics);
            response.put("database", dbHealthy ? "UP" : "DOWN");

            // Determine HTTP status based on health
            if (dbHealthy && memoryUsagePercent < 90) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(503).body(response);
            }

        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            response.put("service", "payment-service");
            return ResponseEntity.status(503).body(response);
        }
    }

    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            return false;
        }
    }
}
