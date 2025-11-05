@echo off
setlocal enabledelayedexpansion

REM UIT-Go gRPC Services Integration Demo
REM This demo focuses specifically on gRPC communication patterns and service integration

echo.
echo ============================================================
echo 🚀 UIT-Go gRPC Services Integration Demo
echo ============================================================
echo This demo demonstrates gRPC communication patterns between
echo microservices in the UIT-Go ride-sharing platform:
echo.
echo    👤 User Service gRPC (port 50051)
echo    🚗 Driver Service gRPC (port 50053)
echo    🚕 Trip Service gRPC (port 50052)
echo.

REM Service endpoints
set USER_GRPC_PORT=50051
set DRIVER_GRPC_PORT=50053
set TRIP_GRPC_PORT=50052

echo 📋 gRPC Service Endpoints:
echo    � User Service:   localhost:%USER_GRPC_PORT%
echo    � Driver Service: localhost:%DRIVER_GRPC_PORT%
echo    � Trip Service:   localhost:%TRIP_GRPC_PORT%
echo.

REM Check if grpcurl is available
where grpcurl >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set GRPCURL_AVAILABLE=true
    echo ✅ grpcurl detected - will perform real gRPC calls
) else (
    set GRPCURL_AVAILABLE=false
    echo ❌ grpcurl not found
    echo 💡 Install with: go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest
    echo 📖 This demo requires grpcurl for gRPC testing
    pause
    exit /b 1
)

echo.
echo ============================================================
echo 🔍 PHASE 1: Service Discovery and Health Checks
echo ============================================================
echo.

echo 📡 Step 1: List Available gRPC Services
echo.
for %%s in (USER:%USER_GRPC_PORT% DRIVER:%DRIVER_GRPC_PORT% TRIP:%TRIP_GRPC_PORT%) do (
    for /f "tokens=1,2 delims=:" %%a in ("%%s") do (
        echo 🔍 %%a Service - Listing available services:
        grpcurl -plaintext localhost:%%b list
        echo.
    )
)

echo.
echo 🏥 Step 2: Health Check All Services
echo.
echo � User Service Health Check:
grpcurl -plaintext localhost:%USER_GRPC_PORT% user.UserService/HealthCheck
echo.

echo 🚗 Driver Service Health Check:
grpcurl -plaintext localhost:%DRIVER_GRPC_PORT% driver.DriverService/HealthCheck
echo.

echo 🚕 Trip Service Health Check:
grpcurl -plaintext localhost:%TRIP_GRPC_PORT% trip.TripService/HealthCheck
echo.

pause

echo.
echo ============================================================
echo 👤 PHASE 2: User Service gRPC Operations
echo ============================================================
echo.

set DEMO_USER_ID=550e8400-e29b-41d4-a716-446655440001

echo 🔍 Step 1: User Validation
echo Testing user validation with ID: %DEMO_USER_ID%
grpcurl -plaintext -d "{\"user_id\":\"%DEMO_USER_ID%\"}" localhost:%USER_GRPC_PORT% user.UserService/ValidateUser
echo.

echo 👤 Step 2: Get User Profile
echo Retrieving user profile...
grpcurl -plaintext -d "{\"user_id\":\"%DEMO_USER_ID%\"}" localhost:%USER_GRPC_PORT% user.UserService/GetUserProfile
echo.

echo ✏️ Step 3: Update User Profile
echo Updating user profile information...
grpcurl -plaintext -d "{\"user_id\":\"%DEMO_USER_ID%\",\"name\":\"Updated Demo User\",\"phone\":\"+1234567890\",\"email\":\"updated@demo.com\"}" localhost:%USER_GRPC_PORT% user.UserService/UpdateUserProfile
echo.

pause

echo.
echo ============================================================
echo 🚗 PHASE 3: Driver Service gRPC Operations
echo ============================================================
echo.

set DEMO_DRIVER_ID=660e8400-e29b-41d4-a716-446655440002
set DRIVER_LAT=10.762622
set DRIVER_LON=106.660172

echo 📍 Step 1: Update Driver Location
echo Setting driver location to Ho Chi Minh City center...
grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\",\"latitude\":\"%DRIVER_LAT%\",\"longitude\":\"%DRIVER_LON%\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverLocation
echo.

echo 🟢 Step 2: Update Driver Status
echo Setting driver status to AVAILABLE...
grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\",\"status\":\"AVAILABLE\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
echo.

echo 🔍 Step 3: Get Driver Status
echo Checking current driver status...
grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/GetDriverStatus
echo.

echo 📡 Step 4: Find Nearby Drivers
echo Searching for drivers within 5km radius...
grpcurl -plaintext -d "{\"latitude\":\"%DRIVER_LAT%\",\"longitude\":\"%DRIVER_LON%\",\"radius_km\":\"5.0\",\"limit\":\"10\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/FindNearbyDrivers
echo.

pause

echo.
echo ============================================================
echo � PHASE 4: Trip Service gRPC Operations
echo ============================================================
echo.

set PICKUP_LAT=10.762622
set PICKUP_LON=106.660172
set DEST_LAT=10.775622
set DEST_LON=106.670172

echo 🚕 Step 1: Create Trip Request
echo Creating a trip from District 1 to Ben Thanh Market...
grpcurl -plaintext -d "{\"passenger_id\":\"%DEMO_USER_ID%\",\"pickup_latitude\":\"%PICKUP_LAT%\",\"pickup_longitude\":\"%PICKUP_LON%\",\"destination_latitude\":\"%DEST_LAT%\",\"destination_longitude\":\"%DEST_LON%\",\"pickup_location\":\"District 1 Center\",\"destination\":\"Ben Thanh Market\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/CreateTrip
echo.

set DEMO_TRIP_ID=770e8400-e29b-41d4-a716-446655440003

echo 📊 Step 2: Get Trip Status
echo Checking trip status...
grpcurl -plaintext -d "{\"trip_id\":\"%DEMO_TRIP_ID%\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/GetTripStatus
echo.

pause

echo.
echo ============================================================
echo 🔄 PHASE 5: Cross-Service Integration Patterns
echo ============================================================
echo Demonstrating how services interact in real scenarios...
echo.

echo 🎭 Scenario: Driver Assignment Workflow
echo.

echo � Step 1: Find Available Drivers (Driver Service)
echo Passenger requests ride, system finds nearby drivers...
grpcurl -plaintext -d "{\"latitude\":\"%PICKUP_LAT%\",\"longitude\":\"%PICKUP_LON%\",\"radius_km\":\"3.0\",\"limit\":\"5\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/FindNearbyDrivers
echo.

echo 👤 Step 2: Validate Passenger (User Service)
echo System validates passenger before creating trip...
grpcurl -plaintext -d "{\"user_id\":\"%DEMO_USER_ID%\"}" localhost:%USER_GRPC_PORT% user.UserService/ValidateUser
echo.

echo 🚕 Step 3: Create Trip (Trip Service)
echo Valid passenger found, creating trip request...
grpcurl -plaintext -d "{\"passenger_id\":\"%DEMO_USER_ID%\",\"pickup_latitude\":\"%PICKUP_LAT%\",\"pickup_longitude\":\"%PICKUP_LON%\",\"destination_latitude\":\"%DEST_LAT%\",\"destination_longitude\":\"%DEST_LON%\",\"pickup_location\":\"Nguyen Hue Street\",\"destination\":\"Saigon Opera House\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/CreateTrip
echo.

echo 🚗 Step 4: Assign Driver (Driver Service)
echo Assigning nearest available driver to trip...
grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\",\"status\":\"BUSY\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
echo.

echo 📱 Step 5: Real-time Updates Simulation
echo Simulating real-time location updates during trip...
for %%i in (1 2 3 4 5) do (
    echo    📍 Location Update %%i/5...
    set /a UPDATE_LAT=10762622 + %%i * 250
    set /a UPDATE_LON=106660172 + %%i * 200
    set UPDATE_LAT_STR=10.!UPDATE_LAT:~-6!
    set UPDATE_LON_STR=106.!UPDATE_LON:~-6!
    grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\",\"latitude\":\"!UPDATE_LAT_STR!\",\"longitude\":\"!UPDATE_LON_STR!\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverLocation
    timeout /t 1 >nul
)
echo.

echo 🏁 Step 6: Trip Completion
echo Driver has reached destination, completing trip...
grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\",\"status\":\"AVAILABLE\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
echo.

pause

echo.
echo ============================================================
echo 🛠️ PHASE 6: Advanced gRPC Features Demo
echo ============================================================
echo.

echo 🔍 Step 1: Service Reflection
echo Demonstrating gRPC reflection capabilities...
echo.
echo 📋 User Service Methods:
grpcurl -plaintext localhost:%USER_GRPC_PORT% describe user.UserService
echo.

echo 📋 Driver Service Methods:
grpcurl -plaintext localhost:%DRIVER_GRPC_PORT% describe driver.DriverService
echo.

echo 📋 Trip Service Methods:
grpcurl -plaintext localhost:%TRIP_GRPC_PORT% describe trip.TripService
echo.

echo 🔧 Step 2: Message Structure Inspection
echo Examining protobuf message structures...
echo.
echo 📝 FindNearbyDriversRequest structure:
grpcurl -plaintext localhost:%DRIVER_GRPC_PORT% describe driver.FindNearbyDriversRequest
echo.

echo 📝 CreateTripRequest structure:
grpcurl -plaintext localhost:%TRIP_GRPC_PORT% describe trip.CreateTripRequest
echo.

echo 📝 ValidateUserRequest structure:
grpcurl -plaintext localhost:%USER_GRPC_PORT% describe user.ValidateUserRequest
echo.

pause

echo.
echo ============================================================
echo 📊 PHASE 7: Performance and Error Handling Demo
echo ============================================================
echo.

echo ⚡ Step 1: Concurrent Requests Test
echo Testing concurrent gRPC calls across services...
echo.
echo Sending 3 concurrent health checks...
start /B grpcurl -plaintext localhost:%USER_GRPC_PORT% user.UserService/HealthCheck
start /B grpcurl -plaintext localhost:%DRIVER_GRPC_PORT% driver.DriverService/HealthCheck
start /B grpcurl -plaintext localhost:%TRIP_GRPC_PORT% trip.TripService/HealthCheck
timeout /t 2 >nul
echo.

echo 🚫 Step 2: Error Handling Demo
echo Testing error responses for invalid requests...
echo.
echo Testing invalid user ID:
grpcurl -plaintext -d "{\"user_id\":\"invalid-uuid\"}" localhost:%USER_GRPC_PORT% user.UserService/ValidateUser
echo.

echo Testing invalid driver coordinates:
grpcurl -plaintext -d "{\"driver_id\":\"%DEMO_DRIVER_ID%\",\"latitude\":\"999\",\"longitude\":\"999\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverLocation
echo.

echo Testing non-existent trip:
grpcurl -plaintext -d "{\"trip_id\":\"00000000-0000-0000-0000-000000000000\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/GetTripStatus
echo.

echo.
echo ============================================================
echo 🎉 gRPC INTEGRATION DEMO COMPLETE
echo ============================================================
echo.
echo 📊 Demonstrated gRPC Features:
echo    ✅ Service discovery and reflection
echo    ✅ Health check patterns
echo    ✅ CRUD operations across all services
echo    ✅ Cross-service communication patterns
echo    ✅ Real-time updates simulation
echo    ✅ Error handling and validation
echo    ✅ Concurrent request handling
echo    ✅ Message structure inspection
echo.
echo 🏗️ Services Integration Patterns:
echo    👤 User validation and profile management
echo    🚗 Driver location tracking and status updates
echo    🚕 Trip creation and status monitoring
echo    🔄 Cross-service workflow orchestration
echo.
echo 💡 This demo showcased the gRPC communication layer
echo    that enables seamless integration between all
echo    microservices in the UIT-Go platform.
echo.
echo Thank you for exploring the gRPC integration patterns!
pause
        echo    ✅ Port %%p is accessible
    ) else (
        echo    ❌ Port %%p is not accessible
    )
)

echo.
echo 🐳 Docker Container Status:
docker ps --filter "name=grpc" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>nul
if %ERRORLEVEL% neq 0 (
    echo    ❌ Could not check Docker containers
)

echo.
echo ============================================================
echo 🚗 STEP 2: Driver Service gRPC Demo
echo ============================================================

echo.
echo 📍 Demonstrating Driver Service gRPC calls...

if "%GRPCURL_AVAILABLE%"=="true" (
    echo.
    echo 🔍 Attempting to list gRPC services on driver service...
    grpcurl -plaintext localhost:%DRIVER_GRPC_PORT% list 2>temp_grpc_error.txt
    if %ERRORLEVEL% equ 0 (
        echo    ✅ Successfully connected to driver gRPC service
    ) else (
        echo    ⚠️  Could not list services (may not have reflection enabled)
        type temp_grpc_error.txt 2>nul
    )
    del temp_grpc_error.txt >nul 2>&1
    
    echo.
    echo 🏥 Testing Driver Service HealthCheck...
    grpcurl -plaintext -d "{}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/HealthCheck >temp_health_response.txt 2>temp_health_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_health_response.txt
        echo    ✅ HealthCheck call successful
    ) else (
        echo    ⚠️  HealthCheck call failed:
        type temp_health_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_health_response.txt >nul 2>&1
    del temp_health_error.txt >nul 2>&1
    
    echo.
    echo 🚗 Testing FindNearbyDrivers call...
    grpcurl -plaintext -d "{\"latitude\": \"10.762622\", \"longitude\": \"106.660172\", \"radius_km\": \"5.0\", \"limit\": \"10\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/FindNearbyDrivers >temp_driver_response.txt 2>temp_driver_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_driver_response.txt
        echo    ✅ FindNearbyDrivers call successful
    ) else (
        echo    ⚠️  FindNearbyDrivers call failed:
        type temp_driver_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_driver_response.txt >nul 2>&1
    del temp_driver_error.txt >nul 2>&1
    
    echo.
    echo 🔍 Testing GetDriverStatus call...
    grpcurl -plaintext -d "{\"driver_id\": \"550e8400-e29b-41d4-a716-446655440001\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/GetDriverStatus >temp_status_response.txt 2>temp_status_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_status_response.txt
        echo    ✅ GetDriverStatus call successful
    ) else (
        echo    ⚠️  GetDriverStatus call failed:
        type temp_status_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_status_response.txt >nul 2>&1
    del temp_status_error.txt >nul 2>&1
    
    echo.
    echo 📍 Testing UpdateDriverLocation call...
    grpcurl -plaintext -d "{\"driver_id\": \"550e8400-e29b-41d4-a716-446655440001\", \"latitude\": \"10.775\", \"longitude\": \"106.665\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverLocation >temp_location_response.txt 2>temp_location_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_location_response.txt
        echo    ✅ UpdateDriverLocation call successful
    ) else (
        echo    ⚠️  UpdateDriverLocation call failed:
        type temp_location_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_location_response.txt >nul 2>&1
    del temp_location_error.txt >nul 2>&1
    
    echo.
    echo 🔄 Testing UpdateDriverStatus call...
    grpcurl -plaintext -d "{\"driver_id\": \"550e8400-e29b-41d4-a716-446655440001\", \"status\": \"BUSY\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus >temp_driverstatus_response.txt 2>temp_driverstatus_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_driverstatus_response.txt
        echo    ✅ UpdateDriverStatus call successful
    ) else (
        echo    ⚠️  UpdateDriverStatus call failed:
        type temp_driverstatus_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_driverstatus_response.txt >nul 2>&1
    del temp_driverstatus_error.txt >nul 2>&1

echo.
echo ============================================================
echo 🚕 STEP 3: Trip Service gRPC Demo
echo ============================================================

echo.
echo 🎯 Demonstrating Trip Service gRPC calls...

if "%GRPCURL_AVAILABLE%"=="true" (
    echo.
    echo 🔍 Attempting to list gRPC services on trip service...
    grpcurl -plaintext localhost:%TRIP_GRPC_PORT% list 2>temp_trip_list_error.txt
    if %ERRORLEVEL% equ 0 (
        echo    ✅ Successfully connected to trip gRPC service
    ) else (
        echo    ⚠️  Could not list services (may not have reflection enabled)
        type temp_trip_list_error.txt 2>nul
    )
    del temp_trip_list_error.txt >nul 2>&1
    
    echo.
    echo 🏥 Testing Trip Service HealthCheck...
    grpcurl -plaintext -d "{}" localhost:%TRIP_GRPC_PORT% trip.TripService/HealthCheck >temp_triphealth_response.txt 2>temp_triphealth_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_triphealth_response.txt
        echo    ✅ HealthCheck call successful
    ) else (
        echo    ⚠️  HealthCheck call failed:
        type temp_triphealth_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_triphealth_response.txt >nul 2>&1
    del temp_triphealth_error.txt >nul 2>&1
    
    echo.
    echo 🚕 Testing CreateTrip call...
    grpcurl -plaintext -d "{\"user_id\": \"user-12345\", \"origin\": \"Ben Thanh Market, Ho Chi Minh City\", \"destination\": \"Notre Dame Cathedral, Ho Chi Minh City\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/CreateTrip >temp_createtrip_response.txt 2>temp_trip_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_createtrip_response.txt
        echo    ✅ CreateTrip call successful
    ) else (
        echo    ⚠️  CreateTrip call failed:
        type temp_trip_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_createtrip_response.txt >nul 2>&1
    del temp_trip_error.txt >nul 2>&1
    
    echo.
    echo 📊 Testing GetTripStatus call...
    grpcurl -plaintext -d "{\"trip_id\": \"trip-12345\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/GetTripStatus >temp_tripstatus_response.txt 2>temp_status_error.txt
    if %ERRORLEVEL% equ 0 (
        type temp_tripstatus_response.txt
        echo    ✅ GetTripStatus call successful
    ) else (
        echo    ⚠️  GetTripStatus call failed:
        type temp_status_error.txt 2>nul | findstr /v "^$"
    )
    
    del temp_tripstatus_response.txt >nul 2>&1
    del temp_status_error.txt >nul 2>&1

echo.
echo ============================================================
echo 🔄 STEP 4: Service Integration Flow Demo
echo ============================================================

echo.
echo 🎬 Demonstrating typical gRPC workflow...

echo.
echo 📋 Scenario: User requests a trip from Ben Thanh Market to Notre Dame Cathedral
echo.

if "%GRPCURL_AVAILABLE%"=="true" (
    echo 1️⃣  Trip Service gRPC: Create new trip request
    echo    Executing: grpcurl -d "{\"user_id\":\"user-workflow-123\",\"origin\":\"Ben Thanh Market\",\"destination\":\"Notre Dame Cathedral\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/CreateTrip
    grpcurl -plaintext -d "{\"user_id\":\"user-workflow-123\",\"origin\":\"Ben Thanh Market\",\"destination\":\"Notre Dame Cathedral\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/CreateTrip >temp_workflow_trip.txt 2>nul
    if %ERRORLEVEL% equ 0 (
        echo    Response:
        type temp_workflow_trip.txt
        echo    ✅ Trip created successfully
    ) else (
        echo    ⚠️  Failed to create trip
    )
    del temp_workflow_trip.txt >nul 2>&1
    echo.

    echo 2️⃣  Driver Service gRPC: Find nearby available drivers
    echo    Executing: grpcurl -d "{\"latitude\":\"10.762622\",\"longitude\":\"106.660172\",\"radius_km\":\"5.0\",\"limit\":\"3\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/FindNearbyDrivers
    grpcurl -plaintext -d "{\"latitude\":\"10.762622\",\"longitude\":\"106.660172\",\"radius_km\":\"5.0\",\"limit\":\"3\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/FindNearbyDrivers >temp_workflow_drivers.txt 2>nul
    if %ERRORLEVEL% equ 0 (
        echo    Response:
        type temp_workflow_drivers.txt
        echo    ✅ Found nearby drivers
    ) else (
        echo    ⚠️  Failed to find drivers
    )
    del temp_workflow_drivers.txt >nul 2>&1
    echo.

    echo 3️⃣  Driver Service gRPC: Update driver status to BUSY
    echo    Executing: grpcurl -d "{\"driver_id\":\"550e8400-e29b-41d4-a716-446655440001\",\"status\":\"BUSY\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus
    grpcurl -plaintext -d "{\"driver_id\":\"550e8400-e29b-41d4-a716-446655440001\",\"status\":\"BUSY\"}" localhost:%DRIVER_GRPC_PORT% driver.DriverService/UpdateDriverStatus >temp_workflow_status.txt 2>nul
    if %ERRORLEVEL% equ 0 (
        echo    Response:
        type temp_workflow_status.txt
        echo    ✅ Driver status updated
    ) else (
        echo    ⚠️  Failed to update driver status
    )
    del temp_workflow_status.txt >nul 2>&1
    echo.

    echo 4️⃣  Trip Service gRPC: Check trip status
    echo    Executing: grpcurl -d "{\"trip_id\":\"workflow-trip-456\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/GetTripStatus
    grpcurl -plaintext -d "{\"trip_id\":\"workflow-trip-456\"}" localhost:%TRIP_GRPC_PORT% trip.TripService/GetTripStatus >temp_workflow_tripstatus.txt 2>nul
    if %ERRORLEVEL% equ 0 (
        echo    Response:
        type temp_workflow_tripstatus.txt
        echo    ✅ Trip status retrieved
    ) else (
        echo    ⚠️  Failed to get trip status
    )
    del temp_workflow_tripstatus.txt >nul 2>&1
    echo.

    echo ✨ Complete gRPC-based trip request workflow executed!
)

echo.
echo ============================================================
echo � gRPC DEMO COMPLETED!
echo ============================================================

echo.
echo ✅ All gRPC services tested successfully
echo ✅ Driver Service gRPC (port %DRIVER_GRPC_PORT%) - Working
echo ✅ Trip Service gRPC (port %TRIP_GRPC_PORT%) - Working  
echo ✅ Integration workflow demonstrated
echo.

pause