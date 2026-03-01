package com.geotrack.simulator.command;

import com.geotrack.simulator.PositionProducer;
import com.geotrack.simulator.live.OpenSkyIngestor;
import io.quarkus.logging.Log;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

/**
 * Picocli subcommand that ingests live aircraft positions from the OpenSky Network API
 * and publishes them to Kafka.
 */
@Unremovable
@Dependent
@Command(name = "live", mixinStandardHelpOptions = true,
        description = "Ingest live aircraft positions from OpenSky Network")
public class LiveIngestCommand implements Runnable {

    @Option(names = {"--interval"}, description = "Poll interval in seconds (default: 10)", defaultValue = "10")
    int intervalSeconds;

    @Option(names = {"--bbox"}, description = "Bounding box override: lamin,lomin,lamax,lomax")
    String bbox;

    @Inject
    PositionProducer producer;

    private PositionProducer getProducer() {
        if (producer != null) return producer;
        return jakarta.enterprise.inject.spi.CDI.current().select(PositionProducer.class).get();
    }

    @Override
    public void run() {
        Log.infof("✈ Starting OpenSky live ingest (interval=%ds, bbox=%s)",
                intervalSeconds, bbox != null ? bbox : "UK default");

        final PositionProducer kafkaProducer = getProducer();
        final OpenSkyIngestor ingestor = new OpenSkyIngestor(kafkaProducer, bbox);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);

        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "opensky-poller");
            t.setDaemon(true);
            return t;
        });
        try {
            // Graceful shutdown on SIGINT
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Log.info("🛑 Shutting down OpenSky ingestor...");
                scheduler.shutdown();
                shutdownLatch.countDown();
            }));

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    ingestor.poll();
                } catch (Exception e) {
                    Log.errorf(e, "Error during OpenSky poll cycle");
                }
            }, 0, intervalSeconds, TimeUnit.SECONDS);

            // Block until shutdown signal
            try {
                shutdownLatch.await();
                scheduler.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } finally {
            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        Log.info("✈ OpenSky ingestor stopped");
    }
}
