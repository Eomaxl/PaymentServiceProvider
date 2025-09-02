package com.paymentprovider.common.database;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Database indexing service for optimizing query performance.
 * Creates and manages indexes for high-performance data access.
 */
@Service
public class IndexingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ShardResolver shardResolver;

    /**
     * Initialize indexes on application startup
     */
    @PostConstruct
    public void initializeIndexes() {
        createPaymentIndexes();
        createFraudIndexes();
        createReconciliationIndexes();
        createRoutingIndexes();
    }

    /**
     * Create payment-related indexes
     */
    @Transactional
    public void createPaymentIndexes() {
        List<String> paymentIndexes = List.of(
                // Composite index for payment queries
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_merchant_status_date " +
                        "ON payments (merchant_id, status, created_at DESC)",

                // Index for payment ID lookups
                "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_payment_id " +
                        "ON payments (payment_id)",

                // Index for amount range queries
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_amount_currency " +
                        "ON payments (amount, currency) WHERE status = 'SUCCESS'",

                // Partial index for failed payments
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_failed " +
                        "ON payments (merchant_id, created_at DESC) WHERE status = 'FAILED'",

                // Index for customer lookups
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_customer_id " +
                        "ON payments (customer_id, created_at DESC)",

                // Index for processor routing
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_processor_type " +
                        "ON payments (processor_type, created_at DESC)"
        );

        executeIndexCreation(paymentIndexes, "payment indexes");
    }

    /**
     * Create fraud-related indexes
     */
    @Transactional
    public void createFraudIndexes() {
        List<String> fraudIndexes = List.of(
                // Index for fraud rule lookups
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_fraud_rules_merchant_active " +
                        "ON fraud_rules (merchant_id, is_active, priority DESC)",

                // Index for blacklist lookups
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_blacklist_type_value " +
                        "ON blacklist_entries (entry_type, entry_value)",

                // Index for velocity tracking
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_velocity_tracking " +
                        "ON velocity_tracking (identifier, time_window, created_at DESC)",

                // Index for fraud scores
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_fraud_scores_payment " +
                        "ON fraud_scores (payment_id, created_at DESC)",

                // Composite index for risk analysis
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_fraud_analysis " +
                        "ON fraud_analysis (merchant_id, risk_level, created_at DESC)"
        );

        executeIndexCreation(fraudIndexes, "fraud indexes");
    }

    /**
     * Create reconciliation-related indexes
     */
    @Transactional
    public void createReconciliationIndexes() {
        List<String> reconciliationIndexes = List.of(
                // Index for settlement reports
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_settlement_reports_merchant_date " +
                        "ON settlement_reports (merchant_id, settlement_date DESC)",

                // Index for transaction records
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transaction_records_merchant_date " +
                        "ON transaction_records (merchant_id, transaction_date DESC)",

                // Index for dispute tracking
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_disputes_status_date " +
                        "ON disputes (status, created_at DESC)",

                // Index for reconciliation status
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reconciliation_status " +
                        "ON reconciliation_batches (status, created_at DESC)",

                // Composite index for reporting
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reporting_merchant_period " +
                        "ON transaction_records (merchant_id, transaction_date, processor_type)"
        );

        executeIndexCreation(reconciliationIndexes, "reconciliation indexes");
    }

    /**
     * Create routing-related indexes
     */
    @Transactional
    public void createRoutingIndexes() {
        List<String> routingIndexes = List.of(
                // Index for processor configurations
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_processor_config_enabled " +
                        "ON processor_configs (is_enabled, priority DESC)",

                // Index for routing statistics
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_routing_stats_processor " +
                        "ON routing_statistics (processor_type, created_at DESC)",

                // Index for failover tracking
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_failover_events " +
                        "ON failover_events (processor_type, created_at DESC)",

                // Composite index for routing decisions
                "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_routing_decisions " +
                        "ON routing_decisions (merchant_id, currency, amount_range, created_at DESC)"
        );

        executeIndexCreation(routingIndexes, "routing indexes");
    }

    /**
     * Create partitioned table indexes
     */
    @Transactional
    public void createPartitionedIndexes(String tableName, String partitionColumn) {
        List<String> partitionIndexes = List.of(
                String.format("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_%s_partition " +
                        "ON %s (%s, created_at DESC)", tableName, tableName, partitionColumn),

                String.format("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_%s_status_partition " +
                        "ON %s (status, %s, created_at DESC)", tableName, tableName, partitionColumn)
        );

        executeIndexCreation(partitionIndexes, tableName + " partition indexes");
    }

    /**
     * Execute index creation with error handling
     */
    private void executeIndexCreation(List<String> indexes, String indexType) {
        for (String indexSql : indexes) {
            try {
                jdbcTemplate.execute(indexSql);
                System.out.println("Created " + indexType + ": " + extractIndexName(indexSql));
            } catch (Exception e) {
                System.err.println("Failed to create index: " + indexSql + " - " + e.getMessage());
            }
        }
    }

    /**
     * Extract index name from SQL statement
     */
    private String extractIndexName(String sql) {
        String[] parts = sql.split("\\s+");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("EXISTS".equals(parts[i])) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }

    /**
     * Drop and recreate indexes for maintenance
     */
    @Transactional
    public void recreateIndexes(String tableName) {
        // Drop existing indexes
        String dropSql = "SELECT 'DROP INDEX CONCURRENTLY ' || indexname || ';' " +
                "FROM pg_indexes WHERE tablename = ?";

        List<String> dropStatements = jdbcTemplate.queryForList(dropSql, String.class, tableName);

        for (String dropStatement : dropStatements) {
            try {
                jdbcTemplate.execute(dropStatement);
            } catch (Exception e) {
                System.err.println("Failed to drop index: " + dropStatement + " - " + e.getMessage());
            }
        }

        // Recreate indexes based on table type
        switch (tableName) {
            case "payments":
                createPaymentIndexes();
                break;
            case "fraud_rules":
            case "blacklist_entries":
                createFraudIndexes();
                break;
            case "settlement_reports":
            case "transaction_records":
                createReconciliationIndexes();
                break;
            case "processor_configs":
                createRoutingIndexes();
                break;
        }
    }
}