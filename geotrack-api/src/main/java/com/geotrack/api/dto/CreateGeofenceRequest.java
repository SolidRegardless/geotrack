package com.geotrack.api.dto;

import com.geotrack.common.model.FenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a geofence.
 * Coordinates are [longitude, latitude] pairs forming a closed polygon.
 */
public record CreateGeofenceRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255)
        String name,

        String description,

        @NotNull(message = "Fence type is required")
        FenceType fenceType,

        @NotNull(message = "Coordinates are required")
        @Size(min = 3, message = "At least 3 coordinate pairs required for a polygon")
        List<double[]> coordinates,

        boolean alertOnEnter,
        boolean alertOnExit
) {
    public CreateGeofenceRequest {
        if (!alertOnEnter && !alertOnExit) {
            // Default: alert on both
            alertOnEnter = true;
            alertOnExit = true;
        }
    }
}
