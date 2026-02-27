package com.geotrack.common.spatial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SpatialEngine.
 * <p>
 * These are pure-function tests — no mocks, no containers, no I/O.
 * They run in milliseconds and form the foundation of our testing pyramid.
 */
class SpatialEngineTest {

    private SpatialEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SpatialEngine();
    }

    // ========================================================================
    // Point creation
    // ========================================================================

    @Nested
    @DisplayName("Point Creation")
    class PointCreation {

        @Test
        @DisplayName("Should create valid WGS84 point from coordinates")
        void shouldCreatePoint() {
            Point point = engine.createPoint(-1.6178, 54.9783);

            assertEquals(-1.6178, point.getX(), 0.0001, "Longitude (X)");
            assertEquals(54.9783, point.getY(), 0.0001, "Latitude (Y)");
            assertEquals(SpatialEngine.SRID_WGS84, point.getSRID(), "SRID");
        }

        @Test
        @DisplayName("Should handle antimeridian coordinates")
        void shouldHandleAntimeridian() {
            Point point = engine.createPoint(179.9999, 0.0);
            assertEquals(179.9999, point.getX(), 0.0001);

            Point pointNeg = engine.createPoint(-179.9999, 0.0);
            assertEquals(-179.9999, pointNeg.getX(), 0.0001);
        }

        @Test
        @DisplayName("Should handle polar coordinates")
        void shouldHandlePoles() {
            Point northPole = engine.createPoint(0, 90);
            assertEquals(90.0, northPole.getY(), 0.0001);

            Point southPole = engine.createPoint(0, -90);
            assertEquals(-90.0, southPole.getY(), 0.0001);
        }
    }

    // ========================================================================
    // Polygon creation
    // ========================================================================

    @Nested
    @DisplayName("Polygon Creation")
    class PolygonCreation {

        @Test
        @DisplayName("Should create closed polygon from coordinate list")
        void shouldCreatePolygon() {
            Polygon polygon = engine.createPolygon(List.of(
                    new double[]{-2.0, 54.0},
                    new double[]{-1.0, 54.0},
                    new double[]{-1.0, 55.0},
                    new double[]{-2.0, 55.0}
            ));

            assertNotNull(polygon);
            assertTrue(polygon.isValid(), "Polygon should be valid");
            assertFalse(polygon.isEmpty(), "Polygon should not be empty");
            // Ring is closed: 4 points + closing point = 5 coordinates
            assertEquals(5, polygon.getExteriorRing().getNumPoints());
        }

        @Test
        @DisplayName("Should reject polygon with fewer than 3 points")
        void shouldRejectTooFewPoints() {
            assertThrows(IllegalArgumentException.class, () ->
                    engine.createPolygon(List.of(
                            new double[]{-2.0, 54.0},
                            new double[]{-1.0, 54.0}
                    ))
            );
        }

        @Test
        @DisplayName("Should create circular geofence")
        void shouldCreateCircularFence() {
            Polygon circle = engine.createCircularFence(-1.6178, 54.9783, 1000); // 1km radius

            assertNotNull(circle);
            assertTrue(circle.isValid());
            // Should contain its own centre
            Point centre = engine.createPoint(-1.6178, 54.9783);
            assertTrue(circle.contains(centre));
        }
    }

    // ========================================================================
    // Containment checks
    // ========================================================================

    @Nested
    @DisplayName("Containment Checks")
    class ContainmentChecks {

        private Polygon northEastEngland;

        @BeforeEach
        void setUp() {
            // Rough bounding box around North East England
            northEastEngland = engine.createPolygon(List.of(
                    new double[]{-2.5, 54.5},
                    new double[]{-1.0, 54.5},
                    new double[]{-1.0, 55.5},
                    new double[]{-2.5, 55.5}
            ));
        }

        @Test
        @DisplayName("Should detect Newcastle is inside North East England")
        void newcastleIsInside() {
            Point newcastle = engine.createPoint(-1.6178, 54.9783);
            assertTrue(engine.contains(northEastEngland, newcastle));
        }

        @Test
        @DisplayName("Should detect London is outside North East England")
        void londonIsOutside() {
            Point london = engine.createPoint(-0.1276, 51.5074);
            assertFalse(engine.contains(northEastEngland, london));
        }

        @ParameterizedTest
        @CsvSource({
                "-1.6178, 54.9783, true",   // Newcastle
                "-1.5491, 54.7753, true",   // Durham
                "-1.3835, 54.9069, true",   // Sunderland
                "-0.1276, 51.5074, false",  // London
                "-2.2426, 53.4808, false",  // Manchester
                "0.0, 0.0, false"           // Null Island
        })
        @DisplayName("Should correctly classify UK cities")
        void shouldClassifyCities(double lon, double lat, boolean expectedInside) {
            Point point = engine.createPoint(lon, lat);
            assertEquals(expectedInside, engine.contains(northEastEngland, point),
                    "Point (%f, %f) containment".formatted(lon, lat));
        }
    }

    // ========================================================================
    // Distance calculations
    // ========================================================================

    @Nested
    @DisplayName("Distance Calculations")
    class DistanceCalculations {

        @Test
        @DisplayName("Should calculate Newcastle to London distance accurately")
        void newcastleToLondon() {
            Point newcastle = engine.createPoint(-1.6178, 54.9783);
            Point london = engine.createPoint(-0.1276, 51.5074);

            double distance = engine.distanceMetres(newcastle, london);

            // Actual geodesic distance: ~394km
            assertEquals(394_000, distance, 5_000,
                    "Distance should be approximately 394km");
        }

        @Test
        @DisplayName("Should return zero for same point")
        void samePoint() {
            Point point = engine.createPoint(-1.6178, 54.9783);
            assertEquals(0.0, engine.distanceMetres(point, point), 0.001);
        }

        @Test
        @DisplayName("Should calculate short distance accurately")
        void shortDistance() {
            // Two points ~1km apart in Newcastle
            Point a = engine.createPoint(-1.6178, 54.9783);
            Point b = engine.createPoint(-1.6050, 54.9783);

            double distance = engine.distanceMetres(a, b);

            // ~0.8km at this latitude
            assertTrue(distance > 500 && distance < 2000,
                    "Short distance should be between 500m and 2km, got: " + distance);
        }

        @Test
        @DisplayName("Should handle antipodal points")
        void antipodalPoints() {
            Point a = engine.createPoint(0, 0);
            Point b = engine.createPoint(180, 0);

            double distance = engine.distanceMetres(a, b);

            // Half the Earth's circumference: ~20,000km
            assertEquals(20_000_000, distance, 100_000);
        }
    }

    // ========================================================================
    // Bearing calculations
    // ========================================================================

    @Nested
    @DisplayName("Bearing Calculations")
    class BearingCalculations {

        @Test
        @DisplayName("Due north should be ~0 degrees")
        void dueNorth() {
            Point a = engine.createPoint(0, 50);
            Point b = engine.createPoint(0, 51);
            double bearing = engine.bearing(a, b);
            assertEquals(0.0, bearing, 0.1);
        }

        @Test
        @DisplayName("Due east should be ~90 degrees")
        void dueEast() {
            Point a = engine.createPoint(0, 50);
            Point b = engine.createPoint(1, 50);
            double bearing = engine.bearing(a, b);
            assertEquals(90.0, bearing, 1.0);
        }

        @Test
        @DisplayName("Due south should be ~180 degrees")
        void dueSouth() {
            Point a = engine.createPoint(0, 51);
            Point b = engine.createPoint(0, 50);
            double bearing = engine.bearing(a, b);
            assertEquals(180.0, bearing, 0.1);
        }
    }

    // ========================================================================
    // Route operations
    // ========================================================================

    @Nested
    @DisplayName("Route Operations")
    class RouteOperations {

        @Test
        @DisplayName("Should create linestring route from coordinates")
        void shouldCreateRoute() {
            LineString route = engine.createLineString(List.of(
                    new double[]{-1.6178, 54.9783},  // Newcastle
                    new double[]{-1.5491, 54.7753},   // Durham
                    new double[]{-1.3249, 54.5653}    // Darlington
            ));

            assertNotNull(route);
            assertEquals(3, route.getNumPoints());
            assertFalse(route.isEmpty());
        }

        @Test
        @DisplayName("Should calculate route length")
        void shouldCalculateRouteLength() {
            LineString route = engine.createLineString(List.of(
                    new double[]{-1.6178, 54.9783},  // Newcastle
                    new double[]{-1.5491, 54.7753}    // Durham
            ));

            double length = engine.routeLengthMetres(route);

            // Newcastle to Durham: ~24km
            assertEquals(24_000, length, 5_000);
        }
    }
}
