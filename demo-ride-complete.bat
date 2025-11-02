@echo off
setlocal enabledelayedexpansion

REM UIT-Go Complete Ride-Sharing Platform Demo
REM This demo showcases the full ride-sharing workflow across all services

echo.
echo ============================================================
echo 🚀 UIT-Go Complete Ride-Sharing Platform Demo
echo ============================================================
echo This demo demonstrates a complete ride scenario using:
echo    👤 User Service (gRPC)
echo    🚗 Driver Service (gRPC)  
echo    🚕 Trip Service (gRPC)
echo    🌐 API Gateway (Spring Boot)
echo    🐰 RabbitMQ (Message Queue)
echo.

REM Service endpoints
set USER_GRPC_PORT=50051
set DRIVER_GRPC_PORT=50053
set TRIP_GRPC_PORT=50052
set API_GATEWAY_PORT=8080
set USER_API_PORT=8081
set TRIP_API_PORT=8082
set DRIVER_API_PORT=8083
set RABBITMQ_PORT=15672

echo 📋 Service Endpoints:
echo    👤 User Service gRPC:    localhost:%USER_GRPC_PORT%
echo    🚗 Driver Service gRPC:  localhost:%DRIVER_GRPC_PORT%
echo    🚕 Trip Service gRPC:    localhost:%TRIP_GRPC_PORT%
echo    🌐 API Gateway:          localhost:%API_GATEWAY_PORT%
echo    👤 User API Backend:     localhost:%USER_API_PORT%
echo    � Driver API Backend:   localhost:%DRIVER_API_PORT%
echo    �🚕 Trip API Backend:     localhost:%TRIP_API_PORT%
echo    🐰 RabbitMQ Management:  localhost:%RABBITMQ_PORT%
echo.

REM Check dependencies
where grpcurl >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set GRPCURL_AVAILABLE=true
    echo ✅ grpcurl detected - will perform real gRPC calls
) else (
    set GRPCURL_AVAILABLE=false
    echo ⚠️  grpcurl not found - install with: go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
)

where curl >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set CURL_AVAILABLE=true
    echo ✅ curl detected - will perform real HTTP calls
) else (
    set CURL_AVAILABLE=false
    echo ⚠️  curl not found - please install curl for HTTP API testing
)

if "%GRPCURL_AVAILABLE%"=="false" if "%CURL_AVAILABLE%"=="false" (
    echo.
    echo ❌ Missing required tools. This demo requires grpcurl and curl.
    echo    Please install them and run again.
    pause
    exit /b 1
)

echo.
pause

echo.
echo ============================================================
echo 🔍 PHASE 1: System Health Check
echo ============================================================
echo Verifying all services are running and healthy...
echo.

REM Check service connectivity
for %%s in (USER_GRPC:50051 DRIVER_GRPC:50053 TRIP_GRPC:50052 API_GATEWAY:8080 USER_API:8081 DRIVER_API:8083 TRIP_API:8082) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        echo Testing %%a service on port %%b...
        powershell -Command "try { Test-NetConnection -ComputerName localhost -Port %%b -InformationLevel Quiet -WarningAction SilentlyContinue | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            echo        ✅ %%a service is accessible
        ) else (
            echo        ❌ %%a service is not accessible
        )
    )
)

echo.
echo 🏥 Testing gRPC Health Checks...
echo.

if "%GRPCURL_AVAILABLE%"=="true" (
    echo 👤 User Service Health Check:
    grpcurl -plaintext localhost:%USER_GRPC_PORT% user.UserService/HealthCheck
    echo.
    
    echo 🚗 Driver Service Health Check:
    grpcurl -plaintext localhost:%DRIVER_GRPC_PORT% driver.DriverService/HealthCheck
    echo.
    
    echo 🚕 Trip Service Health Check:
    grpcurl -plaintext localhost:%TRIP_GRPC_PORT% trip.TripService/HealthCheck
    echo.
) else (
    echo ⚠️  Skipping gRPC health checks (grpcurl not available)
)

echo ✅ Phase 1 Complete: System health verified
pause

echo.
echo ============================================================
echo 👤 PHASE 2: User Registration and Profile Management
echo ============================================================
echo Demonstrating user onboarding process...
echo.

REM User data for demo
set USER_ID=550e8400-e29b-41d4-a716-446655440001
set USER_EMAIL=demo@example.com
set USER_PHONE=+1234567890

echo 📱 Step 1: User Registration (via API Gateway)
if "%CURL_AVAILABLE%"=="true" (
    echo Creating new user profile...
    curl -X POST "http://localhost:%API_GATEWAY_PORT%/api/users/register" ^
        -H "Content-Type: application/json" ^
        -d "{\"email\":\"%USER_EMAIL%\",\"phone\":\"%USER_PHONE%\",\"name\":\"Demo User\",\"password\":\"demo123\"}"
    echo.
) else (
    echo ⚠️  Would create user: %USER_EMAIL%
)

echo.
echo 🔍 Step 2: User Validation (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Validating user via gRPC...
    grpcurl -plaintext -d "{\"user_id\":\"%USER_ID%\"}" localhost:%USER_GRPC_PORT% user.UserService/ValidateUser
    echo.
) else (
    echo ⚠️  Would validate user ID: %USER_ID%
)

echo.
echo 👤 Step 3: Get User Profile (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Retrieving user profile...
    grpcurl -plaintext -d "{\"user_id\":\"%USER_ID%\"}" localhost:%USER_GRPC_PORT% user.UserService/GetUserProfile
    echo.
) else (
    echo ⚠️  Would retrieve profile for user: %USER_ID%
)

echo ✅ Phase 2 Complete: User management demonstrated
pause

echo.
echo ============================================================
echo 🚗 PHASE 3: Driver Onboarding and Location Updates
echo ============================================================
echo Demonstrating driver management and real-time tracking...
echo.

REM Driver data for demo
set DRIVER_ID=660e8400-e29b-41d4-a716-446655440002
set DRIVER_LAT=10.762622
set DRIVER_LON=106.660172

echo 🚗 Step 1: Driver Registration
if "%CURL_AVAILABLE%"=="true" (
    echo Registering new driver...
    curl -X POST "http://localhost:%API_GATEWAY_PORT%/api/drivers/register" ^
        -H "Content-Type: application/json" ^
        -d "{\"email\":\"driver@example.com\",\"phone\":\"+0987654321\",\"name\":\"Demo Driver\",\"license_number\":\"ABC123\",\"vehicle_type\":\"sedan\"}"
    echo.
    echo Note: Using pre-existing driver ID for demo consistency...
    echo Driver ID: %DRIVER_ID%
) else (
    echo ⚠️  Would register driver with license: ABC123
    echo Using demo driver ID: %DRIVER_ID%
)

echo.
echo 📍 Step 2: Driver Location Update (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Updating driver location...
    echo Note: Location updates require driver to exist in database first
    grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\",\"latitude\":\"%DRIVER_LAT%\",\"longitude\":\"%DRIVER_LON%\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverLocation
    echo.
) else (
    echo ⚠️  Would update driver location to: %DRIVER_LAT%, %DRIVER_LON%
)

echo.
echo 🟢 Step 3: Driver Status Change (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Setting driver to AVAILABLE...
    grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\",\"status\":\"AVAILABLE\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
    echo.
) else (
    echo ⚠️  Would set driver status to: AVAILABLE
)

echo.
echo 🔍 Step 4: Get Driver Status (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Checking driver status...
    grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/GetDriverStatus
    echo.
) else (
    echo ⚠️  Would check status for driver: %DRIVER_ID%
)

echo ✅ Phase 3 Complete: Driver management demonstrated
pause

echo.
echo ============================================================
echo 🚕 PHASE 4: Trip Request and Matching Process
echo ============================================================
echo Demonstrating the complete trip booking workflow...
echo.

REM Trip data for demo
set PICKUP_LAT=10.762622
set PICKUP_LON=106.660172
set DEST_LAT=10.775622
set DEST_LON=106.670172

echo 🔍 Step 1: Find Nearby Drivers (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Finding available drivers near pickup location...
    grpcurl -plaintext -d "{\"latitude\":\"%PICKUP_LAT%\",\"longitude\":\"%PICKUP_LON%\",\"radius_km\":\"5.0\",\"limit\":\"10\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/FindNearbyDrivers
    echo.
) else (
    echo ⚠️  Would search for drivers near: %PICKUP_LAT%, %PICKUP_LON%
)

echo.
echo 💰 Step 2: Get Fare Estimate (via API Gateway)
if "%CURL_AVAILABLE%"=="true" (
    echo Calculating trip fare estimate...
    curl -X POST "http://localhost:%API_GATEWAY_PORT%/api/trips/estimate" ^
        -H "Content-Type: application/json" ^
        -d "{\"pickup_latitude\":%PICKUP_LAT%,\"pickup_longitude\":%PICKUP_LON%,\"destination_latitude\":%DEST_LAT%,\"destination_longitude\":%DEST_LON%}"
    echo.
) else (
    echo ⚠️  Would calculate fare from pickup to destination
)

echo.
echo 🚕 Step 3: Create Trip Request (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Creating trip request...
    grpcurl -plaintext -d "{\"passenger_id\":\"%USER_ID%\",\"pickup_latitude\":\"%PICKUP_LAT%\",\"pickup_longitude\":\"%PICKUP_LON%\",\"destination_latitude\":\"%DEST_LAT%\",\"destination_longitude\":\"%DEST_LON%\",\"pickup_location\":\"Downtown District 1\",\"destination\":\"Ben Thanh Market\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/CreateTrip
    echo.
) else (
    echo ⚠️  Would create trip from Downtown District 1 to Ben Thanh Market
)

echo.
echo 📱 Step 4: Assign Driver (via API Gateway)
if "%CURL_AVAILABLE%"=="true" (
    echo Assigning driver to trip...
    curl -X POST "http://localhost:%API_GATEWAY_PORT%/api/trips/assign-driver" ^
        -H "Content-Type: application/json" ^
        -d "{\"trip_id\":\"770e8400-e29b-41d4-a716-446655440003\",\"driver_id\":\"%DRIVER_ID%\"}"
    echo.
) else (
    echo ⚠️  Would assign driver %DRIVER_ID% to trip
)

echo ✅ Phase 4 Complete: Trip creation and assignment demonstrated
pause

echo.
echo ============================================================
echo 📊 PHASE 5: Real-time Trip Tracking
echo ============================================================
echo Demonstrating trip status updates and tracking...
echo.

set TRIP_ID=770e8400-e29b-41d4-a716-446655440003

echo 🔍 Step 1: Get Trip Status (gRPC)
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Checking current trip status...
    grpcurl -plaintext -d "{\"trip_id\":\"%TRIP_ID%\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/GetTripStatus
    echo.
) else (
    echo ⚠️  Would check status for trip: %TRIP_ID%
)

echo.
echo 🚗 Step 2: Driver En Route Update
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Driver heading to pickup location...
    grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\",\"status\":\"EN_ROUTE\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
    echo.
) else (
    echo ⚠️  Would update driver status to: EN_ROUTE
)

echo.
echo 📍 Step 3: Driver Location Updates (Real-time)
echo Simulating real-time location tracking...
for %%i in (1 2 3) do (
    if "%GRPCURL_AVAILABLE%"=="true" (
        echo    Update %%i - Driver moving toward pickup...
        set /a NEW_LAT=10762622 + %%i * 100
        set /a NEW_LON=106660172 + %%i * 150
        set NEW_LAT_STR=10.!NEW_LAT:~-6!
        set NEW_LON_STR=106.!NEW_LON:~-6!
        grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\",\"latitude\":\"!NEW_LAT_STR!\",\"longitude\":\"!NEW_LON_STR!\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverLocation
        timeout /t 2 >nul
    ) else (
        echo    ⚠️  Update %%i - Would send location update
    )
)

echo.
echo 🚗 Step 4: Driver Arrived at Pickup
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Driver has arrived at pickup location...
    grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\",\"status\":\"ARRIVED\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
    echo.
) else (
    echo ⚠️  Would update driver status to: ARRIVED
)

echo ✅ Phase 5 Complete: Real-time tracking demonstrated
pause

echo.
echo ============================================================
echo 🐰 PHASE 6: Message Queue Integration
echo ============================================================
echo Demonstrating RabbitMQ message publishing and queues...
echo.

echo 📊 Step 1: Check RabbitMQ Queue Status
if "%CURL_AVAILABLE%"=="true" (
    echo Checking current queue status...
    curl -s -u guest:guest "http://localhost:%RABBITMQ_PORT%/api/queues" >temp_queue_status.json 2>nul
    if !ERRORLEVEL! equ 0 (
        echo    ✅ RabbitMQ Management API accessible
        echo    📋 Current queues status:
        findstr /i "name.*messages" temp_queue_status.json 2>nul || echo    📝 Queues data retrieved
        del temp_queue_status.json 2>nul
    ) else (
        echo    ❌ RabbitMQ Management API not accessible
    )
    echo.
) else (
    echo ⚠️  Would check RabbitMQ queue status
)

echo.
echo 📨 Step 2: Trigger Location Update Events (via API)
if "%CURL_AVAILABLE%"=="true" (
    echo Publishing driver location updates to RabbitMQ...
    curl -X POST "http://localhost:8080/api/drivers/%DRIVER_ID%/location" ^
        -H "Content-Type: application/json" ^
        -d "{\"latitude\":10.765,\"longitude\":106.665,\"timestamp\":\"$(Get-Date -Format 'yyyy-MM-ddTHH:mm:ss')\"}"
    echo.
) else (
    echo ⚠️  Would publish location update to message queue
)

echo.
echo 📊 Step 3: Check Queue After Updates
if "%CURL_AVAILABLE%"=="true" (
    timeout /t 2 >nul
    echo Checking queue status after location updates...
    curl -s -u guest:guest "http://localhost:%RABBITMQ_PORT%/api/queues" >temp_queue_updated.json 2>nul
    if !ERRORLEVEL! equ 0 (
        echo    📈 Updated queue status retrieved
        del temp_queue_updated.json 2>nul
    ) else (
        echo    ❌ Could not retrieve updated queue status
    )
    echo.
) else (
    echo ⚠️  Would check updated queue status
)

echo ✅ Phase 6 Complete: Message queue integration demonstrated
pause

echo.
echo ============================================================
echo ⭐ PHASE 7: Trip Completion and Rating
echo ============================================================
echo Demonstrating trip completion and rating system...
echo.

echo 🏁 Step 1: Complete Trip (via API)
if "%CURL_AVAILABLE%"=="true" (
    echo Marking trip as completed...
    curl -X PUT "http://localhost:%API_GATEWAY_PORT%/api/trips/%TRIP_ID%/complete" ^
        -H "Content-Type: application/json" ^
        -d "{\"final_fare\":25.50,\"payment_method\":\"credit_card\"}"
    echo.
) else (
    echo ⚠️  Would complete trip with fare: $25.50
)

echo.
echo ⭐ Step 2: Submit Rating (via Trip Service API)
if "%CURL_AVAILABLE%"=="true" (
    echo Passenger rating the trip...
    curl -X POST "http://localhost:%TRIP_API_PORT%/api/ratings" ^
        -H "Content-Type: application/json" ^
        -d "{\"trip_id\":\"%TRIP_ID%\",\"rater_id\":\"%USER_ID%\",\"rated_entity_id\":\"%DRIVER_ID%\",\"rating_type\":\"driver\",\"rating\":5,\"comment\":\"Excellent service, very professional driver!\"}"
    echo.
) else (
    echo ⚠️  Would submit 5-star rating for driver
)

echo.
echo 🚗 Step 3: Driver Available Again
if "%GRPCURL_AVAILABLE%"=="true" (
    echo Setting driver back to available status...
    grpcurl -plaintext -d "{\"driver_id\":\"%DRIVER_ID%\",\"status\":\"AVAILABLE\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
    echo.
) else (
    echo ⚠️  Would set driver status back to: AVAILABLE
)

echo ✅ Phase 7 Complete: Trip completion and rating demonstrated

echo.
echo ============================================================
echo 🎉 DEMO COMPLETE: Full Ride-Sharing Workflow
echo ============================================================
echo.
echo 📊 Summary of demonstrated features:
echo    ✅ User registration and profile management
echo    ✅ Driver onboarding and real-time tracking
echo    ✅ Trip creation and fare estimation
echo    ✅ Driver-trip matching and assignment
echo    ✅ Real-time location updates
echo    ✅ RabbitMQ message queue integration
echo    ✅ Trip completion and rating system
echo.
echo 🏗️ Services demonstrated:
echo    👤 User Service (gRPC + Spring Boot)
echo    🚗 Driver Service (gRPC + Spring Boot)
echo    🚕 Trip Service (gRPC + Spring Boot)
echo    🌐 API Gateway (Spring Boot)
echo    🐰 RabbitMQ (Message Queue)
echo.
echo 💡 This demo showed a complete ride from request to completion,
echo    demonstrating how all microservices work together in the
echo    UIT-Go ride-sharing platform.
echo.
echo Thank you for watching the UIT-Go platform demonstration!
pause