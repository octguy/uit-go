-- Trip Service Database Schema

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE trip (
    id UUID PRIMARY KEY,
    passenger_id UUID NOT NULL,
    driver_id UUID,

    pickup_location VARCHAR(255),
    destination VARCHAR(255) NOT NULL,

    pickup_latitude DECIMAL(10, 8),
    pickup_longitude DECIMAL(11, 8),
    destination_latitude DECIMAL(10, 8),
    destination_longitude DECIMAL(11, 8),

    status VARCHAR(20) NOT NULL DEFAULT 'SEARCHING_DRIVER'
        CHECK (status IN ('SEARCHING_DRIVER', 'ACCEPTED', 'ONGOING', 'COMPLETED', 'CANCELLED')),

    fare DECIMAL(10, 2),
    created_at TIMESTAMP(6) not null,
    updated_at TIMESTAMP(6),
    deleted_at TIMESTAMP(6)
);

CREATE TABLE rating
(
    id UUID PRIMARY KEY,
    trip_id uuid not null
    constraint uc_rating_trip unique
    constraint fk_rating_on_trip
    references trip,
    score INT not null,
    comment VARCHAR(500),
    created_at TIMESTAMP(6) not null
)