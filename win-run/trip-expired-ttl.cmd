@echo off
setlocal enabledelayedexpansion

REM Auto Test Script: Login, Create Trip, Get Driver Notifications
REM This script automates the complete flow of creating a trip and finding which drivers are notified

REM Configuration
set USER_SERVICE_PORT=8080
set TRIP_SERVICE_PORT=8080
set DRIVER_SERVICE_PORT=8080

REM Default test credentials
if not defined PASSENGER_EMAIL set PASSENGER_EMAIL=user1@gmail.com
if not defined PASSENGER_PASSWORD set PASSENGER_PASSWORD=123456

REM Default trip coordinates (District 1, Ho Chi Minh City)
if not defined PICKUP_LAT set PICKUP_LAT=10.762622
if not defined PICKUP_LNG set PICKUP_LNG=106.660172
if not defined DEST_LAT set DEST_LAT=10.777229
if not defined DEST_LNG set DEST_LNG=106.695534
if not defined FARE set FARE=50000

echo ==========================================================
echo   Auto Trip Creation ^& Driver Notification Test
echo ==========================================================
echo.

REM Step 0: Setup - Bring all drivers online and start simulation
echo Step 0: Setting up drivers...
echo.

echo Bringing all drivers online...
curl -s -X POST http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/online-all
echo.

echo Starting driver location simulation...
echo Simulating movement from (10.762622, 106.660172) to (10.776889, 106.700806)
curl -s -X POST "http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000"
echo.

echo Waiting 10 seconds for drivers to start simulating...
for /l %%i in (10,-1,1) do (
    echo   ⏳ %%i seconds remaining...
    timeout /t 1 /nobreak >nul
)
echo   ✅ Ready!
echo.

echo ✅ Drivers setup complete!
echo.

echo ==========================================================

REM Step 1: Login as passenger
echo Step 1: Logging in as passenger...
echo Email: %PASSENGER_EMAIL%
echo.

curl -s -X POST http://localhost:%USER_SERVICE_PORT%/api/users/login -H "Content-Type: application/json" -d "{\"email\":\"%PASSENGER_EMAIL%\",\"password\":\"%PASSENGER_PASSWORD%\"}" > login_response.tmp

REM Extract access token using PowerShell
for /f "delims=" %%a in ('powershell -Command "(Get-Content login_response.tmp | ConvertFrom-Json).accessToken"') do set TOKEN=%%a

if defined TOKEN (
    echo ✅ Login successful!
    echo Access Token: %TOKEN:~0,50%...
) else (
    echo ❌ Login failed!
    type login_response.tmp
    del login_response.tmp
    exit /b 1
)

echo.
echo ==========================================================

REM Step 2: Find nearby drivers before creating trip
echo Step 2: Finding nearby drivers at pickup location...
echo Pickup Location: (%PICKUP_LAT%, %PICKUP_LNG%)
echo.

curl -s "http://localhost:8083/api/internal/drivers/nearby?lat=%PICKUP_LAT%&lng=%PICKUP_LNG%&radiusKm=3.0&limit=10" > nearby_drivers.tmp

REM Get driver count using PowerShell
for /f %%a in ('powershell -Command "(Get-Content nearby_drivers.tmp | ConvertFrom-Json).Count"') do set DRIVER_COUNT=%%a

if defined DRIVER_COUNT (
    if %DRIVER_COUNT% GTR 0 (
        echo ✅ Found %DRIVER_COUNT% nearby driver^(s^)
        echo.
        powershell -Command "Get-Content nearby_drivers.tmp | ConvertFrom-Json | ForEach-Object { Write-Host \"  • Driver ID: $($_.driverId)\"; Write-Host \"    Distance: $($_.distanceInMeters)m | Location: ($($_.latitude), $($_.longitude))\" }"
        
        REM Extract first driver ID
        for /f "delims=" %%a in ('powershell -Command "(Get-Content nearby_drivers.tmp | ConvertFrom-Json)[0].driverId"') do set FIRST_DRIVER_ID=%%a
        echo.
        echo Test Driver ID: !FIRST_DRIVER_ID!
    ) else (
        echo ⚠️  No nearby drivers found
    )
) else (
    echo ⚠️  No nearby drivers found or API error
    type nearby_drivers.tmp
)

echo.
echo ==========================================================

REM Step 3: Create trip
echo Step 3: Creating trip...
echo Pickup: (%PICKUP_LAT%, %PICKUP_LNG%)
echo Destination: (%DEST_LAT%, %DEST_LNG%)
echo Estimated Fare: %FARE% VND
echo.

curl -s -X POST http://localhost:%TRIP_SERVICE_PORT%/api/trips/create -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"pickupLatitude\":%PICKUP_LAT%,\"pickupLongitude\":%PICKUP_LNG%,\"destinationLatitude\":%DEST_LAT%,\"destinationLongitude\":%DEST_LNG%,\"estimatedFare\":%FARE%}" > trip_response.tmp

REM Extract trip ID using PowerShell
for /f "delims=" %%a in ('powershell -Command "(Get-Content trip_response.tmp | ConvertFrom-Json).id"') do set TRIP_ID=%%a
for /f "delims=" %%a in ('powershell -Command "(Get-Content trip_response.tmp | ConvertFrom-Json).status"') do set TRIP_STATUS=%%a

if defined TRIP_ID (
    echo ✅ Trip created successfully!
    echo Trip ID: %TRIP_ID%
    echo Status: %TRIP_STATUS%
) else (
    echo ❌ Trip creation failed!
    type trip_response.tmp
    del trip_response.tmp
    exit /b 1
)

echo.
echo ==========================================================

REM Step 4: Wait a moment for message to propagate through RabbitMQ
echo Step 4: Waiting for RabbitMQ to process notification...
timeout /t 1 /nobreak >nul
echo ✅ Ready

echo.
echo ==========================================================

REM Step 5: Check trip-service logs to see which driver was notified
echo Step 5: Checking trip-service logs for notified driver...
echo.

docker logs trip-service 2>&1 | findstr "Trip %TRIP_ID%" > trip_logs.tmp

if exist trip_logs.tmp (
    for /f "tokens=*" %%a in (trip_logs.tmp) do echo %%a
    echo.
    
    REM Extract notified driver ID
    for /f "tokens=*" %%a in ('type trip_logs.tmp ^| findstr "nearest driver:"') do (
        set "line=%%a"
        for /f "tokens=4 delims= " %%b in ("!line!") do set NOTIFIED_DRIVER_ID=%%b
    )
    
    if defined NOTIFIED_DRIVER_ID (
        echo ✅ Nearest driver notified: !NOTIFIED_DRIVER_ID!
    )
) else (
    echo ⚠️  Could not find specific log entries ^(logs may have rotated^)
)

echo.
echo ==========================================================

REM Step 6: Verify that ONLY the nearest driver received the notification
echo Step 6: Verifying nearest driver received notification...
echo.

if defined FIRST_DRIVER_ID (
    echo Expected nearest driver: !FIRST_DRIVER_ID!
    if defined NOTIFIED_DRIVER_ID (
        echo Actually notified driver: !NOTIFIED_DRIVER_ID!
        echo.
        
        if "!FIRST_DRIVER_ID!"=="!NOTIFIED_DRIVER_ID!" (
            echo ✅ CORRECT: Nearest driver was notified!
        ) else (
            echo ❌ MISMATCH: Expected !FIRST_DRIVER_ID! but !NOTIFIED_DRIVER_ID! was notified
        )
    ) else (
        echo ⚠️  Could not determine which driver was notified from logs
    )
    echo.
    
    REM Check pending notification for the nearest driver
    echo Checking pending trips for nearest driver: !FIRST_DRIVER_ID!
    curl -s "http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/trips/pending?driverId=!FIRST_DRIVER_ID!" > pending_trips.tmp
    
    for /f %%a in ('powershell -Command "(Get-Content pending_trips.tmp | ConvertFrom-Json).Count"') do set PENDING_COUNT=%%a
    
    if defined PENDING_COUNT (
        if !PENDING_COUNT! GTR 0 (
            echo   ✅ Nearest driver has !PENDING_COUNT! pending trip^(s^)
            powershell -Command "Get-Content pending_trips.tmp | ConvertFrom-Json | ForEach-Object { Write-Host \"    Trip ID: $($_.tripId)\"; Write-Host \"    Fare: $($_.estimatedFare) VND\"; Write-Host \"    Distance: $($_.distanceKm) km\"; Write-Host \"    Expires at: $($_.expiresAt)\" }"
        ) else (
            echo   ⚠️  No pending trips ^(may have expired after 15 seconds^)
        )
    ) else (
        echo   ❌ Error checking pending trips
    )
    echo.
) else (
    echo ⚠️  No nearby drivers to check
)

echo ==========================================================

REM Step 7: Check Redis for pending notifications
echo Step 7: Checking Redis for pending notifications...
echo.

docker exec redis redis-cli KEYS "pending_trips:*:%TRIP_ID%" > redis_keys.tmp 2>nul

if exist redis_keys.tmp (
    for /f "tokens=*" %%a in (redis_keys.tmp) do (
        if not "%%a"=="" (
            echo ✅ Found pending notifications in Redis:
            echo   • Key: %%a
            docker exec redis redis-cli TTL "%%a"
        )
    )
) else (
    echo ⚠️  No pending notifications found in Redis ^(may have expired after 15 seconds^)
)

echo.
echo ==========================================================

REM Summary
echo SUMMARY
echo ==========================================================
echo Trip ID: %TRIP_ID%
echo Trip Status: %TRIP_STATUS%
if defined DRIVER_COUNT echo Nearby Drivers Found: %DRIVER_COUNT%
if defined FIRST_DRIVER_ID echo Nearest Driver ID: !FIRST_DRIVER_ID!
if defined NOTIFIED_DRIVER_ID (
    echo Notified Driver ID: !NOTIFIED_DRIVER_ID!
    if "!FIRST_DRIVER_ID!"=="!NOTIFIED_DRIVER_ID!" (
        echo ✅ Verification: Nearest driver was correctly notified
    ) else (
        echo ❌ Verification: Driver mismatch detected
    )
)
echo.

if defined NOTIFIED_DRIVER_ID (
    echo To test driver acceptance ^(within 15 seconds of trip creation^):
    echo.
    echo curl -X POST "http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/trips/%TRIP_ID%/accept?driverId=!NOTIFIED_DRIVER_ID!" ^| jq
    echo.
    echo To check pending trips for notified driver:
    echo.
    echo curl -X GET "http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/trips/pending?driverId=!NOTIFIED_DRIVER_ID!" ^| jq
)

echo.
echo ==========================================================
echo ✅ Test completed!
echo ==========================================================
echo.

REM Export variables for use in shell
echo # You can use these variables in your shell:
echo set TRIP_ID=%TRIP_ID%
if defined NOTIFIED_DRIVER_ID echo set DRIVER_ID=!NOTIFIED_DRIVER_ID!
echo set PASSENGER_TOKEN=%TOKEN%

REM Cleanup temporary files
del login_response.tmp 2>nul
del nearby_drivers.tmp 2>nul
del trip_response.tmp 2>nul
del trip_logs.tmp 2>nul
del pending_trips.tmp 2>nul
del redis_keys.tmp 2>nul

endlocal