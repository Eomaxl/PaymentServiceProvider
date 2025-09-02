package com.paymentprovider.paymentservice.repository;

import com.paymentprovider.paymentservice.domain.Payment;
import com.paymentprovider.paymentservice.domain.PaymentMethod;
import com.paymentprovider.paymentservice.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity operations
 * Provides custom query methods for payment retrieval and filtering
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Find payments by merchant ID with pagination
     */
    Page<Payment> findByMerchantId(String merchantId, Pageable pageable);

    /**
     * Find payments by merchant ID and status
     */
    List<Payment> findByMerchantIdAndStatus(String merchantId, PaymentStatus status);

    /**
     * Find payments by customer ID
     */
    List<Payment> findByCustomerId(String customerId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by payment method
     */
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    /**
     * Find payments created within a date range
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<Payment> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    /**
     * Find payments by merchant ID within a date range
     */
    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<Payment> findByMerchantIdAndCreatedAtBetween(
            @Param("merchantId") String merchantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find payments by merchant ID and status within a date range
     */
    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<Payment> findByMerchantIdAndStatusAndCreatedAtBetween(
            @Param("merchantId") String merchantId,
            @Param("status") PaymentStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find payments by amount range
     */
    @Query("SELECT p FROM Payment p WHERE p.amount >= :minAmount AND p.amount <= :maxAmount")
    List<Payment> findByAmountBetween(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find payments by processor reference
     */
    Optional<Payment> findByProcessorReference(String processorReference);

    /**
     * Find expired payments that are still pending
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.expiresAt < :currentTime")
    List<Payment> findExpiredPayments(@Param("status") PaymentStatus status, @Param("currentTime") Instant currentTime);

    /**
     * Count payments by merchant ID and status
     */
    long countByMerchantIdAndStatus(String merchantId, PaymentStatus status);

    /**
     * Count payments by status within a date range
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    long countByStatusAndCreatedAtBetween(
            @Param("status") PaymentStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Calculate total amount by merchant ID and status
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status")
    BigDecimal sumAmountByMerchantIdAndStatus(@Param("merchantId") String merchantId, @Param("status") PaymentStatus status);

    /**
     * Calculate total amount by merchant ID within a date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.merchantId = :merchantId AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    BigDecimal sumAmountByMerchantIdAndCreatedAtBetween(
            @Param("merchantId") String merchantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );

    /**
     * Find payments that need webhook retry
     */
    @Query("SELECT p FROM Payment p WHERE p.webhookUrl IS NOT NULL AND p.status IN :statuses")
    List<Payment> findPaymentsForWebhookNotification(@Param("statuses") List<PaymentStatus> statuses);

    /**
     * Find payments by multiple statuses with pagination
     */
    Page<Payment> findByStatusIn(List<PaymentStatus> statuses, Pageable pageable);

    /**
     * Find recent payments by merchant ID (last 24 hours)
     */
    @Query("SELECT p FROM Payment p WHERE p.merchantId = :merchantId AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPaymentsByMerchantId(@Param("merchantId") String merchantId, @Param("since") Instant since);

    /**
     * Check if payment exists by merchant ID and external reference
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.merchantId = :merchantId AND p.metadata['external_reference'] = :externalReference")
    boolean existsByMerchantIdAndExternalReference(@Param("merchantId") String merchantId, @Param("externalReference") String externalReference);
}
