package com.paymentprovider.paymentservice.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured logger with correlation ID and sensitive data masking
 */
@Component
public class StructuredLogger {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SensitiveDataMaskingService maskingService;

    public StructuredLogger(SensitiveDataMaskingService maskingService) {
        this.maskingService = maskingService;
    }

    /**
     * Log payment processing event
     */
    public void logPaymentProcessing(String paymentId, String status, String paymentMethod,
                                     String currency, String amount, Map<String, Object> additionalData) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "payment_processing");
        logData.put("payment_id", paymentId);
        logData.put("status", status);
        logData.put("payment_method", paymentMethod);
        logData.put("currency", currency);
        logData.put("amount", amount);
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));
        logData.put("trace_id", MDC.get("traceId"));
        logData.put("span_id", MDC.get("spanId"));

        if (additionalData != null) {
            logData.putAll(additionalData);
        }

        logStructured("info", "Payment processing event", logData);
    }

    /**
     * Log fraud detection event
     */
    public void logFraudDetection(String paymentId, String riskLevel, String reason,
                                  double fraudScore, Map<String, Object> additionalData) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "fraud_detection");
        logData.put("payment_id", paymentId);
        logData.put("risk_level", riskLevel);
        logData.put("reason", reason);
        logData.put("fraud_score", fraudScore);
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));
        logData.put("trace_id", MDC.get("traceId"));
        logData.put("span_id", MDC.get("spanId"));

        if (additionalData != null) {
            logData.putAll(additionalData);
        }

        logStructured("warn", "Fraud detection event", logData);
    }

    /**
     * Log routing decision event
     */
    public void logRoutingDecision(String paymentId, String selectedProcessor, String reason,
                                   Map<String, Object> routingData) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "routing_decision");
        logData.put("payment_id", paymentId);
        logData.put("selected_processor", selectedProcessor);
        logData.put("reason", reason);
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));
        logData.put("trace_id", MDC.get("traceId"));
        logData.put("span_id", MDC.get("spanId"));

        if (routingData != null) {
            logData.putAll(routingData);
        }

        logStructured("info", "Routing decision event", logData);
    }

    /**
     * Log API request/response
     */
    public void logApiCall(String method, String endpoint, int statusCode, long duration,
                           Map<String, Object> requestData, Map<String, Object> responseData) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "api_call");
        logData.put("method", method);
        logData.put("endpoint", endpoint);
        logData.put("status_code", statusCode);
        logData.put("duration_ms", duration);
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));
        logData.put("trace_id", MDC.get("traceId"));
        logData.put("span_id", MDC.get("spanId"));

        if (requestData != null) {
            logData.put("request", maskSensitiveData(requestData));
        }

        if (responseData != null) {
            logData.put("response", maskSensitiveData(responseData));
        }

        String level = statusCode >= 400 ? "error" : "info";
        logStructured(level, "API call event", logData);
    }

    /**
     * Log error event
     */
    public void logError(String operation, Exception error, Map<String, Object> contextData) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "error");
        logData.put("operation", operation);
        logData.put("error_class", error.getClass().getSimpleName());
        logData.put("error_message", error.getMessage());
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));
        logData.put("trace_id", MDC.get("traceId"));
        logData.put("span_id", MDC.get("spanId"));

        if (contextData != null) {
            logData.putAll(maskSensitiveData(contextData));
        }

        logStructured("error", "Error event", logData);
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String eventType, String userId, String ipAddress,
                                 String userAgent, Map<String, Object> securityData) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "security_event");
        logData.put("security_event_type", eventType);
        logData.put("user_id", userId);
        logData.put("ip_address", ipAddress);
        logData.put("user_agent", userAgent);
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));
        logData.put("trace_id", MDC.get("traceId"));
        logData.put("span_id", MDC.get("spanId"));

        if (securityData != null) {
            logData.putAll(maskSensitiveData(securityData));
        }

        logStructured("warn", "Security event", logData);
    }

    /**
     * Log business metric event
     */
    public void logBusinessMetric(String metricName, double value, String unit,
                                  Map<String, Object> dimensions) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("event_type", "business_metric");
        logData.put("metric_name", metricName);
        logData.put("value", value);
        logData.put("unit", unit);
        logData.put("timestamp", Instant.now().toString());
        logData.put("correlation_id", MDC.get("correlationId"));

        if (dimensions != null) {
            logData.put("dimensions", dimensions);
        }

        logStructured("info", "Business metric event", logData);
    }

    /**
     * Log structured data with masking
     */
    private void logStructured(String level, String message, Map<String, Object> data) {
        try {
            Map<String, Object> maskedData = maskSensitiveData(data);
            String jsonData = objectMapper.writeValueAsString(maskedData);
            String logMessage = message + " | " + jsonData;

            switch (level.toLowerCase()) {
                case "debug":
                    logger.debug(logMessage);
                    break;
                case "info":
                    logger.info(logMessage);
                    break;
                case "warn":
                    logger.warn(logMessage);
                    break;
                case "error":
                    logger.error(logMessage);
                    break;
                default:
                    logger.info(logMessage);
            }
        } catch (Exception e) {
            logger.error("Failed to log structured data", e);
        }
    }

    /**
     * Mask sensitive data in map
     */
    private Map<String, Object> maskSensitiveData(Map<String, Object> data) {
        Map<String, Object> maskedData = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (maskingService.isSensitiveField(key)) {
                maskedData.put(key, "***");
            } else if (value instanceof String) {
                maskedData.put(key, maskingService.maskSensitiveData((String) value));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                maskedData.put(key, maskSensitiveData(nestedMap));
            } else {
                maskedData.put(key, value);
            }
        }

        return maskedData;
    }
}
