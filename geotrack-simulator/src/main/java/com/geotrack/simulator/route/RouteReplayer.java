package com.geotrack.simulator.route;

import com.geotrack.common.model.Position;
import com.geotrack.common.model.PositionSource;
import com.geotrack.common.spatial.SpatialEngine;
import io.quarkus.logging.Log;
import org.locationtech.jts.geom.Point;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Replays a parsed GPX route at configurable speed, emitting Position events.
 * Calculates speed and heading between consecutive points using Vincenty geodesic.
 */
public class RouteReplayer {

    private final SpatialEngine spatial = new SpatialEngine();
    private final List<RoutePoint> route;
    private final String assetId;
    private final double speedMultiplier;

    public RouteReplayer(List<RoutePoint> route, String assetId, double speedMultiplier) {
        this.route = route;
        this.assetId = assetId;
        this.speedMultiplier = Math.max(1.0, speedMultiplier);
    }

    /** Target interval between position updates (ms). Shorter = smoother. */
    private static final long UPDATE_INTERVAL_MS = 1500;

    /**
     * Replay the route with interpolation for smooth movement.
     * Emits positions every ~1.5s, interpolating between waypoints.
     */
    public void replay(Consumer<Position> positionConsumer) throws InterruptedException {
        if (route.isEmpty()) return;

        Log.infof("Replaying %d points for asset %s at %.1f× speed (interpolated)", route.size(), assetId, speedMultiplier);

        // Emit starting position
        emitPosition(positionConsumer, route.getFirst(), 0.0, 0.0);

        for (int i = 1; i < route.size(); i++) {
            RoutePoint prev = route.get(i - 1);
            RoutePoint current = route.get(i);

            Point prevPt = spatial.createPoint(prev.longitude(), prev.latitude());
            Point currPt = spatial.createPoint(current.longitude(), current.latitude());

            double distanceM = spatial.distanceMetres(prevPt, currPt);
            double heading = spatial.bearing(prevPt, currPt);

            Duration timeDelta = Duration.between(prev.timestamp(), current.timestamp());
            long segmentMs = (long) (timeDelta.toMillis() / speedMultiplier);
            double hours = timeDelta.toMillis() / 3_600_000.0;
            double speedKmh = hours > 0 ? (distanceM / 1000.0) / hours : 0.0;

            if (segmentMs <= UPDATE_INTERVAL_MS) {
                // Short segment — just sleep and emit the endpoint
                if (segmentMs > 0) Thread.sleep(segmentMs);
                emitPosition(positionConsumer, current, speedKmh, heading);
            } else {
                // Long segment — interpolate intermediate positions
                int steps = (int) Math.max(1, segmentMs / UPDATE_INTERVAL_MS);
                long stepSleepMs = segmentMs / steps;

                double dLat = current.latitude() - prev.latitude();
                double dLon = current.longitude() - prev.longitude();
                double dElev = current.elevation() - prev.elevation();

                for (int s = 1; s <= steps; s++) {
                    Thread.sleep(stepSleepMs);
                    double fraction = (double) s / steps;

                    double lat = prev.latitude() + dLat * fraction;
                    double lon = prev.longitude() + dLon * fraction;
                    double elev = prev.elevation() + dElev * fraction;

                    var interpPoint = new RoutePoint(lat, lon, elev, 
                            prev.timestamp().plus(timeDelta.multipliedBy(s).dividedBy(steps)));
                    emitPosition(positionConsumer, interpPoint, speedKmh, heading);
                }
            }
        }

        Log.infof("Route replay complete for asset %s (%d waypoints)", assetId, route.size());
    }

    private void emitPosition(Consumer<Position> consumer, RoutePoint point, double speed, double heading) {
        var position = new Position(
                UUID.randomUUID(), assetId,
                point.latitude(), point.longitude(),
                point.elevation(), speed, heading,
                Instant.now(), PositionSource.GPS
        );
        consumer.accept(position);
    }
}
