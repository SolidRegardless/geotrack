package com.geotrack.common.spatial;

import org.locationtech.jts.geom.*;

import java.util.List;

/**
 * Core spatial operations engine wrapping JTS Topology Suite.
 * <p>
 * Centralises all geometry creation and spatial calculations.
 * Uses WGS84 (EPSG:4326) as the standard coordinate reference system.
 * <p>
 * JTS coordinate ordering: (longitude, latitude) = (x, y).
 * This is the ISO/OGC standard but opposite to how most people think
 * about coordinates (lat, lon). All public methods in this class
 * accept (longitude, latitude) to match JTS conventions.
 */
public class SpatialEngine {

    /** WGS84 — the GPS coordinate system */
    public static final int SRID_WGS84 = 4326;

    /** Approximate metres per degree of latitude */
    private static final double METRES_PER_DEGREE_LAT = 111_320.0;

    private final GeometryFactory geometryFactory;

    public SpatialEngine() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_WGS84);
    }

    /**
     * Create a JTS Point from WGS84 coordinates.
     *
     * @param longitude WGS84 longitude (-180 to 180)
     * @param latitude  WGS84 latitude (-90 to 90)
     * @return JTS Point with SRID 4326
     */
    public Point createPoint(double longitude, double latitude) {
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    /**
     * Create a polygon from a list of coordinate pairs.
     * The polygon ring is automatically closed (first point == last point).
     *
     * @param coordinates List of [longitude, latitude] pairs
     * @return Closed polygon with SRID 4326
     * @throws IllegalArgumentException if fewer than 3 coordinates provided
     */
    public Polygon createPolygon(List<double[]> coordinates) {
        if (coordinates.size() < 3) {
            throw new IllegalArgumentException(
                    "A polygon requires at least 3 coordinates, got: " + coordinates.size());
        }

        // Close the ring: first point == last point
        Coordinate[] coords = new Coordinate[coordinates.size() + 1];
        for (int i = 0; i < coordinates.size(); i++) {
            coords[i] = new Coordinate(coordinates.get(i)[0], coordinates.get(i)[1]);
        }
        coords[coords.length - 1] = coords[0];

        LinearRing ring = geometryFactory.createLinearRing(coords);
        return geometryFactory.createPolygon(ring);
    }

    /**
     * Create a circular geofence approximated as a polygon.
     * <p>
     * Note: This uses a simple degree-based approximation. For high-precision
     * work at extreme latitudes, a proper geodesic buffer should be used.
     *
     * @param centreLon  Centre longitude
     * @param centreLat  Centre latitude
     * @param radiusMetres Radius in metres
     * @return Polygon approximating a circle
     */
    public Polygon createCircularFence(double centreLon, double centreLat, double radiusMetres) {
        Point centre = createPoint(centreLon, centreLat);
        double radiusDegrees = radiusMetres / METRES_PER_DEGREE_LAT;
        // JTS buffer with 64 segments gives a smooth circle approximation
        return (Polygon) centre.buffer(radiusDegrees, 64);
    }

    /**
     * Create a LineString from a list of coordinate pairs (route reconstruction).
     *
     * @param coordinates Ordered list of [longitude, latitude] pairs
     * @return LineString representing the route
     */
    public LineString createLineString(List<double[]> coordinates) {
        Coordinate[] coords = coordinates.stream()
                .map(c -> new Coordinate(c[0], c[1]))
                .toArray(Coordinate[]::new);
        return geometryFactory.createLineString(coords);
    }

    /**
     * Check if a point is contained within a polygon.
     *
     * @param polygon The containing geometry
     * @param point   The point to test
     * @return true if the point is inside or on the boundary
     */
    public boolean contains(Polygon polygon, Point point) {
        return polygon.contains(point);
    }

    /**
     * Calculate the geodesic distance between two points in metres.
     * Uses the Vincenty formula for accuracy on the WGS84 ellipsoid.
     *
     * @param a First point
     * @param b Second point
     * @return Distance in metres
     */
    public double distanceMetres(Point a, Point b) {
        return vincentyDistance(a.getY(), a.getX(), b.getY(), b.getX());
    }

    /**
     * Calculate the initial bearing from point A to point B.
     *
     * @param from Origin point
     * @param to   Destination point
     * @return Bearing in degrees (0-360, 0 = North)
     */
    public double bearing(Point from, Point to) {
        double lat1 = Math.toRadians(from.getY());
        double lat2 = Math.toRadians(to.getY());
        double dLon = Math.toRadians(to.getX() - from.getX());

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    /**
     * Calculate the total length of a route in metres.
     *
     * @param route LineString representing the route
     * @return Total distance in metres
     */
    public double routeLengthMetres(LineString route) {
        double total = 0;
        Coordinate[] coords = route.getCoordinates();
        for (int i = 1; i < coords.length; i++) {
            total += vincentyDistance(
                    coords[i - 1].y, coords[i - 1].x,
                    coords[i].y, coords[i].x
            );
        }
        return total;
    }

    /**
     * Get the underlying GeometryFactory for advanced operations.
     */
    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    // ========================================================================
    // Vincenty formula — geodesic distance on WGS84 ellipsoid
    // ========================================================================

    /**
     * Vincenty's inverse formula for geodesic distance.
     * Accurate to ~0.5mm on the WGS84 ellipsoid.
     *
     * @param lat1 Latitude of point 1 (degrees)
     * @param lon1 Longitude of point 1 (degrees)
     * @param lat2 Latitude of point 2 (degrees)
     * @param lon2 Longitude of point 2 (degrees)
     * @return Distance in metres
     */
    private double vincentyDistance(double lat1, double lon1, double lat2, double lon2) {
        double a = 6_378_137.0;          // WGS84 semi-major axis (metres)
        double f = 1.0 / 298.257223563;  // WGS84 flattening
        double b = a * (1 - f);          // Semi-minor axis

        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        // U1, U2 = reduced latitudes in Vincenty formula
        double u1 = Math.atan((1 - f) * Math.tan(phi1));
        double u2 = Math.atan((1 - f) * Math.tan(phi2));
        double sinU1 = Math.sin(u1);
        double cosU1 = Math.cos(u1);
        double sinU2 = Math.sin(u2);
        double cosU2 = Math.cos(u2);

        double lambda = deltaLambda;
        double lambdaPrev;
        int iterations = 0;

        double sinSigma;
        double cosSigma;
        double sigma;
        double sinAlpha;
        double cos2Alpha;
        double cos2SigmaM;

        do {
            double sinLambda = Math.sin(lambda);
            double cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt(
                    Math.pow(cosU2 * sinLambda, 2)
                    + Math.pow(cosU1 * sinU2 - sinU1 * cosU2 * cosLambda, 2)
            );
            if (sinSigma == 0) return 0; // Co-incident points

            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cos2Alpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = (cos2Alpha != 0)
                    ? cosSigma - 2 * sinU1 * sinU2 / cos2Alpha
                    : 0;

            // C coefficient in Vincenty's correction formula
            double correctionC = f / 16 * cos2Alpha * (4 + f * (4 - 3 * cos2Alpha));
            lambdaPrev = lambda;
            lambda = deltaLambda + (1 - correctionC) * f * sinAlpha
                    * (sigma + correctionC * sinSigma
                    * (cos2SigmaM + correctionC * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)));
        } while (Math.abs(lambda - lambdaPrev) > 1e-12 && ++iterations < 200);

        if (iterations >= 200) {
            // Failed to converge — fall back to Haversine
            return haversineDistance(lat1, lon1, lat2, lon2);
        }

        double uSquared = cos2Alpha * (a * a - b * b) / (b * b);
        // Vincenty coefficients A and B (named a2/b2 for camelCase compliance)
        double a2 = 1 + uSquared / 16384
                * (4096 + uSquared * (-768 + uSquared * (320 - 175 * uSquared)));
        double b2 = uSquared / 1024
                * (256 + uSquared * (-128 + uSquared * (74 - 47 * uSquared)));
        double deltaSigma = b2 * sinSigma
                * (cos2SigmaM + b2 / 4
                * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)
                - b2 / 6 * cos2SigmaM
                * (-3 + 4 * sinSigma * sinSigma)
                * (-3 + 4 * cos2SigmaM * cos2SigmaM)));

        return b * a2 * (sigma - deltaSigma);
    }

    /**
     * Haversine formula — simpler, slightly less accurate fallback.
     * Treats Earth as a perfect sphere (mean radius 6,371km).
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6_371_000; // Earth mean radius in metres
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
