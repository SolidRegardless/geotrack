package com.geotrack.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.geotrack.simulator.noise.GpsNoiseSimulator;
import com.geotrack.simulator.pattern.PatrolLoop;
import com.geotrack.simulator.pattern.RandomWalk;
import com.geotrack.simulator.pattern.WaypointRoute;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fleet simulator generating realistic GPS tracks around Newcastle upon Tyne.
 * <p>
 * Simulates 10 assets with different movement patterns:
 * - 4 delivery vehicles following routes between Newcastle landmarks
 * - 2 drones doing patrol patterns over the Quayside
 * - 1 vessel moving along the River Tyne
 * - 3 personnel walking around the city centre
 * <p>
 * Positions are published to Kafka every 2 seconds with realistic GPS noise.
 * This is the data source that drives the entire real-time pipeline.
 */
@ApplicationScoped
public class NewcastleFleetSimulator {

    private final List<SimulatedAsset> fleet = new ArrayList<>();
    private final GpsNoiseSimulator gpsNoise = new GpsNoiseSimulator(3.0);
    private final ObjectMapper mapper;

    @Inject
    @Channel("simulated-positions")
    Emitter<String> positionEmitter;

    public NewcastleFleetSimulator() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        initFleet();
    }

    private void initFleet() {
        // === VEHICLES: Delivery routes across Newcastle ===

        // Vehicle 1: City Centre → Jesmond → Heaton → back
        fleet.add(new SimulatedAsset("VEHICLE-001", "VEHICLE",
                new WaypointRoute(List.of(
                        new double[]{54.9738, -1.6131},  // Grey's Monument
                        new double[]{54.9818, -1.6035},  // Jesmond Dene
                        new double[]{54.9800, -1.5850},  // Heaton Park
                        new double[]{54.9740, -1.5950},  // Byker
                        new double[]{54.9690, -1.6050}   // Ouseburn
                ), 40),
                54.9738, -1.6131));

        // Vehicle 2: Gateshead → Team Valley → Metro Centre
        fleet.add(new SimulatedAsset("VEHICLE-002", "VEHICLE",
                new WaypointRoute(List.of(
                        new double[]{54.9527, -1.6037},  // Gateshead
                        new double[]{54.9400, -1.6200},  // Team Valley
                        new double[]{54.9580, -1.6650},  // Metro Centre
                        new double[]{54.9600, -1.6400}   // Dunston
                ), 50),
                54.9527, -1.6037));

        // Vehicle 3: Coast Road route
        fleet.add(new SimulatedAsset("VEHICLE-003", "VEHICLE",
                new WaypointRoute(List.of(
                        new double[]{54.9783, -1.6178},  // Newcastle centre
                        new double[]{54.9850, -1.5700},  // Wallsend
                        new double[]{54.9950, -1.5300},  // North Shields
                        new double[]{55.0100, -1.4800}   // Tynemouth
                ), 60),
                54.9783, -1.6178));

        // Vehicle 4: A1 corridor
        fleet.add(new SimulatedAsset("VEHICLE-004", "VEHICLE",
                new WaypointRoute(List.of(
                        new double[]{54.9783, -1.6178},  // Newcastle
                        new double[]{54.9500, -1.6100},  // Gateshead
                        new double[]{54.9100, -1.5900},  // Washington
                        new double[]{54.8690, -1.5750}   // Chester-le-Street
                ), 70),
                54.9783, -1.6178));

        // === DRONES: Patrol patterns over the Quayside ===

        fleet.add(new SimulatedAsset("DRONE-001", "DRONE",
                new PatrolLoop(List.of(
                        new double[]{54.9705, -1.6100},  // Tyne Bridge
                        new double[]{54.9700, -1.6000},  // Quayside East
                        new double[]{54.9720, -1.5950},  // Ouseburn
                        new double[]{54.9730, -1.6050}   // BALTIC
                ), 25),
                54.9705, -1.6100));

        fleet.add(new SimulatedAsset("DRONE-002", "DRONE",
                new PatrolLoop(List.of(
                        new double[]{54.9680, -1.6200},  // Gateshead Quays
                        new double[]{54.9700, -1.6150},  // Millennium Bridge
                        new double[]{54.9710, -1.6100},  // Sage Gateshead
                        new double[]{54.9690, -1.6250}   // South Shore
                ), 20),
                54.9680, -1.6200));

        // === VESSEL: Along the River Tyne ===

        fleet.add(new SimulatedAsset("VESSEL-001", "VESSEL",
                new WaypointRoute(List.of(
                        new double[]{54.9680, -1.6300},  // Redheugh Bridge
                        new double[]{54.9695, -1.6130},  // Tyne Bridge
                        new double[]{54.9710, -1.5900},  // East Quayside
                        new double[]{54.9800, -1.5500},  // Walker
                        new double[]{54.9900, -1.4700}   // North Shields Ferry
                ), 15),
                54.9680, -1.6300));

        // === PERSONNEL: Walking around city centre ===

        fleet.add(new SimulatedAsset("PERSON-001", "PERSONNEL",
                new RandomWalk(54.9745, -1.6140, 0.003, 5),
                54.9745, -1.6140));  // Around Eldon Square

        fleet.add(new SimulatedAsset("PERSON-002", "PERSONNEL",
                new RandomWalk(54.9783, -1.6050, 0.004, 4),
                54.9783, -1.6050));  // Around St James' Park

        fleet.add(new SimulatedAsset("PERSON-003", "PERSONNEL",
                new RandomWalk(54.9700, -1.6100, 0.003, 5),
                54.9700, -1.6100));  // Around Quayside
    }

    /**
     * Scheduled task — ticks every 2 seconds, publishing positions to Kafka.
     * Each asset advances along its movement pattern, GPS noise is applied,
     * and the resulting position is published to the 'position.raw' topic.
     */
    @Scheduled(every = "2s")
    void tick() {
        for (SimulatedAsset asset : fleet) {
            try {
                // Advance simulation
                asset.tick(2.0);

                // Apply GPS noise
                double[] noisy = gpsNoise.addNoise(
                        asset.getCurrentLat(),
                        asset.getCurrentLon(),
                        asset.getSpeed()
                );

                // Build position event JSON
                var event = new PositionEvent(
                        asset.getAssetId(),
                        noisy[0],  // noisy latitude
                        noisy[1],  // noisy longitude
                        0.0,       // altitude
                        noisy[2],  // noisy speed
                        asset.getHeading(),
                        Instant.now()
                );

                String json = mapper.writeValueAsString(event);
                positionEmitter.send(json);

                Log.debugf("Simulated %s at [%.6f, %.6f] %.1f km/h",
                        asset.getAssetId(), noisy[0], noisy[1], noisy[2]);

            } catch (Exception e) {
                Log.errorf(e, "Failed to simulate asset %s", asset.getAssetId());
            }
        }
        Log.infof("Tick: published %d positions", fleet.size());
    }

    public int getFleetSize() {
        return fleet.size();
    }

    /** JSON-serializable position event */
    public record PositionEvent(
            String assetId,
            double latitude,
            double longitude,
            double altitude,
            double speed,
            double heading,
            Instant timestamp
    ) {}
}
