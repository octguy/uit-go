@echo off
REM Complete rebuild and restart script for UIT-Go project

echo "=== Building all Spring Boot services sequentially ==="
call build-sequential.bat
if %ERRORLEVEL% neq 0 (
    echo "Build failed! Stopping script."
    exit /b 1
)

echo "=== All builds completed successfully ==="
echo "=== Stopping Docker containers ==="
cd infra
docker-compose down

echo "=== Building and starting Docker containers ==="
docker-compose up --build

echo "=== All services rebuilt and restarted! ==="