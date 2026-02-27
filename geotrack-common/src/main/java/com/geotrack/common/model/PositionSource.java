package com.geotrack.common.model;

/**
 * Enumeration of position data sources.
 */
public enum PositionSource {
    GPS,
    GLONASS,
    GALILEO,
    AIS,        // Automatic Identification System (maritime)
    ADS_B,      // Automatic Dependent Surveillance (aviation)
    MANUAL,
    SIMULATED
}
