package com.geotrack.api.dto;

import com.geotrack.common.model.Severity;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID assetId,
        UUID geofenceId,
        String alertType,
        Severity severity,
        String message,
        boolean acknowledged,
        String acknowledgedBy,
        Instant acknowledgedAt,
        Instant createdAt
) {}
