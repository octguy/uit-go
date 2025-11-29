@echo off
echo Building Spring Boot services...

REM ================================
REM Copy required proto files
REM ================================

echo Copying proto files...

REM Trip Service needs user.proto
echo Copying user.proto for trip-service...
copy "backend\shared-proto\user.proto" "backend\trip-service\src\main\proto\user.proto" /Y

REM Driver Service placeholder (add proto dependencies when needed)
REM copy "backend\shared-proto\xxx.proto" "backend\driver-service\src\main\proto\xxx.proto" /Y

REM User Service placeholder (add proto dependencies when needed)  
REM copy "backend\shared-proto\xxx.proto" "backend\user-service\src\main\proto\xxx.proto" /Y

REM API Gateway placeholder (add proto dependencies when needed)
REM copy "backend\shared-proto\xxx.proto" "backend\api-gateway\src\main\proto\xxx.proto" /Y

echo Proto files copied successfully!

REM ================================
REM Build Spring Boot services
REM ================================

echo Building User Service...
cd backend\user-service
call mvnw clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build user-service
    exit /b 1
)
docker build -t user-service:latest .
cd ..\..

echo Building Trip Service...
cd backend\trip-service
call mvnw clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build trip-service
    exit /b 1
)
docker build -t trip-service:latest .
cd ..\..

echo Building Driver Service...
cd backend\driver-service
call mvnw clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build driver-service
    exit /b 1
)
docker build -t driver-service:latest .
cd ..\..

echo Building API Gateway...
cd backend\api-gateway
call mvnw clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to build api-gateway
    exit /b 1
)
docker build -t api-gateway:latest .
cd ..\..

echo All Spring Boot services built successfully!
echo Images created:
echo - api-gateway:latest
echo - user-service:latest
echo - trip-service:latest  
echo - driver-service:latest

pause