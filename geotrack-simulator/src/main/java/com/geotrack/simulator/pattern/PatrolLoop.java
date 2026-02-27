package com.geotrack.simulator.pattern;

import java.util.List;

/**
 * Repeating patrol circuit — visits waypoints in order, then returns to start.
 * Used for: bus routes, security patrols, drone surveillance patterns.
 */
public record PatrolLoop(
        List<double[]> waypoints,
        double speedKmh
) implements MovementPattern {

    private static final double METRES_PER_DEGREE = 111_320.0;

    @Override
    public double[] nextPosition(double currentLat, double currentLon, double elapsedSecs) {
        // Same movement logic as WaypointRoute but loops back to start
        double[] target = findNextTarget(currentLat, currentLon);

        double dLat = target[0] - currentLat;
        double dLon = target[1] - currentLon;
        double heading = Math.toDegrees(Math.atan2(dLon, dLat));
        if (heading < 0) heading += 360;

        double distanceMetres = (speedKmh / 3.6) * elapsedSecs;
        double distanceDegrees = distanceMetres / METRES_PER_DEGREE;

        double totalDist = Math.sqrt(dLat * dLat + dLon * dLon);
        double ratio = (totalDist > 0) ? Math.min(distanceDegrees / totalDist, 1.0) : 0;

        return new double[]{
                currentLat + dLat * ratio,
                currentLon + dLon * ratio,
                speedKmh, heading
        };
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
        return waypoints.get((closestIdx + 1) % waypoints.size());
    }

    @Override
    public String describe() {
        return "Patrol loop with %d waypoints at %.0f km/h".formatted(waypoints.size(), speedKmh);
    }
}
