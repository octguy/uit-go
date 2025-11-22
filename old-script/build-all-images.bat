@echo off
REM Quick Docker image build script for all services

echo Building all Docker images for Terraform deployment...

cd /d "d:\Learning\Sem5\CloudComputingAndMicroservices\uit-go"

REM Spring Boot Services
echo === Building Spring Boot Services ===

echo Building user-service...
cd backend\user-service && mvn clean package -DskipTests && docker build -t user-service-tf:latest . && cd ..\..

echo Building trip-service...
cd backend\trip-service && mvn clean package -DskipTests && docker build -t trip-service-tf:latest . && cd ..\..

echo Building driver-service...
cd backend\driver-service && mvn clean package -DskipTests && docker build -t driver-service-tf:latest . && cd ..\..

echo Building api-gateway...
cd backend\api-gateway && mvn clean package -DskipTests && docker build -t api-gateway-tf:latest . && cd ..\..

REM Go gRPC Services
echo === Building Go gRPC Services ===

echo Building grpc-user-service...
cd grpc-services && docker build -f Dockerfile.user -t grpc-user-service:latest . && cd ..

echo Building grpc-trip-service...
cd grpc-services && docker build -f Dockerfile.trip -t grpc-trip-service:latest . && cd ..

echo Building grpc-driver-service...
cd grpc-services && docker build -f Dockerfile.driver -t grpc-driver-service:latest . && cd ..

echo.
echo === All images built! ===
docker images | findstr "service-tf\|grpc-.*-service"
echo.
echo Ready for Terraform deployment!