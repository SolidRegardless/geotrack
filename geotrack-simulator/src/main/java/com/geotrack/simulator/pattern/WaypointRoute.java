package com.geotrack.simulator.pattern;

import java.util.List;

/**
 * Linear route through a sequence of waypoints at a given speed.
 * When the last waypoint is reached, the route reverses (ping-pong).
 */
public record WaypointRoute(
        List<double[]> waypoints,  // [lat, lon] pairs
        double speedKmh
) implements MovementPattern {

    private static final double METRES_PER_DEGREE = 111_320.0;

    @Override
    public double[] nextPosition(double currentLat, double currentLon, double elapsedSecs) {
        // Find nearest waypoint we haven't reached yet
        double[] target = findNextTarget(currentLat, currentLon);
        double targetLat = target[0];
        double targetLon = target[1];

        // Calculate bearing to target
        double dLat = targetLat - currentLat;
        double dLon = targetLon - currentLon;
        double heading = Math.toDegrees(Math.atan2(dLon, dLat));
        if (heading < 0) heading += 360;

        // Move towards target at configured speed
        double distanceMetres = (speedKmh / 3.6) * elapsedSecs;
        double distanceDegrees = distanceMetres / METRES_PER_DEGREE;

        double totalDist = Math.sqrt(dLat * dLat + dLon * dLon);
        double ratio = (totalDist > 0) ? Math.min(distanceDegrees / totalDist, 1.0) : 0;

        double newLat = currentLat + dLat * ratio;
        double newLon = currentLon + dLon * ratio;

        return new double[]{newLat, newLon, speedKmh, heading};
    }

    private double[] findNextTarget(double lat, double lon) {
        double minDist = Double.MAX_VALUE;
        int closestIdx = 0;
        for (int i = 0; i < waypoints.size(); i++) {
            double d = Math.sqrt(
                    Math.pow(waypoints.get(i)[0] - lat, 2) +
                    Math.pow(waypoints.get(i)[1] - lon, 2));
            if (d < minDist) {
                minDist = d;
                closestIdx = i;
            }
        }
        // Target the NEXT waypoint after the closest
        int targetIdx = (closestIdx + 1) % waypoints.size();
        return waypoints.get(targetIdx);
    }

    @Override
    public String describe() {
        return "Waypoint route with %d points at %.0f km/h".formatted(waypoints.size(), speedKmh);
    }
}
