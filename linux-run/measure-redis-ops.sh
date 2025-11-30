#!/bin/bash

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8083/api/driver-service"
TRIP_URL="http://localhost:8082"
METRICS_URL="http://localhost:8083/api/driver-service/metrics"

echo "========================================"
echo "Redis Read/Write Operation Measurement"
echo "========================================"
echo ""

echo -e "${BLUE}Step 1: Reset Redis operation counters${NC}"
curl -s -X POST $METRICS_URL/redis-ops/reset | jq .
echo ""

echo -e "${BLUE}Step 2: Register a test driver${NC}"
DRIVER_RESPONSE=$(curl -s -X POST "$BASE_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Driver",
    "email": "driver-test@example.com",
    "phone": "0123456789",
    "licenseNumber": "DL123456",
    "vehicleModel": "Toyota Vios",
    "plateNumber": "29A-12345"
  }')
DRIVER_ID=$(echo $DRIVER_RESPONSE | jq -r '.driverId')
echo "Driver ID: $DRIVER_ID"
echo ""

echo -e "${BLUE}Step 3: Update driver location (WRITE operation)${NC}"
curl -s -X PUT "$BASE_URL/location" \
  -H "Content-Type: application/json" \
  -d "{
    \"driverId\": \"$DRIVER_ID\",
    \"latitude\": 10.762622,
    \"longitude\": 106.660172
  }" | jq .
echo ""

echo -e "${BLUE}Step 4: Set driver status to ONLINE (WRITE operation)${NC}"
curl -s -X PUT "$BASE_URL/$DRIVER_ID/status?status=ONLINE" | jq .
echo ""

echo -e "${BLUE}Step 5: Find nearby drivers (READ operation - GEORADIUS + N status checks)${NC}"
curl -s -X GET "$BASE_URL/nearby?latitude=10.762622&longitude=106.660172&radius=5" | jq .
echo ""

echo -e "${BLUE}Step 6: Create a trip (triggers nearby driver search)${NC}"
TRIP_RESPONSE=$(curl -s -X POST "$TRIP_URL/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkMzJkOTc3Zi00MGNkLTQ1YjgtYjczMC03NTYzNWIwMmU3MmYiLCJpYXQiOjE3NjQ0MjgxMTgsImV4cCI6MTc2NDUxNDUxOH0.3pdxYi80aiaO4X_K7Z0MH8Z0P3QAy0YGiHr7sw8oqvU" \
  -d '{
    "pickupLatitude": 10.762622,
    "pickupLongitude": 106.660172,
    "destinationLatitude": 10.772622,
    "destinationLongitude": 106.670172
  }')
TRIP_ID=$(echo $TRIP_RESPONSE | jq -r '.id')
echo "Trip ID: $TRIP_ID"
echo ""

echo -e "${BLUE}Step 7: Get pending notifications for driver (READ operation - KEYS + GET)${NC}"
curl -s -X GET "$BASE_URL/pending-trips/$DRIVER_ID" | jq .
echo ""

echo -e "${BLUE}Step 8: Accept trip (READ + WRITE operations)${NC}"
curl -s -X POST "$BASE_URL/accept-trip" \
  -H "Content-Type: application/json" \
  -d "{
    \"tripId\": \"$TRIP_ID\",
    \"driverId\": \"$DRIVER_ID\"
  }" | jq .
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Redis Operation Statistics${NC}"
echo -e "${GREEN}========================================${NC}"
curl -s -X GET "$METRICS_URL/redis-ops" | jq .
echo ""

echo ""
echo -e "${YELLOW}Detailed stats printed to driver-service logs${NC}"
curl -s -X GET "$METRICS_URL/redis-ops/print"
echo ""
