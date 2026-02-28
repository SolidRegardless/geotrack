# GeoTrack — MicroShift Local Deployment Guide

Deploy the full GeoTrack stack to MicroShift running in Docker Desktop on Windows.

## Prerequisites

- **Docker Desktop** (Windows, K8s disabled — MicroShift replaces it)
- **Helm** (v4+) — used to render templates on the host
- **Java 21** — Eclipse Adoptium / Temurin
- **Maven 3.9+**
- **Node.js 22+** — for Angular frontend build

## Architecture

```
Host (Windows)
├── Docker Desktop
│   └── microshift container (quay.io/microshift/microshift-aio:latest)
│       ├── CRI-O runtime (container images loaded via skopeo)
│       ├── OpenShift Router (port 80 → host port 8080)
│       └── geotrack namespace
│           ├── geotrack-frontend (nginx, Angular SPA)
│           ├── geotrack-api (Quarkus REST + WebSocket)
│           ├── geotrack-processing (Quarkus Kafka consumer)
│           ├── geotrack-simulator-live (OpenSky aircraft)
│           ├── geotrack-simulator-ships (AISStream vessels)
│           ├── geotrack-kafka (Redpanda, KRaft mode)
│           └── geotrack-postgres (PostGIS)
```

## Step 1: Start MicroShift

```powershell
docker run -d --name microshift --privileged `
  -p 8080:80 -p 8443:443 -p 6444:6443 `
  quay.io/microshift/microshift-aio:latest
```

Wait ~30s for MicroShift to initialise:

```powershell
docker exec microshift kubectl get nodes
# Should show: STATUS=Ready
```

## Step 2: Install skopeo (inside MicroShift)

Required to load Docker images into CRI-O:

```powershell
docker exec microshift yum install -y skopeo
```

## Step 3: Build Application

### Backend (Maven)

```powershell
cd geotrack
mvn clean package -DskipTests
```

This produces `target/quarkus-app/` in each module.

### Docker Images

```powershell
docker build -f geotrack-api/src/main/docker/Dockerfile.jvm -t geotrack-api ./geotrack-api
docker build -f geotrack-processing/src/main/docker/Dockerfile.jvm -t geotrack-processing ./geotrack-processing
docker build -f geotrack-simulator/src/main/docker/Dockerfile.jvm -t geotrack-simulator ./geotrack-simulator
docker build -f geotrack-frontend/Dockerfile -t geotrack-frontend ./geotrack-frontend
```

### Pull Dependencies

```powershell
docker pull postgis/postgis:16-3.4
docker pull docker.redpanda.com/redpandadata/redpanda:latest
```

## Step 4: Load Images into MicroShift

Images must be transferred from Docker Desktop into MicroShift's CRI-O storage via `docker save` → `docker cp` → `skopeo copy`:

```powershell
$images = @(
    @{ name = "geotrack-api"; target = "localhost/geotrack-api:latest" },
    @{ name = "geotrack-processing"; target = "localhost/geotrack-processing:latest" },
    @{ name = "geotrack-simulator"; target = "localhost/geotrack-simulator:latest" },
    @{ name = "geotrack-frontend"; target = "localhost/geotrack-frontend:latest" },
    @{ name = "postgis/postgis:16-3.4"; target = "localhost/postgis/postgis:16-3.4" },
    @{ name = "docker.redpanda.com/redpandadata/redpanda:latest"; target = "localhost/docker.redpanda.com/redpandadata/redpanda:latest" }
)

foreach ($img in $images) {
    $safeName = $img.name -replace '[/:.]+', '-'
    $tarPath = "$env:TEMP\$safeName.tar"
    Write-Host "Loading $($img.name)..."
    docker save $img.name -o $tarPath
    docker cp $tarPath "microshift:/tmp/$safeName.tar"
    docker exec microshift skopeo copy "docker-archive:/tmp/$safeName.tar" "containers-storage:$($img.target)"
    Remove-Item $tarPath -Force
    docker exec microshift rm "/tmp/$safeName.tar"
}
```

Verify images are loaded:

```powershell
docker exec microshift crictl images | Select-String "geotrack|postgis|redpanda"
```

> **Note:** Windows PowerShell corrupts binary data when piping (`docker save | docker exec -i`). The file-based approach above is required.

## Step 5: Render Helm Templates

MicroShift doesn't include Helm, so we render templates on the host:

```powershell
helm template geotrack helm/geotrack `
  -f helm/geotrack/values-microshift.yaml `
  -n geotrack > helm/geotrack/rendered-microshift.yaml
```

The `values-microshift.yaml` file sets all image repositories to `localhost/` prefix and `imagePullPolicy: Never`.

## Step 6: Deploy

```powershell
# Create namespace
docker exec microshift kubectl create namespace geotrack

# Copy and apply manifests
docker cp helm/geotrack/rendered-microshift.yaml microshift:/tmp/geotrack.yaml
docker exec microshift kubectl apply -f /tmp/geotrack.yaml -n geotrack
```

### Create OpenShift Routes

```powershell
docker cp k8s/microshift/routes.yaml microshift:/tmp/routes.yaml
docker exec microshift kubectl apply -f /tmp/routes.yaml
```

### Add hosts entry

```powershell
# Run PowerShell as Administrator
Add-Content C:\Windows\System32\drivers\etc\hosts "`n127.0.0.1 geotrack.local"
```

## Step 7: Verify

```powershell
# Check all 7 pods are Running
docker exec microshift kubectl get pods -n geotrack

# Expected (after ~30s for restarts due to startup ordering):
# geotrack-api          1/1  Running
# geotrack-frontend     1/1  Running
# geotrack-kafka        1/1  Running
# geotrack-postgres     1/1  Running
# geotrack-processing   1/1  Running
# geotrack-simulator-live   1/1  Running
# geotrack-simulator-ships  1/1  Running
```

Access the application:

| Service | URL |
|---------|-----|
| Frontend (Map UI) | http://geotrack.local:8080 |
| REST API | http://geotrack.local:8080/api/v1/assets |
| WebSocket | ws://geotrack.local:8080/ws/positions |

## Troubleshooting

### Pods restarting on initial deploy

Normal. Kafka/Postgres take a few seconds to start; API and processing pods will restart 1–2 times then stabilise.

### "No resolvable bootstrap urls" in processing logs

Kafka hasn't started yet. Wait for `geotrack-kafka-0` to be Ready, then processing will reconnect on restart.

### Images not found (ImagePullBackOff)

Images must use `localhost/` prefix in MicroShift's CRI-O. Check with:

```powershell
docker exec microshift crictl images
```

### Cleaning up

```powershell
# Delete all geotrack resources
docker exec microshift kubectl delete namespace geotrack

# Stop MicroShift
docker stop microshift

# Remove MicroShift container (images persist until container is removed)
docker rm microshift
```

## Environment Variables

Set the AISStream API key for live ship tracking:

```powershell
docker exec microshift kubectl set env deployment/geotrack-simulator-ships `
  AISSTREAM_API_KEY=your-key-here -n geotrack
```

## File Reference

| File | Purpose |
|------|---------|
| `helm/geotrack/values-microshift.yaml` | MicroShift-specific Helm values (localhost/ images) |
| `helm/geotrack/rendered-microshift.yaml` | Pre-rendered K8s manifests |
| `k8s/microshift/routes.yaml` | OpenShift Routes for frontend + API |
