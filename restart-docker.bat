@echo off
REM Restart Docker containers without rebuilding Spring Boot

echo "=== Stopping Docker containers ==="
cd infra
docker-compose down

echo "=== Starting Docker containers ==="
docker-compose up

echo "=== Docker containers restarted! ==="