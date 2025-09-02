package com.paymentprovider.paymentservice.event.replay;

import com.paymentprovider.paymentservice.event.PaymentEvent;
import com.paymentprovider.paymentservice.event.PaymentEventType;
import com.paymentprovider.paymentservice.event.repository.PaymentEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for replaying payment events to reconstruct payment state.
 * Supports event sourcing patterns and disaster recovery scenarios.
 */
@Service
public class EventReplayService {

    private static final Logger logger = LoggerFactory.getLogger(EventReplayService.class);

    private final PaymentEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final PaymentStateReconstructor stateReconstructor;

    public EventReplayService(PaymentEventRepository eventRepository,
                              ObjectMapper objectMapper,
                              PaymentStateReconstructor stateReconstructor) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.stateReconstructor = stateReconstructor;
    }

    /**
     * Replay all events for a specific payment to reconstruct its current state
     */
    @Transactional(readOnly = true)
    public PaymentStateSnapshot replayPaymentEvents(String paymentId) {
        logger.info("Starting event replay for payment: {}", paymentId);

        List<PaymentEvent> events = eventRepository.findByPaymentIdOrderByTimestampAsc(paymentId);

        if (events.isEmpty()) {
            logger.warn("No events found for payment: {}", paymentId);
            return PaymentStateSnapshot.empty(paymentId);
        }

        return replayEvents(events);
    }

    /**
     * Replay events from a specific version onwards
     */
    @Transactional(readOnly = true)
    public PaymentStateSnapshot replayFromVersion(String paymentId, String fromVersion) {
        logger.info("Starting event replay for payment: {} from version: {}", paymentId, fromVersion);

        List<PaymentEvent> events = eventRepository.findEventsForReplay(paymentId, fromVersion);

        return replayEvents(events);
    }

    /**
     * Replay events within a specific time range
     */
    @Transactional(readOnly = true)
    public PaymentStateSnapshot replayEventsInTimeRange(String paymentId, Instant startTime, Instant endTime) {
        logger.info("Starting event replay for payment: {} between {} and {}", paymentId, startTime, endTime);

        List<PaymentEvent> events = eventRepository.findByPaymentIdAndTimestampBetweenOrderByTimestampAsc(
                paymentId, startTime, endTime);

        return replayEvents(events);
    }

    /**
     * Replay events for multiple payments
     */
    @Transactional(readOnly = true)
    public Map<String, PaymentStateSnapshot> replayMultiplePayments(List<String> paymentIds) {
        logger.info("Starting bulk event replay for {} payments", paymentIds.size());

        List<PaymentEvent> events = eventRepository.findByPaymentIds(paymentIds);

        return stateReconstructor.reconstructMultiplePaymentStates(events);
    }

    /**
     * Replay all events after a specific timestamp (for disaster recovery)
     */
    @Transactional(readOnly = true)
    public EventReplayResult replayEventsAfterTimestamp(Instant timestamp) {
        logger.info("Starting disaster recovery replay from timestamp: {}", timestamp);

        List<PaymentEvent> events = eventRepository.findByTimestampAfterOrderByTimestampAsc(timestamp);

        logger.info("Found {} events to replay for disaster recovery", events.size());

        return stateReconstructor.reconstructSystemState(events);
    }

    /**
     * Get the current state of a payment without full replay (optimized)
     */
    @Transactional(readOnly = true)
    public Optional<PaymentStateSnapshot> getCurrentPaymentState(String paymentId) {
        // Try to get the latest terminal event first for optimization
        Optional<PaymentEvent> latestEvent = eventRepository.findFirstByPaymentIdOrderByTimestampDesc(paymentId);

        if (latestEvent.isEmpty()) {
            return Optional.empty();
        }

        // If the latest event is terminal, we can optimize by not replaying all events
        if (latestEvent.get().getEventType().isTerminal()) {
            return Optional.of(replayPaymentEvents(paymentId));
        }

        // Otherwise, do full replay
        return Optional.of(replayPaymentEvents(paymentId));
    }

    /**
     * Validate event sequence integrity for a payment
     */
    @Transactional(readOnly = true)
    public EventSequenceValidationResult validateEventSequence(String paymentId) {
        logger.info("Validating event sequence for payment: {}", paymentId);

        List<PaymentEvent> events = eventRepository.findByPaymentIdOrderByTimestampAsc(paymentId);

        return stateReconstructor.validateEventSequence(events);
    }

    /**
     * Find missing events in a payment's event sequence
     */
    @Transactional(readOnly = true)
    public List<PaymentEventType> findMissingEvents(String paymentId) {
        List<PaymentEvent> events = eventRepository.findByPaymentIdOrderByTimestampAsc(paymentId);

        return stateReconstructor.findMissingEvents(events);
    }

    /**
     * Core method to replay a list of events and reconstruct state
     */
    private PaymentStateSnapshot replayEvents(List<PaymentEvent> events) {
        if (events.isEmpty()) {
            return PaymentStateSnapshot.empty("unknown");
        }

        String paymentId = events.get(0).getPaymentId();

        try {
            PaymentStateSnapshot snapshot = stateReconstructor.reconstructPaymentState(events);

            logger.info("Successfully replayed {} events for payment: {}", events.size(), paymentId);

            return snapshot;

        } catch (Exception e) {
            logger.error("Failed to replay events for payment: {}", paymentId, e);
            throw new EventReplayException("Failed to replay events for payment: " + paymentId, e);
        }
    }

    /**
     * Exception thrown when event replay fails
     */
    public static class EventReplayException extends RuntimeException {
        public EventReplayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
