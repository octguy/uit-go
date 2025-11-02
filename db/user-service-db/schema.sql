-- User Service Database Schema

-- Enable UUID extension

CREATE TABLE user (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) not null,
    deleted_at TIMESTAMP(6)
);