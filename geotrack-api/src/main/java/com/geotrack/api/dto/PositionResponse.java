package com.geotrack.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for position data.
 */
public record PositionResponse(
        UUID id,
        String assetId,
        double latitude,
        double longitude,
        double altitude,
        double speed,
        double heading,
        Instant timestamp,
        String source
) {}
