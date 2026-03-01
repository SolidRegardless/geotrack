package com.geotrack.api.dto.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Command DTO for writing a position update (CQRS write model).
 * Contains only the data needed for ingestion, no read-specific fields.
 */
public record SubmitPositionCommand(
        @NotBlank(message = "Asset ID is required")
        String assetId,

        @Min(value = -90, message = "Latitude must be >= -90")
        @Max(value = 90, message = "Latitude must be <= 90")
        double latitude,

        @Min(value = -180, message = "Longitude must be >= -180")
        @Max(value = 180, message = "Longitude must be <= 180")
        double longitude,

        double altitude,
        double speed,

        @Min(value = 0, message = "Heading must be >= 0")
        @Max(value = 360, message = "Heading must be <= 360")
        double heading,

        @NotNull(message = "Timestamp is required")
        Instant timestamp,

        String source
) {}
