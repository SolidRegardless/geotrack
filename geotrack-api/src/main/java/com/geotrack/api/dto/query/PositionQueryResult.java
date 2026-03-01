package com.geotrack.api.dto.query;

import java.time.Instant;
import java.util.UUID;

/**
 * Query DTO for reading position data (CQRS read model).
 * Optimised for display — includes computed/derived fields
 * that are not part of the write model.
 */
public record PositionQueryResult(
        UUID id,
        String assetId,
        double latitude,
        double longitude,
        double altitude,
        double speed,
        double heading,
        Instant timestamp,
        String source,
        Instant receivedAt
) {}
