-- Trip Service Database Schema

-- Enable UUID extension

CREATE TABLE trip (
    id UUID PRIMARY KEY NOT NULL,
    passenger_id UUID NOT NULL,
    driver_id UUID,
    status VARCHAR(255) NOT NULL,
    pickup_location VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    destination_latitude DOUBLE PRECISION NOT NULL,
    destination_longitude DOUBLE PRECISION NOT NULL,
    fare DECIMAL(19, 2),
    requested_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP
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