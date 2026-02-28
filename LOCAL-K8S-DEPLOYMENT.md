# Local Kubernetes Deployment — GeoTrack

## Cluster: Docker Desktop Kubernetes

### What's Deployed

#### GeoTrack Application (namespace: `geotrack`)
All 7 services running via Helm chart:
- **geotrack-postgres** — StatefulSet, PostGIS-enabled PostgreSQL
- **geotrack-kafka** — StatefulSet, Apache Kafka (KRaft mode)
- **geotrack-api** — Deployment, Quarkus REST API + WebSocket gateway
- **geotrack-processing** — Deployment, Kafka consumer + geofence engine
- **geotrack-simulator-live** — Deployment, OpenSky aircraft data
- **geotrack-simulator-ships** — Deployment, AISStream ship data
- **geotrack-frontend** — Deployment, Angular + Leaflet (nginx)

#### Tekton Pipelines (namespace: `tekton-pipelines`)
- Tekton Pipelines controller, webhook, events controller
- Tekton Triggers controller, webhook, interceptors
- **5 Tasks registered:** `git-clone`, `geotrack-maven-build`, `geotrack-docker-build`, `geotrack-sonar-scan`, `geotrack-deploy`
- **1 Pipeline registered:** `geotrack-ci-cd` (full CI/CD: clone → build → scan → Docker build → deploy)
- **Triggers:** GitHub push webhook binding, template, and event listener

#### ArgoCD (namespace: `argocd`)
- Full ArgoCD installation (7 pods: controller, server, repo-server, dex, redis, notifications, applicationset)
- **Application registered:** `geotrack` (GitOps — will auto-sync once a Git remote is configured)

### Access (automatic — no port-forwarding needed)

- **Map UI:** http://localhost:30000
- **API:** http://localhost:30080
- **API Health:** http://localhost:30080/q/health

```powershell
# ArgoCD UI (still needs port-forward — internal tool)
kubectl port-forward -n argocd svc/argocd-server 9443:443
# Get initial admin password:
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath="{.data.password}" | ForEach-Object { [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($_)) }
```

### Issues Fixed During Deployment

1. **Tekton `maven-build.yaml`** — `spec.steps[0].resources` renamed to `computeResources` (breaking change in Tekton v1)
2. **Tekton `pipeline.yaml`** — `ClusterTask` kind removed in latest Tekton; created local `git-clone` Task instead
3. **ArgoCD install** — CRD too large for `kubectl apply`; used `--server-side=true --force-conflicts`
4. **Startup ordering** — API, processing, and simulators restart 3-4 times waiting for Kafka; normal behaviour with K8s (no init-container dependency management)

### Helm Release

```powershell
helm list -n geotrack
helm status geotrack -n geotrack
```
