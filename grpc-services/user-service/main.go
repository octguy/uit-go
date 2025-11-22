package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"

	pb "github.com/uit-go/grpc-services/user-service/proto"
	"google.golang.org/grpc"
)

// UserServiceServer implements the gRPC User service
type UserServiceServer struct {
	pb.UnimplementedUserServer
}

// ValidateUser implements the ValidateUser RPC method
func (s *UserServiceServer) ValidateUser(ctx context.Context, req *pb.ValidateUserRequest) (*pb.ValidateUserResponse, error) {
	log.Printf("📞 gRPC call received: ValidateUser(userId=%s)", req.UserId)
	log.Printf("🔄 Forwarding to Spring User Service...")

	// Forward request to Spring User Service
	// In Kubernetes, services communicate via service names and DNS
	springUserServiceURL := getEnv("SPRING_USER_SERVICE_URL", "http://user-service:8081")

	// Create request to Spring service
	springReq := map[string]string{"userId": req.UserId}
	reqBody, _ := json.Marshal(springReq)
	resp, err := http.Post(fmt.Sprintf("%s/api/users/validate", springUserServiceURL),
		"application/json", bytes.NewBuffer(reqBody))

	if err != nil {
		log.Printf("❌ Failed to call Spring User Service: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			Message: fmt.Sprintf("Failed to validate user: %v", err),
		}, nil
	}
	defer resp.Body.Close()

	// Parse the response from Spring service
	var springResponse struct {
		Valid    bool   `json:"valid"`
		UserName string `json:"userName,omitempty"`
		Message  string `json:"message,omitempty"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&springResponse); err != nil {
		log.Printf("❌ Failed to decode Spring service response: %v", err)
		return &pb.ValidateUserResponse{
			Valid:   false,
			Message: "Invalid response from user service",
		}, nil
	}

	log.Printf("✅ Response from Spring User Service: valid=%v, userName=%s", springResponse.Valid, springResponse.UserName)

	// Return gRPC response
	return &pb.ValidateUserResponse{
		Valid:    springResponse.Valid,
		UserName: springResponse.UserName,
		Message:  springResponse.Message,
	}, nil
}

func main() {
	log.Println("🚀 User gRPC Service - Port :50051")

	// Create gRPC server
	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()

	// Register the User service
	pb.RegisterUserServer(grpcServer, &UserServiceServer{})

	log.Println("✅ gRPC server listening on :50051")

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
