-- Migration: 003_staging_and_gnv.sql
-- Staging tables for atomic ingestion, GNV stations, viewport/search helpers,
-- and sync health view.

-- ============================================================
-- staging_fuel_prices table
-- ============================================================
-- Temporarily holds price data during the ingestion pipeline.
-- Rows are inserted at the start of a sync run and deleted after
-- atomic publish or on rollback.

CREATE TABLE IF NOT EXISTS staging_fuel_prices (
    staging_id VARCHAR(100) NOT NULL,
    station_source_id VARCHAR(255) NOT NULL,
    station_id UUID REFERENCES stations(id) ON DELETE CASCADE,
    fuel_type VARCHAR(10) NOT NULL,
    price_per_liter NUMERIC(10, 3) NOT NULL,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    availability VARCHAR(50) DEFAULT 'unknown',
    rupture_type VARCHAR(50),
    rupture_started_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(staging_id, station_source_id, fuel_type)
);

CREATE INDEX IF NOT EXISTS idx_staging_fuel_prices_staging_id
    ON staging_fuel_prices(staging_id);

CREATE INDEX IF NOT EXISTS idx_staging_fuel_prices_station_id
    ON staging_fuel_prices(station_id);

-- ============================================================
-- staging_fuel_spatial table
-- ============================================================
-- Temporarily holds station spatial data during ingestion.

CREATE TABLE IF NOT EXISTS staging_fuel_spatial (
    staging_id VARCHAR(100) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    address TEXT,
    postal_code VARCHAR(10),
    city VARCHAR(255),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geom GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
        POINT(longitude latitude)
    ) STORED NOT NULL,
    UNIQUE(staging_id, source_id)
);

CREATE INDEX IF NOT EXISTS idx_staging_fuel_spatial_staging_id
    ON staging_fuel_spatial(staging_id);

CREATE INDEX IF NOT EXISTS idx_staging_fuel_spatial_geom
    ON staging_fuel_spatial USING GIST (geom);

-- ============================================================
-- gnv_stations table
-- ============================================================
-- Stores CNG/LNG/GNV station data sourced from data.gouv.fr.
-- Separate from main stations table because the GNV feed has
-- different attributes (operator, capacity, dispenser count).

CREATE TABLE IF NOT EXISTS gnv_stations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id VARCHAR(255) NOT NULL UNIQUE,
    name TEXT,
    address TEXT,
    postal_code VARCHAR(10),
    city VARCHAR(255),
    department VARCHAR(10),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geom GEOGRAPHY(POINT, 4326) GENERATED ALWAYS AS (
        POINT(longitude latitude)
    ) STORED NOT NULL,
    operator TEXT,
    capacity_kg NUMERIC(10, 2),
    dispenser_count INTEGER,
    is_open BOOLEAN DEFAULT true,
    last_update TIMESTAMP WITH TIME ZONE,
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX IF NOT EXISTS idx_gnv_stations_geom ON gnv_stations USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_gnv_stations_source_id ON gnv_stations(source_id);
CREATE INDEX IF NOT EXISTS idx_gnv_stations_postal_code ON gnv_stations(postal_code);
CREATE INDEX IF NOT EXISTS idx_gnv_stations_city ON gnv_stations(city);
CREATE INDEX IF NOT EXISTS idx_gnv_stations_active ON gnv_stations(active);

-- ============================================================
-- RLS for new tables
-- ============================================================
ALTER TABLE staging_fuel_prices ENABLE ROW LEVEL SECURITY;
ALTER TABLE staging_fuel_spatial ENABLE ROW LEVEL SECURITY;
ALTER TABLE gnv_stations ENABLE ROW LEVEL SECURITY;

CREATE POLICY "anon_read_gnv_stations" ON gnv_stations
    FOR SELECT TO anon USING (true);

-- Staging tables are service_role only (no anon access)

-- ============================================================
-- atomic_publish_staging: upsert staged prices into live table
-- ============================================================

CREATE OR REPLACE FUNCTION atomic_publish_staging(p_staging_id UUID)
RETURNS VOID AS $$
BEGIN
    -- 1. Update fuel_prices from staging (upsert based on unique constraint)
    INSERT INTO fuel_prices (station_id, fuel_type, price_per_liter, reported_at, availability, rupture_type, rupture_started_at, data_synchronized_at)
    SELECT station_id, fuel_type, price_per_liter, reported_at, availability, rupture_type, rupture_started_at, now()
    FROM staging_fuel_prices
    WHERE staging_id = p_staging_id
    ON CONFLICT (station_id, fuel_type) DO UPDATE
    SET price_per_liter = EXCLUDED.price_per_liter,
        reported_at = EXCLUDED.reported_at,
        availability = EXCLUDED.availability,
        rupture_type = EXCLUDED.rupture_type,
        rupture_started_at = EXCLUDED.rupture_started_at,
        data_synchronized_at = now();
    
    -- 2. Update stations last_seen_at from staging spatial
    UPDATE stations s
    SET last_seen_at = now(),
        data_synchronized_at = now(),
        active = true
    FROM staging_fuel_spatial sfs
    WHERE sfs.staging_id = p_staging_id
      AND s.source_id = sfs.source_id
      AND s.data_synchronized_at < now() - interval '1 hour';
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- viewport_stations: return stations within a bounding box
-- ============================================================

CREATE OR REPLACE FUNCTION viewport_stations(
    IN p_min_lat DOUBLE PRECISION,
    IN p_min_lng DOUBLE PRECISION,
    IN p_max_lat DOUBLE PRECISION,
    IN p_max_lng DOUBLE PRECISION,
    IN p_fuel_type VARCHAR(10) DEFAULT NULL,
    IN p_limit INTEGER DEFAULT 200
)
RETURNS TABLE (
    station_id UUID,
    source_id VARCHAR(255),
    address TEXT,
    city VARCHAR(255),
    postal_code VARCHAR(10),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    fuel_type VARCHAR(10),
    price_per_liter NUMERIC(10, 3),
    availability VARCHAR(50),
    reported_at TIMESTAMP WITH TIME ZONE
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        s.id AS station_id,
        s.source_id,
        s.address,
        s.city,
        s.postal_code,
        s.latitude,
        s.longitude,
        fp.fuel_type,
        fp.price_per_liter,
        fp.availability,
        fp.reported_at
    FROM stations s
    JOIN fuel_prices fp ON fp.station_id = s.id
    WHERE s.active = true
      AND s.latitude >= p_min_lat
      AND s.latitude <= p_max_lat
      AND s.longitude >= p_min_lng
      AND s.longitude <= p_max_lng
      AND fp.fuel_type = COALESCE(p_fuel_type, fp.fuel_type)
    ORDER BY fp.price_per_liter ASC NULLS LAST
    LIMIT p_limit;
$$;

-- ============================================================
-- search_stations: text search by city or address
-- ============================================================

CREATE OR REPLACE FUNCTION search_stations(
    IN p_query TEXT,
    IN p_limit INTEGER DEFAULT 20
)
RETURNS TABLE (
    station_id UUID,
    source_id VARCHAR(255),
    address TEXT,
    city VARCHAR(255),
    postal_code VARCHAR(10),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        s.id AS station_id,
        s.source_id,
        s.address,
        s.city,
        s.postal_code,
        s.latitude,
        s.longitude
    FROM stations s
    WHERE s.active = true
      AND (
          s.city ILIKE '%' || p_query || '%'
          OR s.address ILIKE '%' || p_query || '%'
          OR s.postal_code ILIKE p_query || '%'
      )
    ORDER BY s.city
    LIMIT p_limit;
$$;

-- ============================================================
-- aggregate_station_prices: return all prices for a station as JSONB
-- ============================================================

CREATE OR REPLACE FUNCTION aggregate_station_prices(
    IN p_station_id UUID
)
RETURNS JSONB
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'fuel_type', fp.fuel_type,
                'price_per_liter', fp.price_per_liter,
                'availability', fp.availability,
                'reported_at', fp.reported_at,
                'rupture_type', fp.rupture_type,
                'rupture_started_at', fp.rupture_started_at
            )
        ),
        '[]'::jsonb
    )
    FROM fuel_prices fp
    WHERE fp.station_id = p_station_id;
$$;

-- ============================================================
-- sync_health view: last sync status at a glance
-- ============================================================

CREATE OR REPLACE VIEW sync_health AS
SELECT
    sr.id AS run_id,
    sr.status,
    sr.started_at,
    sr.completed_at,
    CASE
        WHEN sr.completed_at IS NOT NULL
        THEN EXTRACT(EPOCH FROM (sr.completed_at - sr.started_at))
        ELSE NULL
    END AS duration_seconds,
    sr.records_seen,
    sr.records_accepted,
    sr.records_rejected,
    sr.error_message,
    sr.schema_version,
    sr.source_url
FROM synchronization_runs sr
ORDER BY sr.started_at DESC
LIMIT 50;
