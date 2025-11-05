#!/bin/bash
# Build only Go gRPC services for faster development

set -e  # Exit immediately if a command fails

echo "=== Building Go gRPC services only ==="

cd grpc-services
go mod tidy

echo "Building user-grpc-service..."
go build -o user-service ./user-service || { echo "User gRPC service build failed!"; exit 1; }

echo "Building trip-grpc-service..."
go build -o trip-service ./trip-service || { echo "Trip gRPC service build failed!"; exit 1; }

echo "Building driver-grpc-service..."
go build -o driver-service ./driver-service || { echo "Driver gRPC service build failed!"; exit 1; }

cd ..
echo "=== All Go gRPC services built successfully! ==="
echo "=== Run './restart-docker.sh' to deploy changes ==="
