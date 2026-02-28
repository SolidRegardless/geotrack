package com.geotrack.simulator;

import com.geotrack.common.model.Position;
import com.geotrack.simulator.fleet.NewcastleRoutes;
import com.geotrack.simulator.route.RouteReplayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RouteReplayer")
class RouteReplayerTest {

    @Test
    @DisplayName("emits correct number of positions for route")
    void emitsAllPositions() throws InterruptedException {
        var route = NewcastleRoutes.quaysideRoute();
        List<Position> emitted = new ArrayList<>();

        // Very high speed multiplier so test runs fast
        new RouteReplayer(route, "TEST-001", 100_000).replay(emitted::add);

        assertThat(emitted).hasSize(route.size());
    }

    @Test
    @DisplayName("calculates speed between consecutive points")
    void calculatesSpeed() throws InterruptedException {
        var route = NewcastleRoutes.angelToCity(); // longer route with real spacing
        List<Position> emitted = new ArrayList<>();

        new RouteReplayer(route, "TEST-002", 100_000).replay(emitted::add);

        // First point has no speed (no previous point)
        assertThat(emitted.get(0).speed()).isEqualTo(0.0);

        // Subsequent points should have positive speed (vehicle is moving)
        assertThat(emitted.get(1).speed()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("sets asset ID on all emitted positions")
    void setsAssetId() throws InterruptedException {
        var route = NewcastleRoutes.jesmondDene();
        List<Position> emitted = new ArrayList<>();

        new RouteReplayer(route, "RANGER-01", 100_000).replay(emitted::add);

        assertThat(emitted).allMatch(p -> p.assetId().equals("RANGER-01"));
    }

    @Test
    @DisplayName("handles empty route gracefully")
    void handlesEmptyRoute() throws InterruptedException {
        List<Position> emitted = new ArrayList<>();
        new RouteReplayer(List.of(), "EMPTY", 100_000).replay(emitted::add);
        assertThat(emitted).isEmpty();
    }

    @Test
    @DisplayName("calculates heading between points")
    void calculatesHeading() throws InterruptedException {
        var route = NewcastleRoutes.quaysideRoute();
        List<Position> emitted = new ArrayList<>();

        new RouteReplayer(route, "TEST-003", 100_000).replay(emitted::add);

        // First point: no heading
        assertThat(emitted.get(0).heading()).isEqualTo(0.0);

        // Subsequent headings should be valid (0-360)
        for (int i = 1; i < emitted.size(); i++) {
            assertThat(emitted.get(i).heading()).isBetween(0.0, 360.0);
        }
    }
}
