# PowerShell version - More reliable on Windows
# Redis Read/Write Operation Measurement

$ErrorActionPreference = "Stop"

$BASE_URL = "http://localhost:8080/api/driver-service"
$TRIP_URL = "http://localhost:8080/api/trip-service"
$METRICS_URL = "http://localhost:8080/api/driver-service/metrics"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Redis Read/Write Operation Measurement" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Reset counters
Write-Host "Step 1: Reset Redis operation counters" -ForegroundColor Blue
Invoke-RestMethod -Uri "$METRICS_URL/redis-ops/reset" -Method Post | Out-Null
Write-Host ""

# Step 2: Register driver
Write-Host "Step 2: Register a test driver" -ForegroundColor Blue
$driverPayload = @{
    name = "Test Driver"
    email = "driver-test@example.com"
    phone = "0123456789"
    licenseNumber = "DL123456"
    vehicleModel = "Toyota Vios"
    plateNumber = "29A-12345"
} | ConvertTo-Json

$driverResponse = Invoke-RestMethod -Uri "$BASE_URL/register" -Method Post -Body $driverPayload -ContentType "application/json"
$DRIVER_ID = $driverResponse.driverId
Write-Host "Driver ID: $DRIVER_ID" -ForegroundColor Green
Write-Host ""

# Step 3: Update location
Write-Host "Step 3: Update driver location (WRITE operation)" -ForegroundColor Blue
$locationPayload = @{
    driverId = $DRIVER_ID
    latitude = 10.762622
    longitude = 106.660172
} | ConvertTo-Json

Invoke-RestMethod -Uri "$BASE_URL/location" -Method Put -Body $locationPayload -ContentType "application/json" | Out-Null
Write-Host "Location updated" -ForegroundColor Green
Write-Host ""

# Step 4: Set status
Write-Host "Step 4: Set driver status to ONLINE (WRITE operation)" -ForegroundColor Blue
Invoke-RestMethod -Uri "$BASE_URL/$DRIVER_ID/status?status=ONLINE" -Method Put | Out-Null
Write-Host "Status set to ONLINE" -ForegroundColor Green
Write-Host ""

# Step 5: Find nearby drivers
Write-Host "Step 5: Find nearby drivers (READ operation - GEORADIUS + N status checks)" -ForegroundColor Blue
$nearbyDrivers = Invoke-RestMethod -Uri "$BASE_URL/nearby?latitude=10.762622&longitude=106.660172&radius=5" -Method Get
Write-Host "Found $($nearbyDrivers.Count) nearby drivers" -ForegroundColor Green
Write-Host ""

# Step 6: Create trip
Write-Host "Step 6: Create a trip (triggers nearby driver search)" -ForegroundColor Blue
$tripPayload = @{
    pickupLatitude = 10.762622
    pickupLongitude = 106.660172
    destinationLatitude = 10.772622
    destinationLongitude = 106.670172
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkMzJkOTc3Zi00MGNkLTQ1YjgtYjczMC03NTYzNWIwMmU3MmYiLCJpYXQiOjE3NjQ0MjgxMTgsImV4cCI6MTc2NDUxNDUxOH0.3pdxYi80aiaO4X_K7Z0MH8Z0P3QAy0YGiHr7sw8oqvU"
}

$tripResponse = Invoke-RestMethod -Uri "$TRIP_URL/create" -Method Post -Body $tripPayload -ContentType "application/json" -Headers $headers
$TRIP_ID = $tripResponse.id
Write-Host "Trip ID: $TRIP_ID" -ForegroundColor Green
Write-Host ""

# Step 7: Get pending notifications
Write-Host "Step 7: Get pending notifications for driver (READ operation - KEYS + GET)" -ForegroundColor Blue
$pendingTrips = Invoke-RestMethod -Uri "$BASE_URL/pending-trips/$DRIVER_ID" -Method Get
Write-Host "Found $($pendingTrips.Count) pending notifications" -ForegroundColor Green
Write-Host ""

# Step 8: Accept trip
Write-Host "Step 8: Accept trip (READ + WRITE operations)" -ForegroundColor Blue
$acceptPayload = @{
    tripId = $TRIP_ID
    driverId = $DRIVER_ID
} | ConvertTo-Json

$acceptResponse = Invoke-RestMethod -Uri "$BASE_URL/accept-trip" -Method Post -Body $acceptPayload -ContentType "application/json"
Write-Host "Trip accepted: $($acceptResponse.accepted)" -ForegroundColor Green
Write-Host ""

# Get statistics
Write-Host "========================================" -ForegroundColor Green
Write-Host "Redis Operation Statistics" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

$stats = Invoke-RestMethod -Uri "$METRICS_URL/redis-ops" -Method Get

Write-Host ""
Write-Host "Total Reads:       $($stats.totalReads)" -ForegroundColor Cyan
Write-Host "Total Writes:      $($stats.totalWrites)" -ForegroundColor Cyan
Write-Host "Read/Write Ratio:  $($stats.readWriteRatio)" -ForegroundColor Yellow
Write-Host "Recommendation:    $($stats.recommendation)" -ForegroundColor Magenta
Write-Host ""

# Print detailed stats to logs
Write-Host "Detailed stats printed to driver-service logs" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$METRICS_URL/redis-ops/print" -Method Get | Out-Null

Write-Host ""
Write-Host "Done! Check the results above." -ForegroundColor Green
Write-Host ""
