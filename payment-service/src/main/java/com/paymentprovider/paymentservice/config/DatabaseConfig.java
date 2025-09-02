package com.paymentprovider.paymentservice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Database configuration for connection pooling and transaction management
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.paymentprovider.paymentservice.repository")
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Configure HikariCP connection pool for optimal performance
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Connection pool settings
        config.setMaximumPoolSize(20); // Maximum number of connections in pool
        config.setMinimumIdle(5);      // Minimum number of idle connections
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000);      // 10 minutes
        config.setMaxLifetime(1800000);     // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute

        // Performance optimizations
        config.setAutoCommit(false); // Disable auto-commit for better transaction control

        // PostgreSQL-specific optimizations via data source properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Pool name for monitoring
        config.setPoolName("PaymentServiceHikariCP");

        return new HikariDataSource(config);
    }

    /**
     * Configure JPA transaction manager
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JpaTransactionManager();
    }
}