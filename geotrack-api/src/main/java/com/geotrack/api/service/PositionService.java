package com.geotrack.api.service;

import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.dto.SubmitPositionRequest;
import com.geotrack.api.mapper.PositionMapper;
import com.geotrack.api.model.PositionEntity;
import com.geotrack.api.repository.PositionRepository;
import com.geotrack.common.validation.CoordinateValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for position ingestion and retrieval.
 * Uses Redis caching for latest positions per asset.
 */
@ApplicationScoped
public class PositionService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final PositionCacheService cacheService;
    private final ObjectMapper objectMapper;
    private final Counter positionsProcessed;

    @Inject
    public PositionService(PositionRepository positionRepository, PositionMapper positionMapper,
                           PositionCacheService cacheService, ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.positionRepository = positionRepository;
        this.positionMapper = positionMapper;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
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

        PositionResponse response = positionMapper.toResponse(entity);

        // Cache latest position in Redis
        try {
            cacheService.cacheLatestPosition(request.assetId(), objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            Log.warnf("Failed to cache position for asset %s: %s", request.assetId(), e.getMessage());
        }

        return response;
    }

    public List<PositionResponse> getLatestPositions() {
        return positionRepository.findLatestPositions()
                .stream()
                .map(positionMapper::toResponse)
                .toList();
    }

    public List<PositionResponse> getPositionHistory(UUID assetId, Instant from, Instant to, int limit) {
        if (from == null) from = Instant.now().minusSeconds(86400); // Default: last 24h
        if (to == null) to = Instant.now();

        return positionRepository.findByAssetAndTimeRange(assetId, from, to, limit)
                .stream()
                .map(positionMapper::toResponse)
                .toList();
    }
}
