@echo off
REM Quick health check for all containers in Pattern 2 architecture

echo "=== Checking Docker Container Status ==="
cd infra
docker-compose ps

echo.
echo "=== Quick Service Health Check ==="
echo "Testing Spring Boot services..."
curl -s http://localhost:8080/actuator/health > nul && echo "✅ API Gateway (8080): OK" || echo "❌ API Gateway (8080): DOWN"
curl -s http://localhost:8081/actuator/health > nul && echo "✅ User Service (8081): OK" || echo "❌ User Service (8081): DOWN"  
curl -s http://localhost:8082/actuator/health > nul && echo "✅ Trip Service (8082): OK" || echo "❌ Trip Service (8082): DOWN"
curl -s http://localhost:8083/actuator/health > nul && echo "✅ Driver Service (8083): OK" || echo "❌ Driver Service (8083): DOWN"

echo.
echo "Testing gRPC services..."
netstat -an | findstr :50051 > nul && echo "✅ User gRPC (50051): OK" || echo "❌ User gRPC (50051): DOWN"
netstat -an | findstr :50052 > nul && echo "✅ Trip gRPC (50052): OK" || echo "❌ Trip gRPC (50052): DOWN"
netstat -an | findstr :50053 > nul && echo "✅ Driver gRPC (50053): OK" || echo "❌ Driver gRPC (50053): DOWN"

echo.
echo "Testing databases..."
netstat -an | findstr :5433 > nul && echo "✅ Trip DB (5433): OK" || echo "❌ Trip DB (5433): DOWN"
netstat -an | findstr :5434 > nul && echo "✅ Driver DB (5434): OK" || echo "❌ Driver DB (5434): DOWN"
netstat -an | findstr :5435 > nul && echo "✅ User DB (5435): OK" || echo "❌ User DB (5435): DOWN"

echo.
echo "=== Pattern 2 Health Check Complete ==="