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

	pb "github.com/uit-go/grpc-services/user-service/proto"
)

// UserServer implements the UserService gRPC interface
type UserServer struct {
	pb.UnimplementedUserServiceServer
	springBootURL string
	httpClient    *http.Client
}

// NewUserServer creates a new user server instance
func NewUserServer(springBootURL string) *UserServer {
	return &UserServer{
		springBootURL: springBootURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

// HealthCheck implements the health check endpoint
func (s *UserServer) HealthCheck(ctx context.Context, req *pb.HealthCheckRequest) (*pb.HealthCheckResponse, error) {
	log.Println("gRPC HealthCheck called")
	
	return &pb.HealthCheckResponse{
		Service:   "user-service-grpc",
		Status:    "UP",
		Timestamp: time.Now().Unix(),
		Message:   "User gRPC Service is running and healthy",
	}, nil
}

// ValidateUser validates if a user exists and is active
func (s *UserServer) ValidateUser(ctx context.Context, req *pb.ValidateUserRequest) (*pb.ValidateUserResponse, error) {
	log.Printf("gRPC ValidateUser called: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/users/%s/validate", s.springBootURL, req.UserId)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error calling user service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.ValidateUserResponse{
		UserId: req.UserId,
	}

	if valid, ok := springBootResponse["valid"].(bool); ok {
		response.Valid = valid
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
	log.Printf("gRPC ValidateUser called: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/api/user-service/users/%s/validate", s.springBootURL, req.UserId)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error calling user service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.ValidateUserResponse{
		UserId: req.UserId,
	}

	if valid, ok := springBootResponse["valid"].(bool); ok {
		response.Valid = valid
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

// GetUserProfile gets user profile information
func (s *UserServer) GetUserProfile(ctx context.Context, req *pb.GetUserProfileRequest) (*pb.GetUserProfileResponse, error) {
	log.Printf("gRPC GetUserProfile called: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/api/user-service/users/%s", s.springBootURL, req.UserId)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.GetUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error calling user service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.GetUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.GetUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.GetUserProfileResponse{
		UserId: req.UserId,
	}

	if username, ok := springBootResponse["username"].(string); ok {
		response.Username = username
	}
	if email, ok := springBootResponse["email"].(string); ok {
		response.Email = email
	}
	if fullName, ok := springBootResponse["fullName"].(string); ok {
		response.FullName = fullName
	}
	if phoneNumber, ok := springBootResponse["phoneNumber"].(string); ok {
		response.PhoneNumber = phoneNumber
	}
	if status, ok := springBootResponse["status"].(string); ok {
		response.Status = status
	}
	if createdAt, ok := springBootResponse["createdAt"].(float64); ok {
		response.CreatedAt = int64(createdAt)
	}
	if updatedAt, ok := springBootResponse["updatedAt"].(float64); ok {
		response.UpdatedAt = int64(updatedAt)
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
	}

	return response, nil
}

// UpdateUserProfile updates user profile information
func (s *UserServer) UpdateUserProfile(ctx context.Context, req *pb.UpdateUserProfileRequest) (*pb.UpdateUserProfileResponse, error) {
	log.Printf("gRPC UpdateUserProfile called: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API
	url := fmt.Sprintf("%s/api/user-service/users/%s", s.springBootURL, req.UserId)

	// Create request body
	requestBody := map[string]interface{}{
		"username":    req.Username,
		"email":       req.Email,
		"fullName":    req.FullName,
		"phoneNumber": req.PhoneNumber,
	}
	
	jsonBody, err := json.Marshal(requestBody)
	if err != nil {
		log.Printf("Error marshaling request: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error creating request: %v", err),
		}, nil
	}

	// Make HTTP PUT request to Spring Boot service
	httpReq, err := http.NewRequest("PUT", url, bytes.NewBuffer(jsonBody))
	if err != nil {
		log.Printf("Error creating HTTP request: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error creating request: %v", err),
		}, nil
	}
	
	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := s.httpClient.Do(httpReq)
	if err != nil {
		log.Printf("Error calling Spring Boot API: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error calling user service: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading response: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("Error parsing JSON: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	// Convert to gRPC response
	response := &pb.UpdateUserProfileResponse{
		UserId: req.UserId,
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

func main() {
	// Configuration
	port := ":50051"
	springBootURL := "http://user-service:8081/api/user-service" // Use correct context path

	log.Printf("🚀 Starting gRPC User Service on port %s", port)
	log.Printf("🔗 Spring Boot backend: %s", springBootURL)

	// Create TCP listener
	lis, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("❌ Failed to listen on port %s: %v", port, err)
	}

	// Create gRPC server
	s := grpc.NewServer()

	// Register the user service
	userServer := NewUserServer(springBootURL)
	pb.RegisterUserServiceServer(s, userServer)

	// Enable reflection for debugging
	reflection.Register(s)

	log.Printf("✅ gRPC User Service registered")
	log.Printf("🔍 gRPC reflection enabled")
	log.Printf("📡 Listening on %s", port)
	log.Printf("🧪 Test with: grpcurl -plaintext localhost%s list", port)

	// Start the server
	if err := s.Serve(lis); err != nil {
		log.Fatalf("❌ Failed to serve gRPC server: %v", err)
	}
}
