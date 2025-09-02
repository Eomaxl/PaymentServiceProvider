package com.paymentprovider.paymentservice.event.publisher;

import com.paymentprovider.paymentservice.event.PaymentEvent;
import com.paymentprovider.paymentservice.event.PaymentEventType;
import com.paymentprovider.paymentservice.event.repository.PaymentEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service for publishing payment events to the event store and internal event bus.
 * Ensures events are persisted and propagated for event sourcing and real-time processing.
 */
@Service
public class PaymentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final PaymentEventRepository eventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final Executor paymentEventExecutor;

    // Concurrency controls
    private final Semaphore publishingLimit = new Semaphore(100); // Max 100 concurrent publishes
    private final AtomicInteger activePublishes = new AtomicInteger(0);

    public PaymentEventPublisher(PaymentEventRepository eventRepository,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 ObjectMapper objectMapper,
                                 @Qualifier("paymentEventExecutor") Executor paymentEventExecutor) {
        this.eventRepository = eventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
        this.paymentEventExecutor = paymentEventExecutor;
    }

    /**
     * Publish a payment event with event data
     */
    @Transactional
    public PaymentEvent publishEvent(String paymentId, PaymentEventType eventType, Object eventData) {
        return publishEvent(paymentId, eventType, eventData, null, null);
    }

    /**
     * Publish a payment event with correlation and user context
     */
    @Transactional
    public PaymentEvent publishEvent(String paymentId, PaymentEventType eventType, Object eventData,
                                     String correlationId, String userId) {
        try {
            String eventDataJson = eventData != null ? objectMapper.writeValueAsString(eventData) : null;
            String version = generateVersion();

            PaymentEvent event = new PaymentEvent(paymentId, eventType, eventDataJson, version, correlationId, userId);

            // Persist event to event store
            PaymentEvent savedEvent = eventRepository.save(event);

            // Publish to internal event bus for real-time processing
            applicationEventPublisher.publishEvent(new PaymentEventPublished(savedEvent));

            logger.info("Published payment event: paymentId={}, eventType={}, eventId={}, correlationId={}",
                    paymentId, eventType, savedEvent.getEventId(), correlationId);

            return savedEvent;

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event data for paymentId={}, eventType={}", paymentId, eventType, e);
            throw new EventPublishingException("Failed to serialize event data", e);
        } catch (Exception e) {
            logger.error("Failed to publish payment event: paymentId={}, eventType={}", paymentId, eventType, e);
            throw new EventPublishingException("Failed to publish payment event", e);
        }
    }

    /**
     * Publish a simple event without additional data
     */
    @Transactional
    public PaymentEvent publishSimpleEvent(String paymentId, PaymentEventType eventType) {
        return publishEvent(paymentId, eventType, null);
    }

    /**
     * Publish an event with correlation ID for distributed tracing
     */
    @Transactional
    public PaymentEvent publishEventWithCorrelation(String paymentId, PaymentEventType eventType,
                                                    Object eventData, String correlationId) {
        return publishEvent(paymentId, eventType, eventData, correlationId, null);
    }

    /**
     * Publish multiple events in a single transaction
     */
    @Transactional
    public void publishEvents(PaymentEventBatch eventBatch) {
        for (PaymentEventBatch.EventData eventData : eventBatch.getEvents()) {
            publishEvent(eventData.getPaymentId(), eventData.getEventType(),
                    eventData.getData(), eventData.getCorrelationId(), eventData.getUserId());
        }
    }

    /**
     * Asynchronous event publishing for better performance
     */
    @Async("paymentEventExecutor")
    public CompletableFuture<PaymentEvent> publishEventAsync(String paymentId, PaymentEventType eventType,
                                                             Object eventData, String correlationId, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!publishingLimit.tryAcquire()) {
                logger.warn("Event publishing rejected due to capacity limit for paymentId: {}", paymentId);
                throw new EventPublishingException("Publishing capacity limit reached", null);
            }

            int currentActive = activePublishes.incrementAndGet();
            logger.debug("Publishing event async for paymentId: {} (active: {})", paymentId, currentActive);

            try {
                return publishEvent(paymentId, eventType, eventData, correlationId, userId);
            } finally {
                publishingLimit.release();
                activePublishes.decrementAndGet();
            }
        }, paymentEventExecutor);
    }

    /**
     * Batch event publishing with parallel processing
     */
    public CompletableFuture<List<PaymentEvent>> publishEventsAsync(PaymentEventBatch eventBatch) {
        logger.info("Publishing {} events asynchronously", eventBatch.getEvents().size());

        List<CompletableFuture<PaymentEvent>> futures = eventBatch.getEvents().stream()
                .map(eventData -> publishEventAsync(
                        eventData.getPaymentId(),
                        eventData.getEventType(),
                        eventData.getData(),
                        eventData.getCorrelationId(),
                        eventData.getUserId()))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * Publish events with priority handling
     */
    public CompletableFuture<List<PaymentEvent>> publishEventsWithPriority(PaymentEventBatch eventBatch) {
        // Separate high-priority events (fraud, security) from normal events
        List<PaymentEventBatch.EventData> highPriorityEvents = eventBatch.getEvents().stream()
                .filter(this::isHighPriorityEvent)
                .collect(Collectors.toList());

        List<PaymentEventBatch.EventData> normalEvents = eventBatch.getEvents().stream()
                .filter(event -> !isHighPriorityEvent(event))
                .collect(Collectors.toList());

        // Process high-priority events first
        CompletableFuture<List<PaymentEvent>> highPriorityFuture = CompletableFuture.completedFuture(List.of());
        if (!highPriorityEvents.isEmpty()) {
            PaymentEventBatch highPriorityBatch = new PaymentEventBatch();
            highPriorityEvents.forEach(highPriorityBatch::addEvent);
            highPriorityFuture = publishEventsAsync(highPriorityBatch);
        }

        // Process normal events in parallel
        CompletableFuture<List<PaymentEvent>> normalFuture = CompletableFuture.completedFuture(List.of());
        if (!normalEvents.isEmpty()) {
            PaymentEventBatch normalBatch = new PaymentEventBatch();
            normalEvents.forEach(normalBatch::addEvent);
            normalFuture = publishEventsAsync(normalBatch);
        }

        // Combine results
        return CompletableFuture.allOf(highPriorityFuture, normalFuture)
                .thenApply(v -> {
                    List<PaymentEvent> allEvents = highPriorityFuture.join();
                    allEvents.addAll(normalFuture.join());
                    return allEvents;
                });
    }

    /**
     * Publish event with retry mechanism
     */
    public CompletableFuture<PaymentEvent> publishEventWithRetry(String paymentId, PaymentEventType eventType,
                                                                 Object eventData, String correlationId,
                                                                 String userId, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            Exception lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    return publishEvent(paymentId, eventType, eventData, correlationId, userId);
                } catch (Exception e) {
                    lastException = e;
                    logger.warn("Event publishing attempt {} failed for paymentId: {}, error: {}",
                            attempt, paymentId, e.getMessage());

                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(1000 * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new EventPublishingException("Publishing interrupted", ie);
                        }
                    }
                }
            }

            throw new EventPublishingException("Failed to publish event after " + maxRetries + " attempts", lastException);
        }, paymentEventExecutor);
    }

    /**
     * Check if an event is high priority (fraud, security, etc.)
     */
    private boolean isHighPriorityEvent(PaymentEventBatch.EventData eventData) {
        PaymentEventType eventType = eventData.getEventType();
        return eventType == PaymentEventType.FRAUD_DETECTED ||
                eventType == PaymentEventType.SECURITY_ALERT ||
                eventType == PaymentEventType.PAYMENT_FAILED ||
                eventType == PaymentEventType.CHARGEBACK_RECEIVED;
    }

    /**
     * Get current publishing metrics
     */
    public PublishingMetrics getPublishingMetrics() {
        return new PublishingMetrics(
                activePublishes.get(),
                publishingLimit.availablePermits(),
                100 // total capacity
        );
    }

    /**
     * Metrics class for monitoring publishing performance
     */
    public static class PublishingMetrics {
        private final int activePublishes;
        private final int availableCapacity;
        private final int totalCapacity;

        public PublishingMetrics(int activePublishes, int availableCapacity, int totalCapacity) {
            this.activePublishes = activePublishes;
            this.availableCapacity = availableCapacity;
            this.totalCapacity = totalCapacity;
        }

        public int getActivePublishes() { return activePublishes; }
        public int getAvailableCapacity() { return availableCapacity; }
        public int getTotalCapacity() { return totalCapacity; }
        public double getUtilizationPercentage() {
            return ((double) activePublishes / totalCapacity) * 100;
        }
    }

    /**
     * Generate a unique version identifier for the event
     */
    private String generateVersion() {
        return UUID.randomUUID().toString();
    }

    /**
     * Exception thrown when event publishing fails
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
