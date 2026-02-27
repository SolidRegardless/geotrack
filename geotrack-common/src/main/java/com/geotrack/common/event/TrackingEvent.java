package com.geotrack.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Sealed interface for all tracking events.
 * <p>
 * Sealed types ensure the compiler knows ALL possible event types,
 * enabling exhaustive pattern matching in switch expressions (Java 21).
 * If a new event type is added, every switch that doesn't handle it
 * becomes a compile error — no runtime surprises.
 */
public sealed interface TrackingEvent permits
        PositionUpdated,
        GeofenceBreached,
        GeofenceExited,
        AssetOffline,
        SpeedLimitExceeded {

    UUID eventId();

    String assetId();

    Instant occurredAt();
}
