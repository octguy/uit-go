#!/bin/bash
# Sequential build script for reliable completion

set -e  # Exit immediately if a command fails

cd ..
echo "=== Building services sequentially ==="

echo "Building user-service..."
cd backend/user-service
./mvnw clean || { echo "User service clean failed!"; exit 1; }
./mvnw package -DskipTests || { echo "User service build failed!"; exit 1; }

echo "Building trip-service..."
cd ../trip-service
./mvnw clean || { echo "Trip service clean failed!"; exit 1; }
./mvnw package -DskipTests || { echo "Trip service build failed!"; exit 1; }

echo "Building driver-service..."
cd ../driver-service
./mvnw clean || { echo "Driver service clean failed!"; exit 1; }
./mvnw package -DskipTests || { echo "Driver service build failed!"; exit 1; }

echo "Building api-gateway..."
cd ../api-gateway
./mvnw clean || { echo "API Gateway clean failed!"; exit 1; }
./mvnw package -DskipTests || { echo "API Gateway build failed!"; exit 1; }

cd ../..

echo "=== Building Go gRPC services ==="

cd grpc-services
go mod tidy

echo "Building user-grpc-service..."
go build -o user-service ./user-service || { echo "User gRPC service build failed!"; exit 1; }

echo "Building trip-grpc-service..."
go build -o trip-service ./trip-service || { echo "Trip gRPC service build failed!"; exit 1; }

echo "Building driver-grpc-service..."
go build -o driver-service ./driver-service || { echo "Driver gRPC service build failed!"; exit 1; }

cd ..
echo "=== All services (Spring Boot + Go gRPC) built successfully! ==="
