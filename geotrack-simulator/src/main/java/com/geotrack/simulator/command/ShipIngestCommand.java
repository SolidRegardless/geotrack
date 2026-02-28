package com.geotrack.simulator.command;

import com.geotrack.simulator.PositionProducer;
import com.geotrack.simulator.live.AISStreamIngestor;
import io.quarkus.logging.Log;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.CountDownLatch;

/**
 * Picocli subcommand that ingests live ship positions from AISStream.io
 * and publishes them to Kafka.
 */
@Unremovable
@Dependent
@Command(name = "ships", mixinStandardHelpOptions = true,
        description = "Ingest live ship positions from AISStream.io")
public class ShipIngestCommand implements Runnable {

    @Option(names = {"--api-key"}, description = "AISStream.io API key",
            defaultValue = "${env:AISSTREAM_API_KEY}")
    String apiKey;

    @Inject
    PositionProducer producer;

    private PositionProducer getProducer() {
        if (producer != null) return producer;
        return jakarta.enterprise.inject.spi.CDI.current().select(PositionProducer.class).get();
    }

    @Override
    public void run() {
        if (apiKey == null || apiKey.isBlank()) {
            Log.error("AISSTREAM_API_KEY not set. Pass --api-key or set the env var.");
            return;
        }
        String key = apiKey;
        Log.info("🚢 Starting AISStream.io ship ingest");

        final PositionProducer kafkaProducer = getProducer();
        final AISStreamIngestor ingestor = new AISStreamIngestor(kafkaProducer, key);
        final CountDownLatch shutdownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.info("🛑 Shutting down AIS ingestor...");
            ingestor.stop();
            shutdownLatch.countDown();
        }));

        ingestor.start();

        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Log.info("🚢 AIS ingestor stopped");
    }
}
