package com.geotrack.api.dto;

import com.geotrack.common.model.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new asset.
 * Bean Validation annotations enforce constraints before business logic.
 */
public record CreateAssetRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Name must be 2-255 characters")
        String name,

        @NotNull(message = "Asset type is required")
        AssetType type,

        String metadata
) {}
