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
	"strconv"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"

	pb "github.com/uit-go/grpc-services/trip-service/proto"
)

// TripServer implements the gRPC TripService
type TripServer struct {
	pb.UnimplementedTripServiceServer
	springBootURL  string
	userServiceURL string
	httpClient     *http.Client
}

// CreateTrip creates a new trip
func (s *TripServer) CreateTrip(ctx context.Context, req *pb.CreateTripRequest) (*pb.CreateTripResponse, error) {
	log.Printf("gRPC CreateTrip called: user_id=%s, origin=%s, destination=%s", req.UserId, req.Origin, req.Destination)

	// Prepare request to Spring Boot service
	requestData := map[string]interface{}{
		"user_id":     req.UserId,
		"origin":      req.Origin,
		"destination": req.Destination,
	}

	jsonData, err := json.Marshal(requestData)
	if err != nil {
		log.Printf("Error marshaling request: %v", err)
		return &pb.CreateTripResponse{
			TripId:  "",
			Status:  "ERROR",
			Success: false,
			Message: fmt.Sprintf("Error preparing request: %v", err),
		}, nil
	}

	// Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/trip-service/trips", s.springBootURL)
	resp, err := s.httpClient.Post(url, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		log.Printf("Error calling Spring Boot service: %v", err)
		return &pb.CreateTripResponse{
			TripId:  "",
			Status:  "ERROR", 
			Success: false,
			Message: fmt.Sprintf("Error calling backend service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.CreateTripResponse{
			TripId:  "",
			Status:  "ERROR",
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.CreateTripResponse{
			TripId:  "",
			Status:  "ERROR",
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert response to gRPC format
	response := &pb.CreateTripResponse{
		Success: true,
		Message: "Trip created successfully",
		Status:  "REQUESTED",
	}

	if tripId, ok := springBootResponse["tripId"].(string); ok {
		response.TripId = tripId
	}
	if status, ok := springBootResponse["status"].(string); ok {
		response.Status = status
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
	}

	return response, nil
}

// GetTripStatus gets the status of a trip
func (s *TripServer) GetTripStatus(ctx context.Context, req *pb.GetTripStatusRequest) (*pb.GetTripStatusResponse, error) {
	log.Printf("gRPC GetTripStatus called: trip_id=%s", req.TripId)

	// Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/trip-service/trips/%s", s.springBootURL, req.TripId)
	log.Printf("Calling Spring Boot URL: %s", url)
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("Error calling Spring Boot service: %v", err)
		return &pb.GetTripStatusResponse{
			TripId:  req.TripId,
			Success: false,
			Message: fmt.Sprintf("Error calling backend service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	log.Printf("Spring Boot HTTP status: %d", resp.StatusCode)

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.GetTripStatusResponse{
			TripId:  req.TripId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	log.Printf("Spring Boot raw response: %s", string(body))

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.GetTripStatusResponse{
			TripId:  req.TripId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Log the actual response for debugging
	log.Printf("Spring Boot response: %+v", springBootResponse)

	// Convert response to gRPC format
	response := &pb.GetTripStatusResponse{
		TripId:  req.TripId,
		Success: true,
		Message: "Trip status retrieved successfully",
		Status:  "REQUESTED", // Default status
	}

	if status, ok := springBootResponse["status"].(string); ok {
		response.Status = status
	}
	if driverId, ok := springBootResponse["driverId"].(string); ok {
		response.DriverId = driverId
	}
	if passengerId, ok := springBootResponse["passengerId"].(string); ok {
		response.PassengerId = passengerId
	}
	if origin, ok := springBootResponse["origin"].(string); ok {
		response.Origin = origin
	}
	if destination, ok := springBootResponse["destination"].(string); ok {
		response.Destination = destination
	}
	if fare, ok := springBootResponse["fare"].(float64); ok {
		response.Fare = fare
	}
	if createdAt, ok := springBootResponse["createdAt"].(string); ok {
		response.CreatedAt = createdAt
	}
	if updatedAt, ok := springBootResponse["updatedAt"].(string); ok {
		response.UpdatedAt = updatedAt
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
	}

	return response, nil
}

// HealthCheck provides a health check endpoint
func (s *TripServer) HealthCheck(ctx context.Context, req *pb.HealthCheckRequest) (*pb.HealthCheckResponse, error) {
	log.Printf("gRPC HealthCheck called")
	
	return &pb.HealthCheckResponse{
		Service:   "trip-service-grpc",
		Status:    "UP",
		Timestamp: strconv.FormatInt(time.Now().Unix(), 10),
		Message:   "Trip gRPC Service is running and healthy",
	}, nil
}

func main() {
	// Configuration
	port := ":50052"
	springBootURL := "http://trip-service:8082" // Use correct context path
	userServiceURL := "http://user-service:8081"

	log.Printf("🚀 Starting gRPC Trip Service on port %s", port)
	log.Printf("🔗 Spring Boot backend: %s", springBootURL)
	log.Printf("🔗 User Service: %s", userServiceURL)

	// Create listener
	lis, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	// Create gRPC server
	grpcServer := grpc.NewServer()

	// Initialize and register service
	tripServer := &TripServer{
		springBootURL:  springBootURL,
		userServiceURL: userServiceURL,
		httpClient:     &http.Client{Timeout: 10 * time.Second},
	}

	pb.RegisterTripServiceServer(grpcServer, tripServer)

	// Enable reflection for easier debugging with grpcurl
	reflection.Register(grpcServer)

	log.Printf("✅ gRPC Trip Service registered")
	log.Printf("🔍 gRPC reflection enabled")
	log.Printf("📡 Listening on %s", port)
	log.Printf("🧪 Test with: grpcurl -plaintext localhost%s list", port)

	// Start serving
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}