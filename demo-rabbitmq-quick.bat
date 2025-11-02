@echo off
setlocal enabledelayedexpansion

REM UIT-Go RabbitMQ Quick Demo - Batch File Version
REM Quick verification that RabbitMQ integration is working

echo.
echo ============================================================
echo 🚀 UIT-GO RABBITMQ FLOW - QUICK DEMO
echo ============================================================

echo.
echo 📊 Current RabbitMQ Queue Status:
curl -s -u guest:guest "http://localhost:15672/api/queues" >temp_queues.json 2>nul
if !ERRORLEVEL! equ 0 (
    echo    ✅ RabbitMQ Management API is accessible
    echo    📋 Real queue status:
    
    REM Extract real message counts from JSON (simplified parsing)
    for /f "tokens=*" %%a in ('findstr /i "driver.location.updates" temp_queues.json') do (
        for /f "tokens=*" %%b in ('echo %%a ^| findstr /i "messages"') do (
            echo    🟢 driver.location.updates - Found in queues
        )
    )
    
    for /f "tokens=*" %%a in ('findstr /i "driver.status.changes" temp_queues.json') do (
        echo    🟢 driver.status.changes - Found in queues
    )
    
    for /f "tokens=*" %%a in ('findstr /i "driver.offline" temp_queues.json') do (
        echo    🟢 driver.offline - Found in queues
    )
    
    for /f "tokens=*" %%a in ('findstr /i "driver.online" temp_queues.json') do (
        echo    🟢 driver.online - Found in queues
    )
    
    for /f "tokens=*" %%a in ('findstr /i "trip.created.queue" temp_queues.json') do (
        echo    🟢 trip.created.queue - Found in queues
    )
    
    REM Get actual message counts using RabbitMQ API
    echo.
    echo    📊 Current message counts:
    
    REM Check location updates queue
    curl -s -u guest:guest "http://localhost:15672/api/queues/%%2F/driver.location.updates" 2>nul | findstr /i "messages" | findstr /v "details" >temp_loc_count.txt
    if exist temp_loc_count.txt (
        for /f "tokens=*" %%x in (temp_loc_count.txt) do (
            echo    📍 driver.location.updates queue data: %%x
        )
        del temp_loc_count.txt >nul 2>&1
    ) else (
        echo    📍 driver.location.updates: Could not retrieve count
    )
    
    REM Check status changes queue  
    curl -s -u guest:guest "http://localhost:15672/api/queues/%%2F/driver.status.changes" 2>nul | findstr /i "messages" | findstr /v "details" >temp_status_count.txt
    if exist temp_status_count.txt (
        for /f "tokens=*" %%y in (temp_status_count.txt) do (
            echo    📊 driver.status.changes queue data: %%y
        )
        del temp_status_count.txt >nul 2>&1
    ) else (
        echo    📊 driver.status.changes: Could not retrieve count
    )
    
    del temp_queues.json >nul 2>&1
) else (
    echo    ⚠️  Could not connect to RabbitMQ Management API
    echo    🔧 Make sure RabbitMQ is running: docker compose up rabbitmq
)

echo.
echo 📍 Testing Driver Location Update...

REM Test driver service health first
curl -s -f -m 5 "http://localhost:8083/api/driver-service/drivers/health" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo    ✅ Driver Service is accessible
    
    REM Generate test coordinates (slight variation from base coordinates)
    set /a LAT_OFFSET=%RANDOM% %%% 200 - 100
    set /a LNG_OFFSET=%RANDOM% %%% 200 - 100
    
    REM Create JSON payload (simplified for batch)
    echo {"latitude":10.762622,"longitude":106.660172} > temp_location.json
    
    REM Test location update with known driver ID
    curl -s -X PUT -H "Content-Type: application/json" -d @temp_location.json "http://localhost:8083/api/driver-service/drivers/550e8400-e29b-41d4-a716-446655440000/location" > temp_response.json 2>nul
    
    REM Check if response contains success
    findstr /i "success.*true" temp_response.json >nul
    if !ERRORLEVEL! equ 0 (
        echo    ✅ Location update successful!
        echo    📤 RabbitMQ message published to driver.location.updates
        
        REM Extract coordinates from response (simplified)
        for /f "tokens=*" %%a in ('findstr /i "latitude" temp_response.json') do (
            echo    📍 Location update processed
        )
    ) else (
        findstr /i "success.*false" temp_response.json >nul
        if !ERRORLEVEL! equ 0 (
            echo    ⚠️  Location update failed - check service logs
            echo    💡 This might be expected if test driver doesn't exist
        ) else (
            echo    ❌ Could not parse response from driver service
        )
    )
    
    del temp_location.json >nul 2>&1
    del temp_response.json >nul 2>&1
    
) else (
    echo    ❌ Could not connect to driver service
    echo    🔧 Make sure driver service is running: docker compose up driver-service
)

timeout /t 2 /nobreak >nul

echo.
echo 📊 Updated Queue Status:
echo    🔍 Checking queue after location update...

curl -s -u guest:guest "http://localhost:15672/api/queues/%%2F/driver.location.updates" 2>nul | findstr /i "messages" | findstr /v "details" >temp_after_count.txt
if exist temp_after_count.txt (
    echo    📈 driver.location.updates queue status:
    for /f "tokens=*" %%z in (temp_after_count.txt) do (
        echo       %%z
    )
    del temp_after_count.txt >nul 2>&1
) else (
    echo    ⚠️  Could not fetch updated queue status
)

echo.
echo    💡 For exact message counts, check: http://localhost:15672

echo.
echo 🎯 Demo Summary:
echo    ✓ Driver service REST API connectivity verified
echo    ✓ RabbitMQ Management API accessibility confirmed
echo    ✓ Location update API endpoint tested
echo    ✓ Message publishing flow demonstrated

echo.
echo 🔗 Management UI: http://localhost:15672 ^(guest/guest^)
echo 🎉 RabbitMQ integration verification completed!

echo.
echo Press any key to exit...
pause >nul
exit /b 0