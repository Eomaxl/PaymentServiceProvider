package com.paymentprovider.paymentservice.event.replay;

import com.paymentprovider.paymentservice.event.PaymentEvent;
import com.paymentprovider.paymentservice.event.PaymentEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Component responsible for reconstructing payment state from event sequences.
 * Implements the core logic for event sourcing state reconstruction.
 */
@Component
public class PaymentStateReconstructor {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStateReconstructor.class);

    private final ObjectMapper objectMapper;

    public PaymentStateReconstructor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reconstruct payment state from a sequence of events
     */
    public PaymentStateSnapshot reconstructPaymentState(List<PaymentEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot reconstruct state from empty event list");
        }

        String paymentId = events.get(0).getPaymentId();
        PaymentStateSnapshot.Builder builder = PaymentStateSnapshot.builder(paymentId);

        // Track event history
        List<PaymentEventType> eventHistory = events.stream()
                .map(PaymentEvent::getEventType)
                .collect(Collectors.toList());

        // Process events in chronological order
        String currentStatus = "UNKNOWN";
        BigDecimal amount = null;
        String currency = null;
        String merchantId = null;
        String paymentMethod = null;
        Instant createdAt = null;
        String processorReference = null;
        Map<String, Object> metadata = new HashMap<>();
        boolean isTerminal = false;

        for (PaymentEvent event : events) {
            // Update timestamps
            if (createdAt == null) {
                createdAt = event.getTimestamp();
            }

            // Process event based on type
            switch (event.getEventType()) {
                case PAYMENT_INITIATED:
                    currentStatus = "INITIATED";
                    // Extract payment details from event data
                    Map<String, Object> initiationData = parseEventData(event.getEventData());
                    if (initiationData != null) {
                        amount = extractBigDecimal(initiationData, "amount");
                        currency = extractString(initiationData, "currency");
                        merchantId = extractString(initiationData, "merchantId");
                        paymentMethod = extractString(initiationData, "paymentMethod");
                    }
                    break;

                case PAYMENT_VALIDATED:
                    currentStatus = "VALIDATED";
                    break;

                case PAYMENT_AUTHORIZED:
                    currentStatus = "AUTHORIZED";
                    Map<String, Object> authData = parseEventData(event.getEventData());
                    if (authData != null) {
                        processorReference = extractString(authData, "processorReference");
                    }
                    break;

                case PAYMENT_CAPTURED:
                    currentStatus = "CAPTURED";
                    isTerminal = true;
                    break;

                case PAYMENT_SETTLED:
                    currentStatus = "SETTLED";
                    isTerminal = true;
                    break;

                case PAYMENT_FAILED:
                    currentStatus = "FAILED";
                    isTerminal = true;
                    break;

                case PAYMENT_CANCELLED:
                    currentStatus = "CANCELLED";
                    isTerminal = true;
                    break;

                case PAYMENT_REFUNDED:
                    currentStatus = "REFUNDED";
                    isTerminal = true;
                    break;

                case FRAUD_ALERT_TRIGGERED:
                    currentStatus = "FRAUD_BLOCKED";
                    isTerminal = true;
                    break;

                default:
                    // For other events, just track them in metadata
                    metadata.put("last_" + event.getEventType().name().toLowerCase(), event.getTimestamp());
                    break;
            }
        }

        PaymentEvent lastEvent = events.get(events.size() - 1);

        return builder
                .withCurrentStatus(currentStatus)
                .withAmount(amount)
                .withCurrency(currency)
                .withMerchantId(merchantId)
                .withPaymentMethod(paymentMethod)
                .withCreatedAt(createdAt)
                .withLastUpdatedAt(lastEvent.getTimestamp())
                .withProcessorReference(processorReference)
                .withEventHistory(eventHistory)
                .withMetadata(metadata)
                .withIsTerminal(isTerminal)
                .withLastEventId(lastEvent.getEventId())
                .withEventCount(events.size())
                .build();
    }

    /**
     * Reconstruct state for multiple payments
     */
    public Map<String, PaymentStateSnapshot> reconstructMultiplePaymentStates(List<PaymentEvent> events) {
        Map<String, List<PaymentEvent>> eventsByPayment = events.stream()
                .collect(Collectors.groupingBy(PaymentEvent::getPaymentId));

        Map<String, PaymentStateSnapshot> snapshots = new HashMap<>();

        for (Map.Entry<String, List<PaymentEvent>> entry : eventsByPayment.entrySet()) {
            String paymentId = entry.getKey();
            List<PaymentEvent> paymentEvents = entry.getValue();

            // Sort events by timestamp
            paymentEvents.sort(Comparator.comparing(PaymentEvent::getTimestamp));

            try {
                PaymentStateSnapshot snapshot = reconstructPaymentState(paymentEvents);
                snapshots.put(paymentId, snapshot);
            } catch (Exception e) {
                logger.error("Failed to reconstruct state for payment: {}", paymentId, e);
                snapshots.put(paymentId, PaymentStateSnapshot.empty(paymentId));
            }
        }

        return snapshots;
    }

    /**
     * Reconstruct system state from events (for disaster recovery)
     */
    public EventReplayResult reconstructSystemState(List<PaymentEvent> events) {
        logger.info("Reconstructing system state from {} events", events.size());

        Map<String, PaymentStateSnapshot> paymentStates = reconstructMultiplePaymentStates(events);

        // Calculate system-level statistics
        long totalPayments = paymentStates.size();
        long successfulPayments = paymentStates.values().stream()
                .mapToLong(snapshot -> "CAPTURED".equals(snapshot.getCurrentStatus()) ||
                        "SETTLED".equals(snapshot.getCurrentStatus()) ? 1 : 0)
                .sum();
        long failedPayments = paymentStates.values().stream()
                .mapToLong(snapshot -> "FAILED".equals(snapshot.getCurrentStatus()) ? 1 : 0)
                .sum();

        return new EventReplayResult(paymentStates, totalPayments, successfulPayments, failedPayments);
    }

    /**
     * Validate event sequence integrity
     */
    public EventSequenceValidationResult validateEventSequence(List<PaymentEvent> events) {
        List<String> violations = new ArrayList<>();

        if (events.isEmpty()) {
            return new EventSequenceValidationResult(true, violations);
        }

        // Check for required initial event
        if (!events.get(0).getEventType().equals(PaymentEventType.PAYMENT_INITIATED)) {
            violations.add("Payment sequence must start with PAYMENT_INITIATED event");
        }

        // Check for logical event ordering
        Set<PaymentEventType> seenEvents = new HashSet<>();
        for (PaymentEvent event : events) {
            PaymentEventType eventType = event.getEventType();

            // Check for duplicate terminal events
            if (eventType.isTerminal() && seenEvents.stream().anyMatch(PaymentEventType::isTerminal)) {
                violations.add("Multiple terminal events found: " + eventType);
            }

            // Check for events after terminal state
            if (seenEvents.stream().anyMatch(PaymentEventType::isTerminal) && !eventType.isTerminal()) {
                violations.add("Non-terminal event after terminal state: " + eventType);
            }

            seenEvents.add(eventType);
        }

        return new EventSequenceValidationResult(violations.isEmpty(), violations);
    }

    /**
     * Find missing events in a payment sequence
     */
    public List<PaymentEventType> findMissingEvents(List<PaymentEvent> events) {
        Set<PaymentEventType> presentEvents = events.stream()
                .map(PaymentEvent::getEventType)
                .collect(Collectors.toSet());

        List<PaymentEventType> missingEvents = new ArrayList<>();

        // Check for expected event sequences
        if (presentEvents.contains(PaymentEventType.PAYMENT_CAPTURED) &&
                !presentEvents.contains(PaymentEventType.PAYMENT_AUTHORIZED)) {
            missingEvents.add(PaymentEventType.PAYMENT_AUTHORIZED);
        }

        if (presentEvents.contains(PaymentEventType.PAYMENT_SETTLED) &&
                !presentEvents.contains(PaymentEventType.PAYMENT_CAPTURED)) {
            missingEvents.add(PaymentEventType.PAYMENT_CAPTURED);
        }

        return missingEvents;
    }

    /**
     * Parse event data JSON string to Map
     */
    private Map<String, Object> parseEventData(String eventData) {
        if (eventData == null || eventData.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(eventData, Map.class);
        } catch (Exception e) {
            logger.warn("Failed to parse event data: {}", eventData, e);
            return null;
        }
    }

    /**
     * Extract string value from event data map
     */
    private String extractString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extract BigDecimal value from event data map
     */
    private BigDecimal extractBigDecimal(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
                return new BigDecimal(value.toString());
            }
        } catch (Exception e) {
            logger.warn("Failed to extract BigDecimal from value: {}", value, e);
            return null;
        }
    }
}