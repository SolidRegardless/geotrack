package com.geotrack.simulator.route;

import java.time.Instant;

/**
 * A raw point from a GPX track, before asset assignment.
 */
public record RoutePoint(
        double latitude,
        double longitude,
        double elevation,
        Instant timestamp
) {}
