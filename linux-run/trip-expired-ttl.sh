#!/bin/bash

# Test Script: Driver Accepts Trip After Expiration (>15s)
# This script tests what happens when driver tries to accept after notification expires

set -e

# Configuration
BASE_PORT=8080
USER_SERVICE_PORT=${BASE_PORT}
TRIP_SERVICE_PORT=${BASE_PORT}
DRIVER_SERVICE_PORT=${BASE_PORT}
DRIVER_SIMULATOR_PORT=8084

# Default test credentials
PASSENGER_EMAIL="${PASSENGER_EMAIL:-user1@gmail.com}"
PASSENGER_PASSWORD="${PASSENGER_PASSWORD:-123456}"
DRIVER_PASSWORD="${DRIVER_PASSWORD:-123456}"

# Default trip coordinates (District 1, Ho Chi Minh City)
PICKUP_LAT="${PICKUP_LAT:-10.762622}"
PICKUP_LNG="${PICKUP_LNG:-106.660172}"
DEST_LAT="${DEST_LAT:-10.777229}"
DEST_LNG="${DEST_LNG:-106.695534}"
FARE="${FARE:-50000}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "=========================================================="
echo -e "${BLUE}  Test: Driver Accepts Trip AFTER Expiration (>15s)${NC}"
echo "=========================================================="
echo ""

# Step 0: Setup drivers
echo -e "${BLUE}Step 0: Setting up drivers...${NC}"
echo ""

echo -e "${YELLOW}Bringing all drivers online...${NC}"
ONLINE_RESPONSE=$(curl -s -X POST http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/online-all)
echo "Response: $ONLINE_RESPONSE"
echo ""

echo -e "${YELLOW}Starting driver location simulation...${NC}"
echo "Simulating movement from (10.762622, 106.660172) to (10.776889, 106.700806)"
SIMULATE_RESPONSE=$(curl -s -X POST "http://localhost:${DRIVER_SIMULATOR_PORT}/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000")
echo "Response: $SIMULATE_RESPONSE"
echo ""

echo -e "${YELLOW}Waiting 5 seconds for drivers to start simulating...${NC}"
sleep 5
echo -e "${GREEN}✅ Ready!${NC}"
echo ""

echo -e "${GREEN}✅ Drivers setup complete!${NC}"
echo ""

echo "=========================================================="

# Step 1: Login as passenger
echo -e "${BLUE}Step 1: Logging in as passenger...${NC}"
echo "Email: $PASSENGER_EMAIL"

PASSENGER_LOGIN_RESPONSE=$(curl -s -X POST http://localhost:${USER_SERVICE_PORT}/api/users/login \
  -H 'Content-Type: application/json' \
  -d "{
    \"email\": \"${PASSENGER_EMAIL}\",
    \"password\": \"${PASSENGER_PASSWORD}\"
  }")

if echo "$PASSENGER_LOGIN_RESPONSE" | jq -e '.accessToken' > /dev/null 2>&1; then
    PASSENGER_TOKEN=$(echo "$PASSENGER_LOGIN_RESPONSE" | jq -r '.accessToken')
    echo -e "${GREEN}✅ Login successful!${NC}"
    echo "Token: ${PASSENGER_TOKEN:0:50}..."
else
    echo -e "${RED}❌ Login failed!${NC}"
    echo "Response: $PASSENGER_LOGIN_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 2: Find nearby drivers
echo -e "${BLUE}Step 2: Finding nearby drivers at pickup location...${NC}"
echo "Pickup Location: ($PICKUP_LAT, $PICKUP_LNG)"

NEARBY_DRIVERS=$(curl -s -X GET "http://localhost:8083/api/internal/drivers/nearby?lat=${PICKUP_LAT}&lng=${PICKUP_LNG}&radiusKm=3.0&limit=10")

if echo "$NEARBY_DRIVERS" | jq -e 'type == "array"' > /dev/null 2>&1; then
    DRIVER_COUNT=$(echo "$NEARBY_DRIVERS" | jq 'length')
    echo -e "${GREEN}✅ Found $DRIVER_COUNT nearby driver(s)${NC}"
    echo ""
    echo "$NEARBY_DRIVERS" | jq -r '.[] | "  • Driver ID: \(.driverId)\n    Distance: \(.distance)m | Location: (\(.latitude), \(.longitude))"'
    echo ""
    
    if [ "$DRIVER_COUNT" -gt 0 ]; then
        NEAREST_DRIVER_ID=$(echo "$NEARBY_DRIVERS" | jq -r '.[0].driverId')
        echo -e "${YELLOW}Nearest driver ID: $NEAREST_DRIVER_ID${NC}"
    else
        echo -e "${RED}❌ No nearby drivers found!${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Failed to get nearby drivers${NC}"
    echo "Response: $NEARBY_DRIVERS"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 3: Get driver user info to get email
echo -e "${BLUE}Step 3: Getting driver user information...${NC}"
echo "Driver ID: $NEAREST_DRIVER_ID"

DRIVER_USER_INFO=$(curl -s -X GET "http://localhost:${USER_SERVICE_PORT}/api/users/${NEAREST_DRIVER_ID}")

if echo "$DRIVER_USER_INFO" | jq -e '.email' > /dev/null 2>&1; then
    DRIVER_EMAIL=$(echo "$DRIVER_USER_INFO" | jq -r '.email')
    echo -e "${GREEN}✅ Driver info retrieved${NC}"
    echo "Driver Email: $DRIVER_EMAIL"
else
    echo -e "${RED}❌ Failed to get driver info${NC}"
    echo "Response: $DRIVER_USER_INFO"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 4: Login as driver to get driver token
echo -e "${BLUE}Step 4: Logging in as driver...${NC}"
echo "Email: $DRIVER_EMAIL"

DRIVER_LOGIN_RESPONSE=$(curl -s -X POST http://localhost:${USER_SERVICE_PORT}/api/users/login \
  -H 'Content-Type: application/json' \
  -d "{
    \"email\": \"${DRIVER_EMAIL}\",
    \"password\": \"${DRIVER_PASSWORD}\"
  }")

if echo "$DRIVER_LOGIN_RESPONSE" | jq -e '.accessToken' > /dev/null 2>&1; then
    DRIVER_TOKEN=$(echo "$DRIVER_LOGIN_RESPONSE" | jq -r '.accessToken')
    echo -e "${GREEN}✅ Driver login successful!${NC}"
    echo "Driver Token: ${DRIVER_TOKEN:0:50}..."
else
    echo -e "${RED}❌ Driver login failed!${NC}"
    echo "Response: $DRIVER_LOGIN_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 5: Create trip
echo -e "${BLUE}Step 5: Creating trip as passenger...${NC}"
echo "Pickup: ($PICKUP_LAT, $PICKUP_LNG)"
echo "Destination: ($DEST_LAT, $DEST_LNG)"
echo "Estimated Fare: $FARE VND"
echo ""

TRIP_RESPONSE=$(curl -s -X POST http://localhost:${TRIP_SERVICE_PORT}/api/trips/create \
  -H "Authorization: Bearer $PASSENGER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"pickupLatitude\": ${PICKUP_LAT},
    \"pickupLongitude\": ${PICKUP_LNG},
    \"destinationLatitude\": ${DEST_LAT},
    \"destinationLongitude\": ${DEST_LNG},
    \"estimatedFare\": ${FARE}
  }")

if echo "$TRIP_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
    TRIP_ID=$(echo "$TRIP_RESPONSE" | jq -r '.id')
    TRIP_STATUS=$(echo "$TRIP_RESPONSE" | jq -r '.status')
    TRIP_DRIVER_ID=$(echo "$TRIP_RESPONSE" | jq -r '.driverId')
    TRIP_PASSENGER_ID=$(echo "$TRIP_RESPONSE" | jq -r '.passengerId')
    echo -e "${GREEN}✅ Trip created successfully!${NC}"
    echo "Trip ID: $TRIP_ID"
    echo "Passenger ID: $TRIP_PASSENGER_ID"
    echo "Passenger Email: $PASSENGER_EMAIL"
    echo "Status: $TRIP_STATUS"
    echo "Driver ID: $TRIP_DRIVER_ID"
    echo "Created at: $(date '+%Y-%m-%d %H:%M:%S')"
else
    echo -e "${RED}❌ Trip creation failed!${NC}"
    echo "Response: $TRIP_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 6: Wait for RabbitMQ to process notification
echo -e "${BLUE}Step 6: Waiting for RabbitMQ to process notification...${NC}"
sleep 2
echo -e "${GREEN}✅ Notification sent${NC}"

echo ""
echo "=========================================================="

# Step 7: Check pending trips for ALL drivers immediately (should have notifications)
echo -e "${BLUE}Step 7: Checking pending trips for ALL nearby drivers (before expiration)...${NC}"
echo ""

DRIVERS_WITH_NOTIFICATION_BEFORE=0

for DRIVER_ID in $(echo "$NEARBY_DRIVERS" | jq -r '.[].driverId'); do
    PENDING=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${DRIVER_ID}")
    if echo "$PENDING" | jq -e 'type == "array"' > /dev/null 2>&1; then
        COUNT=$(echo "$PENDING" | jq 'length')
        if [ "$COUNT" -gt 0 ]; then
            echo -e "${GREEN}  ✅ Driver ${DRIVER_ID:0:8}... has $COUNT pending trip(s)${NC}"
            echo "$PENDING" | jq -r '.[] | "    └─ Trip: \(.tripId)\n       Passenger ID: \(.passengerId)\n       Pickup: (\(.pickupLatitude), \(.pickupLongitude))\n       Destination: (\(.destinationLatitude), \(.destinationLongitude))\n       Fare: \(.estimatedFare) VND\n       Distance: \(.distanceKm) km\n       Expires: \(.expiresAt)"'
            echo ""
            DRIVERS_WITH_NOTIFICATION_BEFORE=$((DRIVERS_WITH_NOTIFICATION_BEFORE + 1))
        fi
    fi
done

echo -e "${BLUE}Summary: $DRIVERS_WITH_NOTIFICATION_BEFORE drivers have pending notifications BEFORE expiration${NC}"

echo ""
echo "=========================================================="

# Step 8: Wait for notification to EXPIRE (TTL = 15 seconds)
echo -e "${BLUE}Step 8: Waiting for notification to EXPIRE...${NC}"
echo -e "${YELLOW}Notification TTL: 15 seconds${NC}"
echo ""

for i in {15..1}; do
    echo -ne "\r${YELLOW}⏳ Waiting... $i seconds remaining${NC}   "
    sleep 1
done
echo ""
echo ""
echo -e "${GREEN}✅ 15 seconds elapsed - Notification should be EXPIRED now!${NC}"

echo ""
echo "=========================================================="

# Step 9: Check pending trips for ALL drivers after expiration (should be empty)
echo -e "${BLUE}Step 9: Checking pending trips for ALL nearby drivers AFTER expiration...${NC}"
echo ""

DRIVERS_WITH_NOTIFICATION_AFTER=0
DRIVERS_WITHOUT_NOTIFICATION_AFTER=0

for DRIVER_ID in $(echo "$NEARBY_DRIVERS" | jq -r '.[].driverId'); do
    PENDING=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${DRIVER_ID}")
    if echo "$PENDING" | jq -e 'type == "array"' > /dev/null 2>&1; then
        COUNT=$(echo "$PENDING" | jq 'length')
        if [ "$COUNT" -eq 0 ]; then
            echo -e "${GREEN}  ✅ Driver ${DRIVER_ID:0:8}... notification expired (no pending trips)${NC}"
            DRIVERS_WITHOUT_NOTIFICATION_AFTER=$((DRIVERS_WITHOUT_NOTIFICATION_AFTER + 1))
        else
            echo -e "${YELLOW}  ⚠️  Driver ${DRIVER_ID:0:8}... still has $COUNT pending trip(s)${NC}"
            DRIVERS_WITH_NOTIFICATION_AFTER=$((DRIVERS_WITH_NOTIFICATION_AFTER + 1))
        fi
    fi
done

echo ""
if [ "$DRIVERS_WITHOUT_NOTIFICATION_AFTER" -eq "$DRIVER_COUNT" ]; then
    echo -e "${GREEN}✅ EXPECTED: All $DRIVER_COUNT drivers' notifications expired${NC}"
else
    echo -e "${YELLOW}⚠️  Only $DRIVERS_WITHOUT_NOTIFICATION_AFTER/$DRIVER_COUNT drivers' notifications expired${NC}"
fi

echo ""
echo "=========================================================="

# Step 10: Multiple drivers try to accept the expired trip
echo -e "${BLUE}Step 10: Testing drivers attempting to accept EXPIRED trip...${NC}"
echo "Trip ID: $TRIP_ID"
echo "Time since creation: >15 seconds"
echo ""

ACCEPT_SUCCESS=0
ACCEPT_FAILED=0

# Try first 3 drivers
DRIVER_IDS=($(echo "$NEARBY_DRIVERS" | jq -r '.[].driverId' | head -3))

for DRIVER_ID in "${DRIVER_IDS[@]}"; do
    echo -e "${YELLOW}Driver ${DRIVER_ID:0:8}... attempting to accept...${NC}"
    ACCEPT_RESPONSE=$(curl -s -X POST "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER_ID}")
    
    ACCEPTED=$(echo "$ACCEPT_RESPONSE" | jq -r '.accepted')
    MESSAGE=$(echo "$ACCEPT_RESPONSE" | jq -r '.message')
    
    if [ "$ACCEPTED" == "true" ]; then
        echo -e "  ${RED}❌ UNEXPECTED: Driver was able to accept!${NC}"
        ACCEPT_SUCCESS=$((ACCEPT_SUCCESS + 1))
    else
        echo -e "  ${GREEN}✅ EXPECTED: Driver blocked - $MESSAGE${NC}"
        ACCEPT_FAILED=$((ACCEPT_FAILED + 1))
    fi
    echo ""
done

echo -e "${BLUE}Acceptance Test Results:${NC}"
echo "  Success (should be 0): $ACCEPT_SUCCESS"
echo "  Failed (expected): $ACCEPT_FAILED"
echo ""

if [ "$ACCEPT_SUCCESS" -eq 0 ]; then
    echo -e "${GREEN}✅ CORRECT: All drivers blocked from accepting expired trip!${NC}"
else
    echo -e "${RED}❌ FAILED: $ACCEPT_SUCCESS driver(s) were able to accept expired trip!${NC}"
fi

echo ""
echo "=========================================================="

# Step 11: Verify final trip status
echo -e "${BLUE}Step 11: Verifying final trip status...${NC}"
sleep 1

TRIP_DETAILS=$(curl -s -X GET "http://localhost:${TRIP_SERVICE_PORT}/api/trips/${TRIP_ID}" \
  -H "Authorization: Bearer $PASSENGER_TOKEN")

if echo "$TRIP_DETAILS" | jq -e '.id' > /dev/null 2>&1; then
    FINAL_STATUS=$(echo "$TRIP_DETAILS" | jq -r '.status')
    FINAL_DRIVER_ID=$(echo "$TRIP_DETAILS" | jq -r '.driverId')
    
    echo "Trip ID: $TRIP_ID"
    echo "Final Status: $FINAL_STATUS"
    echo "Assigned Driver: $FINAL_DRIVER_ID"
    echo ""
    
    if [ "$FINAL_DRIVER_ID" = "null" ]; then
        echo -e "${YELLOW}⚠️  Trip is NOT assigned to any driver${NC}"
        echo "Status: $FINAL_STATUS"
    else
        echo -e "${GREEN}✅ Trip is assigned to driver: $FINAL_DRIVER_ID${NC}"
        echo "Status: $FINAL_STATUS"
    fi
else
    echo -e "${RED}❌ Failed to get trip details${NC}"
    echo "Response: $TRIP_DETAILS"
fi

echo ""
echo "=========================================================="
echo -e "${BLUE}TEST SUMMARY${NC}"
echo "=========================================================="
echo "Trip ID: $TRIP_ID"
echo "Passenger: $PASSENGER_EMAIL"
echo "Nearest Driver ID: $NEAREST_DRIVER_ID"
echo "Driver Email: $DRIVER_EMAIL"
echo ""
echo "Timeline:"
echo "  1. Trip created at: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  2. Notification sent to Redis (TTL=15s)"
echo "  3. Waited >15 seconds for expiration"
echo "  4. Driver attempted to accept expired trip"
echo ""
echo "Results:"
echo "  - Pending trips before expiration: $PENDING_COUNT_BEFORE"
echo "  - Pending trips after expiration: $PENDING_COUNT_AFTER"
echo "  - Final trip status: $FINAL_STATUS"
echo "  - Final driver assignment: $FINAL_DRIVER_ID"
echo ""
echo -e "${YELLOW}Key Learning:${NC}"
echo "  Redis notification TTL (15s) only affects the pending notification list."
echo "  Trip acceptance in trip-service may still work if trip status allows it."
echo ""
echo "=========================================================="
echo -e "${GREEN}✅ Test completed!${NC}"
echo "=========================================================="
