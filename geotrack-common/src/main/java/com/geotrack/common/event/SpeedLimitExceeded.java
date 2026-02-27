package com.geotrack.common.event;

import com.geotrack.common.model.Position;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an asset exceeds a configured speed threshold.
 */
public record SpeedLimitExceeded(
        UUID eventId,
        String assetId,
        Instant occurredAt,
        double currentSpeedKmh,
        double limitKmh,
        Position position
) implements TrackingEvent {}
