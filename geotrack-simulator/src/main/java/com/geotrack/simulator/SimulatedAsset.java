package com.geotrack.simulator;

import com.geotrack.simulator.pattern.MovementPattern;

/**
 * Represents a single simulated asset with its movement pattern and current state.
 */
public class SimulatedAsset {

    private final String assetId;
    private final String assetType;
    private final MovementPattern pattern;
    private double currentLat;
    private double currentLon;
    private double speed;
    private double heading;

    public SimulatedAsset(String assetId, String assetType,
                          MovementPattern pattern, double startLat, double startLon) {
        this.assetId = assetId;
        this.assetType = assetType;
        this.pattern = pattern;
        this.currentLat = startLat;
        this.currentLon = startLon;
    }

    /**
     * Advance the simulation by the given time step.
     */
    public void tick(double elapsedSecs) {
        double[] next = pattern.nextPosition(currentLat, currentLon, elapsedSecs);
        this.currentLat = next[0];
        this.currentLon = next[1];
        this.speed = next[2];
        this.heading = next[3];
    }

    public String getAssetId() { return assetId; }
    public String getAssetType() { return assetType; }
    public double getCurrentLat() { return currentLat; }
    public double getCurrentLon() { return currentLon; }
    public double getSpeed() { return speed; }
    public double getHeading() { return heading; }
    public MovementPattern getPattern() { return pattern; }
}
