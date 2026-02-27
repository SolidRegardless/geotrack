package com.geotrack.api.service;

import com.geotrack.api.resource.TrackingWebSocket;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Bridges Kafka topics to WebSocket clients.
 * <p>
 * Consumes processed position events and geofence alerts from Kafka,
 * then broadcasts them to all connected WebSocket clients. This completes
 * the real-time data flow:
 * <pre>
 * GPS Device → Ingestion API → Kafka (position.raw)
 *   → Processing Service → Kafka (position.processed / alert.geofence)
 *   → THIS BRIDGE → WebSocket → Angular Map
 * </pre>
 */
@ApplicationScoped
public class WebSocketBridge {

    private final TrackingWebSocket trackingWebSocket;

    @Inject
    public WebSocketBridge(TrackingWebSocket trackingWebSocket) {
        this.trackingWebSocket = trackingWebSocket;
    }

    /**
     * Consume processed positions and push to WebSocket clients.
     */
    @Incoming("ws-positions")
    @Blocking
    public void onProcessedPosition(String payload) {
        Log.debugf("Broadcasting position to %d WebSocket clients",
                trackingWebSocket.getConnectionCount());

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
}
