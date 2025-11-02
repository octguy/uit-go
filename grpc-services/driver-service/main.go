package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	pb "github.com/uit-go/grpc-services/driver-service/proto"
)

// DriverServer implements the DriverService gRPC interface
type DriverServer struct {
	pb.UnimplementedDriverServiceServer
	springBootURL string
	httpClient    *http.Client
}

// NewDriverServer creates a new driver server instance
func NewDriverServer(springBootURL string) *DriverServer {
	return &DriverServer{
		springBootURL: springBootURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// HealthCheck implements the health check endpoint
func (s *DriverServer) HealthCheck(ctx context.Context, req *pb.HealthCheckRequest) (*pb.HealthCheckResponse, error) {
	log.Println("gRPC HealthCheck called")
	
	return &pb.HealthCheckResponse{
		Service:   "driver-service-grpc",
		Status:    "UP",
		Timestamp: time.Now().Unix(),
		Message:   "Driver gRPC Service is running and healthy",
	}, nil
}

// FindNearbyDrivers finds drivers within a specified radius
func (s *DriverServer) FindNearbyDrivers(ctx context.Context, req *pb.FindNearbyDriversRequest) (*pb.FindNearbyDriversResponse, error) {
	log.Printf("gRPC FindNearbyDrivers called: lat=%f, lng=%f, radius=%f, limit=%d", 
		req.Latitude, req.Longitude, req.RadiusKm, req.Limit)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/drivers/nearby?latitude=%f&longitude=%f&radiusKm=%f&limit=%d",
		s.springBootURL, req.Latitude, req.Longitude, req.RadiusKm, req.Limit)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.FindNearbyDriversResponse{
			Drivers: []*pb.DriverInfo{},
			Count:   0,
			Success: false,
			Message: fmt.Sprintf("Error calling driver service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.FindNearbyDriversResponse{
			Drivers: []*pb.DriverInfo{},
			Count:   0,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.FindNearbyDriversResponse{
			Drivers: []*pb.DriverInfo{},
			Count:   0,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert Spring Boot response to gRPC format
	drivers := []*pb.DriverInfo{}
	if driversData, ok := springBootResponse["drivers"].([]interface{}); ok {
		for _, driverData := range driversData {
			if driverMap, ok := driverData.(map[string]interface{}); ok {
				driver := convertToDriverInfo(driverMap)
				drivers = append(drivers, driver)
			}
		}
	}

	// Extract search location
	searchLocation := &pb.Location{
		Latitude:  req.Latitude,
		Longitude: req.Longitude,
	}

	success, _ := springBootResponse["success"].(bool)
	message, _ := springBootResponse["message"].(string)

	return &pb.FindNearbyDriversResponse{
		Drivers:        drivers,
		Count:          int32(len(drivers)),
		SearchRadius:   req.RadiusKm,
		SearchLocation: searchLocation,
		Success:        success,
		Message:        message,
	}, nil
}

// GetDriverStatus retrieves driver status and information
func (s *DriverServer) GetDriverStatus(ctx context.Context, req *pb.GetDriverStatusRequest) (*pb.GetDriverStatusResponse, error) {
	log.Printf("gRPC GetDriverStatus called: driverId=%s", req.DriverId)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/drivers/%s", s.springBootURL, req.DriverId)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.GetDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error calling driver service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.GetDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.GetDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.GetDriverStatusResponse{
		DriverId: req.DriverId,
	}

	if userId, ok := springBootResponse["userId"].(string); ok {
		response.UserId = userId
	}
	if status, ok := springBootResponse["status"].(string); ok {
		response.Status = status
	}
	if licenseNumber, ok := springBootResponse["licenseNumber"].(string); ok {
		response.LicenseNumber = licenseNumber
	}
	if vehicleModel, ok := springBootResponse["vehicleModel"].(string); ok {
		response.VehicleModel = vehicleModel
	}
	if vehiclePlate, ok := springBootResponse["vehiclePlate"].(string); ok {
		response.VehiclePlate = vehiclePlate
	}
	if rating, ok := springBootResponse["rating"].(float64); ok {
		response.Rating = rating
	}
	if totalTrips, ok := springBootResponse["totalTrips"].(float64); ok {
		response.TotalTrips = int32(totalTrips)
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
	}

	return response, nil
}

// UpdateDriverLocation updates driver's current location
func (s *DriverServer) UpdateDriverLocation(ctx context.Context, req *pb.UpdateDriverLocationRequest) (*pb.UpdateDriverLocationResponse, error) {
	log.Printf("gRPC UpdateDriverLocation called: driverId=%s, lat=%f, lng=%f", 
		req.DriverId, req.Latitude, req.Longitude)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/drivers/%s/location", s.springBootURL, req.DriverId)

	// Create request body
	requestBody := map[string]interface{}{
		"latitude":  req.Latitude,
		"longitude": req.Longitude,
	}
	
	jsonBody, err := json.Marshal(requestBody)
	if err != nil {
		log.Printf("Error marshaling request: %v", err)
		return &pb.UpdateDriverLocationResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error creating request: %v", err),
		}, nil
	}

	// Make HTTP PUT request to Spring Boot service
	httpReq, err := http.NewRequest("PUT", url, bytes.NewBuffer(jsonBody))
	if err != nil {
		log.Printf("Error creating HTTP request: %v", err)
		return &pb.UpdateDriverLocationResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error creating request: %v", err),
		}, nil
	}
	
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(httpReq)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.UpdateDriverLocationResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error calling driver service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.UpdateDriverLocationResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.UpdateDriverLocationResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.UpdateDriverLocationResponse{
		DriverId:  req.DriverId,
		Latitude:  req.Latitude,
		Longitude: req.Longitude,
	}

	if timestamp, ok := springBootResponse["timestamp"].(float64); ok {
		response.Timestamp = int64(timestamp)
	}
	if geohash, ok := springBootResponse["geohash"].(string); ok {
		response.Geohash = geohash
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
	}

	return response, nil
}

// UpdateDriverStatus updates driver's availability status
func (s *DriverServer) UpdateDriverStatus(ctx context.Context, req *pb.UpdateDriverStatusRequest) (*pb.UpdateDriverStatusResponse, error) {
	log.Printf("gRPC UpdateDriverStatus called: driverId=%s, status=%s", req.DriverId, req.Status)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/drivers/%s/status", s.springBootURL, req.DriverId)

	// Create request body
	requestBody := map[string]interface{}{
		"status": req.Status,
	}
	
	jsonBody, err := json.Marshal(requestBody)
	if err != nil {
		log.Printf("Error marshaling request: %v", err)
		return &pb.UpdateDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error creating request: %v", err),
		}, nil
	}

	// Make HTTP PUT request to Spring Boot service
	httpReq, err := http.NewRequest("PUT", url, bytes.NewBuffer(jsonBody))
	if err != nil {
		log.Printf("Error creating HTTP request: %v", err)
		return &pb.UpdateDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error creating request: %v", err),
		}, nil
	}
	
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(httpReq)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.UpdateDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error calling driver service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.UpdateDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.UpdateDriverStatusResponse{
			DriverId: req.DriverId,
			Success:  false,
			Message:  fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.UpdateDriverStatusResponse{
		DriverId: req.DriverId,
		Status:   req.Status,
	}

	if timestamp, ok := springBootResponse["timestamp"].(float64); ok {
		response.Timestamp = int64(timestamp)
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
	}

	return response, nil
}

// Helper function to convert Spring Boot driver data to gRPC DriverInfo
func convertToDriverInfo(driverMap map[string]interface{}) *pb.DriverInfo {
	driver := &pb.DriverInfo{}

	if id, ok := driverMap["driverId"].(string); ok {
		driver.DriverId = id
	}
	if userId, ok := driverMap["userId"].(string); ok {
		driver.UserId = userId
	}
	if latitude, ok := driverMap["latitude"].(float64); ok {
		driver.Latitude = latitude
	}
	if longitude, ok := driverMap["longitude"].(float64); ok {
		driver.Longitude = longitude
	}
	
	if distance, ok := driverMap["distance"].(float64); ok {
		// Handle protobuf3 default value issue: when distance is exactly 0.0,
		// protobuf omits it from output. Add a tiny offset to ensure it appears.
		if distance == 0.0 {
			distance = 0.001 // Very small distance to indicate "at location"
		}
		driver.Distance = distance
	}
	
	if status, ok := driverMap["status"].(string); ok {
		driver.Status = status
	}
	if vehiclePlate, ok := driverMap["vehiclePlate"].(string); ok {
		driver.VehiclePlate = vehiclePlate
	}
	if vehicleModel, ok := driverMap["vehicleModel"].(string); ok {
		driver.VehicleModel = vehicleModel
	}
	if rating, ok := driverMap["rating"].(float64); ok {
		driver.Rating = rating
	}
	if timestamp, ok := driverMap["timestamp"].(float64); ok {
		driver.Timestamp = int64(timestamp)
	}
	if geohash, ok := driverMap["geohash"].(string); ok {
		driver.Geohash = geohash
	}

	return driver
}

func main() {
	// Configuration
	port := ":50053"
	springBootURL := "http://driver-service:8083/api/driver-service" // Use correct context path

	log.Printf("🚀 Starting gRPC Driver Service on port %s", port)
	log.Printf("🔗 Spring Boot backend: %s", springBootURL)

	// Create TCP listener
	lis, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("❌ Failed to listen on port %s: %v", port, err)
	}

	// Create gRPC server
	s := grpc.NewServer()

	// Register the driver service
	driverServer := NewDriverServer(springBootURL)
	pb.RegisterDriverServiceServer(s, driverServer)

	// Enable reflection for debugging
	reflection.Register(s)

	log.Printf("✅ gRPC Driver Service registered")
	log.Printf("🔍 gRPC reflection enabled")
	log.Printf("📡 Listening on %s", port)
	log.Printf("🧪 Test with: grpcurl -plaintext localhost%s list", port)

	// Start the server
	if err := s.Serve(lis); err != nil {
		log.Fatalf("❌ Failed to serve gRPC server: %v", err)
	}
}
