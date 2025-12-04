@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Testing Redis Write/Read Replica
echo ========================================
echo.

:: Generate unique email
set TIMESTAMP=%RANDOM%%RANDOM%
set EMAIL=driver.test%TIMESTAMP%@gmail.com
set PASSWORD=Test123456
set VEHICLE_MODEL=Tesla Model 3
set VEHICLE_NUMBER=TEST%TIMESTAMP%

echo Step 1: Registering new driver...
echo Email: %EMAIL%
echo.

curl -s -X POST "http://localhost:8080/api/users/register-driver" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\",\"vehicleModel\":\"%VEHICLE_MODEL%\",\"vehicleNumber\":\"%VEHICLE_NUMBER%\"}" > register_response.json

type register_response.json
echo.
echo.

echo Step 2: Logging in to get Bearer token...
echo.

curl -s -X POST "http://localhost:8080/api/users/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"%EMAIL%\",\"password\":\"%PASSWORD%\"}" > login_response.json

type login_response.json
echo.
echo.

:: Extract token using PowerShell
for /f "delims=" %%i in ('powershell -Command "(Get-Content login_response.json | ConvertFrom-Json).accessToken"') do set TOKEN=%%i

if "%TOKEN%"=="" (
    echo ERROR: Failed to get token!
    goto :cleanup
)

echo Token obtained: %TOKEN%
echo.

echo Step 3: Setting driver ONLINE (WRITE to Redis Master)...
echo.

curl -s -X POST "http://localhost:8080/api/drivers/online" ^
  -H "Authorization: Bearer %TOKEN%" > set_online_response.json

type set_online_response.json
echo.
echo.

echo Waiting 2 seconds for replication...
timeout /t 2 /nobreak > nul
echo.

echo Step 4: Checking driver status (READ from Redis Replica)...
echo.

curl -s -X GET "http://localhost:8080/api/drivers/status" ^
  -H "Authorization: Bearer %TOKEN%" > check_status_response.json

type check_status_response.json
echo.
echo.

echo Step 5: Verifying data in Redis...
echo.

echo ----------------------------------------
echo Checking Redis Master (redis-0):
echo ----------------------------------------
kubectl exec -it redis-0 -- redis-cli KEYS "driver:*"
echo.
echo Values in Master:
for /f "delims=" %%k in ('kubectl exec redis-0 -- redis-cli KEYS "driver:*"') do (
    echo Key: %%k
    kubectl exec redis-0 -- redis-cli GET "%%k"
    echo.
)

echo ----------------------------------------
echo Checking Redis Replica 1 (redis-1):
echo ----------------------------------------
kubectl exec -it redis-1 -- redis-cli KEYS "driver:*"
echo.
echo Values in Replica 1:
for /f "delims=" %%k in ('kubectl exec redis-1 -- redis-cli KEYS "driver:*"') do (
    echo Key: %%k
    kubectl exec redis-1 -- redis-cli GET "%%k"
    echo.
)

echo ----------------------------------------
echo Checking Redis Replica 2 (redis-2):
echo ----------------------------------------
kubectl exec -it redis-2 -- redis-cli KEYS "driver:*"
echo.
echo Values in Replica 2:
for /f "delims=" %%k in ('kubectl exec redis-2 -- redis-cli KEYS "driver:*"') do (
    echo Key: %%k
    kubectl exec redis-2 -- redis-cli GET "%%k"
    echo.
)

echo Step 6: Setting driver OFFLINE (WRITE to Redis Master)...
echo.

curl -s -X POST "http://localhost:8080/api/drivers/offline" ^
  -H "Authorization: Bearer %TOKEN%" > set_offline_response.json

type set_offline_response.json
echo.
echo.

echo Waiting 2 seconds for replication...
timeout /t 2 /nobreak > nul
echo.

echo Step 7: Checking driver status again (READ from Redis Replica)...
echo.

curl -s -X GET "http://localhost:8080/api/drivers/status" ^
  -H "Authorization: Bearer %TOKEN%" > check_status_response2.json

type check_status_response2.json
echo.
echo.

echo Step 8: Final Redis verification...
echo.

echo ----------------------------------------
echo Keys and Values in Redis Master:
echo ----------------------------------------
for /f "delims=" %%k in ('kubectl exec redis-0 -- redis-cli KEYS "driver:*"') do (
    echo Key: %%k
    echo Value: 
    kubectl exec redis-0 -- redis-cli GET "%%k"
    echo.
)

echo ========================================
echo Summary:
echo ========================================
echo - Write operations go to Redis Master (redis-0)
echo - Read operations come from Redis Replicas (redis-1, redis-2)
echo - Data is replicated across all Redis instances
echo ========================================

:cleanup
echo.
echo ========================================
echo Cleaning up temporary files...
echo ========================================
del register_response.json 2>nul
del login_response.json 2>nul
del set_online_response.json 2>nul
del check_status_response.json 2>nul
del set_offline_response.json 2>nul
del check_status_response2.json 2>nul

echo.
echo ========================================
echo Test completed!
echo ========================================
pause
