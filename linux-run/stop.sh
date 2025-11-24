#!/bin/bash

# Navigate to user-service and trip-service directory
cd ../infra

# Stop any existing containers
echo "Stopping existing containers..."
docker-compose down

echo "All containers stopped successfully!"

