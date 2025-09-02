package com.paymentprovider.common.database;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Database sharding configuration for horizontal scaling.
 * Implements merchant-based and date-based sharding strategies.
 */
@Configuration
public class ShardingConfig {

    /**
     * Primary datasource for read operations
     */
    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Shard 1 - For merchants with ID hash % 3 == 0
     */
    @Bean(name = "shard1DataSource")
    @ConfigurationProperties("spring.datasource.shard1")
    public DataSource shard1DataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Shard 2 - For merchants with ID hash % 3 == 1
     */
    @Bean(name = "shard2DataSource")
    @ConfigurationProperties("spring.datasource.shard2")
    public DataSource shard2DataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Shard 3 - For merchants with ID hash % 3 == 2
     */
    @Bean(name = "shard3DataSource")
    @ConfigurationProperties("spring.datasource.shard3")
    public DataSource shard3DataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Read replica for read-heavy operations
     */
    @Bean(name = "readReplicaDataSource")
    @ConfigurationProperties("spring.datasource.read-replica")
    public DataSource readReplicaDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * JDBC templates for each shard
     */
    @Bean(name = "primaryJdbcTemplate")
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shard1JdbcTemplate")
    public JdbcTemplate shard1JdbcTemplate(@Qualifier("shard1DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shard2JdbcTemplate")
    public JdbcTemplate shard2JdbcTemplate(@Qualifier("shard2DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shard3JdbcTemplate")
    public JdbcTemplate shard3JdbcTemplate(@Qualifier("shard3DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "readReplicaJdbcTemplate")
    public JdbcTemplate readReplicaJdbcTemplate(@Qualifier("readReplicaDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Shard resolver for routing queries to appropriate shards
     */
    @Bean
    public ShardResolver shardResolver() {
        Map<String, JdbcTemplate> shardMap = new HashMap<>();
        shardMap.put("shard1", shard1JdbcTemplate(shard1DataSource()));
        shardMap.put("shard2", shard2JdbcTemplate(shard2DataSource()));
        shardMap.put("shard3", shard3JdbcTemplate(shard3DataSource()));

        return new ShardResolver(shardMap, readReplicaJdbcTemplate(readReplicaDataSource()));
    }
}