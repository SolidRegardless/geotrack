package com.geotrack.api.resource;

import com.geotrack.api.dto.CreateGeofenceRequest;
import com.geotrack.api.dto.GeofenceResponse;
import com.geotrack.api.service.GeofenceService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/geofences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Geofences", description = "Geofence zone management")
public class GeofenceResource {

    private final GeofenceService geofenceService;

    @Inject
    public GeofenceResource(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    @GET
    @Operation(summary = "List all geofences")
    public List<GeofenceResponse> listGeofences(
            @QueryParam("active") @DefaultValue("false") boolean activeOnly) {
        return activeOnly ? geofenceService.findActive() : geofenceService.findAll();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get geofence by ID")
    public GeofenceResponse getGeofence(@PathParam("id") UUID id) {
        return geofenceService.findById(id);
    }

    @POST
    @Operation(summary = "Create a new geofence")
    public Response createGeofence(@Valid CreateGeofenceRequest request) {
        GeofenceResponse geofence = geofenceService.create(request);
        URI location = URI.create("/api/v1/geofences/" + geofence.id());
        return Response.created(location).entity(geofence).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deactivate a geofence")
    public Response deleteGeofence(@PathParam("id") UUID id) {
        geofenceService.delete(id);
        return Response.noContent().build();
    }
}
