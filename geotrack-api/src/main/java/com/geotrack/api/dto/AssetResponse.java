package com.geotrack.api.dto;

import com.geotrack.common.model.AssetStatus;
import com.geotrack.common.model.AssetType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for asset data.
 */
public record AssetResponse(
        UUID id,
        String name,
        AssetType type,
        AssetStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
