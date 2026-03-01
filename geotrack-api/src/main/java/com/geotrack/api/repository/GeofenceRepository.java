package com.geotrack.api.repository;

import com.geotrack.api.model.GeofenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Repository for geofence CRUD and PostGIS spatial queries.
 */
@ApplicationScoped
public class GeofenceRepository implements PanacheRepositoryBase<GeofenceEntity, UUID> {

    @Inject
    EntityManager em;

    public List<GeofenceEntity> findActive() {
        return find("active = true").list();
    }

    public List<GeofenceEntity> findAllGeofences() {
        return listAll();
    }

    /**
     * Find all active geofences that contain a given point using PostGIS ST_Contains.
     *
     * @param longitude point longitude
     * @param latitude  point latitude
     * @return geofences containing the point
     */
    @SuppressWarnings("unchecked")
    public List<GeofenceEntity> findContainingPoint(double longitude, double latitude) {
        return em.createNativeQuery("""
                SELECT g.* FROM geofences g
                WHERE g.active = true
                  AND ST_Contains(g.geometry, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
                """, GeofenceEntity.class)
                .setParameter("lon", longitude)
                .setParameter("lat", latitude)
                .getResultList();
    }

    /**
     * Find geofences that intersect with a given geometry using PostGIS ST_Intersects.
     *
     * @param longitude centre longitude
     * @param latitude  centre latitude
     * @param radiusMetres search radius in metres
     * @return geofences within the search area
     */
    @SuppressWarnings("unchecked")
    public List<GeofenceEntity> findNearby(double longitude, double latitude, double radiusMetres) {
        return em.createNativeQuery("""
                SELECT g.* FROM geofences g
                WHERE g.active = true
                  AND ST_DWithin(
                      g.geometry::geography,
                      ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                      :radius
                  )
                """, GeofenceEntity.class)
                .setParameter("lon", longitude)
                .setParameter("lat", latitude)
                .setParameter("radius", radiusMetres)
                .getResultList();
    }
}
