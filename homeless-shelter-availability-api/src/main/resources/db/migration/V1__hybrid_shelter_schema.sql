DROP TABLE IF EXISTS shelter_sources CASCADE;
DROP TABLE IF EXISTS import_runs CASCADE;
DROP TABLE IF EXISTS shelters CASCADE;

CREATE TABLE shelters (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(64) NOT NULL,
    zip_code VARCHAR(16),
    phone_number VARCHAR(64),
    website VARCHAR(512),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    hours TEXT,
    category VARCHAR(64) NOT NULL DEFAULT 'general',
    description TEXT,
    services JSONB NOT NULL DEFAULT '[]'::jsonb,
    eligibility JSONB NOT NULL DEFAULT '[]'::jsonb,
    available_beds INTEGER,
    total_beds INTEGER,
    last_source_updated_at TIMESTAMPTZ,
    source_system VARCHAR(64),
    source_external_id VARCHAR(255),
    normalized_name VARCHAR(255),
    normalized_address VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE shelter_sources (
    id BIGSERIAL PRIMARY KEY,
    shelter_id BIGINT REFERENCES shelters(id) ON DELETE CASCADE,
    source_system VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(120),
    state VARCHAR(64),
    zip_code VARCHAR(16),
    phone_number VARCHAR(64),
    website VARCHAR(512),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    hours TEXT,
    category VARCHAR(64),
    description TEXT,
    services JSONB NOT NULL DEFAULT '[]'::jsonb,
    eligibility JSONB NOT NULL DEFAULT '[]'::jsonb,
    available_beds INTEGER,
    total_beds INTEGER,
    normalized_name VARCHAR(255),
    normalized_address VARCHAR(255),
    last_source_updated_at TIMESTAMPTZ,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    raw_payload JSONB,
    UNIQUE (source_system, external_id)
);

CREATE TABLE import_runs (
    id UUID PRIMARY KEY,
    import_type VARCHAR(64) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_count INTEGER NOT NULL DEFAULT 0,
    updated_count INTEGER NOT NULL DEFAULT 0,
    skipped_count INTEGER NOT NULL DEFAULT 0,
    error_text TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shelters_category ON shelters (category);
CREATE INDEX idx_shelters_last_updated ON shelters (last_source_updated_at DESC);
CREATE INDEX idx_shelters_normalized_name ON shelters (normalized_name);
CREATE INDEX idx_shelters_normalized_address ON shelters (normalized_address);
CREATE INDEX idx_shelters_lat_lng ON shelters (latitude, longitude);
CREATE INDEX idx_sources_shelter_id ON shelter_sources (shelter_id);
CREATE INDEX idx_sources_normalized_address ON shelter_sources (normalized_address);
CREATE INDEX idx_sources_last_seen_at ON shelter_sources (last_seen_at DESC);

CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER shelters_touch_updated_at
BEFORE UPDATE ON shelters
FOR EACH ROW
EXECUTE FUNCTION touch_updated_at();
