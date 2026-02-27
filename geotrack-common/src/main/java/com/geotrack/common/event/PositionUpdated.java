package com.geotrack.common.event;

import com.geotrack.common.model.Position;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an asset's position has been processed and stored.
 */
public record PositionUpdated(
        UUID eventId,
        String assetId,
        Instant occurredAt,
        Position position,
        Position previousPosition
) implements TrackingEvent {

    public static PositionUpdated create(Position current, Position previous) {
        return new PositionUpdated(
                UUID.randomUUID(),
                current.assetId(),
                Instant.now(),
                current,
                previous
        );
    }
}
