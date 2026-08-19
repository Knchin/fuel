-- Additional database functions for the ingestion pipeline

-- Function to get nearby stations with filtering
CREATE OR REPLACE FUNCTION get_nearby_stations_full(
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
    data_synchronized_at TIMESTAMP WITH TIME ZONE,
    freshness freshness_state
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
        fp.data_synchronized_at,
        -- Calculate freshness
        CASE 
            WHEN EXTRACT(EPOCH FROM (now() - fp.reported_at)) / 3600 <= 2 THEN 'FRESH'::freshness_state
            WHEN EXTRACT(EPOCH FROM (now() - fp.reported_at)) / 3600 <= 6 THEN 'AGING'::freshness_state
            WHEN EXTRACT(EPOCH FROM (now() - fp.reported_at)) / 3600 <= 24 THEN 'STALE'::freshness_state
            ELSE 'VERY_STALE'::freshness_state
        END AS freshness
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

-- Function to get station by source ID with fuel prices
CREATE OR REPLACE FUNCTION get_station_full(p_source_id VARCHAR(255))
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
    active BOOLEAN,
    fuel_prices JSONB
)
LANGUAGE sql
STABLE
AS $$
SELECT
    s.id, s.source_id, s.address, s.postal_code, s.city,
    s.latitude, s.longitude, s.geom, s.presence_type,
    s.opening_hours, s.services, s.source,
    s.first_seen_at, s.last_seen_at, s.data_synchronized_at, s.active,
    -- Get fuel prices as JSON
    (SELECT json_agg(json_build_object(
        'fuel_type', fp.fuel_type,
        'price_per_liter', fp.price_per_liter,
        'reported_at', fp.reported_at,
        'availability', fp.availability,
        'rupture_type', fp.rupture_type,
        'rupture_started_at', fp.rupture_started_at
    )) FROM fuel_prices fp WHERE fp.station_id = s.id) AS fuel_prices
FROM stations s
WHERE s.source_id = p_source_id
LIMIT 1;
$$;
