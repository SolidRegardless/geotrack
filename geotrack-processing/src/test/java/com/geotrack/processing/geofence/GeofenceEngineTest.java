package com.geotrack.processing.geofence;

import com.geotrack.common.model.Position;
import com.geotrack.common.model.PositionSource;
import com.geotrack.common.spatial.SpatialEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the GeofenceEngine.
 * Tests spatial containment detection and state transition logic.
 */
class GeofenceEngineTest {

    private GeofenceEngine engine;
    private SpatialEngine spatial;

    // Newcastle city centre geofence (rough bounding box)
    private UUID newcastleFenceId;
    private final String ASSET_ID = "VEHICLE-001";

    @BeforeEach
    void setUp() {
        engine = new GeofenceEngine();
        spatial = new SpatialEngine();

        // Register a geofence around Newcastle city centre
        newcastleFenceId = UUID.randomUUID();
        Polygon newcastleFence = spatial.createPolygon(List.of(
                new double[]{-1.65, 54.96},   // SW
                new double[]{-1.58, 54.96},   // SE
                new double[]{-1.58, 55.00},   // NE
                new double[]{-1.65, 55.00}    // NW
        ));
        engine.registerGeofence(newcastleFenceId, "Newcastle City Centre", newcastleFence);
    }

    private Position positionAt(double lat, double lon) {
        return new Position(UUID.randomUUID(), ASSET_ID, lat, lon, 0, 0, 0,
                Instant.now(), PositionSource.GPS);
    }

    @Nested
    @DisplayName("Basic Containment")
    class BasicContainment {

        @Test
        @DisplayName("Should register geofences")
        void shouldRegisterGeofences() {
            assertEquals(1, engine.getGeofenceCount());
        }

        @Test
        @DisplayName("First position inside geofence should not trigger transition")
        void firstPositionInsideShouldNotTrigger() {
            // First check sets state to INSIDE, but previous was UNKNOWN → no transition
            Position inside = positionAt(54.9783, -1.6178); // Newcastle
            List<GeofenceEngine.GeofenceTransition> transitions = engine.checkPosition(inside);

            assertTrue(transitions.isEmpty(),
                    "First position should not trigger transition (state was UNKNOWN)");
            assertEquals(GeofenceEngine.GeofenceState.INSIDE,
                    engine.getState(ASSET_ID, newcastleFenceId));
        }

        @Test
        @DisplayName("First position outside geofence should not trigger transition")
        void firstPositionOutsideShouldNotTrigger() {
            Position outside = positionAt(51.5074, -0.1276); // London
            List<GeofenceEngine.GeofenceTransition> transitions = engine.checkPosition(outside);

            assertTrue(transitions.isEmpty());
            assertEquals(GeofenceEngine.GeofenceState.OUTSIDE,
                    engine.getState(ASSET_ID, newcastleFenceId));
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("Should detect ENTRY when moving from outside to inside")
        void shouldDetectEntry() {
            // First: outside (sets state)
            engine.checkPosition(positionAt(51.5074, -0.1276)); // London

            // Second: inside (triggers transition)
            Position entering = positionAt(54.9783, -1.6178); // Newcastle
            List<GeofenceEngine.GeofenceTransition> transitions = engine.checkPosition(entering);

            assertEquals(1, transitions.size());
            assertTrue(transitions.getFirst().isEntry());
            assertEquals("Newcastle City Centre", transitions.getFirst().geofenceName());
            assertEquals(ASSET_ID, transitions.getFirst().assetId());
        }

        @Test
        @DisplayName("Should detect EXIT when moving from inside to outside")
        void shouldDetectExit() {
            // First: inside
            engine.checkPosition(positionAt(54.9783, -1.6178)); // Newcastle

            // Second: outside
            Position leaving = positionAt(51.5074, -0.1276); // London
            List<GeofenceEngine.GeofenceTransition> transitions = engine.checkPosition(leaving);

            assertEquals(1, transitions.size());
            assertTrue(transitions.getFirst().isExit());
        }

        @Test
        @DisplayName("Should NOT trigger when staying inside")
        void shouldNotTriggerWhenStayingInside() {
            // Two positions, both inside Newcastle
            engine.checkPosition(positionAt(54.9783, -1.6178));
            List<GeofenceEngine.GeofenceTransition> transitions =
                    engine.checkPosition(positionAt(54.9800, -1.6200));

            assertTrue(transitions.isEmpty(),
                    "Staying inside should not trigger a transition");
        }

        @Test
        @DisplayName("Should NOT trigger when staying outside")
        void shouldNotTriggerWhenStayingOutside() {
            // Two positions, both outside
            engine.checkPosition(positionAt(51.5074, -0.1276)); // London
            List<GeofenceEngine.GeofenceTransition> transitions =
                    engine.checkPosition(positionAt(53.4808, -2.2426)); // Manchester

            assertTrue(transitions.isEmpty());
        }

        @Test
        @DisplayName("Should detect full ENTRY then EXIT cycle")
        void shouldDetectFullCycle() {
            // 1. Start outside
            engine.checkPosition(positionAt(51.5074, -0.1276)); // London

            // 2. Enter Newcastle → ENTRY
            var entry = engine.checkPosition(positionAt(54.9783, -1.6178));
            assertEquals(1, entry.size());
            assertTrue(entry.getFirst().isEntry());

            // 3. Move within Newcastle → no transition
            var move = engine.checkPosition(positionAt(54.9800, -1.6100));
            assertTrue(move.isEmpty());

            // 4. Leave Newcastle → EXIT
            var exit = engine.checkPosition(positionAt(51.5074, -0.1276));
            assertEquals(1, exit.size());
            assertTrue(exit.getFirst().isExit());
        }
    }

    @Nested
    @DisplayName("Multiple Geofences")
    class MultipleGeofences {

        private UUID durhamFenceId;

        @BeforeEach
        void setUp() {
            durhamFenceId = UUID.randomUUID();
            Polygon durhamFence = spatial.createPolygon(List.of(
                    new double[]{-1.60, 54.75},
                    new double[]{-1.50, 54.75},
                    new double[]{-1.50, 54.80},
                    new double[]{-1.60, 54.80}
            ));
            engine.registerGeofence(durhamFenceId, "Durham City", durhamFence);
        }

        @Test
        @DisplayName("Should track state independently per geofence")
        void shouldTrackIndependently() {
            // Start outside both
            engine.checkPosition(positionAt(51.5074, -0.1276)); // London

            // Enter Newcastle only
            var transitions = engine.checkPosition(positionAt(54.9783, -1.6178));

            // Should only trigger Newcastle entry, not Durham
            assertEquals(1, transitions.size());
            assertEquals("Newcastle City Centre", transitions.getFirst().geofenceName());
        }
    }

    @Nested
    @DisplayName("Circular Geofence")
    class CircularGeofence {

        @Test
        @DisplayName("Should detect entry into circular geofence")
        void shouldDetectCircularEntry() {
            engine.clear();
            UUID circleId = UUID.randomUUID();
            engine.registerCircularGeofence(circleId, "1km around Monument",
                    -1.6131, 54.9738, 1000); // 1km radius

            // Start far away
            engine.checkPosition(positionAt(51.5074, -0.1276)); // London

            // Move into the circle
            var transitions = engine.checkPosition(positionAt(54.9738, -1.6131)); // Monument

            assertEquals(1, transitions.size());
            assertTrue(transitions.getFirst().isEntry());
            assertEquals("1km around Monument", transitions.getFirst().geofenceName());
        }
    }
}
