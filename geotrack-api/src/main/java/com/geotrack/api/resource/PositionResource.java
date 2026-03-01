package com.geotrack.api.resource;

import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.dto.SubmitPositionRequest;
import com.geotrack.api.service.PositionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;

/**
 * REST resource for position ingestion and querying.
 */
@Path("/api/v1/positions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Positions", description = "Position ingestion and spatial queries")
public class PositionResource {

    private final PositionService positionService;

    @Inject
    public PositionResource(PositionService positionService) {
        this.positionService = positionService;
    }

    @POST
    @Operation(summary = "Submit a position update")
    public Response submitPosition(@Valid SubmitPositionRequest request) {
        PositionResponse position = positionService.submit(request);
        return Response.status(Response.Status.CREATED).entity(position).build();
    }

    @GET
    @Operation(summary = "Get all recent positions (defaults to latest per asset)")
    public List<PositionResponse> getAllPositions() {
        return positionService.getLatestPositions();
    }

    @GET
    @Path("/latest")
    @Operation(summary = "Get latest position per asset")
    public List<PositionResponse> getLatestPositions() {
        return positionService.getLatestPositions();
    }

    @GET
    @Path("/history")
    @Operation(summary = "Get position history for an asset by string ID (handles special characters)")
    public List<PositionResponse> getPositionHistoryByQuery(
            @QueryParam("assetId") String assetId,
            @QueryParam("from") String fromStr,
            @QueryParam("to") String toStr,
            @QueryParam("limit") @DefaultValue("1000") int limit) {

        Instant from = fromStr != null ? Instant.parse(fromStr) : null;
        Instant to = toStr != null ? Instant.parse(toStr) : null;
        return positionService.getPositionHistory(assetId, from, to, limit);
    }
}
