package com.geotrack.common.validation;

/**
 * Utility class for validating geographic coordinates.
 */
public final class CoordinateValidator {

    private CoordinateValidator() {
        // Utility class — no instantiation
    }

    /**
     * Validate that latitude is within WGS84 bounds.
     *
     * @param latitude Latitude to validate
     * @return true if valid (-90 to 90 inclusive)
     */
    public static boolean isValidLatitude(double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    /**
     * Validate that longitude is within WGS84 bounds.
     *
     * @param longitude Longitude to validate
     * @return true if valid (-180 to 180 inclusive)
     */
    public static boolean isValidLongitude(double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    /**
     * Validate a coordinate pair.
     *
     * @param latitude  WGS84 latitude
     * @param longitude WGS84 longitude
     * @return true if both are valid
     */
    public static boolean isValidCoordinate(double latitude, double longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

    /**
     * Validate and throw if invalid.
     *
     * @throws IllegalArgumentException if coordinates are out of bounds
     */
    public static void requireValid(double latitude, double longitude) {
        if (!isValidLatitude(latitude)) {
            throw new IllegalArgumentException(
                    "Latitude out of bounds [-90, 90]: " + latitude);
        }
        if (!isValidLongitude(longitude)) {
            throw new IllegalArgumentException(
                    "Longitude out of bounds [-180, 180]: " + longitude);
        }
    }

    /**
     * Check if a point is on water (null island check).
     * Point (0, 0) is in the Gulf of Guinea — almost certainly bad data.
     */
    public static boolean isNullIsland(double latitude, double longitude) {
        return latitude == 0.0 && longitude == 0.0;
    }
}
