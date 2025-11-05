@echo off
echo.
echo ========================================
echo   UIT-Go Platform Demo Suite
echo ========================================
echo.
echo Choose demo type:
echo   1. RabbitMQ Quick Demo (30 seconds)
echo   2. RabbitMQ Full Demo (2-3 minutes)
echo   3. gRPC Quick Demo (30 seconds)
echo   4. gRPC Full Demo (3-5 minutes)
echo   5. RabbitMQ Real Status Check
echo   6. Exit
echo.
set /p choice="Enter your choice (1-6): "

if "%choice%"=="1" (
    echo.
    echo Running RabbitMQ Quick Demo...
    echo.
    call demo-rabbitmq-quick.bat
) else if "%choice%"=="2" (
    echo.
    echo Running RabbitMQ Full Demo...
    echo.
    call demo-rabbitmq-flow.bat /fast /skiphealth
) else if "%choice%"=="3" (
    echo.
    echo Running gRPC Quick Demo...
    echo.
    call demo-grpc-quick.bat
) else if "%choice%"=="4" (
    echo.
    echo Running gRPC Full Demo...
    echo.
    call demo-grpc.bat
) else if "%choice%"=="5" (
    echo.
    echo Running RabbitMQ Real Status Check...
    echo.
    call demo-rabbitmq-status.bat
) else if "%choice%"=="6" (
    echo Exiting...
    exit /b 0
) else (
    echo Invalid choice. Running RabbitMQ Quick Demo by default...
    echo.
    call demo-rabbitmq-quick.bat
)

echo.
echo Demo completed! Press any key to exit...
pause >nul