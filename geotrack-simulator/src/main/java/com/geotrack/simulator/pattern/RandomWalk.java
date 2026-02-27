package com.geotrack.simulator.pattern;

import java.util.Random;

/**
 * Random walk within a bounding box.
 * Used for: pedestrians, personnel on foot, wandering assets.
 * Moves in a random direction, bouncing off the bounding box edges.
 */
public record RandomWalk(
        double centreLat,
        double centreLon,
        double radiusDegrees,
        double speedKmh
) implements MovementPattern {

    private static final Random RANDOM = new Random();
    private static final double METRES_PER_DEGREE = 111_320.0;

    @Override
    public double[] nextPosition(double currentLat, double currentLon, double elapsedSecs) {
        // Random heading with slight persistence (drift)
        double heading = RANDOM.nextDouble() * 360.0;

        double distanceMetres = (speedKmh / 3.6) * elapsedSecs;
        double distanceDegrees = distanceMetres / METRES_PER_DEGREE;

        double newLat = currentLat + distanceDegrees * Math.cos(Math.toRadians(heading));
        double newLon = currentLon + distanceDegrees * Math.sin(Math.toRadians(heading));

        // Bounce off bounding box
        if (Math.abs(newLat - centreLat) > radiusDegrees) {
            newLat = centreLat + Math.signum(centreLat - newLat) * radiusDegrees * 0.9;
        }
        if (Math.abs(newLon - centreLon) > radiusDegrees) {
            newLon = centreLon + Math.signum(centreLon - newLon) * radiusDegrees * 0.9;
        }

        return new double[]{newLat, newLon, speedKmh, heading};
    }

    @Override
    public String describe() {
        return "Random walk around [%.4f, %.4f] radius %.4f° at %.0f km/h"
                .formatted(centreLat, centreLon, radiusDegrees, speedKmh);
    }
}
