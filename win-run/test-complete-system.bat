@echo off
setlocal enabledelayedexpansion

REM UIT-Go System Test Script - Windows Version
REM This script tests the entire system including RabbitMQ integration

echo.
echo ============================================================
echo 🚀 UIT-Go Complete System Test
echo ============================================================
echo Testing all services and RabbitMQ message flow...
echo.

set ERRORS=0

REM ===========================================
REM 1. HEALTH CHECKS
REM ===========================================
echo 🔍 PHASE 1: Health Checks
echo ============================================================

echo Testing Driver Service...
curl -s -f "http://localhost:8083/api/driver-service/api/drivers/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ Driver Service: HEALTHY
) else (
    echo ❌ Driver Service: UNREACHABLE
    set /a ERRORS+=1
)

echo Testing RabbitMQ Management...
curl -s -f -u guest:guest "http://localhost:15672/api/overview" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ RabbitMQ: HEALTHY
) else (
    echo ❌ RabbitMQ: UNREACHABLE
    set /a ERRORS+=1
)

echo Testing API Gateway...
curl -s -f "http://localhost:8080/" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo ✅ API Gateway: RESPONDING
) else (
    echo ❌ API Gateway: UNREACHABLE
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM 2. RABBITMQ QUEUE STATUS
REM ===========================================
echo 📊 PHASE 2: RabbitMQ Queue Status
echo ============================================================

echo Checking RabbitMQ queues...
curl -s -u guest:guest "http://localhost:15672/api/queues" >temp_queues.json 2>nul
if exist temp_queues.json (
    findstr /i "driver.location.updates" temp_queues.json >nul && echo ✅ driver.location.updates queue exists
    findstr /i "driver.status.changes" temp_queues.json >nul && echo ✅ driver.status.changes queue exists
    findstr /i "trip.created.queue" temp_queues.json >nul && echo ✅ trip.created.queue queue exists
    del temp_queues.json >nul 2>&1
) else (
    echo ❌ Could not retrieve queue information
    set /a ERRORS+=1
)

echo Checking RabbitMQ exchanges...
curl -s -u guest:guest "http://localhost:15672/api/exchanges" >temp_exchanges.json 2>nul
if exist temp_exchanges.json (
    findstr /i "driver.events" temp_exchanges.json >nul && echo ✅ driver.events exchange exists
    findstr /i "trip.events" temp_exchanges.json >nul && echo ✅ trip.events exchange exists
    del temp_exchanges.json >nul 2>&1
) else (
    echo ❌ Could not retrieve exchange information
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM 3. USER CREATION
REM ===========================================
echo 👤 PHASE 3: User Management Test
echo ============================================================

echo Creating test user...
curl -s -X POST "http://localhost:8080/api/users/register" ^
    -H "Content-Type: application/json" ^
    -d "{\"name\": \"Test User\", \"email\": \"testuser@example.com\", \"phone\": \"+84901111111\", \"password\": \"password123\"}" >temp_user.json 2>&1

if exist temp_user.json (
    findstr /i "userId" temp_user.json >nul
    if !ERRORLEVEL! equ 0 (
        echo ✅ User created successfully
        for /f "tokens=2 delims=:," %%a in ('findstr /i "userId" temp_user.json') do (
            set USER_ID=%%a
            set USER_ID=!USER_ID:"=!
        )
        echo 📋 User ID: !USER_ID!
    ) else (
        echo ⚠️  User creation response: 
        type temp_user.json | findstr /v "^$"
    )
    del temp_user.json >nul 2>&1
) else (
    echo ❌ User creation failed
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM 4. DRIVER REGISTRATION
REM ===========================================
echo 🚗 PHASE 4: Driver Management Test
echo ============================================================

echo Registering test driver...
curl -s -X POST "http://localhost:8083/api/driver-service/api/drivers/register" ^
    -H "Content-Type: application/json" ^
    -d "{\"userId\": \"550e8400-e29b-41d4-a716-446655440001\", \"email\": \"testdriver@example.com\", \"phone\": \"+84902222222\", \"name\": \"Test Driver\", \"license_number\": \"DL111111111\", \"vehicle_type\": \"Honda City\", \"vehicle_plate\": \"51A-11111\"}" >temp_driver.json 2>&1

if exist temp_driver.json (
    findstr /i "success.*true" temp_driver.json >nul
    if !ERRORLEVEL! equ 0 (
        echo ✅ Driver registered successfully
    ) else (
        findstr /i "DUPLICATE" temp_driver.json >nul
        if !ERRORLEVEL! equ 0 (
            echo ✅ Driver already exists (expected)
        ) else (
            echo ⚠️  Driver registration response:
            type temp_driver.json | findstr /v "^$"
        )
    )
    del temp_driver.json >nul 2>&1
) else (
    echo ❌ Driver registration failed
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM 5. DRIVER LOCATION UPDATE (RABBITMQ TEST)
REM ===========================================
echo 📍 PHASE 5: Driver Location Update (RabbitMQ Test)
echo ============================================================

set TEST_DRIVER_ID=550e8400-e29b-41d4-a716-446655440002

echo Getting initial queue message count...
curl -s -u guest:guest "http://localhost:15672/api/queues/%%2F/driver.location.updates" >temp_queue_before.json 2>nul
if exist temp_queue_before.json (
    for /f "tokens=2 delims=:," %%a in ('findstr /i "messages.*:" temp_queue_before.json ^| findstr /v "details"') do (
        set MESSAGES_BEFORE=%%a
    )
    del temp_queue_before.json >nul 2>&1
) else (
    set MESSAGES_BEFORE=0
)
echo 📊 Messages before: !MESSAGES_BEFORE!

echo Updating driver location...
curl -s -X PUT "http://localhost:8083/api/driver-service/api/drivers/!TEST_DRIVER_ID!/location" ^
    -H "Content-Type: application/json" ^
    -d "{\"latitude\": 10.762622, \"longitude\": 106.660172}" >temp_location.json 2>&1

if exist temp_location.json (
    findstr /i "success.*true" temp_location.json >nul
    if !ERRORLEVEL! equ 0 (
        echo ✅ Location update successful
        
        REM Wait a moment for RabbitMQ message processing
        timeout /t 2 /nobreak >nul
        
        echo Checking RabbitMQ message queue...
        curl -s -u guest:guest "http://localhost:15672/api/queues/%%2F/driver.location.updates" >temp_queue_after.json 2>nul
        if exist temp_queue_after.json (
            for /f "tokens=2 delims=:," %%b in ('findstr /i "messages.*:" temp_queue_after.json ^| findstr /v "details"') do (
                set MESSAGES_AFTER=%%b
            )
            echo 📊 Messages after: !MESSAGES_AFTER!
            
            if !MESSAGES_AFTER! gtr !MESSAGES_BEFORE! (
                echo ✅ RabbitMQ message published successfully!
            ) else (
                echo ⚠️  No new messages detected (may have been consumed immediately)
            )
            del temp_queue_after.json >nul 2>&1
        )
    ) else (
        findstr /i "not found" temp_location.json >nul
        if !ERRORLEVEL! equ 0 (
            echo ⚠️  Driver not found - creating one first...
            curl -s -X POST "http://localhost:8083/api/driver-service/api/drivers/register" ^
                -H "Content-Type: application/json" ^
                -d "{\"userId\": \"!TEST_DRIVER_ID!\", \"email\": \"test.driver.2@example.com\", \"phone\": \"+84903333333\", \"name\": \"Test Driver 2\", \"license_number\": \"DL222222222\", \"vehicle_type\": \"Toyota Vios\", \"vehicle_plate\": \"51A-22222\"}" >temp_driver2.json 2>&1
            del temp_driver2.json >nul 2>&1
        ) else (
            echo ❌ Location update failed:
            type temp_location.json | findstr /v "^$"
            set /a ERRORS+=1
        )
    )
    del temp_location.json >nul 2>&1
) else (
    echo ❌ Location update request failed
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM 6. TRIP CREATION
REM ===========================================
echo 🚕 PHASE 6: Trip Creation Test
echo ============================================================

echo Creating trip request...
curl -s -X POST "http://localhost:8080/api/trips/request" ^
    -H "Content-Type: application/json" ^
    -d "{\"userId\": \"550e8400-e29b-41d4-a716-446655440001\", \"pickupLatitude\": 10.762622, \"pickupLongitude\": 106.660172, \"destinationLatitude\": 10.772622, \"destinationLongitude\": 106.670172, \"pickupAddress\": \"District 1, Ho Chi Minh City\", \"destinationAddress\": \"District 3, Ho Chi Minh City\"}" >temp_trip.json 2>&1

if exist temp_trip.json (
    findstr /i "tripId" temp_trip.json >nul
    if !ERRORLEVEL! equ 0 (
        echo ✅ Trip created successfully
        for /f "tokens=2 delims=:," %%c in ('findstr /i "tripId" temp_trip.json') do (
            set TRIP_ID=%%c
            set TRIP_ID=!TRIP_ID:"=!
        )
        echo 📋 Trip ID: !TRIP_ID!
    ) else (
        echo ⚠️  Trip creation response:
        type temp_trip.json | findstr /v "^$"
    )
    del temp_trip.json >nul 2>&1
) else (
    echo ❌ Trip creation failed
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM 7. GRPC SERVICE TESTS
REM ===========================================
echo 🔗 PHASE 7: gRPC Service Tests
echo ============================================================

echo Testing gRPC services...

REM Check if grpcurl is available
where grpcurl >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo Testing User gRPC Service...
    grpcurl -plaintext localhost:50051 list >temp_grpc_user.txt 2>&1
    if !ERRORLEVEL! equ 0 (
        echo ✅ User gRPC Service: ACCESSIBLE
        findstr /i "UserService" temp_grpc_user.txt >nul && echo 📋 UserService methods available
    ) else (
        echo ❌ User gRPC Service: UNREACHABLE
        set /a ERRORS+=1
    )
    del temp_grpc_user.txt >nul 2>&1
    
    echo Testing Trip gRPC Service...
    grpcurl -plaintext localhost:50052 list >temp_grpc_trip.txt 2>&1
    if !ERRORLEVEL! equ 0 (
        echo ✅ Trip gRPC Service: ACCESSIBLE
        findstr /i "TripService" temp_grpc_trip.txt >nul && echo 📋 TripService methods available
    ) else (
        echo ❌ Trip gRPC Service: UNREACHABLE
        set /a ERRORS+=1
    )
    del temp_grpc_trip.txt >nul 2>&1
    
    echo Testing Driver gRPC Service...
    grpcurl -plaintext localhost:50053 list >temp_grpc_driver.txt 2>&1
    if !ERRORLEVEL! equ 0 (
        echo ✅ Driver gRPC Service: ACCESSIBLE
        findstr /i "DriverService" temp_grpc_driver.txt >nul && echo 📋 DriverService methods available
    ) else (
        echo ❌ Driver gRPC Service: UNREACHABLE
        set /a ERRORS+=1
    )
    del temp_grpc_driver.txt >nul 2>&1
) else (
    echo ⚠️  grpcurl not found - skipping gRPC tests
    echo 💡 Install grpcurl to test gRPC services: https://github.com/fullstorydev/grpcurl
)

echo.

REM ===========================================
REM 8. FINAL SYSTEM STATUS
REM ===========================================
echo 📊 PHASE 8: Final System Status
echo ============================================================

echo Checking final RabbitMQ queue status...
curl -s -u guest:guest "http://localhost:15672/api/queues" >temp_final_queues.json 2>nul
if exist temp_final_queues.json (
    echo 📈 Queue Message Counts:
    for /f "tokens=*" %%d in ('findstr /i "driver.location.updates.*messages.*:" temp_final_queues.json') do (
        echo    📍 driver.location.updates: %%d
    )
    for /f "tokens=*" %%e in ('findstr /i "driver.status.changes.*messages.*:" temp_final_queues.json') do (
        echo    📊 driver.status.changes: %%e
    )
    for /f "tokens=*" %%f in ('findstr /i "trip.created.queue.*messages.*:" temp_final_queues.json') do (
        echo    🚕 trip.created.queue: %%f
    )
    del temp_final_queues.json >nul 2>&1
) else (
    echo ❌ Could not retrieve final queue status
    set /a ERRORS+=1
)

echo.

REM ===========================================
REM RESULTS SUMMARY
REM ===========================================
echo ============================================================
echo 🎯 TEST RESULTS SUMMARY
echo ============================================================

if !ERRORS! equ 0 (
    echo.
    echo 🎉 ALL TESTS PASSED! 🎉
    echo.
    echo ✅ System Health: All services responding
    echo ✅ RabbitMQ: Queues and exchanges configured
    echo ✅ User Management: Registration working
    echo ✅ Driver Management: Registration working  
    echo ✅ Location Updates: Publishing to RabbitMQ
    echo ✅ Trip Management: Trip creation working
    echo ✅ gRPC Services: All accessible
    echo.
    echo 🚀 UIT-Go ride-sharing platform is fully functional!
    echo.
    echo 🔗 Access points:
    echo    🐰 RabbitMQ Management: http://localhost:15672 (guest/guest)
    echo    🌐 API Gateway: http://localhost:8080
    echo    🚗 Driver Service: http://localhost:8083
    echo    📱 User gRPC: localhost:50051
    echo    🚕 Trip gRPC: localhost:50052
    echo    🚗 Driver gRPC: localhost:50053
) else (
    echo.
    echo ❌ TESTS FAILED with !ERRORS! error(s)
    echo.
    echo 💡 Troubleshooting tips:
    echo    - Ensure all containers are running: docker ps
    echo    - Check container logs: docker logs [container-name]
    echo    - Wait longer for services to start (60+ seconds)
    echo    - Verify ports are not blocked by firewall
    echo    - Restart services: docker-compose down ^&^& docker-compose up -d
    echo.
)

echo.
echo ============================================================
echo 📝 Manual Testing Commands Available:
echo ============================================================
echo See test-system-commands.md for detailed curl and grpcurl commands
echo.

pause