@echo off
REM Complete rebuild and restart script for UIT-Go project using Terraform

echo "=== Building all Spring Boot + Go gRPC services sequentially ==="
call build-sequential-tf.bat
if %ERRORLEVEL% neq 0 (
    echo "Build failed! Stopping script."
    exit /b 1
)

echo "=== All builds completed successfully ==="
echo "=== Destroying existing Terraform infrastructure ==="
cd infra\terraform
terraform destroy -auto-approve

echo "=== Building Docker images ==="
cd ..\..\

echo "Building Spring Boot Docker images..."
cd backend\user-service
docker build -t uit-go/user-service:latest .
if %ERRORLEVEL% neq 0 (
    echo "User service Docker build failed!"
    exit /b 1
)

cd ..\trip-service
docker build -t uit-go/trip-service:latest .
if %ERRORLEVEL% neq 0 (
    echo "Trip service Docker build failed!"
    exit /b 1
)

cd ..\driver-service
docker build -t uit-go/driver-service:latest .
if %ERRORLEVEL% neq 0 (
    echo "Driver service Docker build failed!"
    exit /b 1
)

cd ..\api-gateway
docker build -t uit-go/api-gateway:latest .
if %ERRORLEVEL% neq 0 (
    echo "API Gateway Docker build failed!"
    exit /b 1
)

echo "Building gRPC Docker images..."
cd ..\grpc-services
docker build -f Dockerfile.user -t uit-go/grpc-user-service:latest .
if %ERRORLEVEL% neq 0 (
    echo "gRPC User service Docker build failed!"
    exit /b 1
)

docker build -f Dockerfile.trip -t uit-go/grpc-trip-service:latest .
if %ERRORLEVEL% neq 0 (
    echo "gRPC Trip service Docker build failed!"
    exit /b 1
)

docker build -f Dockerfile.driver -t uit-go/grpc-driver-service:latest .
if %ERRORLEVEL% neq 0 (
    echo "gRPC Driver service Docker build failed!"
    exit /b 1
)

echo "=== Deploying with Terraform ==="
cd ..\infra\terraform
terraform apply -auto-approve
if %ERRORLEVEL% neq 0 (
    echo "Terraform apply failed!"
    exit /b 1
)

cd ..\..
echo "=== All services (Spring Boot + Go gRPC) rebuilt and deployed with Terraform! ==="
echo "=== Services Available: ==="
echo "Spring Boot Services:"
echo "  - User Service: http://localhost:8081"
echo "  - Trip Service: http://localhost:8082" 
echo "  - Driver Service: http://localhost:8083"
echo "gRPC Services:"
echo "  - gRPC User Service: http://localhost:50051"
echo "  - gRPC Driver Service: http://localhost:50052"
echo "  - gRPC Trip Service: http://localhost:50053"
echo "Databases:"
echo "  - User DB: localhost:5433"
echo "  - Trip DB: localhost:5434"
echo "  - Driver DB: localhost:5435"