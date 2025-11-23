#!/bin/bash
# Restart Docker containers without rebuilding (Pattern 2: Spring Boot + gRPC)

set -e  # Exit immediately if a command fails

cd ..
echo "=== Stopping Docker containers ==="
cd infra
docker-compose down

echo "=== Starting Docker containers (Spring Boot + gRPC services) ==="
docker-compose up

echo "=== All containers restarted! ==="
echo "=== Spring Boot APIs: 8080-8083 | gRPC Services: 50051-50053 ==="
