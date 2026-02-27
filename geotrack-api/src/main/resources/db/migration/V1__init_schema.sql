-- =============================================================================
-- V1: Initial GeoTrack schema with PostGIS spatial support
-- =============================================================================

-- Enable PostGIS extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- ===========================
-- Assets table
-- ===========================
CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assets_type ON assets (asset_type);
CREATE INDEX idx_assets_status ON assets (status);

-- ===========================
-- Positions table with spatial indexing
-- ===========================
CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id),
    location GEOMETRY(Point, 4326) NOT NULL,
    altitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    source VARCHAR(50),
    timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ DEFAULT NOW(),
    metadata JSONB
);

-- Spatial index on geometry — enables fast ST_DWithin, ST_Contains queries
CREATE INDEX idx_positions_location ON positions USING GIST (location);
-- BRIN index on timestamp for efficient time-range scans
CREATE INDEX idx_positions_timestamp ON positions USING BRIN (timestamp);
-- Composite index for per-asset time queries (route reconstruction)
CREATE INDEX idx_positions_asset_time ON positions (asset_id, timestamp DESC);

-- ===========================
-- Geofences table
-- ===========================
CREATE TABLE geofences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    fence_type VARCHAR(20) NOT NULL,
    geometry GEOMETRY(Polygon, 4326) NOT NULL,
    buffer_metres DOUBLE PRECISION DEFAULT 0,
    active BOOLEAN DEFAULT true,
    alert_on_enter BOOLEAN DEFAULT true,
    alert_on_exit BOOLEAN DEFAULT true,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_geofences_geometry ON geofences USING GIST (geometry);
CREATE INDEX idx_geofences_active ON geofences (active) WHERE active = true;

-- ===========================
-- Alerts table
-- ===========================
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id),
    geofence_id UUID REFERENCES geofences(id),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    position GEOMETRY(Point, 4326),
    message TEXT,
    acknowledged BOOLEAN DEFAULT false,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_alerts_asset ON alerts (asset_id, created_at DESC);
CREATE INDEX idx_alerts_unacknowledged ON alerts (acknowledged, created_at DESC)
    WHERE acknowledged = false;
