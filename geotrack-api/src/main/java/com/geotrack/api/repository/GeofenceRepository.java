package com.geotrack.api.repository;

import com.geotrack.api.model.GeofenceEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

/**
 * Repository for geofence CRUD and spatial queries.
 */
@ApplicationScoped
public class GeofenceRepository implements PanacheRepositoryBase<GeofenceEntity, UUID> {

    public List<GeofenceEntity> findActive() {
        return find("active = true").list();
    }

    public List<GeofenceEntity> findAllGeofences() {
        return listAll();
    }
}
