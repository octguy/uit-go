@echo off
echo ============================================================
echo 🧪 UIT-Go gRPC Test Calls
echo ============================================================
echo.

echo 🚗 Testing Driver Service gRPC calls...
echo.

echo 1️⃣ Health Check:
grpcurl -plaintext localhost:50053 driver.DriverService/HealthCheck
echo.

echo 2️⃣ Find Nearby Drivers:
grpcurl -plaintext -d "{\"latitude\": \"10.762622\", \"longitude\": \"106.660172\", \"radius_km\": \"5.0\", \"limit\": \"10\"}" localhost:50053 driver.DriverService/FindNearbyDrivers
echo.

echo 3️⃣ Get Driver Status:
grpcurl -plaintext -d "{\"driver_id\": \"550e8400-e29b-41d4-a716-446655440001\"}" localhost:50053 driver.DriverService/GetDriverStatus
echo.

echo 4️⃣ Update Driver Location:
grpcurl -plaintext -d "{\"driver_id\": \"550e8400-e29b-41d4-a716-446655440001\", \"latitude\": \"10.765\", \"longitude\": \"106.665\"}" localhost:50053 driver.DriverService/UpdateDriverLocation
echo.

echo 5️⃣ Update Driver Status:
grpcurl -plaintext -d "{\"driver_id\": \"550e8400-e29b-41d4-a716-446655440001\", \"status\": \"BUSY\"}" localhost:50053 driver.DriverService/UpdateDriverStatus
echo.

echo ============================================================
echo 🚕 Testing Trip Service gRPC calls...
echo.

echo 1️⃣ Create Trip:
grpcurl -plaintext -d "{\"user_id\": \"user-12345\", \"origin\": \"Ben Thanh Market\", \"destination\": \"Notre Dame Cathedral\"}" localhost:50052 trip.TripService/CreateTrip
echo.

echo 2️⃣ Get Trip Status:
grpcurl -plaintext -d "{\"trip_id\": \"trip-12345\"}" localhost:50052 trip.TripService/GetTripStatus
echo.

echo ============================================================
echo ✅ All gRPC test calls completed!
echo ============================================================

pause