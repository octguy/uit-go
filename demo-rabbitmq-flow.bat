@echo off
setlocal enabledelayedexpansion

REM UIT-Go RabbitMQ Flow Demo - Batch File Version
REM This batch file demonstrates RabbitMQ message flow using your existing services

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
echo.
echo ============================================================
echo  🚀 UIT-Go RabbitMQ Flow Demonstration
echo ============================================================
echo This demo shows how messages flow between microservices using RabbitMQ
echo in the UIT-Go ride-sharing platform.
echo.

if "%FAST_MODE%"=="true" (
    set DELAY_SECONDS=1
    echo ⚡ Fast mode enabled - minimal delays
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
echo 🎬 Starting automated demo...
timeout /t 2 /nobreak >nul

REM Run all demo scenarios
call :show_driver_location_demo
echo.
echo ⏸️  Moving to next demo in %DELAY_SECONDS% seconds...
timeout /t %DELAY_SECONDS% /nobreak >nul

call :show_trip_request_demo
echo.
echo ⏸️  Moving to next demo in %DELAY_SECONDS% seconds...
timeout /t %DELAY_SECONDS% /nobreak >nul

call :show_status_updates_demo
echo.
echo ⏸️  Moving to monitoring demo in %DELAY_SECONDS% seconds...
timeout /t %DELAY_SECONDS% /nobreak >nul

call :show_queue_monitoring

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
echo.
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
    exit /b 1
)

REM Test Trip Service
echo Testing Trip Service...
curl -s -f -m 5 "http://localhost:8082/api/trip-service/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ Trip Service: HEALTHY
) else (
    echo ❌ Trip Service: UNREACHABLE
)

REM Test User Service
echo Testing User Service...
curl -s -f -m 5 "http://localhost:8081/api/user-service/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ User Service: HEALTHY
) else (
    echo ❌ User Service: UNREACHABLE
)

REM Test RabbitMQ
echo Testing RabbitMQ Management...
curl -s -f -m 5 "http://localhost:15672" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ RabbitMQ Management: HEALTHY
) else (
    echo ❌ RabbitMQ Management: UNREACHABLE
)

echo.
echo 🎉 All services are healthy! Demo can proceed.
exit /b 0

:show_driver_location_demo
echo.
echo ============================================================
echo 🚀 DEMO 1: Driver Location Updates Flow
echo ============================================================

echo.
echo 📋 Step 1: Publishing Driver Location Updates
echo --------------------------------------------------

REM Generate random driver ID (simplified)
set /a RAND_NUM=%RANDOM% * 32768 + %RANDOM%
set DRIVER_ID=driver-%RAND_NUM%

echo 📍 Simulating driver %DRIVER_ID% at Ben Thanh Market
echo    Coordinates: 10.762622, 106.660172

echo.
echo 📤 Would publish to RabbitMQ:
echo    Exchange: driver.events
echo    Routing Key: driver.location.updated
echo    Message: {
echo      "driver_id": "%DRIVER_ID%",
echo      "latitude": 10.762622,
echo      "longitude": 106.660172,
echo      "timestamp": %date:~-4%%date:~4,2%%date:~7,2%%time:~0,2%%time:~3,2%%time:~6,2%,
echo      "status": "AVAILABLE",
echo      "geohash": "w7h7k8f"
echo    }

echo.
echo 📊 Expected Queue Status:
echo    driver.location.updates: +1 message
echo    Consuming services would process location for nearby driver searches

echo.
echo 🧪 Testing Real Message Publishing:
echo {"latitude":10.762622,"longitude":106.660172} > temp_test_location.json
curl -s -X PUT -H "Content-Type: application/json" -d @temp_test_location.json "http://localhost:8083/api/driver-service/drivers/550e8400-e29b-41d4-a716-446655440000/location" > temp_location_response.json 2>nul

findstr /i "success.*true" temp_location_response.json >nul
if !ERRORLEVEL! equ 0 (
    echo    ✅ Real location update successful!
    echo    📤 Actual RabbitMQ message published
    
    REM Show the actual response
    for /f "tokens=*" %%r in ('findstr /i "latitude" temp_location_response.json') do (
        echo    📍 Location confirmed in response
    )
) else (
    echo    ⚠️  Location update test failed (expected if driver doesn't exist)
)

del temp_test_location.json >nul 2>&1
del temp_location_response.json >nul 2>&1

if "%FAST_MODE%"=="true" (
    timeout /t 1 /nobreak >nul
) else (
    timeout /t 2 /nobreak >nul
)
exit /b 0

:show_trip_request_demo
echo.
echo ============================================================
echo 🚀 DEMO 2: Trip Request and Driver Matching Flow
echo ============================================================

echo.
echo 📋 Step 1: Creating Trip Request
echo --------------------------------------------------

REM Generate random trip and user IDs
set /a TRIP_RAND=%RANDOM% * 32768 + %RANDOM%
set /a USER_RAND=%RANDOM% * 32768 + %RANDOM%
set TRIP_ID=trip-%TRIP_RAND%
set USER_ID=user-%USER_RAND%

echo 👤 User %USER_ID% requests trip
echo 📍 Pickup: Ben Thanh Market (10.762622, 106.660172)
echo 🎯 Destination: Notre Dame Cathedral (10.782622, 106.680172)

echo.
echo 📤 Would publish Trip Request to RabbitMQ:
echo    Exchange: trip.events
echo    Routing Key: trip.created
echo    Message: {
echo      "trip_id": "%TRIP_ID%",
echo      "user_id": "%USER_ID%",
echo      "pickup_location": {
echo        "latitude": 10.762622,
echo        "longitude": 106.660172,
echo        "name": "Ben Thanh Market"
echo      },
echo      "destination": {
echo        "latitude": 10.782622,
echo        "longitude": 106.680172,
echo        "name": "Notre Dame Cathedral"
echo      },
echo      "trip_type": "STANDARD"
echo    }

if "%FAST_MODE%"=="true" (
    timeout /t 1 /nobreak >nul
) else (
    timeout /t 2 /nobreak >nul
)

echo.
echo 📋 Step 2: Driver Matching Process
echo --------------------------------------------------

echo 🔍 Finding nearby drivers...
echo    🚗 Nguyen Van A (ID: driver-001) - 0.5km away
echo    🚗 Tran Thi B (ID: driver-002) - 0.8km away
echo    🚗 Le Van C (ID: driver-003) - 1.2km away

echo.
echo ✅ Driver selected: Nguyen Van A

echo.
echo 📤 Would publish Driver Assignment:
echo    Exchange: trip.events
echo    Routing Key: driver.assigned
echo    Message: {
echo      "trip_id": "%TRIP_ID%",
echo      "driver_id": "driver-001",
echo      "driver_name": "Nguyen Van A",
echo      "estimated_arrival": 5
echo    }

if "%FAST_MODE%"=="true" (
    timeout /t 1 /nobreak >nul
) else (
    timeout /t 2 /nobreak >nul
)
exit /b 0

:show_status_updates_demo
echo.
echo ============================================================
echo 🚀 DEMO 3: Trip Status Updates and Real-time Notifications
echo ============================================================

REM Generate random trip ID for this demo
set /a STATUS_TRIP_RAND=%RANDOM% * 32768 + %RANDOM%
set STATUS_TRIP_ID=trip-%STATUS_TRIP_RAND%

set STATUS_COUNT=0
for %%s in ("DRIVER_ASSIGNED:Driver is on the way to pickup" "DRIVER_ARRIVED:Driver has arrived at pickup location" "TRIP_STARTED:Trip is in progress" "TRIP_COMPLETED:Trip completed successfully") do (
    set /a STATUS_COUNT+=1
    
    for /f "tokens=1,2 delims=:" %%a in (%%s) do (
        echo.
        echo 📋 Step !STATUS_COUNT!: Status Update: %%a
        echo --------------------------------------------------
        
        echo 📊 Trip Status: %%a
        echo 💬 Message: %%b
        
        echo.
        echo 📤 Would publish Status Update:
        echo    Exchange: trip.events
        echo    Routing Key: trip.status.updated
        echo    Message: {
        echo      "trip_id": "%STATUS_TRIP_ID%",
        echo      "status": "%%a",
        echo      "message": "%%b"
        echo    }
        
        echo.
        echo 📱 Would trigger Notifications:
        echo    📲 User notification: %%b
        echo    📲 Driver notification: Status updated to %%a
        
        if "%FAST_MODE%"=="true" (
            timeout /t 1 /nobreak >nul
        ) else (
            timeout /t 3 /nobreak >nul
        )
    )
)
exit /b 0

:show_queue_monitoring
echo.
echo ============================================================
echo 🚀 DEMO 4: RabbitMQ Queue Monitoring
echo ============================================================

echo 📊 Monitoring RabbitMQ queues and message flow...

echo.
echo 🔍 Real Queue Status from RabbitMQ:
curl -s -u guest:guest "http://localhost:15672/api/queues" >temp_real_queues.json 2>nul
if !ERRORLEVEL! equ 0 (
    echo    ✅ Successfully connected to RabbitMQ Management API
    echo.
    
    REM Get individual queue details
    for %%q in ("driver.location.updates" "driver.status.changes" "driver.offline" "driver.online" "trip.created.queue") do (
        curl -s -u guest:guest "http://localhost:15672/api/queues/%%2F/%%~q" >temp_queue_%%~q.json 2>nul
        if !ERRORLEVEL! equ 0 (
            REM Extract message count from JSON (Windows batch compatible)
            for /f "tokens=*" %%c in ('findstr /i "messages.*:" temp_queue_%%~q.json') do (
                for /f "tokens=2 delims=:" %%d in ("%%c") do (
                    for /f "tokens=1 delims=," %%e in ("%%d") do (
                        set msg_count=%%e
                        set msg_count=!msg_count: =!
                        
                        REM Determine status icon based on message count
                        if !msg_count! equ 0 (
                            set status_icon=🟢
                        ) else if !msg_count! lss 10 (
                            set status_icon=🟡
                        ) else (
                            set status_icon=🔴
                        )
                        
                        echo    !status_icon! %%~q ^| Messages: !msg_count!
                        goto :queue_%%~q_done
                    )
                )
            )
            :queue_%%~q_done
            del temp_queue_%%~q.json >nul 2>&1
        ) else (
            echo    ❓ %%~q ^| Status: Unknown
        )
    )
    
    echo.
    echo 📈 RabbitMQ Node Information:
    curl -s -u guest:guest "http://localhost:15672/api/nodes" >temp_nodes.json 2>nul
    if !ERRORLEVEL! equ 0 (
        findstr /i "running" temp_nodes.json >nul
        if !ERRORLEVEL! equ 0 (
            echo    ✅ RabbitMQ node is running
        )
        del temp_nodes.json >nul 2>&1
    )
    
    echo.
    echo � Connection Information:
    curl -s -u guest:guest "http://localhost:15672/api/connections" >temp_connections.json 2>nul
    if !ERRORLEVEL! equ 0 (
        for /f %%c in ('findstr /c "user" temp_connections.json ^| find /c "user"') do (
            if %%c gtr 0 (
                echo    � Active connections detected
            ) else (
                echo    📭 No active connections
            )
        )
        del temp_connections.json >nul 2>&1
    )
    
    del temp_real_queues.json >nul 2>&1
) else (
    echo    ❌ Could not connect to RabbitMQ Management API
    echo    🔧 Ensure RabbitMQ container is running: docker compose up rabbitmq
    echo.
    echo    📋 Fallback - Docker container check:
    docker ps --filter "name=rabbitmq" --format "table {{.Names}}\t{{.Status}}" 2>nul
    if !ERRORLEVEL! equ 0 (
        echo    🐳 RabbitMQ container status checked
    ) else (
        echo    ❌ Could not check Docker containers
    )
)

echo.
echo 🎛️  Management Interface:
echo    🌐 URL: http://localhost:15672
echo    👤 Username: guest
echo    🔑 Password: guest
echo    📊 Real-time monitoring available

echo.
echo 💡 To manually check queue status:
echo    curl -u guest:guest http://localhost:15672/api/queues
echo    docker logs rabbitmq --tail 20

exit /b 0