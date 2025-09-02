package com.paymentprovider.paymentservice.services;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing distributed tracing and correlation IDs
 */
@Service
public class TracingService {

    private static final Logger logger = LoggerFactory.getLogger(TracingService.class);
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";

    private final Tracer tracer;

    public TracingService(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Start a new span with correlation ID
     */
    public Span startSpan(String operationName) {
        Span span = tracer.nextSpan().name(operationName).start();

        // Generate correlation ID if not present
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }

        // Add tracing information to MDC
        if (span.context() != null) {
            MDC.put(TRACE_ID_KEY, span.context().traceId());
            MDC.put(SPAN_ID_KEY, span.context().spanId());
        }

        span.tag("correlation.id", correlationId);

        logger.debug("Started span: {} with correlation ID: {}", operationName, correlationId);

        return span;
    }

    /**
     * Add tag to current span
     */
    public void addTag(String key, String value) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag(key, value);
        }
    }

    /**
     * Add event to current span
     */
    public void addEvent(String eventName) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.event(eventName);
        }
    }

    /**
     * Set correlation ID for the current thread
     */
    public void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    /**
     * Get correlation ID for the current thread
     */
    public String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Clear tracing context
     */
    public void clearContext() {
        MDC.remove(CORRELATION_ID_KEY);
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }

    /**
     * Trace payment processing operation
     */
    public Span tracePaymentProcessing(String paymentId, String paymentMethod) {
        Span span = startSpan("payment.processing");
        span.tag("payment.id", paymentId);
        span.tag("payment.method", paymentMethod);
        span.tag("service", "payment-service");

        logger.info("Tracing payment processing for payment ID: {}", paymentId);

        return span;
    }

    /**
     * Trace fraud detection operation
     */
    public Span traceFraudDetection(String paymentId) {
        Span span = startSpan("fraud.detection");
        span.tag("payment.id", paymentId);
        span.tag("service", "fraud-service");

        logger.info("Tracing fraud detection for payment ID: {}", paymentId);

        return span;
    }

    /**
     * Trace routing decision operation
     */
    public Span traceRoutingDecision(String paymentId, String processor) {
        Span span = startSpan("routing.decision");
        span.tag("payment.id", paymentId);
        span.tag("processor", processor);
        span.tag("service", "routing-service");

        logger.info("Tracing routing decision for payment ID: {} to processor: {}", paymentId, processor);

        return span;
    }

    /**
     * Trace external API call
     */
    public Span traceExternalCall(String serviceName, String operation) {
        Span span = startSpan("external.call");
        span.tag("external.service", serviceName);
        span.tag("operation", operation);

        logger.info("Tracing external call to service: {} operation: {}", serviceName, operation);

        return span;
    }

    /**
     * Handle span error
     */
    public void handleError(Span span, Exception error) {
        if (span != null) {
            span.tag("error", "true");
            span.tag("error.message", error.getMessage());
            span.tag("error.class", error.getClass().getSimpleName());

            logger.error("Error in span: {}", span.context().spanId(), error);
        }
    }

    /**
     * End span with success
     */
    public void endSpanWithSuccess(Span span) {
        if (span != null) {
            span.tag("success", "true");
            span.end();

            logger.debug("Ended span successfully: {}", span.context().spanId());
        }
    }
}
