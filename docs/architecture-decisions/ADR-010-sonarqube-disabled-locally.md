# ADR-010: SonarQube Disabled for Local Development

**Status:** Accepted

**Date:** 2026-03-01

## Context

SonarQube requires significant resources (2+ CPU cores, 4GB+ RAM) for its Elasticsearch backend. The local development environment runs on Docker Desktop Kubernetes (single node) already hosting 8 application deployments plus Tekton and ArgoCD. Adding SonarQube would exceed available resources and destabilize other pods.

## Decision

Include SonarQube in the Helm chart but disable it for local development via `values-local.yaml` (`sonarqube.enabled: false`). The Tekton CI/CD pipeline includes a `geotrack-sonar-scan` task that runs against a production SonarQube instance. All manifests are production-ready; only the local toggle is off.

## Consequences

**Positive:**

- Local Kubernetes cluster remains stable within Docker Desktop resource constraints
- Helm chart and manifests stay production-ready — no drift between environments
- CI/CD pipeline provides full SonarQube scanning against a dedicated server

**Negative:**

- No local code quality scanning — developers rely on IDE plugins (SonarLint) for immediate feedback
- Code quality issues may only surface at CI/CD time rather than during local development

**Mitigations:**

- Developers should install and configure SonarLint in their IDE for real-time feedback
- The Tekton `geotrack-sonar-scan` task enforces quality gates before merge

## References

- [Tekton Runbook](../runbooks/tekton.md)
- [SonarQube Runbook](../runbooks/sonarqube.md)
