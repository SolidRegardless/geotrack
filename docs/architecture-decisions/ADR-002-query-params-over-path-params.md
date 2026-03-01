# ADR-002: Query Parameters Over Path Parameters for Asset IDs

**Status:** Accepted  
**Date:** 2026-03-01  
**Deciders:** Stuart  

## Context

GeoTrack ingests ship asset data from AISStream.io where asset IDs frequently contain special characters, including forward slashes and spaces (e.g., `SHIP-F/V LE SQUALE`).

The initial API design used path parameters to identify assets:

```
GET /api/v1/assets/{assetId}/positions
```

This caused **404 errors** because slashes within the asset ID were interpreted as path separators by the HTTP server, even when properly URL-encoded (`%2F`). Many web servers and reverse proxies (including common Quarkus/Vert.x defaults) decode `%2F` before routing, breaking path-based lookup. Workarounds (double-encoding, server configuration flags) are fragile and non-portable.

## Decision

Use **query parameters** instead of path parameters for asset IDs. A dedicated endpoint was created:

```
GET /api/v1/positions/history?assetId=SHIP-F/V%20LE%20SQUALE&from=...&to=...&limit=...
```

The endpoint uses `@QueryParam` instead of `@PathParam` for the asset identifier. Query parameters handle special characters reliably with standard URL encoding — the `?` boundary cleanly separates routing from parameter values.

This aligns with the CQRS query model established in [ADR-001](ADR-001-cqrs-event-sourcing.md), where position history is a dedicated read-side query rather than a sub-resource of the asset entity.

## Consequences

### Positive

- **Reliable handling of special characters** — slashes, spaces, and other characters in asset IDs work with standard URL encoding
- **No server-specific configuration** — no need for `allowEncodedSlash` flags or double-encoding hacks
- **Portable** — works consistently across servers, proxies, and API gateways
- **Cleaner query semantics** — the endpoint represents a query operation, not a resource traversal, which is consistent with CQRS (ADR-001)

### Negative

- **Less RESTful in the traditional sense** — asset positions are not modelled as a sub-resource of the asset
- **Discoverability** — clients cannot navigate from an asset resource to its positions via URL structure alone

### Neutral

- Existing asset endpoints using path parameters (where IDs don't contain slashes) can remain unchanged
- OpenAPI documentation clearly describes the query parameter contract
