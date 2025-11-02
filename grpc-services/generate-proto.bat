@echo off
echo 🔧 Generating gRPC Go code from proto files...

REM Create proto directory if it doesn't exist
if not exist proto mkdir proto

REM Generate driver service
echo 📁 Generating driver service proto...
protoc --go_out=. --go_opt=paths=source_relative --go-grpc_out=. --go-grpc_opt=paths=source_relative proto/driver.proto

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Proto generation failed!
    echo 💡 Make sure protoc is installed and in your PATH
    echo    Download from: https://github.com/protocolbuffers/protobuf/releases
    echo    Install protoc-gen-go: go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
    echo    Install protoc-gen-go-grpc: go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
    pause
    exit /b 1
)

echo ✅ Proto generation complete!
echo 📋 Generated files:
echo    - proto/driver.pb.go
echo    - proto/driver_grpc.pb.go

echo.
echo 🚀 Next steps:
echo    1. Run 'go mod tidy' to download dependencies
echo    2. Update your Go services to use the generated code  
echo    3. Test with grpcurl or gRPC client

pause