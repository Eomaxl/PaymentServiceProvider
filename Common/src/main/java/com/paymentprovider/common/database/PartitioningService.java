package com.paymentprovider.common.database;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Database partitioning service for managing large tables.
 * Implements time-based and hash-based partitioning strategies.
 */
@Service
public class PartitioningService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ShardResolver shardResolver;

    private static final DateTimeFormatter PARTITION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM");

    /**
     * Initialize partitions on application startup
     */
    @PostConstruct
    public void initializePartitions() {
        createPaymentPartitions();
        createFraudPartitions();
        createReconciliationPartitions();
        createAuditPartitions();
    }

    /**
     * Create payment table partitions (time-based)
     */
    @Transactional
    public void createPaymentPartitions() {
        // Create master partition table
        String createMasterTable = """
            CREATE TABLE IF NOT EXISTS payments_partitioned (
                id BIGSERIAL,
                payment_id VARCHAR(255) NOT NULL,
                merchant_id VARCHAR(255) NOT NULL,
                amount DECIMAL(19,2) NOT NULL,
                currency VARCHAR(3) NOT NULL,
                status VARCHAR(50) NOT NULL,
                processor_type VARCHAR(100),
                customer_id VARCHAR(255),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id, created_at)
            ) PARTITION BY RANGE (created_at)
            """;

        jdbcTemplate.execute(createMasterTable);

        // Create monthly partitions for current and next 12 months
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        for (int i = 0; i < 12; i++) {
            LocalDate partitionDate = startDate.plusMonths(i);
            createMonthlyPartition("payments_partitioned", partitionDate);
        }
    }

    /**
     * Create fraud table partitions
     */
    @Transactional
    public void createFraudPartitions() {
        // Fraud analysis partitions (time-based)
        String createFraudTable = """
            CREATE TABLE IF NOT EXISTS fraud_analysis_partitioned (
                id BIGSERIAL,
                payment_id VARCHAR(255) NOT NULL,
                merchant_id VARCHAR(255) NOT NULL,
                risk_level VARCHAR(20) NOT NULL,
                risk_score INTEGER NOT NULL,
                analysis_data JSONB,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id, created_at)
            ) PARTITION BY RANGE (created_at)
            """;

        jdbcTemplate.execute(createFraudTable);

        // Create monthly partitions
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        for (int i = 0; i < 12; i++) {
            LocalDate partitionDate = startDate.plusMonths(i);
            createMonthlyPartition("fraud_analysis_partitioned", partitionDate);
        }

        // Velocity tracking partitions (hash-based by identifier)
        String createVelocityTable = """
            CREATE TABLE IF NOT EXISTS velocity_tracking_partitioned (
                id BIGSERIAL,
                identifier VARCHAR(255) NOT NULL,
                time_window VARCHAR(20) NOT NULL,
                count_value BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NOT NULL,
                PRIMARY KEY (id, identifier)
            ) PARTITION BY HASH (identifier)
            """;

        jdbcTemplate.execute(createVelocityTable);

        // Create hash partitions
        for (int i = 0; i < 4; i++) {
            createHashPartition("velocity_tracking_partitioned", i, 4);
        }
    }

    /**
     * Create reconciliation table partitions
     */
    @Transactional
    public void createReconciliationPartitions() {
        // Transaction records partitions (time-based)
        String createTransactionTable = """
            CREATE TABLE IF NOT EXISTS transaction_records_partitioned (
                id BIGSERIAL,
                transaction_id VARCHAR(255) NOT NULL,
                merchant_id VARCHAR(255) NOT NULL,
                amount DECIMAL(19,2) NOT NULL,
                currency VARCHAR(3) NOT NULL,
                processor_type VARCHAR(100) NOT NULL,
                transaction_date DATE NOT NULL,
                settlement_date DATE,
                status VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id, transaction_date)
            ) PARTITION BY RANGE (transaction_date)
            """;

        jdbcTemplate.execute(createTransactionTable);

        // Create monthly partitions
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        for (int i = -12; i < 12; i++) { // Include past 12 months
            LocalDate partitionDate = startDate.plusMonths(i);
            createMonthlyPartition("transaction_records_partitioned", partitionDate);
        }

        // Settlement reports partitions
        String createSettlementTable = """
            CREATE TABLE IF NOT EXISTS settlement_reports_partitioned (
                id BIGSERIAL,
                report_id VARCHAR(255) NOT NULL,
                merchant_id VARCHAR(255) NOT NULL,
                settlement_date DATE NOT NULL,
                total_amount DECIMAL(19,2) NOT NULL,
                currency VARCHAR(3) NOT NULL,
                processor_type VARCHAR(100) NOT NULL,
                status VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id, settlement_date)
            ) PARTITION BY RANGE (settlement_date)
            """;

        jdbcTemplate.execute(createSettlementTable);

        // Create monthly partitions for settlement reports
        for (int i = -12; i < 12; i++) {
            LocalDate partitionDate = startDate.plusMonths(i);
            createMonthlyPartition("settlement_reports_partitioned", partitionDate);
        }
    }

    /**
     * Create audit table partitions
     */
    @Transactional
    public void createAuditPartitions() {
        String createAuditTable = """
            CREATE TABLE IF NOT EXISTS audit_logs_partitioned (
                id BIGSERIAL,
                event_type VARCHAR(100) NOT NULL,
                entity_type VARCHAR(100) NOT NULL,
                entity_id VARCHAR(255) NOT NULL,
                user_id VARCHAR(255),
                changes JSONB,
                ip_address INET,
                user_agent TEXT,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id, created_at)
            ) PARTITION BY RANGE (created_at)
            """;

        jdbcTemplate.execute(createAuditTable);

        // Create daily partitions for audit logs (high volume)
        LocalDate startDate = LocalDate.now();
        for (int i = -30; i < 30; i++) { // 30 days past and future
            LocalDate partitionDate = startDate.plusDays(i);
            createDailyPartition("audit_logs_partitioned", partitionDate);
        }
    }

    /**
     * Create monthly partition
     */
    private void createMonthlyPartition(String tableName, LocalDate partitionDate) {
        String partitionName = tableName + "_" + partitionDate.format(PARTITION_DATE_FORMAT);
        LocalDate startDate = partitionDate.withDayOfMonth(1);
        LocalDate endDate = startDate.plusMonths(1);

        String createPartitionSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s PARTITION OF %s
            FOR VALUES FROM ('%s') TO ('%s')
            """, partitionName, tableName, startDate, endDate);

        try {
            jdbcTemplate.execute(createPartitionSql);
            System.out.println("Created monthly partition: " + partitionName);
        } catch (Exception e) {
            System.err.println("Failed to create partition " + partitionName + ": " + e.getMessage());
        }
    }

    /**
     * Create daily partition
     */
    private void createDailyPartition(String tableName, LocalDate partitionDate) {
        String partitionName = tableName + "_" + partitionDate.format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        LocalDate endDate = partitionDate.plusDays(1);

        String createPartitionSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s PARTITION OF %s
            FOR VALUES FROM ('%s') TO ('%s')
            """, partitionName, tableName, partitionDate, endDate);

        try {
            jdbcTemplate.execute(createPartitionSql);
            System.out.println("Created daily partition: " + partitionName);
        } catch (Exception e) {
            System.err.println("Failed to create partition " + partitionName + ": " + e.getMessage());
        }
    }

    /**
     * Create hash partition
     */
    private void createHashPartition(String tableName, int partitionIndex, int totalPartitions) {
        String partitionName = tableName + "_hash_" + partitionIndex;

        String createPartitionSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s PARTITION OF %s
            FOR VALUES WITH (modulus %d, remainder %d)
            """, partitionName, tableName, totalPartitions, partitionIndex);

        try {
            jdbcTemplate.execute(createPartitionSql);
            System.out.println("Created hash partition: " + partitionName);
        } catch (Exception e) {
            System.err.println("Failed to create hash partition " + partitionName + ": " + e.getMessage());
        }
    }

    /**
     * Scheduled task to create future partitions
     */
    @Scheduled(cron = "0 0 1 * * ?") // Run on 1st day of each month
    @Transactional
    public void createFuturePartitions() {
        LocalDate futureDate = LocalDate.now().plusMonths(12);

        // Create future partitions for all partitioned tables
        createMonthlyPartition("payments_partitioned", futureDate);
        createMonthlyPartition("fraud_analysis_partitioned", futureDate);
        createMonthlyPartition("transaction_records_partitioned", futureDate);
        createMonthlyPartition("settlement_reports_partitioned", futureDate);

        // Create future daily partitions for audit logs
        for (int i = 30; i < 60; i++) {
            LocalDate auditDate = LocalDate.now().plusDays(i);
            createDailyPartition("audit_logs_partitioned", auditDate);
        }
    }

    /**
     * Scheduled task to drop old partitions
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run on 2nd day of each month
    @Transactional
    public void dropOldPartitions() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(24); // Keep 24 months

        String findOldPartitionsSql = """
            SELECT schemaname, tablename 
            FROM pg_tables 
            WHERE tablename LIKE '%_partitioned_%' 
            AND tablename < ?
            """;

        String cutoffString = cutoffDate.format(PARTITION_DATE_FORMAT);

        List<String> oldPartitions = jdbcTemplate.queryForList(
                findOldPartitionsSql, String.class, cutoffString);

        for (String partition : oldPartitions) {
            try {
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + partition);
                System.out.println("Dropped old partition: " + partition);
            } catch (Exception e) {
                System.err.println("Failed to drop partition " + partition + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get partition statistics
     */
    public List<PartitionInfo> getPartitionStatistics() {
        String sql = """
            SELECT 
                schemaname,
                tablename,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
                (SELECT count(*) FROM information_schema.tables WHERE table_name = tablename) as row_count
            FROM pg_tables 
            WHERE tablename LIKE '%_partitioned_%'
            ORDER BY tablename
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new PartitionInfo(
                rs.getString("schemaname"),
                rs.getString("tablename"),
                rs.getString("size"),
                rs.getLong("row_count")
        ));
    }

    /**
     * Partition information DTO
     */
    public static class PartitionInfo {
        private final String schema;
        private final String tableName;
        private final String size;
        private final long rowCount;

        public PartitionInfo(String schema, String tableName, String size, long rowCount) {
            this.schema = schema;
            this.tableName = tableName;
            this.size = size;
            this.rowCount = rowCount;
        }

        // Getters
        public String getSchema() { return schema; }
        public String getTableName() { return tableName; }
        public String getSize() { return size; }
        public long getRowCount() { return rowCount; }
    }
}