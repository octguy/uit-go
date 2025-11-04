@echo off
REM Sequential Docker image build script for Terraform deployment

echo ============================================================
echo    Building Docker Images Sequentially for TF Deployment
echo ============================================================

cd /d "d:\Learning\Sem5\CloudComputingAndMicroservices\uit-go"

echo.
echo === Phase 1: Building Spring Boot Services ===

echo [1/4] Building User Service...
cd backend\user-service
echo - Building Maven package...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ User service Maven build failed!
    exit /b 1
)
echo - Building Docker image: user-service-tf:latest...
docker build -t user-service-tf:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ User service Docker build failed!
    exit /b 1
)
echo ✅ User service image built successfully
cd ..\..

echo [2/4] Building Trip Service...
cd backend\trip-service
echo - Building Maven package...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ Trip service Maven build failed!
    exit /b 1
)
echo - Building Docker image: trip-service-tf:latest...
docker build -t trip-service-tf:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ Trip service Docker build failed!
    exit /b 1
)
echo ✅ Trip service image built successfully
cd ..\..

echo [3/4] Building Driver Service...
cd backend\driver-service
echo - Building Maven package...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ Driver service Maven build failed!
    exit /b 1
)
echo - Building Docker image: driver-service-tf:latest...
docker build -t driver-service-tf:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ Driver service Docker build failed!
    exit /b 1
)
echo ✅ Driver service image built successfully
cd ..\..

echo [4/4] Building API Gateway...
cd backend\api-gateway
echo - Building Maven package...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ❌ API Gateway Maven build failed!
    exit /b 1
)
echo - Building Docker image: api-gateway-tf:latest...
docker build -t api-gateway-tf:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ API Gateway Docker build failed!
    exit /b 1
)
echo ✅ API Gateway image built successfully
cd ..\..

echo.
echo === Phase 2: Building Go gRPC Services ===

echo [1/3] Building User gRPC Service...
cd grpc-services
echo - Building Docker image: grpc-user-service:latest...
docker build -f Dockerfile.user -t grpc-user-service:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ User gRPC service Docker build failed!
    exit /b 1
)
echo ✅ User gRPC service image built successfully
cd ..

echo [2/3] Building Trip gRPC Service...
cd grpc-services
echo - Building Docker image: grpc-trip-service:latest...
docker build -f Dockerfile.trip -t grpc-trip-service:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ Trip gRPC service Docker build failed!
    exit /b 1
)
echo ✅ Trip gRPC service image built successfully
cd ..

echo [3/3] Building Driver gRPC Service...
cd grpc-services
echo - Building Docker image: grpc-driver-service:latest...
docker build -f Dockerfile.driver -t grpc-driver-service:latest .
if %ERRORLEVEL% neq 0 (
    echo ❌ Driver gRPC service Docker build failed!
    exit /b 1
)
echo ✅ Driver gRPC service image built successfully
cd ..

echo.
echo === Phase 3: Summary ===
echo.
echo 🎉 All Docker images built successfully!
echo.
echo Spring Boot Services:
echo   - user-service-tf:latest
echo   - trip-service-tf:latest  
echo   - driver-service-tf:latest
echo   - api-gateway-tf:latest
echo.
echo Go gRPC Services:
echo   - grpc-user-service:latest
echo   - grpc-trip-service:latest
echo   - grpc-driver-service:latest
echo.
echo 📋 Listing built images:
docker images | findstr "service-tf\|grpc-.*-service"
echo.
echo ✅ Ready for Terraform deployment!
echo    Run: cd infra\terraform ^&^& terraform apply -auto-approve
echo.
pause