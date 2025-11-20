#!/bin/bash

echo "Starting User Service, Trip Service and their databases..."

# Navigate to user-service and trip-service directory
cd ../infra

# Stop any existing containers
echo "Stopping existing containers..."
docker-compose down

# Start PostgreSQL databases, user-service and trip-service
echo "Starting containers..."
docker-compose up -d --build user-service-db user-service
docker-compose up -d --build trip-service-db trip-service

# Wait for database to be ready
echo "Waiting for database to be ready..."
sleep 10

# Show running containers
echo "User, Trip Service containers:"
docker-compose ps

echo "User Service started successfully!"
echo "Trip Service started successfully!"
echo "Application available at: http://localhost:8081, http://localhost:8082"
echo "Database available at: localhost:5435, localhost:5433"

