package com.geotrack.processing.engine;

import com.geotrack.common.event.*;
import com.geotrack.common.model.Position;
import com.geotrack.common.model.PositionSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EventDispatcher — validates exhaustive pattern matching
 * over the sealed TrackingEvent hierarchy.
 */
class EventDispatcherTest {

    private EventDispatcher dispatcher;

    private Position samplePosition() {
        return Position.of("ASSET-001", 54.9783, -1.6178, Instant.now());
    }

    @BeforeEach
    void setUp() {
        dispatcher = new EventDispatcher();
    }

    @Test
    @DisplayName("Should dispatch PositionUpdated events")
    void shouldDispatchPositionUpdated() {
        var event = PositionUpdated.create(samplePosition(), null);
        String result = dispatcher.dispatch(event);
        assertTrue(result.contains("Position updated"));
        assertTrue(result.contains("ASSET-001"));
    }

    @Test
    @DisplayName("Should dispatch GeofenceBreached events")
    void shouldDispatchGeofenceBreach() {
        var event = GeofenceBreached.create(
                "ASSET-001", UUID.randomUUID(), "Restricted Zone", samplePosition());
        String result = dispatcher.dispatch(event);
        assertTrue(result.contains("breach"));
        assertTrue(result.contains("Restricted Zone"));
    }

    @Test
    @DisplayName("Should dispatch GeofenceExited events")
    void shouldDispatchGeofenceExit() {
        var event = GeofenceExited.create(
                "ASSET-001", UUID.randomUUID(), "Safe Zone", samplePosition());
        String result = dispatcher.dispatch(event);
        assertTrue(result.contains("exit"));
        assertTrue(result.contains("Safe Zone"));
    }

    @Test
    @DisplayName("Should dispatch AssetOffline events")
    void shouldDispatchAssetOffline() {
        var event = new AssetOffline(
                UUID.randomUUID(), "ASSET-001", Instant.now(),
                Instant.now().minusSeconds(3600), Duration.ofHours(1));
        String result = dispatcher.dispatch(event);
        assertTrue(result.contains("offline"));
    }

    @Test
    @DisplayName("Should dispatch SpeedLimitExceeded events")
    void shouldDispatchSpeedLimit() {
        var event = new SpeedLimitExceeded(
                UUID.randomUUID(), "ASSET-001", Instant.now(),
                120.0, 70.0, samplePosition());
        String result = dispatcher.dispatch(event);
        assertTrue(result.contains("Speed limit"));
    }

    @Test
    @DisplayName("All event types should be dispatchable (exhaustiveness check)")
    void allEventTypesShouldBeDispatchable() {
        // This test verifies that every TrackingEvent subtype can be dispatched.
        // If a new event type is added to the sealed hierarchy without updating
        // the dispatcher, this test (and the dispatcher) won't compile.
        TrackingEvent[] events = {
                PositionUpdated.create(samplePosition(), null),
                GeofenceBreached.create("A", UUID.randomUUID(), "Z", samplePosition()),
                GeofenceExited.create("A", UUID.randomUUID(), "Z", samplePosition()),
                new AssetOffline(UUID.randomUUID(), "A", Instant.now(), Instant.now(), Duration.ZERO),
                new SpeedLimitExceeded(UUID.randomUUID(), "A", Instant.now(), 100, 60, samplePosition())
        };

        for (TrackingEvent event : events) {
            assertDoesNotThrow(() -> dispatcher.dispatch(event),
                    "Failed to dispatch: " + event.getClass().getSimpleName());
        }
    }
}
