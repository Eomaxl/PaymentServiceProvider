package com.paymentprovider.paymentservice.services;

import com.paymentprovider.common.batch.BatchProcessingService;
import com.paymentprovider.common.performance.PerformanceOptimizationService;
import com.paymentprovider.paymentservice.domain.Payment;
import com.paymentprovider.paymentservice.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Payment-specific batch processing service.
 * Handles bulk payment operations, reconciliation, and cleanup tasks.
 */
@Service
public class PaymentBatchProcessor {

    @Autowired
    private BatchProcessingService batchProcessingService;

    @Autowired
    private PerformanceOptimizationService performanceService;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Batch process pending payments
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    @Transactional
    public void processPendingPayments() {
        List<Payment> pendingPayments = paymentRepository.findByStatusAndCreatedAtBefore(
                "PENDING", LocalDateTime.now().minusMinutes(5));

        if (!pendingPayments.isEmpty()) {
            CompletableFuture<Integer> batchResult = batchProcessingService.batchUpdate(
                    "UPDATE payments SET status = ?, updated_at = ? WHERE id = ?",
                    pendingPayments,
                    (ps, payment) -> {
                        ps.setString(1, "EXPIRED");
                        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setLong(3, payment.getId());
                    }
            );

            batchResult.thenAccept(count ->
                    System.out.println("Expired " + count + " pending payments"));
        }
    }

    /**
     * Batch insert payment events
     */
    public CompletableFuture<Integer> batchInsertPaymentEvents(List<PaymentEvent> events) {
        String sql = """
            INSERT INTO payment_events (payment_id, event_type, event_data, created_at)
            VALUES (?, ?, ?::jsonb, ?)
            """;

        return batchProcessingService.batchInsert(sql, events,
                (ps, event) -> {
                    ps.setString(1, event.getPaymentId());
                    ps.setString(2, event.getEventType());
                    ps.setString(3, event.getEventData());
                    ps.setTimestamp(4, Timestamp.valueOf(event.getCreatedAt()));
                });
    }

    /**
     * Batch update payment statuses with optimistic locking
     */
    public CompletableFuture<Integer> batchUpdatePaymentStatuses(List<PaymentStatusUpdate> updates) {
        String sql = """
            UPDATE payments 
            SET status = ?, updated_at = ?, version = version + 1
            WHERE id = ? AND version = ?
            """;

        return batchProcessingService.batchUpdate(sql, updates,
                (ps, update) -> {
                    ps.setString(1, update.getNewStatus());
                    ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(3, update.getPaymentId());
                    ps.setInt(4, update.getCurrentVersion());
                });
    }

    /**
     * Bulk payment reconciliation
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void bulkReconciliation() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        // Get unreconciled payments
        List<Payment> unreconciledPayments = paymentRepository
                .findByStatusAndCreatedAtBetween("SUCCESS",
                        yesterday.withHour(0).withMinute(0).withSecond(0),
                        yesterday.withHour(23).withMinute(59).withSecond(59));

        if (!unreconciledPayments.isEmpty()) {
            // Process in parallel batches
            CompletableFuture<BatchProcessingService.BatchResult> result =
                    batchProcessingService.parallelBatchProcess(
                            "UPDATE payments SET reconciliation_status = ? WHERE id = ?",
                            unreconciledPayments,
                            (ps, payment) -> {
                                ps.setString(1, "RECONCILED");
                                ps.setLong(2, payment.getId());
                            },
                            4 // 4 parallel threads
                    );

            result.thenAccept(batchResult ->
                    System.out.println("Reconciled " + batchResult.getProcessedCount() + " payments"));
        }
    }

    /**
     * Archive old payment data
     */
    @Scheduled(cron = "0 0 3 * * SUN") // Weekly on Sunday at 3 AM
    @Transactional
    public void archiveOldPayments() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(12);

        // First, copy to archive table
        String archiveSql = """
            INSERT INTO payments_archive 
            SELECT * FROM payments 
            WHERE created_at < ? AND status IN ('SUCCESS', 'FAILED', 'CANCELLED')
            """;

        CompletableFuture.runAsync(() -> {
            int archivedCount = batchProcessingService.jdbcTemplate
                    .update(archiveSql, Timestamp.valueOf(cutoffDate));

            if (archivedCount > 0) {
                // Then delete from main table
                String deleteSql = """
                    DELETE FROM payments 
                    WHERE created_at < ? AND status IN ('SUCCESS', 'FAILED', 'CANCELLED')
                    """;

                int deletedCount = batchProcessingService.jdbcTemplate
                        .update(deleteSql, Timestamp.valueOf(cutoffDate));

                System.out.println("Archived " + archivedCount + " and deleted " +
                        deletedCount + " old payments");
            }
        });
    }

    /**
     * Batch calculate payment statistics
     */
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    public void calculatePaymentStatistics() {
        String sql = """
            INSERT INTO payment_statistics (merchant_id, date_hour, total_amount, 
                                          transaction_count, success_rate, created_at)
            SELECT 
                merchant_id,
                date_trunc('hour', created_at) as date_hour,
                SUM(amount) as total_amount,
                COUNT(*) as transaction_count,
                (COUNT(CASE WHEN status = 'SUCCESS' THEN 1 END) * 100.0 / COUNT(*)) as success_rate,
                NOW() as created_at
            FROM payments 
            WHERE created_at >= NOW() - INTERVAL '1 hour'
            AND created_at < date_trunc('hour', NOW())
            GROUP BY merchant_id, date_trunc('hour', created_at)
            ON CONFLICT (merchant_id, date_hour) DO UPDATE SET
                total_amount = EXCLUDED.total_amount,
                transaction_count = EXCLUDED.transaction_count,
                success_rate = EXCLUDED.success_rate,
                created_at = EXCLUDED.created_at
            """;

        CompletableFuture.runAsync(() -> {
            int updatedRows = batchProcessingService.jdbcTemplate.update(sql);
            System.out.println("Updated payment statistics for " + updatedRows + " merchant-hour combinations");
        });
    }

    /**
     * Batch fraud score updates
     */
    public CompletableFuture<Integer> batchUpdateFraudScores(List<FraudScoreUpdate> updates) {
        String sql = """
            UPDATE payments 
            SET fraud_score = ?, fraud_risk_level = ?, updated_at = ?
            WHERE payment_id = ?
            """;

        return batchProcessingService.batchUpdate(sql, updates,
                (ps, update) -> {
                    ps.setInt(1, update.getFraudScore());
                    ps.setString(2, update.getRiskLevel());
                    ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setString(4, update.getPaymentId());
                });
    }

    /**
     * Cleanup failed payment attempts
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupFailedAttempts() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

        String sql = "DELETE FROM payment_attempts WHERE status = 'FAILED' AND created_at < ?";

        CompletableFuture.runAsync(() -> {
            int deletedCount = batchProcessingService.jdbcTemplate
                    .update(sql, Timestamp.valueOf(cutoffDate));
            System.out.println("Cleaned up " + deletedCount + " failed payment attempts");
        });
    }

    // DTOs for batch operations
    public static class PaymentEvent {
        private String paymentId;
        private String eventType;
        private String eventData;
        private LocalDateTime createdAt;

        // Constructors, getters, setters
        public PaymentEvent(String paymentId, String eventType, String eventData) {
            this.paymentId = paymentId;
            this.eventType = eventType;
            this.eventData = eventData;
            this.createdAt = LocalDateTime.now();
        }

        public String getPaymentId() { return paymentId; }
        public String getEventType() { return eventType; }
        public String getEventData() { return eventData; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class PaymentStatusUpdate {
        private Long paymentId;
        private String newStatus;
        private Integer currentVersion;

        public PaymentStatusUpdate(Long paymentId, String newStatus, Integer currentVersion) {
            this.paymentId = paymentId;
            this.newStatus = newStatus;
            this.currentVersion = currentVersion;
        }

        public Long getPaymentId() { return paymentId; }
        public String getNewStatus() { return newStatus; }
        public Integer getCurrentVersion() { return currentVersion; }
    }

    public static class FraudScoreUpdate {
        private String paymentId;
        private Integer fraudScore;
        private String riskLevel;

        public FraudScoreUpdate(String paymentId, Integer fraudScore, String riskLevel) {
            this.paymentId = paymentId;
            this.fraudScore = fraudScore;
            this.riskLevel = riskLevel;
        }

        public String getPaymentId() { return paymentId; }
        public Integer getFraudScore() { return fraudScore; }
        public String getRiskLevel() { return riskLevel; }
    }
}
