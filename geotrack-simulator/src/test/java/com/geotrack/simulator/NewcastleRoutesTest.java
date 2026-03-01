package com.geotrack.simulator;

import com.geotrack.simulator.fleet.NewcastleRoutes;
import com.geotrack.simulator.route.RoutePoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NewcastleRoutes")
class NewcastleRoutesTest {

    record NamedRoute(String name, List<RoutePoint> points) {}

    static Stream<NamedRoute> allRoutes() {
        return Stream.of(
                new NamedRoute("Bus Route", NewcastleRoutes.busRoute()),
                new NamedRoute("Train Route", NewcastleRoutes.trainRoute()),
                new NamedRoute("Plane Route", NewcastleRoutes.planeRoute()),
                new NamedRoute("Boat Route", NewcastleRoutes.boatRoute())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allRoutes")
    @DisplayName("route has valid waypoints")
    void routeHasValidWaypoints(NamedRoute route) {
        assertThat(route.points()).isNotEmpty();
        assertThat(route.points()).hasSizeGreaterThanOrEqualTo(5);

        for (RoutePoint p : route.points()) {
            // Wider range to include Durham (train) and approaches (plane)
            assertThat(p.latitude()).isBetween(54.7, 55.1);
            assertThat(p.longitude()).isBetween(-1.8, -1.3);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allRoutes")
    @DisplayName("route timestamps are monotonically increasing")
    void timestampsIncrease(NamedRoute route) {
        for (int i = 1; i < route.points().size(); i++) {
            assertThat(route.points().get(i).timestamp())
                    .isAfter(route.points().get(i - 1).timestamp());
        }
    }

    @Test
    @DisplayName("plane route has descending altitude")
    void planeDescends() {
        var route = NewcastleRoutes.planeRoute();
        RoutePoint first = route.getFirst();
        RoutePoint last = route.getLast();

        assertThat(first.elevation()).isGreaterThan(last.elevation());
    }

    @Test
    @DisplayName("boat route follows river east (longitude increases)")
    void boatGoesEast() {
        var route = NewcastleRoutes.boatRoute();
        RoutePoint first = route.getFirst();
        RoutePoint last = route.getLast();

        // River Tyne flows east, so longitude becomes less negative
        assertThat(last.longitude()).isGreaterThan(first.longitude());
    }
}
