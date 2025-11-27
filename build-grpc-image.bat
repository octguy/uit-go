@echo off
echo Building gRPC services...

REM ================================
REM Copy required proto files
REM ================================

echo Copying proto files for gRPC services...

REM User gRPC Service - copy user.proto
echo Copying user.proto for user-grpc service...
copy "backend\shared-proto\user.proto" "grpc-services\user-service\proto\user.proto" /Y

REM Trip gRPC Service - copy trip.proto  
echo Copying trip.proto for trip-grpc service...
copy "backend\shared-proto\trip.proto" "grpc-services\trip-service\proto\trip.proto" /Y

REM Driver gRPC Service - copy driver.proto
echo Copying driver.proto for driver-grpc service...
copy "backend\shared-proto\driver.proto" "grpc-services\driver-service\proto\driver.proto" /Y

echo Proto files copied successfully!

REM ================================
REM Build gRPC services
REM ================================

echo Building User gRPC Service...
docker build -f grpc-services\Dockerfile.user -t user-grpc:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build user-grpc
    exit /b 1
)

echo Building Trip gRPC Service...
docker build -f grpc-services\Dockerfile.trip -t trip-grpc:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build trip-grpc
    exit /b 1
)

echo Building Driver gRPC Service...
docker build -f grpc-services\Dockerfile.driver -t driver-grpc:latest .
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build driver-grpc
    exit /b 1
)

echo All gRPC services built successfully!
echo Images created:
echo - user-grpc:latest
echo - trip-grpc:latest
echo - driver-grpc:latest

pause