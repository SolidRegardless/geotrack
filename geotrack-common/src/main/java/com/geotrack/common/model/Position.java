package com.geotrack.common.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a validated geographic position with metadata.
 * Uses Java 21 records for automatic equals/hashCode/toString.
 *
 * @param id        Unique position identifier
 * @param assetId   The asset this position belongs to
 * @param latitude  WGS84 latitude (-90 to 90)
 * @param longitude WGS84 longitude (-180 to 180)
 * @param altitude  Altitude in metres above sea level
 * @param speed     Speed in km/h
 * @param heading   Heading in degrees (0-360, 0 = North)
 * @param timestamp When this position was recorded
 * @param source    Source of the position data
 */
public record Position(
        UUID id,
        String assetId,
        double latitude,
        double longitude,
        double altitude,
        double speed,
        double heading,
        Instant timestamp,
        PositionSource source
) {
    /**
     * Compact constructor — validates invariants at construction time.
     * If any constraint is violated, the object simply cannot exist.
     */
    public Position {
        Objects.requireNonNull(assetId, "assetId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(
                    "Latitude must be between -90 and 90, got: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(
                    "Longitude must be between -180 and 180, got: " + longitude);
        }
        if (heading < 0 || heading > 360) {
            throw new IllegalArgumentException(
                    "Heading must be between 0 and 360, got: " + heading);
        }
    }

    /**
     * Convenience factory for creating a position with defaults.
     */
    public static Position of(String assetId, double latitude, double longitude, Instant timestamp) {
        return new Position(
                UUID.randomUUID(), assetId, latitude, longitude,
                0.0, 0.0, 0.0, timestamp, PositionSource.GPS
        );
    }
}
