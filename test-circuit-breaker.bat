@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Circuit Breaker Demo Script
echo ========================================
echo.

set API_GATEWAY=http://localhost:8080
REM Use fast endpoint that goes through circuit breaker
set TRIP_ENDPOINT=%API_GATEWAY%/api/trips/ratings/hello
set CB_ENDPOINT=%API_GATEWAY%/actuator/circuitbreakers

echo Step 1: Check initial state
echo ----------------------------------------
echo Current pods:
kubectl get pods -l app=trip-service
echo.

echo Checking Circuit Breaker initial state...
curl -s "%CB_ENDPOINT%"
echo.
echo.

echo Step 2: Scaling trip-service to 0 pods...
echo ----------------------------------------
kubectl scale deployment trip-service --replicas=0
echo Waiting for pods to terminate (15s)...
timeout /t 15 /nobreak
kubectl get pods -l app=trip-service
echo.
echo.

echo Step 3: Sending requests to trigger Circuit Breaker...
echo ----------------------------------------
echo (Circuit breaker opens when failure rate exceeds 50%% after 5+ calls)
echo.

for /L %%i in (1,1,15) do (
    echo Request %%i:
    curl -s -w " [HTTP %%{http_code}]" "%TRIP_ENDPOINT%" -o nul
    echo.
    
    REM Check CB state every 5 requests
    set /a mod=%%i %% 5
    if !mod! equ 0 (
        echo --- Checking Circuit Breaker state ---
        curl -s "%CB_ENDPOINT%" | findstr /i "state"
        echo.
    )
    timeout /t 1 /nobreak >nul
)

echo.
echo Step 4: Check Circuit Breaker state
echo ----------------------------------------
echo Full Circuit Breaker status:
curl -s "%CB_ENDPOINT%"
echo.
echo.

echo Step 5: Scaling trip-service back to 2 pods...
echo ----------------------------------------
kubectl scale deployment trip-service --replicas=2
echo Waiting for pods to be ready...
kubectl wait --for=condition=ready pod -l app=trip-service --timeout=120s
echo.
kubectl get pods -l app=trip-service
echo.
echo.

echo Step 6: Wait for Circuit Breaker to transition (10s wait)...
echo ----------------------------------------
echo Circuit breaker will try HALF_OPEN after waitDurationInOpenState (10s)
timeout /t 12 /nobreak
echo.

echo Sending recovery requests...
for /L %%i in (1,1,10) do (
    echo Request %%i:
    curl -s -w " [HTTP %%{http_code}]" "%TRIP_ENDPOINT%" -o nul
    echo.
    timeout /t 1 /nobreak >nul
)

echo.
echo Step 7: Final Circuit Breaker state
echo ----------------------------------------
curl -s "%CB_ENDPOINT%"
echo.
echo.

echo ========================================
echo Circuit Breaker Demo Complete!
echo ========================================
echo.
echo Expected flow:
echo   CLOSED --(failures exceed threshold)--^> OPEN
echo   OPEN --(waitDuration expires)--^> HALF_OPEN  
echo   HALF_OPEN --(success)--^> CLOSED
echo.
echo Current pod status:
kubectl get pods -l app=trip-service
echo.
pause
