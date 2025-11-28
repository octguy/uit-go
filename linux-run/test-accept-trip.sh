#!/bin/bash

# Test Script: Driver Accepts Trip
# This script tests the complete flow: create trip -> driver accepts -> verify trip is assigned

set -e

# Configuration
USER_SERVICE_PORT=8081
TRIP_SERVICE_PORT=8082
DRIVER_SERVICE_PORT=8083

# Default test credentials
PASSENGER_EMAIL="${PASSENGER_EMAIL:-user1@gmail.com}"
PASSENGER_PASSWORD="${PASSENGER_PASSWORD:-123456}"
DRIVER_PASSWORD="${DRIVER_PASSWORD:-123456}"  # Assuming all drivers have same password

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
echo -e "${BLUE}  Test: Driver Accepts Trip & Trip Assignment${NC}"
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
else
    echo -e "${RED}❌ Trip creation failed!${NC}"
    echo "Response: $TRIP_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 6: Wait for RabbitMQ to process notification
echo -e "${BLUE}Step 6: Waiting for RabbitMQ to process notification...${NC}"
sleep 1
echo -e "${GREEN}✅ Ready${NC}"

echo ""
echo "=========================================================="

# Step 7: Check pending trips for the nearest driver
echo -e "${BLUE}Step 7: Checking pending trips for nearest driver...${NC}"
echo "Driver ID: $NEAREST_DRIVER_ID"
echo ""

PENDING_TRIPS=$(curl -s "http://localhost:${DRIVER_SERVICE_PORT}/api/drivers/trips/pending?driverId=${NEAREST_DRIVER_ID}")

if echo "$PENDING_TRIPS" | jq -e 'type == "array"' > /dev/null 2>&1; then
    PENDING_COUNT=$(echo "$PENDING_TRIPS" | jq 'length')
    if [ "$PENDING_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✅ Driver has $PENDING_COUNT pending trip(s)${NC}"
        echo ""
        echo "$PENDING_TRIPS" | jq -r '.[] | "  • Trip ID: \(.tripId)\n    Passenger: \(.passengerName)\n    Fare: \(.estimatedFare) VND\n    Distance: \(.distanceKm) km\n    Expires at: \(.expiresAt)"'
        echo ""
        
        # Check if our trip is in the pending list
        OUR_TRIP_IN_LIST=$(echo "$PENDING_TRIPS" | jq -r --arg trip_id "$TRIP_ID" '.[] | select(.tripId == $trip_id) | .tripId')
        if [ -n "$OUR_TRIP_IN_LIST" ]; then
            echo -e "${GREEN}✅ Our trip $TRIP_ID is in the pending list${NC}"
        else
            echo -e "${YELLOW}⚠️  Our trip $TRIP_ID is NOT in the pending list (may be for a different driver)${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  No pending trips (may have expired or wrong driver)${NC}"
    fi
else
    echo -e "${RED}❌ Error checking pending trips${NC}"
    echo "Response: $PENDING_TRIPS"
fi

echo ""
echo "=========================================================="

# Step 8: Driver accepts the trip (using driver token)
echo -e "${BLUE}Step 8: Driver accepting trip...${NC}"
echo "Driver ID: $NEAREST_DRIVER_ID"
echo "Driver Email: $DRIVER_EMAIL"
echo "Trip ID: $TRIP_ID"
echo ""

ACCEPT_RESPONSE=$(curl -s -X POST "http://localhost:${TRIP_SERVICE_PORT}/api/trips/${TRIP_ID}/accept" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

if echo "$ACCEPT_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
    ACCEPTED_TRIP_STATUS=$(echo "$ACCEPT_RESPONSE" | jq -r '.status')
    ACCEPTED_TRIP_DRIVER=$(echo "$ACCEPT_RESPONSE" | jq -r '.driverId')
    
    echo -e "${GREEN}✅ Trip accepted successfully!${NC}"
    echo "New Status: $ACCEPTED_TRIP_STATUS"
    echo "Assigned Driver: $ACCEPTED_TRIP_DRIVER"
else
    echo -e "${RED}❌ Trip acceptance failed!${NC}"
    echo "Response: $ACCEPT_RESPONSE"
    exit 1
fi

echo ""
echo "=========================================================="

# Step 9: Verify trip is assigned to driver
echo -e "${BLUE}Step 9: Verifying trip assignment...${NC}"
sleep 1  # Wait a moment for updates to propagate

TRIP_DETAILS=$(curl -s -X GET "http://localhost:${TRIP_SERVICE_PORT}/api/trips/${TRIP_ID}" \
  -H "Authorization: Bearer $PASSENGER_TOKEN")

if echo "$TRIP_DETAILS" | jq -e '.id' > /dev/null 2>&1; then
    UPDATED_STATUS=$(echo "$TRIP_DETAILS" | jq -r '.status')
    ASSIGNED_DRIVER_ID=$(echo "$TRIP_DETAILS" | jq -r '.driverId')
    
    echo "Trip ID: $TRIP_ID"
    echo "Status: $UPDATED_STATUS"
    echo "Assigned Driver ID: $ASSIGNED_DRIVER_ID"
    echo ""
    
    if [ "$ASSIGNED_DRIVER_ID" = "$NEAREST_DRIVER_ID" ] && [ "$ASSIGNED_DRIVER_ID" != "null" ]; then
        echo -e "${GREEN}✅ SUCCESS: Trip is assigned to driver $ASSIGNED_DRIVER_ID${NC}"
    else
        echo -e "${RED}❌ FAILED: Trip is NOT properly assigned${NC}"
        echo "Expected Driver: $NEAREST_DRIVER_ID"
        echo "Actual Driver: $ASSIGNED_DRIVER_ID"
        exit 1
    fi
    
    # Check status
    if [ "$UPDATED_STATUS" = "DRIVER_ASSIGNED" ] || [ "$UPDATED_STATUS" = "ACCEPTED" ]; then
        echo -e "${GREEN}✅ Trip status updated to: $UPDATED_STATUS${NC}"
    else
        echo -e "${YELLOW}⚠️  Trip status is: $UPDATED_STATUS (expected DRIVER_ASSIGNED or ACCEPTED)${NC}"
    fi
else
    echo -e "${RED}❌ Failed to get trip details${NC}"
    echo "Response: $TRIP_DETAILS"
    exit 1
fi

echo ""
echo "=========================================================="
echo -e "${GREEN}SUMMARY${NC}"
echo "=========================================================="
echo "Trip ID: $TRIP_ID"
echo "Passenger: $PASSENGER_EMAIL"
echo "Driver ID: $ASSIGNED_DRIVER_ID"
echo "Driver Email: $DRIVER_EMAIL"
echo "Trip Status: $UPDATED_STATUS"
echo ""
echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
echo "=========================================================="
echo -e "${GREEN}✅ Test completed successfully!${NC}"
echo "=========================================================="
echo ""

# Export variables for use in shell
echo "# You can use these variables in your shell:"
echo "export TRIP_ID=\"$TRIP_ID\""
echo "export DRIVER_ID=\"$ASSIGNED_DRIVER_ID\""
echo "export DRIVER_EMAIL=\"$DRIVER_EMAIL\""
echo "export PASSENGER_TOKEN=\"$PASSENGER_TOKEN\""
echo "export DRIVER_TOKEN=\"$DRIVER_TOKEN\""
