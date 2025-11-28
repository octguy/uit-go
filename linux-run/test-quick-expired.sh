#!/bin/bash

# Quick test: Try to accept an old expired trip
# This demonstrates that trips created >15s ago cannot be accepted

set -e

# Use one of the old SEARCHING_DRIVER trips from database
OLD_TRIP_ID="89966f76-74ae-4a5f-8171-0558c739f796"  # Created at 2025-11-28T08:51:48
DRIVER_EMAIL="driver1@gmail.com"
DRIVER_PASSWORD="123456"

echo "=========================================="
echo "Testing: Accepting an EXPIRED trip"
echo "=========================================="
echo ""

# Login as driver
echo "Step 1: Login as driver..."
DRIVER_LOGIN=$(curl -s -X POST http://localhost:8081/api/users/login \
  -H 'Content-Type: application/json' \
  -d "{\"email\": \"${DRIVER_EMAIL}\", \"password\": \"${DRIVER_PASSWORD}\"}")

DRIVER_TOKEN=$(echo "$DRIVER_LOGIN" | jq -r '.accessToken')
echo "✅ Driver logged in: $DRIVER_EMAIL"
echo ""

# Get trip details
echo "Step 2: Check trip details..."
TRIP=$(curl -s "http://localhost:8082/api/trips/$OLD_TRIP_ID")
TRIP_STATUS=$(echo "$TRIP" | jq -r '.status')
TRIP_REQUESTED_AT=$(echo "$TRIP" | jq -r '.requestedAt')

echo "Trip ID: $OLD_TRIP_ID"
echo "Status: $TRIP_STATUS"  
echo "Created at: $TRIP_REQUESTED_AT"
echo "Current time: $(date -u '+%Y-%m-%dT%H:%M:%S')"
echo ""

# Try to accept
echo "Step 3: Attempting to accept expired trip..."
ACCEPT_RESPONSE=$(curl -s -X POST "http://localhost:8082/api/trips/$OLD_TRIP_ID/accept" \
  -H "Authorization: Bearer $DRIVER_TOKEN")

echo "Response:"
echo "$ACCEPT_RESPONSE" | jq '.'
echo ""

# Check if it was rejected
if echo "$ACCEPT_RESPONSE" | jq -e '.message' | grep -q "expired"; then
    echo "✅ SUCCESS: Trip was correctly REJECTED (expired)"
elif echo "$ACCEPT_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
    echo "❌ FAILED: Trip was accepted (should have been rejected)"
else
    echo "⚠️  Unknown response"
fi

echo ""
echo "=========================================="
