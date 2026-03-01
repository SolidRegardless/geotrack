package com.geotrack.simulator.fleet;

import com.geotrack.common.model.AssetType;
import com.geotrack.common.model.Position;
import com.geotrack.simulator.route.RoutePoint;
import com.geotrack.simulator.route.RouteReplayer;
import io.quarkus.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Simulates a fleet of vehicles following Newcastle routes concurrently.
 * Each vehicle replays its route on a virtual thread, emitting positions
 * to a shared consumer (typically a Kafka producer).
 */
public class FleetSimulator {

    public record Vehicle(String assetId, String name, AssetType assetType, List<RoutePoint> route) {}

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final double speedMultiplier;

    public FleetSimulator(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    /**
     * Add default Newcastle demo fleet with real routes.
     */
    public FleetSimulator withNewcastleFleet() {
        vehicles.add(new Vehicle("TYNE-BUS-01", "Newcastle Q3 Bus", AssetType.VEHICLE, NewcastleRoutes.busRoute()));
        vehicles.add(new Vehicle("TYNE-TRAIN-01", "ECML Express", AssetType.VEHICLE, NewcastleRoutes.trainRoute()));
        vehicles.add(new Vehicle("TYNE-PLANE-01", "NCL Approach RW07", AssetType.AIRCRAFT, NewcastleRoutes.planeRoute()));
        vehicles.add(new Vehicle("TYNE-BOAT-01", "Tyne River Cruise", AssetType.VESSEL, NewcastleRoutes.boatRoute()));
        return this;
    }

    /**
     * Add a custom vehicle with a given route.
     */
    public FleetSimulator addVehicle(String assetId, String name, AssetType assetType, List<RoutePoint> route) {
        vehicles.add(new Vehicle(assetId, name, assetType, route));
        return this;
    }

    /**
     * Run the simulation — each vehicle replays its route on a virtual thread.
     */
    public List<Future<Void>> simulate(Consumer<Position> positionConsumer) {
        final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Log.infof("Starting fleet simulation: %d vehicles at %.1f× speed", vehicles.size(), speedMultiplier);

            return new ArrayList<>(vehicles.stream()
                    .map(vehicle -> executor.submit(() -> {
                        Thread.currentThread().setName("sim-" + vehicle.assetId());
                        Log.infof("Vehicle %s (%s) [%s] starting route with %d waypoints",
                                vehicle.assetId(), vehicle.name(), vehicle.assetType(), vehicle.route().size());
                        try {
                            new RouteReplayer(vehicle.route(), vehicle.assetId(), speedMultiplier)
                                    .replay(positionConsumer);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            Log.warnf("Vehicle %s interrupted", vehicle.assetId());
                        }
                        return (Void) null; // Cast required for Future<Void> type inference
                    }))
                    .toList());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
