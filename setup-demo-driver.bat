@echo off
echo.
echo ============================================================
echo 🚗 Registering Test Driver for Demos
echo ============================================================
echo.

REM Test driver registration data
set DRIVER_ID=660e8400-e29b-41d4-a716-446655440002
set USER_ID=660e8400-e29b-41d4-a716-446655440001

echo 📝 Registering driver with ID: %DRIVER_ID%
echo.

curl -s -X POST "http://localhost:8083/api/driver-service/drivers/register" ^
    -H "Content-Type: application/json" ^
    -d "{\"userId\": \"%USER_ID%\", \"email\": \"test.driver@example.com\", \"phone\": \"+84901234567\", \"name\": \"Test Driver\", \"license_number\": \"DL12345678\", \"vehicle_type\": \"Toyota Vios\", \"vehicle_plate\": \"51A-12345\"}" >temp_register.json 2>&1

if %ERRORLEVEL% equ 0 (
    echo ✅ Driver registration successful
    findstr /i "success.*true" temp_register.json >nul 2>&1 && (
        echo    📋 Driver is now in database and ready for demos
    ) || (
        echo    ⚠️  Registration may have failed - check response:
        type temp_register.json
    )
) else (
    echo ❌ Driver registration failed
    echo    💡 Make sure driver service is running on port 8083
)

if exist temp_register.json del temp_register.json

echo.
echo Demo driver setup completed!
pause