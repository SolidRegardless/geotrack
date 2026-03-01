package com.geotrack.api.service;

import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.dto.command.SubmitPositionCommand;
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

import java.util.UUID;

/**
 * Command service for position writes (CQRS write side).
 * Handles position ingestion, validation, persistence, and cache updates.
 * Separated from query concerns for clear responsibility boundaries.
 */
@ApplicationScoped
public class PositionCommandService {

    private final PositionRepository positionRepository;
    private final PositionMapper positionMapper;
    private final PositionCacheService cacheService;
    private final ObjectMapper objectMapper;
    private final Counter positionsProcessed;

    @Inject
    public PositionCommandService(PositionRepository positionRepository, PositionMapper positionMapper,
                                   PositionCacheService cacheService, ObjectMapper objectMapper,
                                   MeterRegistry meterRegistry) {
        this.positionRepository = positionRepository;
        this.positionMapper = positionMapper;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.positionsProcessed = Counter.builder("geotrack.positions.commands")
                .description("Total position commands processed")
                .register(meterRegistry);
    }

    /**
     * Process a position submission command.
     */
    @Transactional
    public PositionResponse submit(SubmitPositionCommand command) {
        CoordinateValidator.requireValid(command.latitude(), command.longitude());

        if (CoordinateValidator.isNullIsland(command.latitude(), command.longitude())) {
            throw new IllegalArgumentException("Position at Null Island (0,0) is likely invalid data");
        }

        PositionEntity entity = PositionEntity.fromCoordinates(
                UUID.fromString(command.assetId()),
                command.longitude(),
                command.latitude(),
                command.timestamp()
        );
        entity.altitude = command.altitude();
        entity.speed = command.speed();
        entity.heading = command.heading();
        entity.source = command.source();

        positionRepository.persist(entity);
        positionsProcessed.increment();

        PositionResponse response = positionMapper.toResponse(entity);

        try {
            cacheService.cacheLatestPosition(command.assetId(), objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            Log.warnf("Failed to cache position for asset %s: %s", command.assetId(), e.getMessage());
        }

        return response;
    }
}
