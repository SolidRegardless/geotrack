package com.geotrack.common.event;

import com.geotrack.common.model.Position;

import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an asset enters a geofence zone.
 */
public record GeofenceBreached(
        UUID eventId,
        String assetId,
        Instant occurredAt,
        UUID geofenceId,
        String geofenceName,
        Position position
) implements TrackingEvent {

    public static GeofenceBreached create(String assetId, UUID geofenceId, String geofenceName, Position position) {
        return new GeofenceBreached(
                UUID.randomUUID(), assetId, Instant.now(),
                geofenceId, geofenceName, position
        );
    }
}
