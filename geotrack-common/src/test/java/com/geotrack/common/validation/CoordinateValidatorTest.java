package com.geotrack.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateValidatorTest {

    @ParameterizedTest
    @ValueSource(doubles = {-90.0, -45.0, 0.0, 45.0, 54.9783, 90.0})
    @DisplayName("Should accept valid latitudes")
    void shouldAcceptValidLatitudes(double lat) {
        assertTrue(CoordinateValidator.isValidLatitude(lat));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.001, -91.0, 90.001, 91.0, 180.0, -180.0, Double.NaN})
    @DisplayName("Should reject invalid latitudes")
    void shouldRejectInvalidLatitudes(double lat) {
        assertFalse(CoordinateValidator.isValidLatitude(lat));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.0, -1.6178, 0.0, 1.0, 179.9999, 180.0})
    @DisplayName("Should accept valid longitudes")
    void shouldAcceptValidLongitudes(double lon) {
        assertTrue(CoordinateValidator.isValidLongitude(lon));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.001, -181.0, 180.001, 181.0, 360.0, Double.NaN})
    @DisplayName("Should reject invalid longitudes")
    void shouldRejectInvalidLongitudes(double lon) {
        assertFalse(CoordinateValidator.isValidLongitude(lon));
    }

    @ParameterizedTest
    @CsvSource({
            "54.9783, -1.6178, true",   // Newcastle
            "51.5074, -0.1276, true",   // London
            "90.0, 180.0, true",        // North pole at dateline
            "-90.0, -180.0, true",      // South pole at dateline
            "91.0, 0.0, false",         // Invalid latitude
            "0.0, 181.0, false",        // Invalid longitude
            "91.0, 181.0, false"        // Both invalid
    })
    @DisplayName("Should validate coordinate pairs")
    void shouldValidateCoordinatePairs(double lat, double lon, boolean expected) {
        assertEquals(expected, CoordinateValidator.isValidCoordinate(lat, lon));
    }

    @Test
    @DisplayName("Should throw on invalid coordinates via requireValid")
    void shouldThrowOnInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> CoordinateValidator.requireValid(91.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> CoordinateValidator.requireValid(0.0, 181.0));
    }

    @Test
    @DisplayName("Should not throw on valid coordinates via requireValid")
    void shouldNotThrowOnValid() {
        assertDoesNotThrow(() -> CoordinateValidator.requireValid(54.9783, -1.6178));
    }

    @Test
    @DisplayName("Should detect null island")
    void shouldDetectNullIsland() {
        assertTrue(CoordinateValidator.isNullIsland(0.0, 0.0));
        assertFalse(CoordinateValidator.isNullIsland(54.9783, -1.6178));
        assertFalse(CoordinateValidator.isNullIsland(0.0, 1.0));
    }
}
