<#
.SYNOPSIS
    GeoTrack - Full local deployment to Docker Desktop Kubernetes.
.DESCRIPTION
    Builds all services from source, creates Docker images, and deploys
    the full stack to a local Kubernetes cluster using Helm.
.EXAMPLE
    .\deploy-local.ps1
    .\deploy-local.ps1 -AISStreamApiKey "your-key-here"
    .\deploy-local.ps1 -SkipBuild
    .\deploy-local.ps1 -Clean
#>

param(
    [string]$AISStreamApiKey = "",
    [switch]$SkipBuild,
    [switch]$Clean
)

$ErrorActionPreference = "Continue"
$ProjectRoot = $PSScriptRoot
$Namespace = "geotrack"
$HelmRelease = "geotrack"
$HelmChart = Join-Path $ProjectRoot "helm\geotrack"
$ValuesFile = Join-Path $ProjectRoot "helm\geotrack\values-local.yaml"

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    OK: $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "    WARN: $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "    ERROR: $msg" -ForegroundColor Red }

# ── Prerequisites ──────────────────────────────────────────────
Write-Step "Checking prerequisites"

$tools = @("java", "mvn", "docker", "kubectl", "helm", "node")
foreach ($tool in $tools) {
    if (Get-Command $tool -ErrorAction SilentlyContinue) {
        Write-Ok "$tool found"
    } else {
        Write-Err "$tool not found - install it and retry."
        exit 1
    }
}

$nodeCheck = kubectl get nodes 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Err "Kubernetes not available. Enable it in Docker Desktop settings."
    exit 1
}
Write-Ok "Kubernetes cluster reachable"

# ── Clean (optional) ──────────────────────────────────────────
if ($Clean) {
    Write-Step "Cleaning existing deployment"
    helm uninstall $HelmRelease -n $Namespace 2>$null
    kubectl delete namespace $Namespace --ignore-not-found 2>$null
    Write-Ok "Cleaned"
    Start-Sleep 5
}

# ── Build ──────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Step "Building Java services (Maven)"
    Push-Location $ProjectRoot
    & mvn clean package "-Dmaven.test.skip=true" -q 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Maven build failed"
        Pop-Location
        exit 1
    }
    Write-Ok "Maven build complete"
    Pop-Location

    Write-Step "Building Docker images"

    $dockerBuilds = @(
        @("geotrack-api",        "geotrack-api\src\main\docker\Dockerfile.jvm", "geotrack-api"),
        @("geotrack-processing", "geotrack-processing\src\main\docker\Dockerfile.jvm", "geotrack-processing"),
        @("geotrack-simulator",  "geotrack-simulator\src\main\docker\Dockerfile.jvm", "geotrack-simulator"),
        @("geotrack-frontend",   "geotrack-frontend\Dockerfile", "geotrack-frontend")
    )

    foreach ($b in $dockerBuilds) {
        $name = $b[0]
        $df = Join-Path $ProjectRoot $b[1]
        $ctx = Join-Path $ProjectRoot $b[2]
        Write-Host "    Building $name..." -NoNewline
        docker build -f $df -t "${name}:latest" $ctx 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host " FAILED" -ForegroundColor Red
            exit 1
        }
        Write-Host " done" -ForegroundColor Green
    }

    Write-Step "Pulling dependency images"
    docker pull postgis/postgis:16-3.4 2>&1 | Select-Object -Last 1
    docker pull redpandadata/redpanda:v24.1.1 2>&1 | Select-Object -Last 1
    Write-Ok "Dependencies pulled"
} else {
    Write-Step "Skipping build (SkipBuild flag set)"
}

# ── Namespace ──────────────────────────────────────────────────
Write-Step "Creating namespace"
kubectl create namespace $Namespace --dry-run=client -o yaml 2>&1 | kubectl apply -f - 2>&1 | Out-Null
Write-Ok "Namespace ready"

# ── Helm Deploy ────────────────────────────────────────────────
Write-Step "Deploying with Helm"

$helmArgs = @(
    "upgrade", "--install", $HelmRelease, $HelmChart,
    "-f", $ValuesFile,
    "-n", $Namespace,
    "--wait",
    "--timeout", "5m"
)

if ($AISStreamApiKey) {
    $helmArgs += @("--set", "secrets.aisstreamApiKey=$AISStreamApiKey")
}

& helm @helmArgs 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Warn "Helm reported issues - pods may still be starting"
}

# ── Wait for Pods ──────────────────────────────────────────────
Write-Step "Waiting for pods to be ready"

$maxWait = 120
$elapsed = 0
while ($elapsed -lt $maxWait) {
    $podLines = (kubectl get pods -n $Namespace --no-headers 2>&1) -split "`n" | Where-Object { $_.Trim() }
    $total = $podLines.Count
    $ready = ($podLines | Where-Object { $_ -match "1/1\s+Running" }).Count

    if ($total -gt 0 -and $ready -eq $total) {
        break
    }

    Write-Host "    $ready/$total pods ready... (${elapsed}s)" -ForegroundColor Gray
    Start-Sleep 10
    $elapsed += 10
}

# ── Status ─────────────────────────────────────────────────────
Write-Step "Deployment status"
kubectl get pods -n $Namespace
Write-Host ""
kubectl get services -n $Namespace

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  GeoTrack deployed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend:  http://localhost:30000" -ForegroundColor White
Write-Host "  API:       http://localhost:30080/api/v1/assets" -ForegroundColor White
Write-Host "  WebSocket: ws://localhost:30080/ws/positions" -ForegroundColor White
Write-Host "  Health:    http://localhost:30080/q/health" -ForegroundColor White
Write-Host ""

if (-not $AISStreamApiKey) {
    Write-Warn "No AISStream API key provided. Ship tracking disabled."
    Write-Warn "Re-deploy with: .\deploy-local.ps1 -AISStreamApiKey YOUR_KEY"
}
