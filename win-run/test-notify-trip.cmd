@echo off
setlocal EnableDelayedExpansion

:: ==========================================================
::  Auto Trip Creation & Driver Notification Test
::  (Native Windows Version - No jq required)
:: ==========================================================

:: Configuration
set USER_SERVICE_PORT=8080
set TRIP_SERVICE_PORT=8080
set DRIVER_SERVICE_PORT=8080

:: Temp file for JSON processing
set "JSON_FILE=%TEMP%\response_%RANDOM%.json"

:: Default Credentials
if "%PASSENGER_EMAIL%"=="" set PASSENGER_EMAIL=user1@gmail.com
if "%PASSENGER_PASSWORD%"=="" set PASSENGER_PASSWORD=123456

:: Default Coordinates (District 1, HCMC)
if "%PICKUP_LAT%"=="" set PICKUP_LAT=10.762622
if "%PICKUP_LNG%"=="" set PICKUP_LNG=106.660172
if "%DEST_LAT%"=="" set DEST_LAT=10.777229
if "%DEST_LNG%"=="" set DEST_LNG=106.695534
if "%FARE%"=="" set FARE=50000

echo ==========================================================
echo  Auto Trip Creation ^& Driver Notification Test
echo ==========================================================
echo.

:: Step 0: Setup - Bring all drivers online and start simulation
echo [Step 0] Setting up drivers...
echo.

echo Bringing all drivers online...
curl -s -X POST http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/online-all
echo.

echo Starting driver location simulation...
echo Simulating movement from (10.762622, 106.660172) to (10.776889, 106.700806)
curl -s -X POST "http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000"
echo.

echo Waiting 10 seconds for drivers to start simulating...
for /L %%i in (10,-1,1) do (
    <nul set /p "=...%%i "
    timeout /t 1 >nul
)
echo.
echo [OK] Ready!
echo.
echo [OK] Drivers setup complete!
echo.
echo ==========================================================

:: Step 1: Login as passenger
echo [Step 1] Logging in as passenger...
echo Email: %PASSENGER_EMAIL%
echo.

set "LOGIN_BODY={\"email\":\"%PASSENGER_EMAIL%\",\"password\":\"%PASSENGER_PASSWORD%\"}"

:: Save output to file to handle JSON safely in PowerShell
curl -s -X POST http://localhost:%USER_SERVICE_PORT%/api/users/login -H "Content-Type: application/json" -d "%LOGIN_BODY%" -o "%JSON_FILE%"

:: Parse Access Token using PowerShell
for /f "delims=" %%i in ('powershell -NoProfile -Command "try { (Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json).accessToken } catch { Write-Output '' }"') do set TOKEN=%%i

if "%TOKEN%"=="" (
    echo [ERROR] Login failed!
    type "%JSON_FILE%"
    del "%JSON_FILE%"
    exit /b 1
)

echo [OK] Login successful!
echo Token (first 50 chars): %TOKEN:~0,50%...
echo.
echo ==========================================================

:: Step 2: Find nearby drivers before creating trip
echo [Step 2] Finding nearby drivers at pickup location...
echo Pickup Location: (%PICKUP_LAT%, %PICKUP_LNG%)
echo.

curl -s "http://localhost:8083/api/internal/drivers/nearby?lat=%PICKUP_LAT%&lng=%PICKUP_LNG%&radiusKm=3.0&limit=10" -o "%JSON_FILE%"

:: Get Driver Count
for /f %%c in ('powershell -NoProfile -Command "try { @(Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json).Count } catch { 0 }"') do set DRIVER_COUNT=%%c

if %DRIVER_COUNT% GTR 0 (
    echo [OK] Found !DRIVER_COUNT! nearby driver(s^)
    echo.
    :: Print details using PowerShell formatting
    powershell -NoProfile -Command "(Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json) | ForEach-Object { Write-Host ('  - Driver ID: ' + $_.driverId + ' | Dist: ' + $_.distanceInMeters + 'm') }"
    
    :: Extract first driver ID
    for /f "delims=" %%d in ('powershell -NoProfile -Command "(Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json)[0].driverId"') do set FIRST_DRIVER_ID=%%d
    echo.
    echo [TEST TARGET] First Driver ID: !FIRST_DRIVER_ID!
) else (
    echo [WARN] No nearby drivers found or API error
    type "%JSON_FILE%"
)

echo.
echo ==========================================================

:: Step 3: Create trip
echo [Step 3] Creating trip...
echo Pickup: (%PICKUP_LAT%, %PICKUP_LNG%)
echo Destination: (%DEST_LAT%, %DEST_LNG%)
echo Estimated Fare: %FARE% VND
echo.

set "TRIP_BODY={\"pickupLatitude\":%PICKUP_LAT%,\"pickupLongitude\":%PICKUP_LNG%,\"destinationLatitude\":%DEST_LAT%,\"destinationLongitude\":%DEST_LNG%,\"estimatedFare\":%FARE%}"

curl -s -X POST http://localhost:%TRIP_SERVICE_PORT%/api/trips/create -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "%TRIP_BODY%" -o "%JSON_FILE%"

:: Parse Trip ID and Status
for /f "delims=" %%i in ('powershell -NoProfile -Command "try { (Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json).id } catch { Write-Output '' }"') do set TRIP_ID=%%i
for /f "delims=" %%i in ('powershell -NoProfile -Command "try { (Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json).status } catch { Write-Output '' }"') do set TRIP_STATUS=%%i

if "%TRIP_ID%"=="" (
    echo [ERROR] Trip creation failed!
    type "%JSON_FILE%"
    del "%JSON_FILE%"
    exit /b 1
)

echo [OK] Trip created successfully!
echo Trip ID: %TRIP_ID%
echo Status: %TRIP_STATUS%
echo.
echo ==========================================================

:: Step 4: Wait a moment for message to propagate through RabbitMQ
echo [Step 4] Waiting for RabbitMQ to process notification...
timeout /t 2 /nobreak >nul
echo [OK] Ready
echo.
echo ==========================================================

:: Step 5: Check trip-service logs to see which driver (nearest) was notified
echo [Step 5] Checking trip-service logs for notified driver...
echo.

set "PS_CMD=docker logs trip-service 2>&1 | Select-String 'nearest driver: ([a-f0-9-]+)' | Select-Object -ExpandProperty Matches | ForEach-Object {$_.Groups[1].Value} | Select-Object -Last 1"
for /f "delims=" %%i in ('powershell -noprofile -command "%PS_CMD%"') do set NOTIFIED_DRIVER_ID=%%i

if "%NOTIFIED_DRIVER_ID%"=="" (
    set "PS_CMD_2=docker logs trip-service 2>&1 | Select-String '\[([a-f0-9-]+)\]' | Select-Object -ExpandProperty Matches | ForEach-Object {$_.Groups[1].Value} | Select-Object -Last 1"
    for /f "delims=" %%j in ('powershell -noprofile -command "%PS_CMD_2%"') do set NOTIFIED_DRIVER_ID=%%j
)

if not "%NOTIFIED_DRIVER_ID%"=="" (
    echo [OK] Nearest driver notified: %NOTIFIED_DRIVER_ID%
) else (
    echo [WARN] Could not find specific log entries (logs may have rotated or format changed)
)

echo.
echo ==========================================================

:: Step 6: Verify that ONLY the nearest driver received the notification
echo [Step 6] Verifying nearest driver received notification...
echo.

if not "%FIRST_DRIVER_ID%"=="" (
    echo Expected nearest driver: %FIRST_DRIVER_ID%
    
    if not "%NOTIFIED_DRIVER_ID%"=="" (
        echo Actually notified driver: %NOTIFIED_DRIVER_ID%
        echo.
        
        if "%FIRST_DRIVER_ID%"=="%NOTIFIED_DRIVER_ID%" (
            echo [SUCCESS] CORRECT: Nearest driver was notified!
        ) else (
            echo [FAILURE] MISMATCH: Expected %FIRST_DRIVER_ID% but %NOTIFIED_DRIVER_ID% was notified
        )
    ) else (
        echo [WARN] Could not determine which driver was notified from logs
    )
    echo.
    
    :: Check pending notification
    echo Checking pending trips for nearest driver: %FIRST_DRIVER_ID%
    curl -s "http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/trips/pending?driverId=%FIRST_DRIVER_ID%" -o "%JSON_FILE%"
    
    for /f %%c in ('powershell -NoProfile -Command "try { @(Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json).Count } catch { 0 }"') do set PENDING_COUNT=%%c
    
    if !PENDING_COUNT! GTR 0 (
        echo [OK] Nearest driver has !PENDING_COUNT! pending trip^(s^)
        powershell -NoProfile -Command "(Get-Content '%JSON_FILE%' -Raw | ConvertFrom-Json) | ForEach-Object { Write-Host ('    Trip ID: ' + $_.tripId + ' | Fare: ' + $_.estimatedFare) }"
    ) else (
        echo [WARN] No pending trips ^(may have expired after 15 seconds^)
    )
) else (
    echo [WARN] No nearby drivers to check
)

echo ==========================================================

:: Step 7: Check Redis for pending notifications
echo [Step 7] Checking Redis for pending notifications...
echo.

set "REDIS_PATTERN=pending_trips:*:%TRIP_ID%"
for /f "delims=" %%k in ('docker exec redis redis-cli KEYS "%REDIS_PATTERN%" 2^>nul') do (
    set REDIS_KEY=%%k
    echo [OK] Found pending notification in Redis: !REDIS_KEY!
)

if "%REDIS_KEY%"=="" (
    echo [WARN] No pending notifications found in Redis (may have expired after 15 seconds)
)

:: Cleanup
if exist "%JSON_FILE%" del "%JSON_FILE%"

echo.
echo ==========================================================
echo SUMMARY
echo ==========================================================
echo Trip ID: %TRIP_ID%
echo Trip Status: %TRIP_STATUS%
if not "%FIRST_DRIVER_ID%"=="" echo Nearest Driver ID: %FIRST_DRIVER_ID%
if not "%NOTIFIED_DRIVER_ID%"=="" echo Notified Driver ID: %NOTIFIED_DRIVER_ID%

if not "%NOTIFIED_DRIVER_ID%"=="" (
    if "%FIRST_DRIVER_ID%"=="%NOTIFIED_DRIVER_ID%" (
        echo [VERIFICATION] SUCCESS
    ) else (
        echo [VERIFICATION] FAILED
    )
)
echo.

if not "%NOTIFIED_DRIVER_ID%"=="" (
    echo To test driver acceptance (within 15 seconds):
    echo curl -X POST "http://localhost:%DRIVER_SERVICE_PORT%/api/drivers/trips/%TRIP_ID%/accept?driverId=%NOTIFIED_DRIVER_ID%"
)

echo.
echo ==========================================================
echo [DONE] Test completed!
echo ==========================================================
pause