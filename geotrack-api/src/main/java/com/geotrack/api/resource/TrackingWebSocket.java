package com.geotrack.api.resource;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time position tracking.
 * <p>
 * Connected Angular clients receive position updates and alert notifications
 * pushed from the Kafka consumer. This avoids polling and gives sub-second
 * latency from GPS device → map marker movement.
 */
@ServerEndpoint("/ws/tracking")
@ApplicationScoped
public class TrackingWebSocket {

    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        Log.infof("WebSocket client connected: %s (total: %d)",
                session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        Log.infof("WebSocket client disconnected: %s (total: %d)",
                session.getId(), sessions.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        Log.errorf("WebSocket error for %s: %s",
                session.getId(), throwable.getMessage());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        // Client messages could be subscription filters (e.g., specific asset IDs)
        Log.debugf("Received from %s: %s", session.getId(), message);
    }

    /**
     * Broadcast a JSON message to all connected clients.
     * Called by the Kafka consumer bridge when processed events arrive.
     */
    public void broadcast(String json) {
        sessions.forEach(session -> {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(json, result -> {
                    if (!result.isOK()) {
                        Log.warnf("Failed to send to %s: %s",
                                session.getId(),
                                result.getException() != null
                                        ? result.getException().getMessage()
                                        : "unknown error");
                    }
                });
            }
        });
    }

    /**
     * Get the number of connected clients (for metrics/health).
     */
    public int getConnectionCount() {
        return sessions.size();
    }
}
