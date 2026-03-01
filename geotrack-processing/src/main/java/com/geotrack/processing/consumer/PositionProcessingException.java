package com.geotrack.processing.consumer;

public class PositionProcessingException extends RuntimeException {

    public PositionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
