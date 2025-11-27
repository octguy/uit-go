@echo off
REM Complete rebuild and restart script for UIT-Go project

echo "=== Stopping Docker containers ==="
cd infra
docker-compose down

echo "=== Building and starting Docker containers ==="
docker-compose up --build

echo "=== All services (Spring Boot + Go gRPC) rebuilt and restarted! ==="