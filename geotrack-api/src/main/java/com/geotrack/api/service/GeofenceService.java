package com.geotrack.api.service;

import com.geotrack.api.dto.CreateGeofenceRequest;
import com.geotrack.api.dto.GeofenceResponse;
import com.geotrack.api.mapper.GeofenceMapper;
import com.geotrack.api.model.GeofenceEntity;
import com.geotrack.api.repository.GeofenceRepository;
import com.geotrack.common.spatial.SpatialEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;
    private final GeofenceMapper geofenceMapper;
    private final SpatialEngine spatialEngine;

    @Inject
    public GeofenceService(GeofenceRepository geofenceRepository, GeofenceMapper geofenceMapper) {
        this.geofenceRepository = geofenceRepository;
        this.geofenceMapper = geofenceMapper;
        this.spatialEngine = new SpatialEngine();
    }

    public List<GeofenceResponse> findAll() {
        return geofenceRepository.findAllGeofences().stream()
                .map(geofenceMapper::toResponse)
                .toList();
    }

    public List<GeofenceResponse> findActive() {
        return geofenceRepository.findActive().stream()
                .map(geofenceMapper::toResponse)
                .toList();
    }

    public GeofenceResponse findById(UUID id) {
        return geofenceRepository.findByIdOptional(id)
                .map(geofenceMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Geofence not found: " + id));
    }

    @Transactional
    public GeofenceResponse create(CreateGeofenceRequest request) {
        GeofenceEntity entity = new GeofenceEntity();
        entity.name = request.name();
        entity.description = request.description();
        entity.fenceType = request.fenceType();
        entity.geometry = spatialEngine.createPolygon(request.coordinates());
        entity.alertOnEnter = request.alertOnEnter();
        entity.alertOnExit = request.alertOnExit();

        geofenceRepository.persist(entity);
        return geofenceMapper.toResponse(entity);
    }

    @Transactional
    public void delete(UUID id) {
        GeofenceEntity entity = geofenceRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("Geofence not found: " + id));
        entity.active = false;
        geofenceRepository.persist(entity);
    }
}
