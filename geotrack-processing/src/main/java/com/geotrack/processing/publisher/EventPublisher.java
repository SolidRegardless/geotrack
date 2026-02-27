package com.geotrack.processing.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geotrack.common.event.GeofenceBreached;
import com.geotrack.common.event.GeofenceExited;
import com.geotrack.common.event.PositionUpdated;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * Publishes processed events to downstream Kafka topics.
 */
@ApplicationScoped
public class EventPublisher {

    @Inject
    @Channel("position-processed")
    Emitter<String> positionEmitter;

    @Inject
    @Channel("alert-geofence")
    Emitter<String> alertEmitter;

    @Inject
    ObjectMapper objectMapper;

    public void publishProcessedPosition(PositionUpdated event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            positionEmitter.send(json);
            Log.debugf("Published processed position for asset %s", event.assetId());
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialise PositionUpdated event");
        }
    }

    public void publishGeofenceBreach(GeofenceBreached event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            alertEmitter.send(json);
            Log.warnf("Published geofence BREACH alert: asset %s entered '%s'",
                    event.assetId(), event.geofenceName());
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialise GeofenceBreached event");
        }
    }

    public void publishGeofenceExit(GeofenceExited event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            alertEmitter.send(json);
            Log.infof("Published geofence EXIT: asset %s left '%s'",
                    event.assetId(), event.geofenceName());
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialise GeofenceExited event");
        }
    }
}
