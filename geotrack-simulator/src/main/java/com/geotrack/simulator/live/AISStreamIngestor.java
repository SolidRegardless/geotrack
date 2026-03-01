package com.geotrack.simulator.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geotrack.common.model.Position;
import com.geotrack.common.model.PositionSource;
import com.geotrack.simulator.PositionProducer;
import io.quarkus.logging.Log;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connects to AISStream.io WebSocket and ingests live ship position reports.
 */
public class AISStreamIngestor {

    private static final String WS_URL = "wss://stream.aisstream.io/v0/stream";
    private static final long RATE_LIMIT_MS = 15_000;
    private static final long RECONNECT_DELAY_MS = 5_000;

    private static final DateTimeFormatter TIME_PARSER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendLiteral(' ')
            .appendOffset("+HHMM", "+0000")
            .toFormatter();

    private final PositionProducer producer;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Instant> lastPublishTime = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicInteger totalReceived = new AtomicInteger();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ScheduledExecutorService scheduler;
    private HttpClient httpClient;

    public AISStreamIngestor(PositionProducer producer, String apiKey) {
        this.producer = producer;
        this.apiKey = apiKey;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ais-stats");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            int count = counter.getAndSet(0);
            if (count > 0) {
                Log.infof("🚢 Published %d ship positions in last 10s (%d vessels tracked)",
                        count, lastPublishTime.size());
            }
        }, 10, 10, TimeUnit.SECONDS);

        connect();
    }

    public void stop() {
        running.set(false);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

    private void connect() {
        if (!running.get()) return;

        Log.info("🚢 Connecting to AISStream.io...");

        String subscriptionJson = String.format("""
                {"Apikey":"%s","BoundingBoxes":[[[49.0,-12.0],[61.0,3.0]]],"FilterMessageTypes":["PositionReport"]}""",
                apiKey);

        this.httpClient = HttpClient.newHttpClient();
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(WS_URL), new WebSocket.Listener() {

                    private final StringBuilder buffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        Log.info("🚢 Connected to AISStream.io, sending subscription...");
                        webSocket.sendText(subscriptionJson, true)
                                .thenRun(() -> {
                                    Log.info("🚢 Subscription sent successfully, waiting for data...");
                                    webSocket.request(1);
                                });
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        buffer.append(data);
                        if (last) {
                            String message = buffer.toString();
                            buffer.setLength(0);
                            handleMessage(message);
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        buffer.append(new String(bytes));
                        if (last) {
                            String message = buffer.toString();
                            buffer.setLength(0);
                            handleMessage(message);
                        }
                        webSocket.request(1);
                        return null;
                    }

                    private void handleMessage(String message) {
                        int total = totalReceived.incrementAndGet();
                        if (total == 1 || total % 500 == 0) {
                            Log.infof("🚢 Raw AIS message #%d (len=%d): %.200s", total, message.length(), message);
                        }
                        processMessage(message);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        Log.warnf("🚢 WebSocket closed: %d %s", statusCode, reason);
                        scheduleReconnect();
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        Log.errorf(error, "🚢 WebSocket error");
                        scheduleReconnect();
                    }
                })
                .exceptionally(ex -> {
                    Log.errorf(ex, "🚢 Failed to connect to AISStream.io");
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (!running.get()) return;
        Log.infof("🚢 Reconnecting in %dms...", RECONNECT_DELAY_MS);
        scheduler.schedule(this::connect, RECONNECT_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void processMessage(String message) {
        try {
            JsonNode root = mapper.readTree(message);
            String messageType = root.path("MessageType").asText();
            if (!"PositionReport".equals(messageType)) {
                Log.debugf("Non-position message type: %s", messageType);
                return;
            }

            JsonNode meta = root.path("MetaData");
            JsonNode posReport = root.path("Message").path("PositionReport");

            String mmsi = meta.path("MMSI_String").asText("");
            if (mmsi.isEmpty()) return;

            Instant now = Instant.now();
            if (isRateLimited(mmsi, now)) return;

            double latitude = meta.path("latitude").asDouble();
            double longitude = meta.path("longitude").asDouble();
            if (latitude == 0.0 && longitude == 0.0) return;

            Position position = buildPosition(meta, posReport, mmsi, latitude, longitude, now);
            producer.send(position);
            lastPublishTime.put(mmsi, now);
            counter.incrementAndGet();

        } catch (Exception e) {
            Log.warnf(e, "Failed to process AIS message: %s", message.substring(0, Math.min(200, message.length())));
        }
    }

    private boolean isRateLimited(String mmsi, Instant now) {
        Instant lastTime = lastPublishTime.get(mmsi);
        return lastTime != null && now.toEpochMilli() - lastTime.toEpochMilli() < RATE_LIMIT_MS;
    }

    private Position buildPosition(JsonNode meta, JsonNode posReport, String mmsi,
                                   double latitude, double longitude, Instant now) {
        String shipName = meta.path("ShipName").asText("").trim();
        String assetId = "SHIP-" + (!shipName.isEmpty() ? shipName : mmsi);

        double speed = posReport.path("Sog").asDouble(0) * 1.852;
        double heading = resolveHeading(posReport);
        Instant timestamp = parseTimestamp(meta, now);

        return new Position(
                UUID.randomUUID(),
                assetId,
                latitude,
                longitude,
                0.0,
                speed,
                heading,
                timestamp,
                PositionSource.AIS
        );
    }

    private double resolveHeading(JsonNode posReport) {
        int trueHeading = posReport.path("TrueHeading").asInt(511);
        if (trueHeading != 511 && trueHeading >= 0 && trueHeading <= 360) {
            return trueHeading;
        }
        double cog = posReport.path("Cog").asDouble(0);
        if (cog < 0 || cog >= 360) {
            return 0;
        }
        return cog;
    }

    private Instant parseTimestamp(JsonNode meta, Instant fallback) {
        try {
            String timeUtc = meta.path("time_utc").asText("");
            String cleaned = timeUtc.replace(" UTC", "");
            return Instant.from(TIME_PARSER.parse(cleaned));
        } catch (Exception e) {
            return fallback;
        }
    }
}
