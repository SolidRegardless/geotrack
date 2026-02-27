package com.geotrack.api.repository;

import com.geotrack.api.model.PositionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for position data with spatial query support.
 */
@ApplicationScoped
public class PositionRepository implements PanacheRepositoryBase<PositionEntity, UUID> {

    /**
     * Find position history for an asset within a time range.
     */
    public List<PositionEntity> findByAssetAndTimeRange(UUID assetId, Instant from, Instant to, int limit) {
        return find("assetId = ?1 AND timestamp BETWEEN ?2 AND ?3 ORDER BY timestamp DESC",
                assetId, from, to)
                .page(0, limit)
                .list();
    }

    /**
     * Find the latest position for each asset.
     */
    public List<PositionEntity> findLatestPositions() {
        return find("""
                SELECT p FROM PositionEntity p
                WHERE p.timestamp = (
                    SELECT MAX(p2.timestamp) FROM PositionEntity p2
                    WHERE p2.assetId = p.assetId
                )
                ORDER BY p.timestamp DESC
                """).list();
    }

    /**
     * Find all positions for a specific asset, most recent first.
     */
    public List<PositionEntity> findByAssetId(UUID assetId, int limit) {
        return find("assetId = ?1 ORDER BY timestamp DESC", assetId)
                .page(0, limit)
                .list();
    }
}
