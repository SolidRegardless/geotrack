package com.geotrack.simulator;

import com.geotrack.simulator.command.LiveIngestCommand;
import com.geotrack.simulator.command.ShipIngestCommand;
import com.geotrack.simulator.fleet.FleetSimulator;
import com.geotrack.simulator.route.GpxParser;
import com.geotrack.simulator.route.RouteReplayer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileInputStream;
import java.util.concurrent.Future;

/**
 * CLI entry point for the GeoTrack Simulator.
 * <p>
 * Must be {@code @Dependent} so Quarkus CDI creates the instance and injects fields.
 */
@Dependent
@Command(name = "simulate", mixinStandardHelpOptions = true,
        subcommands = {LiveIngestCommand.class, ShipIngestCommand.class},
        description = "GeoTrack fleet simulation and GPX route replay")
public class SimulatorCommand implements Runnable {

    @Option(names = {"--fleet"}, description = "Run Newcastle demo fleet (4 vehicles)")
    boolean fleet;

    @Option(names = {"--gpx"}, description = "Path to GPX file for single-vehicle replay")
    String gpxFile;

    @Option(names = {"--asset-id"}, description = "Asset ID for GPX replay", defaultValue = "GPX-001")
    String assetId;

    @Option(names = {"--speed"}, description = "Speed multiplier (e.g. 10 = 10× real time)", defaultValue = "10")
    double speedMultiplier;

    @Option(names = {"--dry-run"}, description = "Print positions to stdout instead of Kafka")
    boolean dryRun;

    @Inject
    PositionProducer producer;

    @Inject
    GpxParser gpxParser;

    private PositionProducer getProducer() {
        if (producer != null) return producer;
        // Fallback: lookup from CDI container directly
        return jakarta.enterprise.inject.spi.CDI.current().select(PositionProducer.class).get();
    }

    @Override
    public void run() {
        if (fleet) {
            runFleet();
        } else if (gpxFile != null) {
            runGpxReplay();
        } else {
            Log.info("No mode specified. Use --fleet for Newcastle demo or --gpx <file> for GPX replay.");
            Log.info("Try: simulate --fleet --speed 10");
        }
    }

    private void runFleet() {
        Log.infof("🚛 Starting Newcastle fleet simulation at %.1f× speed", speedMultiplier);

        // Capture CDI proxy reference before spawning virtual threads
        final PositionProducer kafkaProducer = getProducer();
        Log.infof("Producer injected: %s", kafkaProducer != null ? "yes" : "NO");

        var simulator = new FleetSimulator(speedMultiplier).withNewcastleFleet();
        var futures = simulator.simulate(position -> {
            if (dryRun) {
                System.out.printf("[%s] %.6f, %.6f | speed=%.1f km/h heading=%.0f°%n",
                        position.assetId(), position.latitude(), position.longitude(),
                        position.speed(), position.heading());
            } else {
                kafkaProducer.send(position);
            }
        });

        // Wait for all vehicles to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Log.errorf(e, "Vehicle simulation failed");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.errorf(e, "Vehicle simulation failed");
            }
        }

        Log.info("🏁 Fleet simulation complete");
    }

    private void runGpxReplay() {
        Log.infof("📍 Replaying GPX file: %s as asset %s at %.1f× speed", gpxFile, assetId, speedMultiplier);

        try (var input = new FileInputStream(gpxFile)) {
            var route = gpxParser.parse(input);
            Log.infof("Parsed %d waypoints from GPX", route.size());

            final PositionProducer kafkaProducer = getProducer();

            new RouteReplayer(route, assetId, speedMultiplier).replay(position -> {
                if (dryRun) {
                    System.out.printf("[%s] %.6f, %.6f | speed=%.1f km/h%n",
                            position.assetId(), position.latitude(), position.longitude(), position.speed());
                } else {
                    kafkaProducer.send(position);
                }
            });

            Log.info("🏁 GPX replay complete");
        } catch (InterruptedException e) {
            Log.errorf(e, "GPX replay failed");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.errorf(e, "GPX replay failed");
        }
    }
}
