package com.geotrack.api.repository;

import com.geotrack.api.model.PositionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for position data with PostGIS spatial query support.
 * Uses native SQL with ST_DWithin, ST_Contains etc. for spatial operations.
 */
@ApplicationScoped
public class PositionRepository implements PanacheRepositoryBase<PositionEntity, UUID> {

    @Inject
    EntityManager em;

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
     * Uses DISTINCT ON (PostgreSQL extension) for efficient latest-per-group.
     */
    @SuppressWarnings("unchecked")
    public List<PositionEntity> findLatestPositions() {
        return em.createNativeQuery("""
                SELECT DISTINCT ON (asset_id) *
                FROM positions
                ORDER BY asset_id, timestamp DESC
                """, PositionEntity.class)
                .getResultList();
    }

    /**
     * Find all positions for a specific asset, most recent first.
     */
    public List<PositionEntity> findByAssetId(UUID assetId, int limit) {
        return find("assetId = ?1 ORDER BY timestamp DESC", assetId)
                .page(0, limit)
                .list();
    }

    /**
     * Find positions within a radius of a point using PostGIS ST_DWithin.
     * Uses the spatial GIST index for fast lookups.
     *
     * @param longitude centre longitude
     * @param latitude  centre latitude
     * @param radiusMetres search radius in metres
     * @return positions within the radius
     */
    @SuppressWarnings("unchecked")
    public List<PositionEntity> findWithinRadius(double longitude, double latitude, double radiusMetres) {
        return em.createNativeQuery("""
                SELECT p.* FROM positions p
                WHERE ST_DWithin(
                    p.location::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :radius
                )
                ORDER BY p.timestamp DESC
                LIMIT 1000
                """, PositionEntity.class)
                .setParameter("lon", longitude)
                .setParameter("lat", latitude)
                .setParameter("radius", radiusMetres)
                .getResultList();
    }

    /**
     * Find positions that fall within a geofence polygon using PostGIS ST_Contains.
     *
     * @param geofenceId the geofence to check against
     * @return positions inside the geofence
     */
    @SuppressWarnings("unchecked")
    public List<PositionEntity> findWithinGeofence(UUID geofenceId) {
        return em.createNativeQuery("""
                SELECT p.* FROM positions p
                JOIN geofences g ON g.id = :fenceId
                WHERE g.active = true
                  AND ST_Contains(g.geometry, p.location)
                ORDER BY p.timestamp DESC
                LIMIT 1000
                """, PositionEntity.class)
                .setParameter("fenceId", geofenceId)
                .getResultList();
    }

    /**
     * Find the nearest positions to a point using PostGIS distance ordering.
     * Uses the spatial index via KNN operator (<->).
     *
     * @param longitude reference longitude
     * @param latitude  reference latitude
     * @param limit     max results
     * @return nearest positions ordered by distance
     */
    @SuppressWarnings("unchecked")
    public List<PositionEntity> findNearest(double longitude, double latitude, int limit) {
        return em.createNativeQuery("""
                SELECT p.* FROM positions p
                ORDER BY p.location <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
                LIMIT :lim
                """, PositionEntity.class)
                .setParameter("lon", longitude)
                .setParameter("lat", latitude)
                .setParameter("lim", limit)
                .getResultList();
    }
}
