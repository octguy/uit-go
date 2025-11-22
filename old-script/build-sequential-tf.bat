@echo off
REM Sequential build script for Terraform deployment

echo "=== Building Spring Boot services sequentially ==="

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

echo "=== Building Go gRPC services ==="

echo "Building gRPC services..."
cd grpc-services
go mod tidy
if %ERRORLEVEL% neq 0 (
    echo "Go mod tidy failed!"
    exit /b 1
)

echo "Building user-grpc..."
go build -o user-service ./user-service
if %ERRORLEVEL% neq 0 (
    echo "User gRPC service build failed!"
    exit /b 1
)

echo "Building trip-grpc..."
go build -o trip-service ./trip-service
if %ERRORLEVEL% neq 0 (
    echo "Trip gRPC service build failed!"
    exit /b 1
)

echo "Building driver-grpc..."
go build -o driver-service ./driver-service
if %ERRORLEVEL% neq 0 (
    echo "Driver gRPC service build failed!"
    exit /b 1
)

cd ..
echo "=== All services (Spring Boot + Go gRPC) built successfully! ==="
echo "=== Ready for Terraform deployment ==="