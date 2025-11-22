@echo off
REM Quick Terraform deployment script (without rebuilding)

echo "=== Deploying with Terraform (existing images) ==="
cd infra\terraform

echo "Applying Terraform configuration..."
terraform apply -auto-approve
if %ERRORLEVEL% neq 0 (
    echo "Terraform apply failed!"
    exit /b 1
)

cd ..\..
echo "=== Terraform deployment completed! ==="
echo "=== Services Available: ==="
echo "Spring Boot Services:"
echo "  - User Service: http://localhost:8081"
echo "  - Trip Service: http://localhost:8082" 
echo "  - Driver Service: http://localhost:8083"
echo "gRPC Services:"
echo "  - gRPC User Service: http://localhost:50051"
echo "  - gRPC Driver Service: http://localhost:50052"
echo "  - gRPC Trip Service: http://localhost:50053"
echo "Databases:"
echo "  - User DB: localhost:5433"
echo "  - Trip DB: localhost:5434"
echo "  - Driver DB: localhost:5435"