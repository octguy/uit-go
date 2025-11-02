@echo off
echo 🔧 Generating gRPC Go code from proto files...

REM Generate User Service proto
echo 📁 Generating user service proto...
protoc --go_out=. --go_opt=paths=source_relative --go-grpc_out=. --go-grpc_opt=paths=source_relative user-service/proto/user.proto

if %ERRORLEVEL% NEQ 0 (
    echo ❌ User service proto generation failed!
    goto :error
)

REM Generate Trip Service proto
echo 📁 Generating trip service proto...
protoc --go_out=. --go_opt=paths=source_relative --go-grpc_out=. --go-grpc_opt=paths=source_relative trip-service/proto/trip.proto

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Trip service proto generation failed!
    goto :error
)

REM Generate Driver Service proto
echo 📁 Generating driver service proto...
protoc --go_out=. --go_opt=paths=source_relative --go-grpc_out=. --go-grpc_opt=paths=source_relative driver-service/proto/driver.proto

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Driver service proto generation failed!
    goto :error
)

echo ✅ All proto generation complete!
echo 📋 Generated files:
echo    User Service:
echo    - user-service/proto/user.pb.go
echo    - user-service/proto/user_grpc.pb.go
echo    Trip Service:
echo    - trip-service/proto/trip.pb.go
echo    - trip-service/proto/trip_grpc.pb.go
echo    Driver Service:
echo    - driver-service/proto/driver.pb.go
echo    - driver-service/proto/driver_grpc.pb.go

echo.
echo 🚀 Next steps:
echo    1. Run 'go mod tidy' to download dependencies
echo    2. Update your Go services to use the generated code  
echo    3. Test with grpcurl or gRPC client

goto :end

:error
echo ❌ Proto generation failed!
echo 💡 Make sure protoc is installed and in your PATH
echo    Download from: https://github.com/protocolbuffers/protobuf/releases
echo    Install protoc-gen-go: go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
echo    Install protoc-gen-go-grpc: go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
pause
exit /b 1

:end
pause