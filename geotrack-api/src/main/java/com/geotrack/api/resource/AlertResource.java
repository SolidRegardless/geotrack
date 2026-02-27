package com.geotrack.api.resource;

import com.geotrack.api.dto.AlertResponse;
import com.geotrack.api.service.AlertService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Alerts", description = "Alert management and acknowledgement")
public class AlertResource {

    private final AlertService alertService;

    @Inject
    public AlertResource(AlertService alertService) {
        this.alertService = alertService;
    }

    @GET
    @Operation(summary = "List alerts")
    public List<AlertResponse> listAlerts(
            @QueryParam("acknowledged") Boolean acknowledged) {
        if (acknowledged != null && !acknowledged) {
            return alertService.findUnacknowledged();
        }
        return alertService.findAll();
    }

    @GET
    @Path("/asset/{assetId}")
    @Operation(summary = "Get alerts for a specific asset")
    public List<AlertResponse> getAlertsByAsset(@PathParam("assetId") UUID assetId) {
        return alertService.findByAssetId(assetId);
    }

    @PUT
    @Path("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an alert")
    public Response acknowledgeAlert(
            @PathParam("id") UUID alertId,
            @QueryParam("username") @DefaultValue("operator") String username) {
        alertService.acknowledge(alertId, username);
        return Response.ok().build();
    }
}
