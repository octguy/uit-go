@echo off
setlocal enabledelayedexpansion

REM UIT-Go gRPC Quick Demo
REM Quick verification that gRPC services are running and accessible

echo.
echo ============================================================
echo 🚀 UIT-Go gRPC Quick Demo
echo ============================================================

echo.
echo 📊 Checking gRPC Service Status:

REM Service ports
set DRIVER_PORT=50053
set TRIP_PORT=50052
set USER_PORT=50051

echo.
echo 🔍 Testing gRPC service connectivity...

REM Test each gRPC port individually
echo    Testing Driver Service (port %DRIVER_PORT%)...
powershell -Command "try { $tcp = New-Object System.Net.Sockets.TcpClient; $tcp.Connect('localhost', %DRIVER_PORT%); $tcp.Close(); Write-Host '        ✅ Driver Service gRPC is accessible' } catch { Write-Host '        ❌ Driver Service gRPC is not accessible' }" 2>nul

echo    Testing Trip Service (port %TRIP_PORT%)...
powershell -Command "try { $tcp = New-Object System.Net.Sockets.TcpClient; $tcp.Connect('localhost', %TRIP_PORT%); $tcp.Close(); Write-Host '        ✅ Trip Service gRPC is accessible' } catch { Write-Host '        ❌ Trip Service gRPC is not accessible' }" 2>nul

echo    Testing User Service (port %USER_PORT%)...
powershell -Command "try { $tcp = New-Object System.Net.Sockets.TcpClient; $tcp.Connect('localhost', %USER_PORT%); $tcp.Close(); Write-Host '        ✅ User Service gRPC is accessible' } catch { Write-Host '        ❌ User Service gRPC is not accessible' }" 2>nul

echo.
echo 🐳 Docker Container Status:
docker ps --filter "name=grpc" --format "{{.Names}} - {{.Status}}" 2>nul | findstr /v "^$"

echo.
echo 📋 Service Endpoints:
echo    🚗 Driver Service gRPC: localhost:%DRIVER_PORT%
echo    🚕 Trip Service gRPC:   localhost:%TRIP_PORT%
echo    👤 User Service gRPC:   localhost:%USER_PORT%

echo.
echo 🧪 Testing Basic gRPC Communication:

REM Check if grpcurl is available
where grpcurl >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo    ✅ grpcurl detected - testing real connections...
    
    echo.
    echo    🚗 Testing Driver Service:
    timeout /t 1 /nobreak >nul
    grpcurl -plaintext -max-time 3 localhost:%DRIVER_PORT% list >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo       ✅ Driver gRPC responds to service listing
    ) else (
        echo       ⚠️  Driver gRPC connection test failed (reflection may be disabled)
    )
    
    echo.
    echo    🚕 Testing Trip Service:
    timeout /t 1 /nobreak >nul
    grpcurl -plaintext -max-time 3 localhost:%TRIP_PORT% list >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo       ✅ Trip gRPC responds to service listing
    ) else (
        echo       ⚠️  Trip gRPC connection test failed (reflection may be disabled)
    )
    
    echo.
    echo    👤 Testing User Service:
    timeout /t 1 /nobreak >nul
    grpcurl -plaintext -max-time 3 localhost:%USER_PORT% list >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo       ✅ User gRPC responds to service listing
    ) else (
        echo       ⚠️  User gRPC connection test failed (reflection may be disabled)
    )
    
) else (
    echo    💡 grpcurl not found - install for advanced testing
    echo       Download from: https://github.com/fullstorydev/grpcurl
    echo.
    echo    🔧 Alternative: Test with Postman gRPC or BloomRPC
)

echo.
echo ============================================================
echo 🎯 gRPC Quick Demo Summary
echo ============================================================

echo.
echo ✅ gRPC Service Status Check Complete
echo.
echo 📊 Available Services:
echo    • Driver Service - Port %DRIVER_PORT% (Find nearby drivers, update locations)
echo    • Trip Service   - Port %TRIP_PORT% (Create trips, get status)
echo    • User Service   - Port %USER_PORT% (User management, authentication)
echo.
echo 🌟 gRPC Benefits in UIT-Go:
echo    • High-performance binary protocol
echo    • Type-safe service contracts
echo    • Cross-language compatibility
echo    • Built-in load balancing support
echo.
echo 🔗 Next Steps:
echo    • Use demo-grpc.bat for comprehensive testing
echo    • Install grpcurl for interactive API testing
echo    • Explore service definitions in .proto files
echo.

pause