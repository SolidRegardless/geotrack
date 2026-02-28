package com.geotrack.simulator.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geotrack.common.model.Position;
import com.geotrack.common.model.PositionSource;
import com.geotrack.simulator.PositionProducer;
import io.quarkus.logging.Log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

/**
 * Polls the OpenSky Network REST API for live aircraft state vectors
 * and publishes each as a Position to Kafka.
 */
public class OpenSkyIngestor {

    private static final String DEFAULT_URL =
            "https://opensky-network.org/api/states/all?lamin=49.9&lomin=-8.2&lamax=60.9&lomax=1.8";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final PositionProducer producer;
    private final String apiUrl;

    public OpenSkyIngestor(PositionProducer producer, String bboxOverride) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.producer = producer;
        this.apiUrl = (bboxOverride != null && !bboxOverride.isBlank())
                ? buildUrl(bboxOverride)
                : DEFAULT_URL;
    }

    private String buildUrl(String bbox) {
        // bbox format: "lamin,lomin,lamax,lomax"
        String[] parts = bbox.split(",");
        if (parts.length != 4) {
            Log.warnf("Invalid bbox '%s', using default", bbox);
            return DEFAULT_URL;
        }
        return String.format(
                "https://opensky-network.org/api/states/all?lamin=%s&lomin=%s&lamax=%s&lomax=%s",
                parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
    }

    /**
     * Polls OpenSky once and publishes all valid positions. Returns count of published positions.
     */
    public int poll() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                Log.warnf("OpenSky API returned HTTP %d", response.statusCode());
                return 0;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode states = root.get("states");

            if (states == null || !states.isArray()) {
                Log.warn("No aircraft states in response");
                return 0;
            }

            int count = 0;
            for (JsonNode state : states) {
                Position position = mapToPosition(state);
                if (position != null) {
                    producer.send(position);
                    count++;
                }
            }

            Log.infof("✈ Ingested %d aircraft positions from OpenSky", count);
            return count;

        } catch (Exception e) {
            Log.errorf(e, "Failed to poll OpenSky API");
            return 0;
        }
    }

    private Position mapToPosition(JsonNode state) {
        // Filter out aircraft with null lat/lon
        if (state.get(5).isNull() || state.get(6).isNull()) {
            return null;
        }

        double longitude = state.get(5).asDouble();
        double latitude = state.get(6).asDouble();

        // assetId: callsign (trimmed) or icao24 fallback
        String callsign = state.get(1).isNull() ? null : state.get(1).asText().trim();
        String icao24 = state.get(0).asText();
        String assetId = (callsign != null && !callsign.isEmpty()) ? callsign : icao24;

        // altitude: prefer geo_altitude, fallback baro_altitude
        double altitude = 0.0;
        if (!state.get(13).isNull()) {
            altitude = state.get(13).asDouble();
        } else if (!state.get(7).isNull()) {
            altitude = state.get(7).asDouble();
        }

        // speed: velocity in m/s → km/h
        double speed = 0.0;
        if (!state.get(9).isNull()) {
            speed = state.get(9).asDouble() * 3.6;
        }

        // heading: true_track
        double heading = 0.0;
        if (!state.get(10).isNull()) {
            heading = state.get(10).asDouble();
        }

        // timestamp from time_position, fallback to now
        Instant timestamp;
        if (!state.get(3).isNull()) {
            timestamp = Instant.ofEpochSecond(state.get(3).asLong());
        } else {
            timestamp = Instant.now();
        }

        try {
            return new Position(
                    UUID.randomUUID(),
                    assetId,
                    latitude,
                    longitude,
                    altitude,
                    speed,
                    heading,
                    timestamp,
                    PositionSource.ADS_B
            );
        } catch (IllegalArgumentException e) {
            Log.debugf("Skipping invalid position for %s: %s", assetId, e.getMessage());
            return null;
        }
    }
}
