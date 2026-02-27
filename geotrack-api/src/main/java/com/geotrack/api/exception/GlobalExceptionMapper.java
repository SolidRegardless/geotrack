package com.geotrack.api.exception;

import com.geotrack.api.service.AssetService;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import io.quarkus.logging.Log;

/**
 * Global exception mapper providing consistent error responses.
 * Maps domain exceptions to appropriate HTTP status codes.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    public record ErrorResponse(String error, String message, int status) {}

    @Override
    public Response toResponse(Exception exception) {
        // Use Java 21 pattern matching switch for clean exception dispatch
        return switch (exception) {
            case AssetService.AssetNotFoundException e ->
                    error(Response.Status.NOT_FOUND, "Not Found", e.getMessage());
            case IllegalArgumentException e ->
                    error(Response.Status.BAD_REQUEST, "Bad Request", e.getMessage());
            case jakarta.validation.ConstraintViolationException e ->
                    error(Response.Status.BAD_REQUEST, "Validation Error", e.getMessage());
            default -> {
                Log.error("Unhandled exception", exception);
                yield error(Response.Status.INTERNAL_SERVER_ERROR,
                        "Internal Server Error", "An unexpected error occurred");
            }
        };
    }

    private Response error(Response.Status status, String error, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(error, message, status.getStatusCode()))
                .build();
    }
}
