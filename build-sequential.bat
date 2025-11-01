@echo off
REM Sequential build script for reliable completion

echo "=== Building services sequentially ==="

echo "Building user-service..."
cd backend\user-service
call mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo "User service build failed!"
    exit /b 1
)

echo "Building trip-service..."
cd ..\trip-service
call mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo "Trip service build failed!"
    exit /b 1
)

echo "Building driver-service..."
cd ..\driver-service
call mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo "Driver service build failed!"
    exit /b 1
)

echo "Building api-gateway..."
cd ..\api-gateway
call mvnw.cmd clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo "API Gateway build failed!"
    exit /b 1
)

cd ..\..
echo "=== All services built successfully! ==="