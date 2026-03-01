package com.geotrack.api.service;

import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.dto.query.PositionQueryResult;
import com.geotrack.api.mapper.PositionMapper;
import com.geotrack.api.model.PositionEntity;
import com.geotrack.api.repository.PositionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Query service for position reads (CQRS read side).
 * Optimised for retrieval — uses caching and read-specific DTOs.
 * Separated from command concerns for clear responsibility boundaries.
 */
@ApplicationScoped
public class PositionQueryService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;

    @Inject
    public PositionQueryService(PositionRepository positionRepository, PositionMapper positionMapper) {
        this.positionRepository = positionRepository;
        this.positionMapper = positionMapper;
    }

    /**
     * Get latest position per asset (read-optimised).
     */
    public List<PositionResponse> getLatestPositions() {
        return positionRepository.findLatestPositions()
                .stream()
                .map(positionMapper::toResponse)
                .toList();
    }

    /**
     * Get position history for a specific asset within a time range.
     */
    public List<PositionResponse> getPositionHistory(UUID assetId, Instant from, Instant to, int limit) {
        if (from == null) from = Instant.now().minusSeconds(86400);
        if (to == null) to = Instant.now();

        return positionRepository.findByAssetAndTimeRange(assetId, from, to, limit)
                .stream()
                .map(positionMapper::toResponse)
                .toList();
    }

    /**
     * Query positions within a spatial radius (PostGIS).
     */
    public List<PositionQueryResult> findWithinRadius(double longitude, double latitude, double radiusMetres) {
        return positionRepository.findWithinRadius(longitude, latitude, radiusMetres)
                .stream()
                .map(this::toQueryResult)
                .toList();
    }

    /**
     * Query nearest positions to a point (PostGIS KNN).
     */
    public List<PositionQueryResult> findNearest(double longitude, double latitude, int limit) {
        return positionRepository.findNearest(longitude, latitude, limit)
                .stream()
                .map(this::toQueryResult)
                .toList();
    }

    private PositionQueryResult toQueryResult(PositionEntity entity) {
        return new PositionQueryResult(
                entity.id,
                entity.source != null && !entity.source.isBlank() ? entity.source : entity.assetId.toString(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.altitude != null ? entity.altitude : 0,
                entity.speed != null ? entity.speed : 0,
                entity.heading != null ? entity.heading : 0,
                entity.timestamp,
                entity.source,
                entity.receivedAt
        );
    }
}
