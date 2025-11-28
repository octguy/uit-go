#!/bin/bash

# Test Script: Driver Accepts Trip After Expiration (>15s)
# This script tests what happens when driver tries to accept after notification expires

set -e

# Configuration
USER_SERVICE_PORT=8081
TRIP_SERVICE_PORT=8082
DRIVER_SERVICE_PORT=8083

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
SIMULATE_RESPONSE=$(curl -s -X POST "http://localhost:8084/api/simulate/start-all?startLat=10.762622&startLng=106.660172&endLat=10.776889&endLng=106.700806&steps=200&delayMillis=1000")
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

NEARBY_DRIVERS=$(curl -s -X GET "http://localhost:${DRIVER_SERVICE_PORT}/api/internal/drivers/nearby?lat=${PICKUP_LAT}&lng=${PICKUP_LNG}&radiusKm=3.0&limit=10")

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
    DRIVER_NAME=$(echo "$DRIVER_USER_INFO" | jq -r '.name')
    echo -e "${GREEN}✅ Driver info retrieved${NC}"
    echo "Driver Name: $DRIVER_NAME"
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
    echo -e "${GREEN}✅ Trip created successfully!${NC}"
    echo "Trip ID: $TRIP_ID"
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

# Step 7: Check pending trips immediately (should have notification)
echo -e "${BLUE}Step 7: Checking pending trips immediately (before expiration)...${NC}"
echo "Driver ID: $NEAREST_DRIVER_ID"
echo ""

PENDING_TRIPS_BEFORE=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${NEAREST_DRIVER_ID}")

if echo "$PENDING_TRIPS_BEFORE" | jq -e 'type == "array"' > /dev/null 2>&1; then
    PENDING_COUNT_BEFORE=$(echo "$PENDING_TRIPS_BEFORE" | jq 'length')
    if [ "$PENDING_COUNT_BEFORE" -gt 0 ]; then
        echo -e "${GREEN}✅ Driver has $PENDING_COUNT_BEFORE pending trip(s) BEFORE expiration${NC}"
        echo ""
        echo "$PENDING_TRIPS_BEFORE" | jq -r '.[] | "  • Trip ID: \(.tripId)\n    Passenger: \(.passengerName)\n    Fare: \(.estimatedFare) VND\n    Distance: \(.distanceKm) km\n    Expires at: \(.expiresAt)"'
        echo ""
        
        # Check if our trip is in the pending list
        OUR_TRIP_IN_LIST=$(echo "$PENDING_TRIPS_BEFORE" | jq -r --arg trip_id "$TRIP_ID" '.[] | select(.tripId == $trip_id) | .tripId')
        if [ -n "$OUR_TRIP_IN_LIST" ]; then
            echo -e "${GREEN}✅ Our trip $TRIP_ID is in the pending list${NC}"
        else
            echo -e "${YELLOW}⚠️  Our trip $TRIP_ID is NOT in the pending list${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  No pending trips found${NC}"
    fi
else
    echo -e "${RED}❌ Error checking pending trips${NC}"
    echo "Response: $PENDING_TRIPS_BEFORE"
fi

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

# Step 9: Check pending trips after expiration (should be empty)
echo -e "${BLUE}Step 9: Checking pending trips AFTER expiration...${NC}"
echo "Driver ID: $NEAREST_DRIVER_ID"
echo ""

PENDING_TRIPS_AFTER=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${NEAREST_DRIVER_ID}")

if echo "$PENDING_TRIPS_AFTER" | jq -e 'type == "array"' > /dev/null 2>&1; then
    PENDING_COUNT_AFTER=$(echo "$PENDING_TRIPS_AFTER" | jq 'length')
    if [ "$PENDING_COUNT_AFTER" -eq 0 ]; then
        echo -e "${GREEN}✅ EXPECTED: Pending trips list is EMPTY (notification expired)${NC}"
    else
        echo -e "${YELLOW}⚠️  UNEXPECTED: Driver still has $PENDING_COUNT_AFTER pending trip(s)${NC}"
        echo "$PENDING_TRIPS_AFTER" | jq '.'
    fi
else
    echo -e "${RED}❌ Error checking pending trips${NC}"
    echo "Response: $PENDING_TRIPS_AFTER"
fi

echo ""
echo "=========================================================="

# Step 10: Driver tries to accept the expired trip
echo -e "${BLUE}Step 10: Driver attempting to accept EXPIRED trip...${NC}"
echo "Driver ID: $NEAREST_DRIVER_ID"
echo "Driver Email: $DRIVER_EMAIL"
echo "Trip ID: $TRIP_ID"
echo "Time since creation: >15 seconds"
echo ""

ACCEPT_RESPONSE=$(curl -s -X POST "http://localhost:${TRIP_SERVICE_PORT}/api/trips/${TRIP_ID}/accept" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

echo "Accept Response:"
echo "$ACCEPT_RESPONSE" | jq '.'
echo ""

if echo "$ACCEPT_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
    ACCEPTED_TRIP_STATUS=$(echo "$ACCEPT_RESPONSE" | jq -r '.status')
    ACCEPTED_TRIP_DRIVER=$(echo "$ACCEPT_RESPONSE" | jq -r '.driverId')
    
    echo -e "${GREEN}✅ Trip was accepted (even though notification expired)${NC}"
    echo "New Status: $ACCEPTED_TRIP_STATUS"
    echo "Assigned Driver: $ACCEPTED_TRIP_DRIVER"
    echo ""
    echo -e "${YELLOW}⚠️  NOTE: Trip can still be accepted even after notification expires!${NC}"
    echo -e "${YELLOW}    Redis notification expiration (15s TTL) is separate from trip acceptance logic.${NC}"
else
    ERROR_MESSAGE=$(echo "$ACCEPT_RESPONSE" | jq -r '.message // .error // "Unknown error"')
    echo -e "${RED}❌ Trip acceptance failed!${NC}"
    echo "Error: $ERROR_MESSAGE"
    echo ""
    echo -e "${YELLOW}This could be because:${NC}"
    echo "  - Another driver already accepted"
    echo "  - Trip was cancelled"
    echo "  - Trip status changed"
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
