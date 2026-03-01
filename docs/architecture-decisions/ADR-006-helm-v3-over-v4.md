# ADR-006: Helm v3 Over Helm v4

## Status

Accepted — 2026-03-01

## Context

GeoTrack's local development pipeline uses Skaffold v2.17 to orchestrate builds, deploys, and Helm chart rendering for Kubernetes.

Helm v4.1.1 was initially installed as the latest available release. However, Skaffold v2.17's Helm render plugin is incompatible with Helm v4. Attempting to render charts fails with:

```
failed to peek plugin.yaml API version
```

Helm v4 introduced breaking changes to its plugin API versioning scheme that the Skaffold ecosystem does not yet support. This incompatibility blocked the entire local development pipeline — no developer could build, render, or deploy via Skaffold.

## Decision

Downgrade to **Helm v3.17.3**, the latest v3 release.

Helm v3.17.3 is fully compatible with Skaffold v2.17 and provides all the chart management capabilities GeoTrack requires. The features introduced in Helm v4 are not critical to our current workflows.

## Consequences

**Positive:**
- Local development pipeline is unblocked and fully functional with Skaffold v2.17.
- Helm v3.17.3 is a stable, well-supported release with an active maintenance window.

**Negative:**
- Must **pin Helm to v3.x** across all developer environments and CI/CD pipelines until Skaffold releases Helm v4-compatible support.
- Cannot adopt Helm v4 features or improvements in the interim.

**Actions:**
- Document the Helm v3 version constraint in developer runbooks and CI configuration.
- Monitor Skaffold releases for Helm v4 compatibility (track [GoogleContainerTools/skaffold](https://github.com/GoogleContainerTools/skaffold) issues).
- Revisit this decision when Skaffold adds Helm v4 support.

## References

- [ADR-005: Skaffold](ADR-005-skaffold.md)
