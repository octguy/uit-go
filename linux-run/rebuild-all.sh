#!/bin/bash
# Complete rebuild and restart script for UIT-Go project

set -e  # Exit immediately if a command fails

echo "=== Building all Spring Boot + Go gRPC services sequentially ==="
./build-sequential.sh || { echo "Build failed! Stopping script."; exit 1; }

echo "=== All builds completed successfully ==="
echo "=== Stopping Docker containers ==="

cd ..
cd infra
docker-compose down

echo "=== Building and starting Docker containers ==="
docker-compose up --build

echo "=== All services (Spring Boot + Go gRPC) rebuilt and restarted! ==="
