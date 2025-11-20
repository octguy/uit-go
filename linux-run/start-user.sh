#!/bin/bash

echo "Starting User Service and Database..."

# Navigate to user-service directory
cd ../infra

# Stop any existing containers
echo "Stopping existing containers..."
docker-compose down

# Start PostgreSQL database and user-service
echo "Starting containers..."
docker-compose up -d --build user-service-db user-service

# Wait for database to be ready
echo "Waiting for database to be ready..."
sleep 10

# Show running containers
echo "User Service containers:"
docker-compose ps

echo "User Service started successfully!"
echo "Application available at: http://localhost:8081"
echo "Database available at: localhost:5432"

