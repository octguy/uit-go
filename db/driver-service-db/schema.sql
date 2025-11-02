-- Driver Service Database Schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Main drivers table
CREATE TABLE drivers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    vehicle_model VARCHAR(255),
    vehicle_plate VARCHAR(20) UNIQUE,
    rating DOUBLE PRECISION DEFAULT 0.0,
    total_completed_trips INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'OFFLINE' 
        CHECK (status IN ('AVAILABLE', 'BUSY', 'OFFLINE', 'ON_BREAK')),
    vehicle_capacity INTEGER DEFAULT 4,
    created_at BIGINT,
    updated_at BIGINT
);

-- Driver location history table
CREATE TABLE driver_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    timestamp BIGINT NOT NULL,
    geohash VARCHAR(50)
);

-- Driver session tracking table
CREATE TABLE driver_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    driver_id UUID NOT NULL REFERENCES drivers(id) ON DELETE CASCADE,
    online_at BIGINT NOT NULL,
    offline_at BIGINT,
    is_active BOOLEAN DEFAULT true,
    total_distance_km DOUBLE PRECISION DEFAULT 0.0,
    total_earnings DOUBLE PRECISION DEFAULT 0.0
);

-- Indexes for performance
CREATE INDEX idx_driver_locations_driver_timestamp ON driver_locations(driver_id, timestamp DESC);
CREATE INDEX idx_driver_locations_timestamp ON driver_locations(timestamp DESC);
CREATE INDEX idx_driver_locations_geohash ON driver_locations(geohash);
CREATE INDEX idx_driver_sessions_active ON driver_sessions(driver_id, is_active);
CREATE INDEX idx_driver_sessions_online ON driver_sessions(online_at DESC);
CREATE INDEX idx_drivers_status ON drivers(status);