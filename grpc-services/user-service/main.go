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
	log.Printf("🔍 gRPC HealthCheck called with request: %+v", req)
	
	response := &pb.HealthCheckResponse{
		Service:   "user-service-grpc",
		Status:    "UP",
		Timestamp: time.Now().Unix(),
		Message:   "User gRPC Service is running and healthy",
	}
	
	log.Printf("✅ gRPC HealthCheck response: %+v", response)
	return response, nil
}

// ValidateUser validates if a user exists and is active
func (s *UserServer) ValidateUser(ctx context.Context, req *pb.ValidateUserRequest) (*pb.ValidateUserResponse, error) {
	log.Printf("🔍 gRPC ValidateUser called with request: %+v", req)
	log.Printf("📞 Calling Spring Boot API for user validation: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API - Fix the endpoint path
	url := fmt.Sprintf("%s/api/users/%s/validate", s.springBootURL, req.UserId)
	log.Printf("🌐 Making HTTP GET request to: %s", url)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("❌ Error calling Spring Boot API: %v", err)
		errorResponse := &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error calling user service: %v", err),
		}
		log.Printf("📤 gRPC ValidateUser error response: %+v", errorResponse)
		return errorResponse, nil
	}
	defer resp.Body.Close()

	log.Printf("📡 HTTP Response Status: %s", resp.Status)

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("❌ Error reading response: %v", err)
		errorResponse := &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}
		log.Printf("📤 gRPC ValidateUser error response: %+v", errorResponse)
		return errorResponse, nil
	}

	log.Printf("📄 Raw HTTP Response Body: %s", string(body))

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("❌ Error parsing JSON: %v", err)
		errorResponse := &pb.ValidateUserResponse{
			Valid:   false,
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}
		log.Printf("📤 gRPC ValidateUser error response: %+v", errorResponse)
		return errorResponse, nil
	}

	log.Printf("🔍 Parsed Spring Boot Response: %+v", springBootResponse)

	// Convert to gRPC response
	response := &pb.ValidateUserResponse{
		UserId: req.UserId,
	}

	if valid, ok := springBootResponse["valid"].(bool); ok {
		response.Valid = valid
		log.Printf("✅ User valid status: %t", valid)
	} else {
		log.Printf("⚠️ 'valid' field not found or not boolean in response")
	}
	
	if status, ok := springBootResponse["status"].(string); ok {
		response.Status = status
		log.Printf("📊 User status: %s", status)
	}
	
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
		log.Printf("✅ Operation success: %t", success)
	}
	
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
		log.Printf("💬 Response message: %s", message)
	}

	log.Printf("📤 Final gRPC ValidateUser response: %+v", response)
	return response, nil
}

// GetUserProfile gets user profile information
func (s *UserServer) GetUserProfile(ctx context.Context, req *pb.GetUserProfileRequest) (*pb.GetUserProfileResponse, error) {
	log.Printf("🔍 gRPC GetUserProfile called with request: %+v", req)
	log.Printf("📞 Calling Spring Boot API for user profile: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API - Fix the endpoint path
	url := fmt.Sprintf("%s/api/users/%s", s.springBootURL, req.UserId)
	log.Printf("🌐 Making HTTP GET request to: %s", url)

	// Make HTTP GET request to Spring Boot service
	resp, err := s.httpClient.Get(url)
	if err != nil {
		log.Printf("❌ Error calling Spring Boot API: %v", err)
		errorResponse := &pb.GetUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error calling user service: %v", err),
		}
		log.Printf("📤 gRPC GetUserProfile error response: %+v", errorResponse)
		return errorResponse, nil
	}
	defer resp.Body.Close()

	log.Printf("📡 HTTP Response Status: %s", resp.Status)

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("❌ Error reading response: %v", err)
		errorResponse := &pb.GetUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}
		log.Printf("📤 gRPC GetUserProfile error response: %+v", errorResponse)
		return errorResponse, nil
	}

	log.Printf("📄 Raw HTTP Response Body: %s", string(body))

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("❌ Error parsing JSON: %v", err)
		errorResponse := &pb.GetUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}
		log.Printf("📤 gRPC GetUserProfile error response: %+v", errorResponse)
		return errorResponse, nil
	}

	log.Printf("🔍 Parsed Spring Boot Response: %+v", springBootResponse)

	// Convert to gRPC response
	response := &pb.GetUserProfileResponse{
		UserId: req.UserId,
	}

	if username, ok := springBootResponse["username"].(string); ok {
		response.Username = username
		log.Printf("👤 Username: %s", username)
	}
	if email, ok := springBootResponse["email"].(string); ok {
		response.Email = email
		log.Printf("📧 Email: %s", email)
	}
	if fullName, ok := springBootResponse["fullName"].(string); ok {
		response.FullName = fullName
		log.Printf("📝 Full Name: %s", fullName)
	}
	if phoneNumber, ok := springBootResponse["phoneNumber"].(string); ok {
		response.PhoneNumber = phoneNumber
		log.Printf("📞 Phone Number: %s", phoneNumber)
	}
	if status, ok := springBootResponse["status"].(string); ok {
		response.Status = status
		log.Printf("📊 Status: %s", status)
	}
	if createdAt, ok := springBootResponse["createdAt"].(float64); ok {
		response.CreatedAt = int64(createdAt)
		log.Printf("📅 Created At: %s", time.Unix(response.CreatedAt, 0).Format(time.RFC3339))
	}
	if updatedAt, ok := springBootResponse["updatedAt"].(float64); ok {
		response.UpdatedAt = int64(updatedAt)
		log.Printf("🕒 Updated At: %s", time.Unix(response.UpdatedAt, 0).Format(time.RFC3339))
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
		log.Printf("✅ Operation success: %t", success)
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
		log.Printf("💬 Response message: %s", message)
	}

	log.Printf("📤 Final gRPC GetUserProfile response: %+v", response)
	return response, nil
}

// UpdateUserProfile updates user profile information
func (s *UserServer) UpdateUserProfile(ctx context.Context, req *pb.UpdateUserProfileRequest) (*pb.UpdateUserProfileResponse, error) {
	log.Printf("🔍 gRPC UpdateUserProfile called with request: %+v", req)
	log.Printf("📞 Calling Spring Boot API for user profile update: user_id=%s", req.UserId)

	// Construct URL for Spring Boot REST API - Fix the endpoint path
	url := fmt.Sprintf("%s/api/users/%s", s.springBootURL, req.UserId)
	log.Printf("🌐 Making HTTP PUT request to: %s", url)

	// Create request body
	requestBody := map[string]interface{}{
		"username":    req.Username,
		"email":       req.Email,
		"fullName":    req.FullName,
		"phoneNumber": req.PhoneNumber,
	}
	
	jsonBody, err := json.Marshal(requestBody)
	if err != nil {
		log.Printf("❌ Error marshaling request: %v", err)
		errorResponse := &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error creating request: %v", err),
		}
		log.Printf("📤 gRPC UpdateUserProfile error response: %+v", errorResponse)
		return errorResponse, nil
	}

	log.Printf("📄 HTTP Request Body: %s", string(jsonBody))

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

	log.Printf("📡 HTTP Response Status: %s", resp.Status)

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("❌ Error reading response: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error reading response: %v", err),
		}, nil
	}

	log.Printf("📄 Raw HTTP Response Body: %s", string(body))

	// Parse JSON response
	var springBootResponse map[string]interface{}
	if err := json.Unmarshal(body, &springBootResponse); err != nil {
		log.Printf("❌ Error parsing JSON: %v", err)
		return &pb.UpdateUserProfileResponse{
			UserId:  req.UserId,
			Success: false,
			Message: fmt.Sprintf("Error parsing response: %v", err),
		}, nil
	}

	log.Printf("🔍 Parsed Spring Boot Response: %+v", springBootResponse)

	// Convert to gRPC response
	response := &pb.UpdateUserProfileResponse{
		UserId: req.UserId,
	}

	if timestamp, ok := springBootResponse["timestamp"].(float64); ok {
		response.Timestamp = int64(timestamp)
		log.Printf("⏰ Timestamp: %s", time.Unix(response.Timestamp, 0).Format(time.RFC3339))
	}
	if success, ok := springBootResponse["success"].(bool); ok {
		response.Success = success
		log.Printf("✅ Operation success: %t", success)
	}
	if message, ok := springBootResponse["message"].(string); ok {
		response.Message = message
		log.Printf("💬 Response message: %s", message)
	}

	log.Printf("📤 Final gRPC UpdateUserProfile response: %+v", response)
	return response, nil
}

func main() {
	// Configuration
	port := ":50051"
	springBootURL := "http://user-service:8081" // Remove the redundant path part

	log.Printf("🚀 Starting gRPC User Service on port %s", port)
	log.Printf("🔗 Spring Boot backend: %s", springBootURL)
	log.Printf("📋 Service will log all gRPC calls and responses")

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
