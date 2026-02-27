package com.geotrack.processing.config;

import com.geotrack.common.spatial.SpatialEngine;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI producer for SpatialEngine.
 * Makes the spatial engine available for injection throughout the processing service.
 */
public class SpatialConfig {

    @Produces
    @ApplicationScoped
    public SpatialEngine spatialEngine() {
        return new SpatialEngine();
    }
}
