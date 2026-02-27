package com.geotrack.api.service;

import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.dto.SubmitPositionRequest;
import com.geotrack.api.model.PositionEntity;
import com.geotrack.api.repository.PositionRepository;
import com.geotrack.common.validation.CoordinateValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for position ingestion and retrieval.
 */
@ApplicationScoped
public class PositionService {

    private final PositionRepository positionRepository;
    private final Counter positionsProcessed;

    @Inject
    public PositionService(PositionRepository positionRepository, MeterRegistry meterRegistry) {
        this.positionRepository = positionRepository;
        this.positionsProcessed = Counter.builder("geotrack.positions.processed")
                .description("Total positions processed")
                .register(meterRegistry);
    }

    @Transactional
    public PositionResponse submit(SubmitPositionRequest request) {
        // Validate coordinates
        CoordinateValidator.requireValid(request.latitude(), request.longitude());

        if (CoordinateValidator.isNullIsland(request.latitude(), request.longitude())) {
            throw new IllegalArgumentException("Position at Null Island (0,0) is likely invalid data");
        }

        // Create and persist entity
        PositionEntity entity = PositionEntity.fromCoordinates(
                UUID.fromString(request.assetId()),
                request.longitude(),
                request.latitude(),
                request.timestamp()
        );
        entity.altitude = request.altitude();
        entity.speed = request.speed();
        entity.heading = request.heading();

        positionRepository.persist(entity);
        positionsProcessed.increment();

        return toResponse(entity);
    }

    public List<PositionResponse> getLatestPositions() {
        return positionRepository.findLatestPositions()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PositionResponse> getPositionHistory(UUID assetId, Instant from, Instant to, int limit) {
        if (from == null) from = Instant.now().minusSeconds(86400); // Default: last 24h
        if (to == null) to = Instant.now();

        return positionRepository.findByAssetAndTimeRange(assetId, from, to, limit)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PositionResponse toResponse(PositionEntity entity) {
        return new PositionResponse(
                entity.id,
                entity.assetId.toString(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.altitude != null ? entity.altitude : 0,
                entity.speed != null ? entity.speed : 0,
                entity.heading != null ? entity.heading : 0,
                entity.timestamp,
                entity.source
        );
    }
}
