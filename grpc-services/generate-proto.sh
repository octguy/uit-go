#!/bin/bash

# Generate gRPC Go code from proto files

echo "🔧 Generating gRPC Go code from proto files..."

# Create proto directory if it doesn't exist
mkdir -p proto

# Generate driver service
echo "📁 Generating driver service proto..."
protoc --go_out=. --go_opt=paths=source_relative \
    --go-grpc_out=. --go-grpc_opt=paths=source_relative \
    proto/driver.proto

echo "✅ Proto generation complete!"
echo "📋 Generated files:"
echo "   - proto/driver.pb.go"
echo "   - proto/driver_grpc.pb.go"

echo ""
echo "🚀 Next steps:"
echo "   1. Run 'go mod tidy' to download dependencies"
echo "   2. Update your Go services to use the generated code"
echo "   3. Test with grpcurl or gRPC client"