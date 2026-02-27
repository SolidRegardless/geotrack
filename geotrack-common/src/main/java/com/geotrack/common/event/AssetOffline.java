package com.geotrack.common.event;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when an asset hasn't reported a position within the expected interval.
 */
public record AssetOffline(
        UUID eventId,
        String assetId,
        Instant occurredAt,
        Instant lastSeenAt,
        Duration silenceDuration
) implements TrackingEvent {}
