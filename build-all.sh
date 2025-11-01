#!/bin/bash

# Build all Spring Boot services in separate terminals using Maven Wrapper

echo "Starting build for all services..."

# Function to build a service
build_service() {
    local service_name=$1
    echo "Building $service_name..."
    cd backend/$service_name
    ./mvnw clean package -DskipTests
    cd ../..
    echo "$service_name build completed!"
}

# Build all services sequentially
build_service "user-service"
build_service "trip-service" 
build_service "driver-service"
build_service "api-gateway"

echo "All services built successfully!"