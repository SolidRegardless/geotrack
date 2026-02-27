package com.geotrack.api.resource;

import com.geotrack.api.dto.AssetResponse;
import com.geotrack.api.dto.CreateAssetRequest;
import com.geotrack.api.dto.PositionResponse;
import com.geotrack.api.service.AssetService;
import com.geotrack.api.service.PositionService;
import com.geotrack.common.model.AssetStatus;
import com.geotrack.common.model.AssetType;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST resource for asset management.
 * JAX-RS endpoints with OpenAPI documentation.
 */
@Path("/api/v1/assets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Assets", description = "Asset management and tracking")
public class AssetResource {

    private final AssetService assetService;
    private final PositionService positionService;

    @Inject
    public AssetResource(AssetService assetService, PositionService positionService) {
        this.assetService = assetService;
        this.positionService = positionService;
    }

    @GET
    @Operation(summary = "List assets", description = "Returns paginated list of assets with optional filtering")
    public Response listAssets(
            @QueryParam("type") AssetType type,
            @QueryParam("status") AssetStatus status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") @DefaultValue("name") String sort) {

        List<AssetResponse> assets = assetService.findAll(type, status, page, size, sort);
        long total = assetService.countAll(type, status);

        return Response.ok(assets)
                .header("X-Total-Count", total)
                .header("X-Page", page)
                .header("X-Page-Size", size)
                .build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get asset by ID")
    public AssetResponse getAsset(@PathParam("id") UUID id) {
        return assetService.findById(id);
    }

    @POST
    @Operation(summary = "Create a new asset")
    public Response createAsset(@Valid CreateAssetRequest request) {
        AssetResponse asset = assetService.create(request);
        URI location = URI.create("/api/v1/assets/" + asset.id());
        return Response.created(location).entity(asset).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deactivate an asset")
    public Response deleteAsset(@PathParam("id") UUID id) {
        assetService.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/positions")
    @Operation(summary = "Get position history for an asset")
    public List<PositionResponse> getPositionHistory(
            @PathParam("id") UUID assetId,
            @QueryParam("from") Instant from,
            @QueryParam("to") Instant to,
            @QueryParam("limit") @DefaultValue("1000") int limit) {

        return positionService.getPositionHistory(assetId, from, to, limit);
    }
}
