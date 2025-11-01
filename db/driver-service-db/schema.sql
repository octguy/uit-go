-- Driver Service Database Schema

CREATE TABLE drivers (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE NOT NULL,
    license_number VARCHAR(50) UNIQUE NOT NULL,
    vehicle_info VARCHAR(200) NOT NULL,
    status VARCHAR(20) DEFAULT 'OFFLINE' 
        CHECK (status IN ('OFFLINE', 'ONLINE', 'BUSY')),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);