package com.paymentprovider.common.database;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * Shard resolver for routing database operations to appropriate shards.
 * Implements merchant-based and date-based sharding strategies.
 */
@Component
public class ShardResolver {

    private final Map<String, JdbcTemplate> shardMap;
    private final JdbcTemplate readReplicaTemplate;

    public ShardResolver(Map<String, JdbcTemplate> shardMap, JdbcTemplate readReplicaTemplate) {
        this.shardMap = shardMap;
        this.readReplicaTemplate = readReplicaTemplate;
    }

    /**
     * Resolve shard based on merchant ID
     */
    public JdbcTemplate resolveShardByMerchant(String merchantId) {
        int shardIndex = Math.abs(merchantId.hashCode()) % 3;
        String shardKey = "shard" + (shardIndex + 1);
        return shardMap.get(shardKey);
    }

    /**
     * Resolve shard based on date (for time-series data)
     */
    public JdbcTemplate resolveShardByDate(LocalDate date) {
        int shardIndex = date.getYear() % 3;
        String shardKey = "shard" + (shardIndex + 1);
        return shardMap.get(shardKey);
    }

    /**
     * Resolve shard based on payment ID
     */
    public JdbcTemplate resolveShardByPaymentId(String paymentId) {
        int shardIndex = Math.abs(paymentId.hashCode()) % 3;
        String shardKey = "shard" + (shardIndex + 1);
        return shardMap.get(shardKey);
    }

    /**
     * Get read replica for read-heavy operations
     */
    public JdbcTemplate getReadReplica() {
        return readReplicaTemplate;
    }

    /**
     * Get all shards for cross-shard queries
     */
    public Map<String, JdbcTemplate> getAllShards() {
        return shardMap;
    }

    /**
     * Resolve shard with custom strategy
     */
    public JdbcTemplate resolveShardCustom(String key, ShardingStrategy strategy) {
        return strategy.resolveShard(key, shardMap);
    }

    /**
     * Interface for custom sharding strategies
     */
    public interface ShardingStrategy {
        JdbcTemplate resolveShard(String key, Map<String, JdbcTemplate> shardMap);
    }
}