package com.geotrack.api.service;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import java.time.Duration;

/**
 * Redis-backed cache for latest asset positions.
 * Provides sub-millisecond reads for the latest position per asset,
 * avoiding repeated database queries for the hot path.
 */
@ApplicationScoped
public class PositionCacheService {

    private static final String KEY_PREFIX = "geotrack:position:latest:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final ValueCommands<String, String> valueCommands;

    @Inject
    public PositionCacheService(RedisDataSource redisDataSource) {
        this.valueCommands = redisDataSource.value(String.class, String.class);
    }

    /**
     * Cache the latest position JSON for an asset.
     */
    public void cacheLatestPosition(String assetId, String positionJson) {
        try {
            valueCommands.setex(KEY_PREFIX + assetId, TTL.getSeconds(), positionJson);
        } catch (Exception e) {
            Log.warnf("Redis cache write failed for asset %s: %s", assetId, e.getMessage());
        }
    }

    /**
     * Retrieve cached latest position for an asset.
     *
     * @return position JSON or null if not cached
     */
    public String getCachedPosition(String assetId) {
        try {
            return valueCommands.get(KEY_PREFIX + assetId);
        } catch (Exception e) {
            Log.warnf("Redis cache read failed for asset %s: %s", assetId, e.getMessage());
            return null;
        }
    }

    /**
     * Evict cached position for an asset.
     */
    public void evict(String assetId) {
        try {
            valueCommands.getdel(KEY_PREFIX + assetId);
        } catch (Exception e) {
            Log.warnf("Redis cache evict failed for asset %s: %s", assetId, e.getMessage());
        }
    }
}
