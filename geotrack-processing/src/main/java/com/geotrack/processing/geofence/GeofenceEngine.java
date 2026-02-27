package com.geotrack.processing.geofence;

import com.geotrack.common.model.Position;
import com.geotrack.common.spatial.SpatialEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geofence detection engine.
 * <p>
 * Checks positions against registered geofences and detects
 * state transitions (OUTSIDE → INSIDE = breach, INSIDE → OUTSIDE = exit).
 * <p>
 * Uses an in-memory state map for tracking per-asset geofence state.
 * In production, this would be backed by Redis for cross-instance consistency.
 */
@ApplicationScoped
public class GeofenceEngine {

    public enum GeofenceState { INSIDE, OUTSIDE, UNKNOWN }

    public record Geofence(UUID id, String name, Polygon geometry) {}

    public record GeofenceTransition(
            UUID geofenceId,
            String geofenceName,
            String assetId,
            GeofenceState previousState,
            GeofenceState currentState,
            Instant detectedAt
    ) {
        public boolean isEntry() {
            return previousState != GeofenceState.INSIDE && currentState == GeofenceState.INSIDE;
        }

        public boolean isExit() {
            return previousState == GeofenceState.INSIDE && currentState == GeofenceState.OUTSIDE;
        }
    }

    private final SpatialEngine spatialEngine;
    private final List<Geofence> registeredFences = new ArrayList<>();

    /** State tracking: key = "assetId:geofenceId", value = current state */
    private final Map<String, GeofenceState> stateMap = new ConcurrentHashMap<>();

    @Inject
    public GeofenceEngine(SpatialEngine spatialEngine) {
        this.spatialEngine = spatialEngine;
    }

    /** Constructor for testing without CDI */
    public GeofenceEngine() {
        this.spatialEngine = new SpatialEngine();
    }

    /**
     * Register a geofence for monitoring.
     */
    public void registerGeofence(UUID id, String name, Polygon geometry) {
        registeredFences.add(new Geofence(id, name, geometry));
    }

    /**
     * Register a circular geofence.
     */
    public void registerCircularGeofence(UUID id, String name,
                                          double centreLon, double centreLat, double radiusMetres) {
        Polygon circle = spatialEngine.createCircularFence(centreLon, centreLat, radiusMetres);
        registerGeofence(id, name, circle);
    }

    /**
     * Check a position against all registered geofences.
     * Returns state transitions (entries and exits) detected.
     *
     * @param position The position to check
     * @return List of geofence state transitions (may be empty)
     */
    public List<GeofenceTransition> checkPosition(Position position) {
        Point point = spatialEngine.createPoint(position.longitude(), position.latitude());
        List<GeofenceTransition> transitions = new ArrayList<>();

        for (Geofence fence : registeredFences) {
            boolean inside = spatialEngine.contains(fence.geometry(), point);
            GeofenceState currentState = inside ? GeofenceState.INSIDE : GeofenceState.OUTSIDE;

            String stateKey = position.assetId() + ":" + fence.id();
            GeofenceState previousState = stateMap.getOrDefault(stateKey, GeofenceState.UNKNOWN);

            if (previousState != currentState && previousState != GeofenceState.UNKNOWN) {
                transitions.add(new GeofenceTransition(
                        fence.id(), fence.name(), position.assetId(),
                        previousState, currentState, Instant.now()
                ));
            }

            stateMap.put(stateKey, currentState);
        }

        return transitions;
    }

    /**
     * Get current state for an asset-geofence pair.
     */
    public GeofenceState getState(String assetId, UUID geofenceId) {
        return stateMap.getOrDefault(assetId + ":" + geofenceId, GeofenceState.UNKNOWN);
    }

    /**
     * Get count of registered geofences.
     */
    public int getGeofenceCount() {
        return registeredFences.size();
    }

    /**
     * Clear all geofences and state (for testing).
     */
    public void clear() {
        registeredFences.clear();
        stateMap.clear();
    }
}
