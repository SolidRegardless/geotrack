package com.geotrack.processing.engine;

import com.geotrack.common.event.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dispatches tracking events using Java 21 pattern matching switch.
 * <p>
 * The sealed TrackingEvent hierarchy + exhaustive switch means the compiler
 * guarantees every event type is handled. Adding a new event type without
 * updating this dispatcher is a compile error, not a runtime surprise.
 */
@ApplicationScoped
public class EventDispatcher {

    /**
     * Process a tracking event — exhaustive pattern matching.
     *
     * @param event The tracking event to dispatch
     * @return Human-readable description of action taken
     */
    public String dispatch(TrackingEvent event) {
        return switch (event) {
            case PositionUpdated pu -> handlePositionUpdate(pu);
            case GeofenceBreached gb -> handleGeofenceBreach(gb);
            case GeofenceExited ge -> handleGeofenceExit(ge);
            case AssetOffline ao -> handleAssetOffline(ao);
            case SpeedLimitExceeded sle -> handleSpeedLimit(sle);
            // No default needed — sealed interface guarantees exhaustiveness
        };
    }

    private String handlePositionUpdate(PositionUpdated event) {
        Log.infof("Position updated for asset %s at [%f, %f]",
                event.assetId(),
                event.position().latitude(),
                event.position().longitude());
        return "Position updated for " + event.assetId();
    }

    private String handleGeofenceBreach(GeofenceBreached event) {
        Log.warnf("ALERT: Asset %s breached geofence '%s'",
                event.assetId(), event.geofenceName());
        return "Geofence breach alert for " + event.assetId() + " in " + event.geofenceName();
    }

    private String handleGeofenceExit(GeofenceExited event) {
        Log.infof("Asset %s exited geofence '%s'",
                event.assetId(), event.geofenceName());
        return "Geofence exit for " + event.assetId() + " from " + event.geofenceName();
    }

    private String handleAssetOffline(AssetOffline event) {
        Log.warnf("Asset %s offline since %s (silence: %s)",
                event.assetId(), event.lastSeenAt(), event.silenceDuration());
        return "Asset offline: " + event.assetId();
    }

    private String handleSpeedLimit(SpeedLimitExceeded event) {
        Log.warnf("Asset %s exceeding speed limit: %.1f km/h (limit: %.1f km/h)",
                event.assetId(), event.currentSpeedKmh(), event.limitKmh());
        return "Speed limit exceeded by " + event.assetId();
    }
}
