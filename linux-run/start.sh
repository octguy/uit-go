#!/bin/bash

# Navigate to user-service and trip-service directory
cd ../infra

# Stop any existing containers
echo "Stopping existing containers..."
docker-compose down

# Start PostgreSQL databases, user-service and trip-service
echo "Starting containers..."

echo "Starting driver service and its database..."
docker-compose up -d --build driver-service-db driver-service

echo "Starting user service and its database..."
docker-compose up -d --build user-service-db user-service

echo "Starting trip service and its database..."
docker-compose up -d --build trip-service-db trip-service

# Wait for database to be ready
echo "Waiting for database to be ready..."

# Show running containers
echo "User, Trip, Driver Service containers:"
docker-compose ps

echo "User Service started successfully!"
echo "Trip Service started successfully!"
echo "Driver Service started successfully!"
echo "Application available at: http://localhost:8081, http://localhost:8082, http://localhost:8083"
echo "Database available at: localhost:5435, localhost:5433, localhost:5434"

