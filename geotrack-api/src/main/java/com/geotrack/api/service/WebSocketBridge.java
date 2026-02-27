package com.geotrack.api.service;

import com.geotrack.api.model.PositionEntity;
import com.geotrack.api.repository.PositionRepository;
import com.geotrack.api.resource.TrackingWebSocket;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Bridges Kafka topics to WebSocket clients and persists processed positions.
 * <p>
 * Consumes processed position events and geofence alerts from Kafka,
 * persists positions to PostGIS, then broadcasts to all connected
 * WebSocket clients. This completes the real-time data flow:
 * <pre>
 * GPS Device → Ingestion API → Kafka (position.raw)
 *   → Processing Service → Kafka (position.processed / alert.geofence)
 *   → THIS BRIDGE → PostGIS + WebSocket → Angular Map
 * </pre>
 */
@ApplicationScoped
public class WebSocketBridge {

    private final TrackingWebSocket trackingWebSocket;
    private final PositionRepository positionRepository;

    @Inject
    public WebSocketBridge(TrackingWebSocket trackingWebSocket,
                           PositionRepository positionRepository) {
        this.trackingWebSocket = trackingWebSocket;
        this.positionRepository = positionRepository;
    }

    /**
     * Consume processed positions, persist to DB, and push to WebSocket clients.
     */
    @Incoming("ws-positions")
    @Blocking
    @Transactional
    public void onProcessedPosition(String payload) {
        Log.debugf("Broadcasting position to %d WebSocket clients",
                trackingWebSocket.getConnectionCount());

        // Persist to PostGIS for historical queries and initial load
        try {
            persistPosition(payload);
        } catch (Exception e) {
            Log.warnf("Failed to persist position: %s", e.getMessage());
        }

        String wsMessage = """
                {"type":"POSITION_UPDATED","payload":%s}
                """.formatted(payload).trim();

        trackingWebSocket.broadcast(wsMessage);
    }

    /**
     * Consume geofence alerts and push to WebSocket clients.
     */
    @Incoming("ws-alerts")
    @Blocking
    public void onGeofenceAlert(String payload) {
        Log.warnf("Broadcasting alert to %d WebSocket clients",
                trackingWebSocket.getConnectionCount());

        String wsMessage = """
                {"type":"GEOFENCE_BREACHED","payload":%s}
                """.formatted(payload).trim();

        trackingWebSocket.broadcast(wsMessage);
    }

    private void persistPosition(String payload) {
        JsonObject json = new JsonObject(payload);
        String assetId = json.getString("assetId");

        // PositionUpdated event has coordinates nested under "position"
        JsonObject pos = json.getJsonObject("position");
        if (pos == null) {
            // Fallback: flat structure
            pos = json;
        }

        double latitude = pos.getDouble("latitude", 0.0);
        double longitude = pos.getDouble("longitude", 0.0);
        double altitude = pos.getDouble("altitude", 0.0);
        double speed = pos.getDouble("speed", 0.0);
        double heading = pos.getDouble("heading", 0.0);
        String ts = pos.getString("timestamp");

        Instant timestamp = ts != null ? Instant.parse(ts) : Instant.now();

        PositionEntity entity = PositionEntity.fromCoordinates(
                UUID.nameUUIDFromBytes(assetId.getBytes()),
                longitude, latitude, timestamp
        );
        entity.altitude = altitude;
        entity.speed = speed;
        entity.heading = heading;
        entity.source = assetId; // Store original string ID for display

        positionRepository.persist(entity);
        Log.debugf("Persisted position for %s at [%.6f, %.6f]", assetId, latitude, longitude);
    }
}
