package com.geotrack.simulator.noise;

import java.util.Random;

/**
 * Adds realistic GPS noise to simulated positions.
 * <p>
 * Real-world GPS has ~3-5m accuracy with occasional larger jumps
 * caused by multipath reflections in urban canyons, atmospheric
 * interference, or temporary loss of satellite lock.
 */
public class GpsNoiseSimulator {

    private final Random random;
    private final double accuracyMetres;

    /**
     * @param accuracyMetres Typical GPS accuracy (standard deviation in metres)
     */
    public GpsNoiseSimulator(double accuracyMetres) {
        this.random = new Random();
        this.accuracyMetres = accuracyMetres;
    }

    /** Default: 3m accuracy (typical consumer GPS) */
    public GpsNoiseSimulator() {
        this(3.0);
    }

    /**
     * Apply GPS noise to a position.
     *
     * @param lat   Clean latitude
     * @param lon   Clean longitude
     * @param speed Clean speed (km/h)
     * @return [noisyLat, noisyLon, noisySpeed]
     */
    public double[] addNoise(double lat, double lon, double speed) {
        double metresPerDegreeLat = 111_320.0;
        double metresPerDegreeLon = 111_320.0 * Math.cos(Math.toRadians(lat));

        // Standard GPS drift: Gaussian, σ = accuracyMetres
        double latNoise = random.nextGaussian() * (accuracyMetres / metresPerDegreeLat);
        double lonNoise = random.nextGaussian() * (accuracyMetres / metresPerDegreeLon);

        // 1-in-50 chance of a larger jump (multipath / urban canyon effect)
        if (random.nextInt(50) == 0) {
            latNoise *= 5;
            lonNoise *= 5;
        }

        // Speed jitter: ±5%
        double speedNoise = speed * (1.0 + random.nextGaussian() * 0.05);
        if (speedNoise < 0) speedNoise = 0;

        return new double[]{
                lat + latNoise,
                lon + lonNoise,
                speedNoise
        };
    }
}
