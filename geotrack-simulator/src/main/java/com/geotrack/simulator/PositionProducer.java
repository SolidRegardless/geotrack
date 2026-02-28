package com.geotrack.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.geotrack.common.model.Position;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.Record;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * Publishes Position events to Kafka's position.raw topic.
 * Thread-safe — called from multiple virtual threads during fleet simulation.
 */
@Unremovable
@ApplicationScoped
public class PositionProducer {

    @Inject
    @Channel("sim-positions")
    Emitter<Record<String, String>> emitter;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public void send(Position position) {
        try {
            String json = objectMapper.writeValueAsString(position);
            emitter.send(Record.of(position.assetId(), json));
            Log.debugf("Sent position for %s: [%.6f, %.6f] speed=%.1f km/h",
                    position.assetId(), position.latitude(), position.longitude(), position.speed());
        } catch (Exception e) {
            Log.errorf(e, "Failed to send position for %s", position.assetId());
        }
    }
}
