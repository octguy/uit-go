package main

import (
	"context"
	"fmt"
	"log"
	"net"

	"google.golang.org/grpc"

	pb "github.com/uit-go/grpc-services/proto/user"
)

type UserServer struct {
	pb.UnimplementedUserServiceServer
	springBootURL string
}

func (s *UserServer) GetUser(ctx context.Context, req *pb.GetUserRequest) (*pb.UserResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/users/%d", s.springBootURL, req.UserId)

	// TODO: Make HTTP GET request to Spring Boot
	// TODO: Parse JSON response
	// TODO: Convert to gRPC response format

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.UserResponse{
		Id:       req.UserId,
		Email:    "TODO",
		Name:     "TODO",
		UserType: "TODO",
		Phone:    "TODO",
	}, nil
}

func (s *UserServer) ValidateUser(ctx context.Context, req *pb.ValidateUserRequest) (*pb.ValidateUserResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/users/email/%s", s.springBootURL, req.Email)

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.ValidateUserResponse{
		Valid:    true,
		UserId:   0,
		UserType: "TODO",
	}, nil
}

func (s *UserServer) GetUsersByType(ctx context.Context, req *pb.GetUsersByTypeRequest) (*pb.GetUsersByTypeResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/users/type/%s", s.springBootURL, req.UserType)

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.GetUsersByTypeResponse{
		Users: []*pb.UserResponse{},
	}, nil
}

func main() {
	// TODO: Load Spring Boot URL from environment
	springBootURL := "http://localhost:8081" // user-service port

	server := &UserServer{
		springBootURL: springBootURL,
	}

	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterUserServiceServer(grpcServer, server)

	log.Println("gRPC User Service running on :50051")
	log.Printf("Will proxy to Spring Boot at: %s", springBootURL)

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
