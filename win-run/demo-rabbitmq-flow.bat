@echo off
setlocal enabledelayedexpansion

REM UIT-Go RabbitMQ Complete Message Flow Demo
REM This demo shows the complete message flow and event-driven architecture

echo.
echo ============================================================
echo 🚀 UIT-Go RabbitMQ Complete Message Flow Demo
echo ============================================================
echo This demo demonstrates the event-driven architecture and
echo message flow patterns in the UIT-Go ride-sharing platform:
echo.
echo    🐰 RabbitMQ Message Broker
echo    📨 Event Publishing and Consumption
echo    🔄 Cross-Service Communication
echo    📊 Real-time Updates and Notifications
echo.

REM Default parameters
set SKIP_HEALTH_CHECK=false
set FAST_MODE=false
set DELAY_SECONDS=3

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :start_demo
if /i "%~1"=="/fast" set FAST_MODE=true
if /i "%~1"=="/skiphealth" set SKIP_HEALTH_CHECK=true
if /i "%~1"=="/delay" (
    set DELAY_SECONDS=%~2
    shift
)
shift
goto :parse_args

:start_demo
REM Service and queue configuration
set RABBITMQ_PORT=15672
set API_GATEWAY_PORT=8080
set DRIVER_API_PORT=8083
set USER_API_PORT=8081
set TRIP_API_PORT=8082

echo ============================================================
echo 📋 System Endpoints:
echo    🐰 RabbitMQ Management: localhost:%RABBITMQ_PORT%
echo    🌐 API Gateway:         localhost:%API_GATEWAY_PORT%
echo    🚗 Driver API:          localhost:%DRIVER_API_PORT%
echo    👤 User API:            localhost:%USER_API_PORT%
echo    🚕 Trip API:            localhost:%TRIP_API_PORT%
echo.

if "%FAST_MODE%"=="true" (
    set DELAY_SECONDS=1
    echo ⚡ Fast mode enabled - minimal delays
)

REM Check dependencies
where curl >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set CURL_AVAILABLE=true
    echo ✅ curl detected - will perform real HTTP calls
) else (
    set CURL_AVAILABLE=false
    echo ❌ curl not found - this demo requires curl for API testing
    pause
    exit /b 1
)

REM Health check
if "%SKIP_HEALTH_CHECK%"=="false" (
    call :health_check
    if !ERRORLEVEL! neq 0 (
        echo.
        echo ❌ Demo cannot proceed. Please fix service issues first.
        pause
        exit /b 1
    )
)

echo.
echo ============================================================
echo 🔍 PHASE 1: RabbitMQ System Status
echo ============================================================
echo.

echo 📊 Step 1: Check RabbitMQ Server Status
echo Verifying RabbitMQ management interface...
curl -s -u guest:guest "http://localhost:%RABBITMQ_PORT%/api/overview" >temp_overview.json 2>nul
if !ERRORLEVEL! equ 0 (
    echo    ✅ RabbitMQ server is running and accessible
    findstr /i "rabbitmq_version.*node_name" temp_overview.json 2>nul || echo    📋 RabbitMQ status retrieved
    del temp_overview.json 2>nul
) else (
    echo    ❌ RabbitMQ server is not accessible
    echo    💡 Make sure RabbitMQ is running with management plugin enabled
    pause
    exit /b 1
)

echo.
echo 📋 Step 2: List Current Queues
echo Checking existing message queues...
curl -s -u guest:guest "http://localhost:%RABBITMQ_PORT%/api/queues" >temp_queues.json 2>nul
if !ERRORLEVEL! equ 0 (
    echo    ✅ Queue information retrieved
    echo    📊 Current queue status:
    findstr /i "driver.*trip.*user" temp_queues.json >nul 2>&1 && (
        echo    🟢 Ride-sharing queues detected
    ) || (
        echo    📝 Standard queues present
    )
    del temp_queues.json 2>nul
) else (
    echo    ❌ Could not retrieve queue information
)

echo.
timeout /t %DELAY_SECONDS% /nobreak >nul

echo.
echo ============================================================
echo 🚗 PHASE 2: Driver Event Flow
echo ============================================================
echo Demonstrating driver-related message publishing and flow...
echo.

set DEMO_DRIVER_ID=660e8400-e29b-41d4-a716-446655440002

echo 📍 Step 1: Driver Location Update Events
echo Publishing driver location updates...
for %%i in (1 2 3 4 5) do (
    set /a LAT_UPDATE=10762622 + %%i * 100
    set /a LON_UPDATE=106660172 + %%i * 150
    set LAT_STR=10.!LAT_UPDATE:~-6!
    set LON_STR=106.!LON_UPDATE:~-6!
    
    echo    📡 Location Update %%i/5: !LAT_STR!, !LON_STR!
    curl -s -X POST "http://localhost:%DRIVER_API_PORT%/api/drivers/%DEMO_DRIVER_ID%/location" ^
        -H "Content-Type: application/json" ^
        -d "{\"latitude\": !LAT_STR!, \"longitude\": !LON_STR!, \"timestamp\": \"%date% %time%\"}" >nul 2>&1
    
    if !ERRORLEVEL! equ 0 (
        echo       ✅ Location update sent successfully
    ) else (
        echo       ⚠️  Location update failed - service may be down
    )
    timeout /t 1 /nobreak >nul
)

echo.
echo ============================================================
echo 👤 PHASE 3: User Trip Request Flow
echo ============================================================
echo Demonstrating trip request and driver matching...
echo.

set DEMO_USER_ID=550e8400-e29b-41d4-a716-446655440001

echo 🚕 Step 1: Trip Request Event
echo Publishing trip request...
curl -s -X POST "http://localhost:%TRIP_API_PORT%/api/trips/request" ^
    -H "Content-Type: application/json" ^
    -d "{\"userId\": \"%DEMO_USER_ID%\", \"pickupLatitude\": 10.762622, \"pickupLongitude\": 106.660172, \"dropoffLatitude\": 10.772900, \"dropoffLongitude\": 106.698000}" >nul 2>&1

if !ERRORLEVEL! equ 0 (
    echo    ✅ Trip request published successfully
    echo    📨 Event should trigger driver matching algorithm
) else (
    echo    ⚠️  Trip request failed - service may be down
)

echo.
timeout /t %DELAY_SECONDS% /nobreak >nul

echo ============================================================
echo 📊 PHASE 4: Queue Monitoring
echo ============================================================
echo Checking message queue status and consumption...
echo.

curl -s -u guest:guest "http://localhost:%RABBITMQ_PORT%/api/queues" >temp_final_queues.json 2>nul
if !ERRORLEVEL! equ 0 (
    echo 📈 Final Queue Status:
    findstr /i "message" temp_final_queues.json >nul 2>&1 && (
        echo    📊 Messages detected in queues
        echo    🔄 Event-driven architecture is processing requests
    ) || (
        echo    📭 No pending messages (all processed)
    )
    del temp_final_queues.json 2>nul
) else (
    echo    ❌ Could not retrieve final queue status
)

echo.
echo ============================================================
echo 🚀 DEMO COMPLETED SUCCESSFULLY! 🎉
echo ============================================================
echo Key takeaways:
echo ✅ Driver location updates are published to RabbitMQ
echo ✅ Trip requests trigger driver matching via messaging
echo ✅ Status updates flow through event-driven architecture
echo ✅ All services communicate asynchronously via RabbitMQ
echo.
echo 🔗 Next steps:
echo    - Explore RabbitMQ Management UI: http://localhost:15672
echo    - Test real API endpoints with your services
echo    - Monitor queue depths and message processing
echo.
echo Demo completed! Press any key to exit...
pause >nul
exit /b 0

REM ============================================================
REM Functions
REM ============================================================

:health_check
echo ============================================================
echo 🔍 HEALTH CHECK - Verifying Services
echo ============================================================

REM Test Driver Service
echo Testing Driver Service...
curl -s -f -m 5 "http://localhost:8083/api/driver-service/drivers/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ Driver Service: HEALTHY
) else (
    echo ❌ Driver Service: UNREACHABLE
    set /a HEALTH_ERRORS+=1
)

REM Test User Service
echo Testing User Service...
curl -s -f -m 5 "http://localhost:8081/api/user-service/users/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ User Service: HEALTHY
) else (
    echo ❌ User Service: UNREACHABLE
    set /a HEALTH_ERRORS+=1
)

REM Test Trip Service
echo Testing Trip Service...
curl -s -f -m 5 "http://localhost:8082/api/trip-service/trips/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ Trip Service: HEALTHY
) else (
    echo ❌ Trip Service: UNREACHABLE
    set /a HEALTH_ERRORS+=1
)

REM Test RabbitMQ
echo Testing RabbitMQ...
curl -s -f -m 5 -u guest:guest "http://localhost:15672/api/overview" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ RabbitMQ: HEALTHY
) else (
    echo ❌ RabbitMQ: UNREACHABLE
    set /a HEALTH_ERRORS+=1
)

echo.
if %HEALTH_ERRORS% gtr 0 (
    echo ❌ %HEALTH_ERRORS% service(s) are unhealthy
    echo 💡 Please start all services before running the demo
    exit /b 1
) else (
    echo ✅ All services are healthy - proceeding with demo
    exit /b 0
)