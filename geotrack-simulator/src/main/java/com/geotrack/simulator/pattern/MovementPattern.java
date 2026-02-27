package com.geotrack.simulator.pattern;

/**
 * Sealed interface for asset movement patterns.
 * Each pattern defines how an asset moves through space over time.
 * Java 21 sealed types ensure exhaustive handling in simulation loop.
 */
public sealed interface MovementPattern permits
        WaypointRoute,
        PatrolLoop,
        RandomWalk {

    /**
     * Get the next position along this movement pattern.
     *
     * @param currentLat  Current latitude
     * @param currentLon  Current longitude
     * @param elapsedSecs Seconds elapsed since last update
     * @return [latitude, longitude, speed_kmh, heading_degrees]
     */
    double[] nextPosition(double currentLat, double currentLon, double elapsedSecs);

    /** Human-readable description */
    String describe();
}
