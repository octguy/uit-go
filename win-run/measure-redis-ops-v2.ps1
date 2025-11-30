# PowerShell version - Redis Operation Measurement (Updated for current API)
# This script measures Redis read/write operations using existing endpoints

$ErrorActionPreference = "Stop"

$API_GATEWAY = "http://localhost:8080"
$DRIVER_API = "$API_GATEWAY/api/drivers"
$INTERNAL_API = "$API_GATEWAY/api/internal/drivers"
$METRICS_API = "$API_GATEWAY/api/driver-service/metrics"
$TRIP_API = "$API_GATEWAY/api/trips"

# Use a test driver ID (you may need to adjust this)
$DRIVER_ID = "d32d977f-40cd-45b8-b730-75635b02e72f"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Redis Read/Write Operation Measurement" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Reset counters
Write-Host "Step 1: Reset Redis operation counters" -ForegroundColor Blue
try {
    Invoke-RestMethod -Uri "$METRICS_API/redis-ops/reset" -Method Post | Out-Null
    Write-Host "Counters reset successfully" -ForegroundColor Green
} catch {
    Write-Host "Warning: Could not reset counters - $_" -ForegroundColor Yellow
}
Write-Host ""

# Step 2: Set driver online (WRITE operation)
Write-Host "Step 2: Set driver status to ONLINE (WRITE operation)" -ForegroundColor Blue
try {
    Invoke-RestMethod -Uri "$DRIVER_API/online" -Method Post | Out-Null
    Write-Host "Driver set to ONLINE" -ForegroundColor Green
} catch {
    Write-Host "Warning: Could not set driver online - $_" -ForegroundColor Yellow
}
Write-Host ""

# Step 3: Find nearby drivers (READ operation - GEORADIUS + status checks)
Write-Host "Step 3: Find nearby drivers (READ operation - GEORADIUS + N status checks)" -ForegroundColor Blue
try {
    $nearbyDrivers = Invoke-RestMethod -Uri "$INTERNAL_API/nearby?lat=10.762622&lng=106.660172&radiusKm=5&limit=10" -Method Get
    Write-Host "Found $($nearbyDrivers.Count) nearby drivers" -ForegroundColor Green
} catch {
    Write-Host "Warning: Could not find nearby drivers - $_" -ForegroundColor Yellow
}
Write-Host ""

# Step 4: Get pending notifications (READ operation - KEYS + GET)
Write-Host "Step 4: Get pending notifications for driver (READ operation - KEYS + GET)" -ForegroundColor Blue
try {
    $pendingTrips = Invoke-RestMethod -Uri "$DRIVER_API/trips/pending?driverId=$DRIVER_ID" -Method Get
    Write-Host "Found $($pendingTrips.Count) pending notifications" -ForegroundColor Green
    
    # If there are pending trips, try to accept one
    if ($pendingTrips.Count -gt 0) {
        $tripId = $pendingTrips[0].tripId
        Write-Host ""
        Write-Host "Step 5: Accept trip (READ + WRITE operations)" -ForegroundColor Blue
        
        try {
            $acceptResponse = Invoke-RestMethod -Uri "$DRIVER_API/trips/$tripId/accept?driverId=$DRIVER_ID" -Method Post
            Write-Host "Trip accepted: $($acceptResponse.accepted)" -ForegroundColor Green
        } catch {
            Write-Host "Warning: Could not accept trip - $_" -ForegroundColor Yellow
        }
    } else {
        Write-Host "No pending trips to accept" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Warning: Could not get pending trips - $_" -ForegroundColor Yellow
}
Write-Host ""

# Step 6: Perform multiple nearby driver searches to generate more reads
Write-Host "Step 6: Perform multiple nearby searches (simulating real load)" -ForegroundColor Blue
$locations = @(
    @{lat=10.762622; lng=106.660172},
    @{lat=10.772622; lng=106.670172},
    @{lat=10.752622; lng=106.650172},
    @{lat=10.782622; lng=106.680172},
    @{lat=10.742622; lng=106.640172}
)

foreach ($loc in $locations) {
    try {
        $drivers = Invoke-RestMethod -Uri "$INTERNAL_API/nearby?lat=$($loc.lat)&lng=$($loc.lng)&radiusKm=3&limit=5" -Method Get
        Write-Host "  Location ($($loc.lat), $($loc.lng)): Found $($drivers.Count) drivers" -ForegroundColor Gray
    } catch {
        Write-Host "  Location ($($loc.lat), $($loc.lng)): Error" -ForegroundColor DarkGray
    }
}
Write-Host ""

# Get statistics
Write-Host "========================================" -ForegroundColor Green
Write-Host "Redis Operation Statistics" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

try {
    $stats = Invoke-RestMethod -Uri "$METRICS_API/redis-ops" -Method Get
    
    Write-Host ""
    Write-Host "Total Reads:       $($stats.totalReads)" -ForegroundColor Cyan
    Write-Host "Total Writes:      $($stats.totalWrites)" -ForegroundColor Cyan
    Write-Host "Read/Write Ratio:  $($stats.readWriteRatio)" -ForegroundColor Yellow
    Write-Host "Recommendation:    $($stats.recommendation)" -ForegroundColor Magenta
    Write-Host ""
    
    # Print detailed stats to logs
    Write-Host "Detailed stats printed to driver-service logs" -ForegroundColor Yellow
    Invoke-RestMethod -Uri "$METRICS_API/redis-ops/print" -Method Get | Out-Null
} catch {
    Write-Host ""
    Write-Host "Error: Could not retrieve statistics - $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "Done! Check the results above." -ForegroundColor Green
Write-Host ""
