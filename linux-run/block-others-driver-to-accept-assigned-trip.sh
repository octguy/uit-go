#!/bin/bash

# Test Script: Block Other Drivers from Accepting Assigned Trip
# This script tests the complete flow with proper setup and verification

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
echo -e "${BLUE}  Test: Block Other Drivers from Accepting Assigned Trip${NC}"
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

sleep 3
echo "=========================================================="

# Step 1: Login as passenger
echo -e "${BLUE}Step 1: Logging in as passenger...${NC}"
echo "Email: $PASSENGER_EMAIL"

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:${USER_SERVICE_PORT}/api/users/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${PASSENGER_EMAIL}\",\"password\":\"${PASSENGER_PASSWORD}\"}")

if echo "$LOGIN_RESPONSE" | jq -e '.accessToken' > /dev/null 2>&1; then
    TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')
    PASSENGER_ID=$(echo "$LOGIN_RESPONSE" | jq -r '.userId')
    echo -e "${GREEN}✅ Login successful${NC}"
    echo "Passenger ID: $PASSENGER_ID"
    echo "Email: $PASSENGER_EMAIL"
else
    echo -e "${RED}❌ Login failed${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

echo ""
echo "=========================================================="

# Step 2: Create trip
echo -e "${BLUE}Step 2: Creating trip as passenger...${NC}"
echo "Pickup: ($PICKUP_LAT, $PICKUP_LNG)"
echo "Destination: ($DEST_LAT, $DEST_LNG)"
echo "Fare: $FARE VND"

TRIP_RESPONSE=$(curl -s -X POST http://localhost:${TRIP_SERVICE_PORT}/api/trips/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
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
    echo -e "${GREEN}✅ Trip created successfully${NC}"
    echo "Trip ID: $TRIP_ID"
    echo "Status: $TRIP_STATUS"
    echo ""
    echo "Full trip details:"
    echo "$TRIP_RESPONSE" | jq '.'
else
    echo -e "${RED}❌ Failed to create trip${NC}"
    echo "Response: $TRIP_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 3: Wait for RabbitMQ to deliver notifications
echo -e "${BLUE}Step 3: Waiting for notifications to be delivered...${NC}"
sleep 2
echo -e "${GREEN}✅ Wait complete${NC}"

echo ""
echo "=========================================================="

# Step 4: Get nearby drivers from trip-service logs
echo -e "${BLUE}Step 4: Checking which drivers were notified...${NC}"
echo ""

DRIVER_LOG=$(docker logs trip-service 2>&1 | grep "$TRIP_ID" | grep "nearby driver(s):" | tail -1)

if [ -n "$DRIVER_LOG" ]; then
    echo "Log entry found:"
    echo "$DRIVER_LOG"
    echo ""
    
    # Extract driver count
    DRIVER_COUNT=$(echo "$DRIVER_LOG" | grep -oP '\d+(?= nearby driver\(s\))' || echo "0")
    echo -e "${GREEN}✅ Found $DRIVER_COUNT nearby drivers notified${NC}"
    
    # Extract all driver IDs
    NEARBY_DRIVERS=($(echo "$DRIVER_LOG" | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' | tail -n +2))
    
    echo ""
    echo "Notified drivers:"
    for i in "${!NEARBY_DRIVERS[@]}"; do
        echo "  Driver $((i+1)): ${NEARBY_DRIVERS[$i]}"
    done
else
    echo -e "${RED}❌ Could not find driver notification in logs${NC}"
    exit 1
fi

if [ ${#NEARBY_DRIVERS[@]} -lt 3 ]; then
    echo -e "${RED}❌ Not enough drivers (need at least 3, got ${#NEARBY_DRIVERS[@]})${NC}"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 5: Verify all drivers have pending notifications
echo -e "${BLUE}Step 5: Verifying all drivers received notifications...${NC}"
echo ""

DRIVERS_WITH_NOTIFICATION=0

for i in "${!NEARBY_DRIVERS[@]}"; do
    DRIVER_ID="${NEARBY_DRIVERS[$i]}"
    PENDING=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${DRIVER_ID}")
    
    if echo "$PENDING" | jq -e 'type == "array"' > /dev/null 2>&1; then
        COUNT=$(echo "$PENDING" | jq 'length')
        if [ "$COUNT" -gt 0 ]; then
            echo -e "${GREEN}  ✅ Driver $((i+1)) (${DRIVER_ID:0:8}...) has $COUNT pending trip(s)${NC}"
            DRIVERS_WITH_NOTIFICATION=$((DRIVERS_WITH_NOTIFICATION + 1))
        else
            echo -e "${YELLOW}  ⚠️  Driver $((i+1)) (${DRIVER_ID:0:8}...) has no pending trips${NC}"
        fi
    fi
done

echo ""
if [ "$DRIVERS_WITH_NOTIFICATION" -eq "${#NEARBY_DRIVERS[@]}" ]; then
    echo -e "${GREEN}✅ All ${#NEARBY_DRIVERS[@]} drivers received notifications!${NC}"
else
    echo -e "${YELLOW}⚠️  Only $DRIVERS_WITH_NOTIFICATION/${#NEARBY_DRIVERS[@]} drivers have pending notifications${NC}"
fi

echo ""
echo "=========================================================="

# Step 6: Driver 1 accepts the trip
DRIVER1_ID="${NEARBY_DRIVERS[0]}"
echo -e "${BLUE}Step 6: Driver 1 accepting trip...${NC}"
echo "Driver ID: $DRIVER1_ID"
echo "Trip ID: $TRIP_ID"
echo ""

ACCEPT1=$(curl -s -X POST "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER1_ID}")

echo "Accept Response:"
echo "$ACCEPT1" | jq '.'
echo ""

ACCEPTED1=$(echo "$ACCEPT1" | jq -r '.accepted')

if [ "$ACCEPTED1" == "true" ]; then
    echo -e "${GREEN}✅ Driver 1 successfully ACCEPTED the trip${NC}"
    ASSIGNED_DRIVER=$(echo "$ACCEPT1" | jq -r '.driverId')
    echo "Trip now assigned to: $ASSIGNED_DRIVER"
else
    echo -e "${RED}❌ Driver 1 was REJECTED${NC}"
    MESSAGE=$(echo "$ACCEPT1" | jq -r '.message')
    echo "Message: $MESSAGE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 7: Driver 2 tries to accept (should be blocked)
DRIVER2_ID="${NEARBY_DRIVERS[1]}"
echo -e "${BLUE}Step 7: Driver 2 trying to accept the ASSIGNED trip...${NC}"
echo "Driver ID: $DRIVER2_ID"
echo "Trip ID: $TRIP_ID"
echo ""

ACCEPT2=$(curl -s -X POST "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER2_ID}")

echo "Accept Response:"
echo "$ACCEPT2" | jq '.'
echo ""

ACCEPTED2=$(echo "$ACCEPT2" | jq -r '.accepted')

if [ "$ACCEPTED2" == "true" ]; then
    echo -e "${RED}❌ FAIL: Driver 2 was able to accept an assigned trip!${NC}"
else
    echo -e "${GREEN}✅ PASS: Driver 2 was correctly blocked${NC}"
    MESSAGE=$(echo "$ACCEPT2" | jq -r '.message')
    echo "Message: $MESSAGE"
fi

echo ""
echo "=========================================================="

# Step 8: Driver 3 tries to accept (should also be blocked)
DRIVER3_ID="${NEARBY_DRIVERS[2]}"
echo -e "${BLUE}Step 8: Driver 3 trying to accept the ASSIGNED trip...${NC}"
echo "Driver ID: $DRIVER3_ID"
echo "Trip ID: $TRIP_ID"
echo ""

ACCEPT3=$(curl -s -X POST "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/${TRIP_ID}/accept?driverId=${DRIVER3_ID}")

echo "Accept Response:"
echo "$ACCEPT3" | jq '.'
echo ""

ACCEPTED3=$(echo "$ACCEPT3" | jq -r '.accepted')

if [ "$ACCEPTED3" == "true" ]; then
    echo -e "${RED}❌ FAIL: Driver 3 was able to accept an assigned trip!${NC}"
else
    echo -e "${GREEN}✅ PASS: Driver 3 was correctly blocked${NC}"
    MESSAGE=$(echo "$ACCEPT3" | jq -r '.message')
    echo "Message: $MESSAGE"
fi

echo ""
echo "=========================================================="

# Step 9: Verify final trip status
echo -e "${BLUE}Step 9: Verifying final trip status...${NC}"
echo ""

FINAL_TRIP=$(curl -s "http://localhost:${TRIP_SERVICE_PORT}/api/trips/${TRIP_ID}" \
  -H "Authorization: Bearer $TOKEN")

if echo "$FINAL_TRIP" | jq -e '.id' > /dev/null 2>&1; then
    FINAL_STATUS=$(echo "$FINAL_TRIP" | jq -r '.status')
    FINAL_DRIVER=$(echo "$FINAL_TRIP" | jq -r '.driverId')
    
    echo "Final trip details:"
    echo "  Trip ID: $TRIP_ID"
    echo "  Status: $FINAL_STATUS"
    echo "  Assigned Driver: $FINAL_DRIVER"
    echo ""
    
    if [ "$FINAL_STATUS" == "ASSIGNED" ] && [ "$FINAL_DRIVER" == "$DRIVER1_ID" ]; then
        echo -e "${GREEN}✅ Trip correctly assigned to Driver 1${NC}"
    else
        echo -e "${YELLOW}⚠️  Trip status or driver mismatch${NC}"
    fi
else
    echo -e "${RED}❌ Failed to get final trip status${NC}"
    echo "Response: $FINAL_TRIP"
fi

echo ""
echo "=========================================================="
echo -e "${BLUE}TEST SUMMARY${NC}"
echo "=========================================================="
echo ""

# Summary
TEST_PASSED=true

if [ "$ACCEPTED1" == "true" ]; then
    echo -e "${GREEN}✅ Driver 1 accepted: PASS${NC}"
else
    echo -e "${RED}❌ Driver 1 accepted: FAIL${NC}"
    TEST_PASSED=false
fi

if [ "$ACCEPTED2" == "false" ]; then
    echo -e "${GREEN}✅ Driver 2 blocked: PASS${NC}"
else
    echo -e "${RED}❌ Driver 2 blocked: FAIL${NC}"
    TEST_PASSED=false
fi

if [ "$ACCEPTED3" == "false" ]; then
    echo -e "${GREEN}✅ Driver 3 blocked: PASS${NC}"
else
    echo -e "${RED}❌ Driver 3 blocked: FAIL${NC}"
    TEST_PASSED=false
fi

echo ""
if [ "$TEST_PASSED" == "true" ]; then
    echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✅ TEST PASSED: Only one driver can accept! ║${NC}"
    echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
else
    echo -e "${RED}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ❌ TEST FAILED: Multiple acceptance issue   ║${NC}"
    echo -e "${RED}╚══════════════════════════════════════════════╝${NC}"
fi
echo "=========================================================="
