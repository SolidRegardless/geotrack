package com.geotrack.processing.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geotrack.common.event.GeofenceBreached;
import com.geotrack.common.event.GeofenceExited;
import com.geotrack.common.event.PositionUpdated;
import com.geotrack.common.model.Position;
import com.geotrack.common.model.PositionSource;
import com.geotrack.common.validation.CoordinateValidator;
import com.geotrack.processing.geofence.GeofenceEngine;
import com.geotrack.processing.geofence.GeofenceEngine.GeofenceTransition;
import com.geotrack.processing.publisher.EventPublisher;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer for raw position events.
 * <p>
 * Consumes from the 'position.raw' topic, validates, enriches,
 * checks geofences, and publishes processed events.
 * <p>
 * {@code @Blocking} ensures processing runs on a worker thread (or virtual thread),
 * keeping the Vert.x event loop free.
 */
@ApplicationScoped
public class PositionEventConsumer {

    private final GeofenceEngine geofenceEngine;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Inject
    public PositionEventConsumer(
            GeofenceEngine geofenceEngine,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.geofenceEngine = geofenceEngine;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes raw position events from Kafka.
     * SmallRye Reactive Messaging handles deserialization, offset commits,
     * and back-pressure automatically.
     */
    @Incoming("position-raw")
    @Blocking
    public void consume(String payload) {
        try {
            RawPositionEvent raw = objectMapper.readValue(payload, RawPositionEvent.class);

            // Validate coordinates
            if (!CoordinateValidator.isValidCoordinate(raw.latitude(), raw.longitude())) {
                Log.warnf("Invalid coordinates for asset %s: [%f, %f] — discarding",
                        raw.assetId(), raw.latitude(), raw.longitude());
                return;
            }

            if (CoordinateValidator.isNullIsland(raw.latitude(), raw.longitude())) {
                Log.warnf("Null Island position for asset %s — discarding", raw.assetId());
                return;
            }

            // Create domain position
            Position position = new Position(
                    UUID.randomUUID(),
                    raw.assetId(),
                    raw.latitude(),
                    raw.longitude(),
                    raw.altitude(),
                    raw.speed(),
                    raw.heading(),
                    raw.timestamp() != null ? raw.timestamp() : Instant.now(),
                    PositionSource.GPS
            );

            Log.debugf("Processing position for asset %s at [%f, %f]",
                    position.assetId(), position.latitude(), position.longitude());

            // Check geofences
            var transitions = geofenceEngine.checkPosition(position);
            for (GeofenceTransition transition : transitions) {
                if (transition.isEntry()) {
                    eventPublisher.publishGeofenceBreach(
                            GeofenceBreached.create(
                                    position.assetId(),
                                    transition.geofenceId(),
                                    transition.geofenceName(),
                                    position
                            )
                    );
                } else if (transition.isExit()) {
                    eventPublisher.publishGeofenceExit(
                            GeofenceExited.create(
                                    position.assetId(),
                                    transition.geofenceId(),
                                    transition.geofenceName(),
                                    position
                            )
                    );
                }
            }

            // Publish processed position
            eventPublisher.publishProcessedPosition(
                    PositionUpdated.create(position, null)
            );

        } catch (Exception e) {
            Log.errorf(e, "Failed to process position event: %s", payload);
            // SmallRye DLQ strategy will route this to position.dlq
            throw new RuntimeException("Position processing failed", e);
        }
    }

    /**
     * Raw position event DTO — matches the JSON published by ingestion.
     */
    public record RawPositionEvent(
            String assetId,
            double latitude,
            double longitude,
            double altitude,
            double speed,
            double heading,
            Instant timestamp
    ) {}
}
