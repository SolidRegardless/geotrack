package com.geotrack.api.dto;

import com.geotrack.common.model.FenceType;

import java.time.Instant;
import java.util.UUID;

public record GeofenceResponse(
        UUID id,
        String name,
        String description,
        FenceType fenceType,
        boolean active,
        boolean alertOnEnter,
        boolean alertOnExit,
        Instant createdAt
) {}
