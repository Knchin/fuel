-- Migration: 001_initial_schema.sql
-- Initial database schema for Fuel Station & Price Comparison Application
-- Uses PostGIS for geographic queries

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Custom ENUM type for price freshness
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'freshness_state') THEN
        CREATE TYPE freshness_state AS ENUM ('FRESH', 'AGING', 'STALE', 'VERY_STALE');
    END IF;
END
$$;

-- ============================================================
-- stations table
-- ============================================================
-- Stores normalized station information from the government feed.
-- source_id is the stable external identity from the government source.
-- geom is a PostGIS geography field for spatial queries.
-- active/last_seen strategy avoids accidental data loss if source
-- temporarily omits records.

CREATE TABLE IF NOT EXISTS stations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id VARCHAR(255) NOT NULL,
    address TEXT,
    postal_code VARCHAR(10),
    city VARCHAR(255),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geom GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
        POINT(longitude latitude)
    ) STORED NOT NULL,
    presence_type VARCHAR(50),
    opening_hours TEXT,
    services TEXT,
    source VARCHAR(255) NOT NULL DEFAULT 'gouvernement_francais',
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    data_synchronized_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(source_id, source)
);

-- Spatial index for geographic queries (GIST)
CREATE INDEX IF NOT EXISTS idx_stations_geom ON stations USING GIST (geom);

-- Index on source_id for identity lookups
CREATE INDEX IF NOT EXISTS idx_stations_source_id ON stations(source_id);

-- Index on postal_code for location-based searches
CREATE INDEX IF NOT EXISTS idx_stations_postal_code ON stations(postal_code);

-- Index on city for location-based searches
CREATE INDEX IF NOT EXISTS idx_stations_city ON stations(city);

-- Index on active status for filtering
CREATE INDEX IF NOT EXISTS idx_stations_active ON stations(active);

-- ============================================================
-- fuel_prices table
-- ============================================================
-- Stores fuel price information linked to stations.
-- Uses exact numeric type for prices (DECIMAL(10,3)) to avoid
-- floating point precision issues.
-- Price is per liter in EUR.

CREATE TABLE IF NOT EXISTS fuel_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    fuel_type VARCHAR(10) NOT NULL,
    price_per_liter NUMERIC(10, 3) NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    availability VARCHAR(50) NOT NULL DEFAULT 'unknown',
    rupture_type VARCHAR(50),
    rupture_started_at TIMESTAMP WITH TIME ZONE,
    data_synchronized_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(station_id, fuel_type)
);

-- Index on station_id for nearby queries
CREATE INDEX IF NOT EXISTS idx_fuel_prices_station_id ON fuel_prices(station_id);

-- Index on fuel_type for filtering
CREATE INDEX IF NOT EXISTS idx_fuel_prices_fuel_type ON fuel_prices(fuel_type);

-- Index on reported_at for freshness queries
CREATE INDEX IF NOT EXISTS idx_fuel_prices_reported_at ON fuel_prices(reported_at);

-- Index on data_synchronized_at for sync metadata
CREATE INDEX IF NOT EXISTS idx_fuel_prices_synchronized_at ON fuel_prices(data_synchronized_at);

-- ============================================================
-- synchronization_runs table
-- ============================================================
-- Tracks ingestion pipeline runs for observability and failure handling.
-- Ensures we can detect and recover from failed synchronizations.

CREATE TABLE IF NOT EXISTS synchronization_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    source_url TEXT NOT NULL,
    source_retrieved_at TIMESTAMP WITH TIME ZONE,
    records_seen INTEGER NOT NULL DEFAULT 0,
    records_accepted INTEGER NOT NULL DEFAULT 0,
    records_rejected INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    schema_version VARCHAR(50)
);

-- Index on status for querying recent runs
CREATE INDEX IF NOT EXISTS idx_sync_runs_status ON synchronization_runs(status);

-- Index on started_at for chronological ordering
CREATE INDEX IF NOT EXISTS idx_sync_runs_started_at ON synchronization_runs(started_at DESC);

-- ============================================================
-- source_anomalies table
-- ============================================================
-- Tracks malformed/rejected records for debugging and monitoring.
-- One malformed station record should not destroy otherwise valid data.

CREATE TABLE IF NOT EXISTS source_anomalies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID REFERENCES synchronization_runs(id) ON DELETE SET NULL,
    station_source_id VARCHAR(255),
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    raw_payload JSONB,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Index on run_id for querying anomalies per run
CREATE INDEX IF NOT EXISTS idx_source_anomalies_run_id ON source_anomalies(run_id);

-- Index on error_type for categorizing issues
CREATE INDEX IF NOT EXISTS idx_source_anomalies_error_type ON source_anomalies(error_type);

-- ============================================================
-- Row Level Security (RLS) policies
-- ============================================================

-- Enable RLS on all tables
ALTER TABLE stations ENABLE ROW LEVEL SECURITY;
ALTER TABLE fuel_prices ENABLE ROW LEVEL SECURITY;
ALTER TABLE synchronization_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_anomalies ENABLE ROW LEVEL SECURITY;

-- Anonymous/public users can READ station and fuel price data
-- (for the application's public API)
CREATE POLICY "anon_read_stations" ON stations
    FOR SELECT TO anon USING (true);

CREATE POLICY "anon_read_fuel_prices" ON fuel_prices
    FOR SELECT TO anon USING (true);

-- Operational tables (ingestion, anomalies) are NOT publicly writable
-- Only service_role can manage these

-- Synchronization metadata is read-only for clients
CREATE POLICY "anon_read_sync_runs" ON synchronization_runs
    FOR SELECT TO anon USING (true);

-- Anomalies are read-only for operational monitoring
CREATE POLICY "anon_read_anomalies" ON source_anomalies
    FOR SELECT TO anon USING (true);

-- ============================================================
-- Helpful functions
-- ============================================================

-- Get nearby stations using PostGIS geography
-- Returns stations within radius km, with optional fuel type filtering
CREATE OR REPLACE FUNCTION get_nearby_stations(
    IN p_latitude DOUBLE PRECISION,
    IN p_longitude DOUBLE PRECISION,
    IN p_radius_km DOUBLE PRECISION DEFAULT 10.0,
    IN p_fuel_type VARCHAR(10) DEFAULT NULL,
    IN p_page INTEGER DEFAULT 1,
    IN p_page_size INTEGER DEFAULT 20
)
RETURNS TABLE (
    station_id UUID,
    source_id VARCHAR(255),
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    distance_km DOUBLE PRECISION,
    fuel_type VARCHAR(10),
    price_per_liter NUMERIC(10, 3),
    availability VARCHAR(50),
    reported_at TIMESTAMP WITH TIME ZONE,
    data_synchronized_at TIMESTAMP WITH TIME ZONE
)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    offset_val INTEGER;
BEGIN
    offset_val := (p_page - 1) * p_page_size;

    RETURN QUERY
    SELECT
        s.id AS station_id,
        s.source_id,
        s.address,
        s.latitude,
        s.longitude,
        ST_Distance(
            s.geom::geography,
            ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography
        ) / 1000.0 AS distance_km,
        fp.fuel_type,
        fp.price_per_liter,
        fp.availability,
        fp.reported_at,
        fp.data_synchronized_at
    FROM stations s
    JOIN fuel_prices fp ON fp.station_id = s.id
    WHERE ST_DWithin(
        s.geom::geography,
        ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography,
        p_radius_km * 1000.0
    )
    AND fp.fuel_type = COALESCE(p_fuel_type, fp.fuel_type)
    AND s.active = true
    ORDER BY
        CASE WHEN p_fuel_type IS NOT NULL AND fp.price_per_liter IS NOT NULL THEN fp.price_per_liter END ASC NULLS FIRST,
        ST_Distance(
            s.geom::geography,
            ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography
        ) ASC NULLS FIRST
    LIMIT p_page_size OFFSET offset_val;
END;
$$;

-- Get station by source ID
CREATE OR REPLACE FUNCTION get_station_by_source_id(
    IN p_source_id VARCHAR(255)
)
RETURNS TABLE (
    id UUID,
    source_id VARCHAR(255),
    address TEXT,
    postal_code VARCHAR(10),
    city VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geom GEOGRAPHY(POINT, 4326),
    presence_type VARCHAR(50),
    opening_hours TEXT,
    services TEXT,
    source VARCHAR(255),
    first_seen_at TIMESTAMP WITH TIME ZONE,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    data_synchronized_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN
)
LANGUAGE sql
STABLE
AS $$
SELECT
    s.id, s.source_id, s.address, s.postal_code, s.city,
    s.latitude, s.longitude, s.geom, s.presence_type,
    s.opening_hours, s.services, s.source,
    s.first_seen_at, s.last_seen_at, s.data_synchronized_at, s.active
FROM stations s
WHERE s.source_id = p_source_id
LIMIT 1;
$$;

-- Check if a station source_id exists
CREATE OR REPLACE FUNCTION station_exists(
    IN p_source_id VARCHAR(255)
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
SELECT EXISTS (
    SELECT 1 FROM stations WHERE source_id = p_source_id LIMIT 1
);
$$;