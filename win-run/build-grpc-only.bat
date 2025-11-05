@echo off
REM Build only Go gRPC services for faster development

echo "=== Building Go gRPC services only ==="

echo "Building user-grpc-service..."
cd grpc-services
go mod tidy
go build -o user-service ./user-service
if %ERRORLEVEL% neq 0 (
    echo "User gRPC service build failed!"
    exit /b 1
)

echo "Building trip-grpc-service..."
go build -o trip-service ./trip-service
if %ERRORLEVEL% neq 0 (
    echo "Trip gRPC service build failed!"
    exit /b 1
)

echo "Building driver-grpc-service..."
go build -o driver-service ./driver-service
if %ERRORLEVEL% neq 0 (
    echo "Driver gRPC service build failed!"
    exit /b 1
)

cd ..
echo "=== All Go gRPC services built successfully! ==="
echo "=== Run 'restart-docker.bat' to deploy changes ==="