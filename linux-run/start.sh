#!/bin/bash

set -e  # Stop script when any command fails

echo "=============================================="
echo "            UIT-GO DEPLOY SCRIPT"
echo "=============================================="

# Move into infra folder
cd ../infra

echo "Stopping existing containers..."
docker-compose down

echo "Starting containers (with build cache)..."
docker-compose up -d --build

echo "Waiting for databases to be ready..."
sleep 5

echo ""
echo "=============================================="
echo "               RUNNING CONTAINERS"
echo "=============================================="
docker-compose ps
echo "=============================================="

echo ""
echo "🚀 Services started successfully!"

echo ""
echo "🔧 Application Endpoints:"
echo "User Service:    http://localhost:8081"
echo "Trip Service:    http://localhost:8082"
echo "Driver Service:  http://localhost:8083"

echo ""
echo "🗄  Database endpoints:"
echo "User DB:    localhost:5435"
echo "Trip DB:    localhost:5433"

echo ""
echo "🧰 Redis:"
echo "Redis:       localhost:6379"

echo ""
echo "🎉 Deploy done!"
