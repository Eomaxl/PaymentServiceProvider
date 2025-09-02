package com.paymentprovider.paymentservice.event.repository;

import org.springframework.stereotype.Repository;
import com.paymentprovider.paymentservice.event.PaymentEvent;
import com.paymentprovider.paymentservice.event.PaymentEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentEvent entity.
 * Provides methods for event store operations including event retrieval and replay capabilities.
 */
@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, String> {

    /**
     * Find all events for a specific payment ordered by timestamp
     */
    List<PaymentEvent> findByPaymentIdOrderByTimestampAsc(String paymentId);

    /**
     * Find events for a payment within a specific time range
     */
    List<PaymentEvent> findByPaymentIdAndTimestampBetweenOrderByTimestampAsc(
            String paymentId, Instant startTime, Instant endTime);

    /**
     * Find events by type for a specific payment
     */
    List<PaymentEvent> findByPaymentIdAndEventTypeOrderByTimestampAsc(
            String paymentId, PaymentEventType eventType);

    /**
     * Find the latest event for a payment
     */
    Optional<PaymentEvent> findFirstByPaymentIdOrderByTimestampDesc(String paymentId);

    /**
     * Find events by correlation ID for distributed tracing
     */
    List<PaymentEvent> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    /**
     * Find events by event type within time range
     */
    List<PaymentEvent> findByEventTypeAndTimestampBetweenOrderByTimestampAsc(
            PaymentEventType eventType, Instant startTime, Instant endTime);

    /**
     * Find events for multiple payments
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.paymentId IN :paymentIds ORDER BY e.timestamp ASC")
    List<PaymentEvent> findByPaymentIds(@Param("paymentIds") List<String> paymentIds);

    /**
     * Find events after a specific timestamp for replay scenarios
     */
    List<PaymentEvent> findByTimestampAfterOrderByTimestampAsc(Instant timestamp);

    /**
     * Find events by user ID for audit purposes
     */
    Page<PaymentEvent> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Count events by type for analytics
     */
    @Query("SELECT COUNT(e) FROM PaymentEvent e WHERE e.eventType = :eventType AND e.timestamp BETWEEN :startTime AND :endTime")
    long countByEventTypeAndTimestampBetween(
            @Param("eventType") PaymentEventType eventType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find events for replay from a specific version
     */
    @Query("SELECT e FROM PaymentEvent e WHERE e.paymentId = :paymentId AND e.version >= :fromVersion ORDER BY e.timestamp ASC")
    List<PaymentEvent> findEventsForReplay(
            @Param("paymentId") String paymentId,
            @Param("fromVersion") String fromVersion);

    /**
     * Check if payment has any events of specific types
     */
    @Query("SELECT COUNT(e) > 0 FROM PaymentEvent e WHERE e.paymentId = :paymentId AND e.eventType IN :eventTypes")
    boolean existsByPaymentIdAndEventTypeIn(
            @Param("paymentId") String paymentId,
            @Param("eventTypes") List<PaymentEventType> eventTypes);

    /**
     * Find the first event of a specific type for a payment
     */
    Optional<PaymentEvent> findFirstByPaymentIdAndEventTypeOrderByTimestampAsc(
            String paymentId, PaymentEventType eventType);

    /**
     * Delete old events for cleanup (use with caution)
     */
    void deleteByTimestampBefore(Instant cutoffTime);
}
