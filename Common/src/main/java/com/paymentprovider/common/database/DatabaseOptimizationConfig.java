package com.paymentprovider.common.database;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Database optimization configuration for high-performance data access.
 * Implements connection pooling, query optimization, and batch processing.
 */
@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
public class DatabaseOptimizationConfig {

    /**
     * Hibernate performance optimizations
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            // Batch processing optimizations
            hibernateProperties.put("hibernate.jdbc.batch_size", 50);
            hibernateProperties.put("hibernate.order_inserts", true);
            hibernateProperties.put("hibernate.order_updates", true);
            hibernateProperties.put("hibernate.jdbc.batch_versioned_data", true);

            // Connection pool optimizations
            hibernateProperties.put("hibernate.connection.provider_disables_autocommit", true);
            hibernateProperties.put("hibernate.connection.autocommit", false);

            // Query optimizations
            hibernateProperties.put("hibernate.query.plan_cache_max_size", 2048);
            hibernateProperties.put("hibernate.query.plan_parameter_metadata_max_size", 128);
            hibernateProperties.put("hibernate.jdbc.fetch_size", 50);

            // Second-level cache
            hibernateProperties.put("hibernate.cache.use_second_level_cache", true);
            hibernateProperties.put("hibernate.cache.use_query_cache", true);
            hibernateProperties.put("hibernate.cache.region.factory_class",
                    "org.hibernate.cache.jcache.JCacheRegionFactory");

            // Statistics for monitoring
            hibernateProperties.put("hibernate.generate_statistics", true);
        };
    }
}