package com.geotrack.simulator;

import com.geotrack.simulator.noise.GpsNoiseSimulator;
import com.geotrack.simulator.pattern.PatrolLoop;
import com.geotrack.simulator.pattern.RandomWalk;
import com.geotrack.simulator.pattern.WaypointRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulatorTest {

    @Nested
    @DisplayName("WaypointRoute")
    class WaypointRouteTest {

        @Test
        @DisplayName("Should move towards next waypoint")
        void shouldMoveTowardsWaypoint() {
            var route = new WaypointRoute(List.of(
                    new double[]{54.97, -1.61},
                    new double[]{54.98, -1.60}
            ), 40);

            double[] next = route.nextPosition(54.97, -1.61, 2.0);

            // Should have moved north-east towards second waypoint
            assertTrue(next[0] >= 54.97, "Should move north");
            assertTrue(next[2] > 0, "Speed should be positive");
        }

        @Test
        @DisplayName("Should return valid heading")
        void shouldReturnValidHeading() {
            var route = new WaypointRoute(List.of(
                    new double[]{54.97, -1.61},
                    new double[]{54.98, -1.61}  // Due north
            ), 40);

            double[] next = route.nextPosition(54.97, -1.61, 1.0);
            assertTrue(next[3] >= 0 && next[3] <= 360, "Heading must be 0-360");
        }
    }

    @Nested
    @DisplayName("PatrolLoop")
    class PatrolLoopTest {

        @Test
        @DisplayName("Should move in patrol pattern")
        void shouldMoveInLoop() {
            var patrol = new PatrolLoop(List.of(
                    new double[]{54.970, -1.610},
                    new double[]{54.971, -1.610},
                    new double[]{54.971, -1.609},
                    new double[]{54.970, -1.609}
            ), 20);

            double lat = 54.970, lon = -1.610;
            for (int i = 0; i < 10; i++) {
                double[] next = patrol.nextPosition(lat, lon, 2.0);
                lat = next[0];
                lon = next[1];
            }

            // After 10 ticks, should still be in the patrol area
            assertTrue(lat > 54.969 && lat < 54.972);
            assertTrue(lon > -1.611 && lon < -1.608);
        }
    }

    @Nested
    @DisplayName("RandomWalk")
    class RandomWalkTest {

        @Test
        @DisplayName("Should stay within bounding box")
        void shouldStayInBounds() {
            var walk = new RandomWalk(54.975, -1.614, 0.003, 5);

            double lat = 54.975, lon = -1.614;
            for (int i = 0; i < 100; i++) {
                double[] next = walk.nextPosition(lat, lon, 2.0);
                lat = next[0];
                lon = next[1];
            }

            // After 100 ticks, should still be roughly near centre
            assertTrue(Math.abs(lat - 54.975) < 0.01,
                    "Lat should be near centre, got: " + lat);
            assertTrue(Math.abs(lon - (-1.614)) < 0.01,
                    "Lon should be near centre, got: " + lon);
        }
    }

    @Nested
    @DisplayName("GpsNoiseSimulator")
    class GpsNoiseTest {

        @Test
        @DisplayName("Should add noise within reasonable bounds")
        void shouldAddReasonableNoise() {
            var noise = new GpsNoiseSimulator(3.0);

            double baseLat = 54.9783, baseLon = -1.6178;
            for (int i = 0; i < 100; i++) {
                double[] noisy = noise.addNoise(baseLat, baseLon, 50.0);
                // ~50m tolerance (very generous, accounting for occasional jumps)
                assertTrue(Math.abs(noisy[0] - baseLat) < 0.001,
                        "Lat noise too large: " + (noisy[0] - baseLat));
                assertTrue(Math.abs(noisy[1] - baseLon) < 0.001,
                        "Lon noise too large: " + (noisy[1] - baseLon));
                assertTrue(noisy[2] >= 0, "Speed must be non-negative");
            }
        }

        @Test
        @DisplayName("Should produce varying results (not deterministic)")
        void shouldVary() {
            var noise = new GpsNoiseSimulator(3.0);
            double[] first = noise.addNoise(54.9783, -1.6178, 50.0);
            double[] second = noise.addNoise(54.9783, -1.6178, 50.0);
            // Extremely unlikely to be exactly the same
            assertFalse(first[0] == second[0] && first[1] == second[1],
                    "Two noise applications should differ");
        }
    }

    @Nested
    @DisplayName("SimulatedAsset")
    class SimulatedAssetTest {

        @Test
        @DisplayName("Should update position on tick")
        void shouldUpdateOnTick() {
            var asset = new SimulatedAsset("TEST-001", "VEHICLE",
                    new WaypointRoute(List.of(
                            new double[]{54.97, -1.61},
                            new double[]{54.98, -1.60}
                    ), 40),
                    54.97, -1.61);

            double startLat = asset.getCurrentLat();
            asset.tick(5.0);

            assertNotEquals(startLat, asset.getCurrentLat(),
                    "Position should change after tick");
        }
    }

    @Nested
    @DisplayName("MovementPattern sealed hierarchy")
    class PatternExhaustivenessTest {

        @Test
        @DisplayName("All pattern types should be describable")
        void allPatternsShouldDescribe() {
            var patterns = List.of(
                    new WaypointRoute(List.of(new double[]{0, 0}, new double[]{1, 1}), 30),
                    new PatrolLoop(List.of(new double[]{0, 0}, new double[]{1, 1}), 20),
                    new RandomWalk(0, 0, 0.01, 5)
            );

            for (var pattern : patterns) {
                // Java 21 exhaustive switch
                String desc = switch (pattern) {
                    case WaypointRoute wr -> wr.describe();
                    case PatrolLoop pl -> pl.describe();
                    case RandomWalk rw -> rw.describe();
                };
                assertNotNull(desc);
                assertFalse(desc.isEmpty());
            }
        }
    }
}
