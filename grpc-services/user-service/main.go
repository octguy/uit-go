package main

import (
	"context"
	"fmt"
	"log"
)

// TODO: Replace with actual protobuf generated code
type UserRequest struct {
	UserId int64
}

type UserResponse struct {
	Id       int64
	Email    string
	Name     string
	UserType string
	Phone    string
}

type ValidateUserRequest struct {
	Email string
}

type ValidateUserResponse struct {
	Valid    bool
	UserId   int64
	UserType string
}

type GetUsersByTypeRequest struct {
	UserType string
}

type GetUsersByTypeResponse struct {
	Users []*UserResponse
}

type UserServer struct {
	springBootURL string
}

func (s *UserServer) GetUser(ctx context.Context, req *UserRequest) (*UserResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/users/%d", s.springBootURL, req.UserId)

	// TODO: Make HTTP GET request to Spring Boot
	// TODO: Parse JSON response
	// TODO: Convert to gRPC response format

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &UserResponse{
		Id:       req.UserId,
		Email:    "TODO",
		Name:     "TODO",
		UserType: "TODO",
		Phone:    "TODO",
	}, nil
}

func (s *UserServer) ValidateUser(ctx context.Context, req *ValidateUserRequest) (*ValidateUserResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/users/email/%s", s.springBootURL, req.Email)

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &ValidateUserResponse{
		Valid:    true,
		UserId:   0,
		UserType: "TODO",
	}, nil
}

func (s *UserServer) GetUsersByType(ctx context.Context, req *GetUsersByTypeRequest) (*GetUsersByTypeResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/users/type/%s", s.springBootURL, req.UserType)

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &GetUsersByTypeResponse{
		Users: []*UserResponse{},
	}, nil
}

func main() {
	// TODO: Load Spring Boot URL from environment
	springBootURL := "http://localhost:8081" // user-service port

	// TODO: Initialize UserServer
	_ = &UserServer{
		springBootURL: springBootURL,
	}

	// TODO: Setup gRPC server with protobuf
	// TODO: Register UserServiceServer
	// TODO: Listen on port 50051

	log.Println("gRPC User Service (Interface Only) - Port :50051")
	log.Printf("Will proxy to Spring Boot at: %s", springBootURL)

	// TODO: Implement actual gRPC server
	log.Println("TODO: Implement gRPC server with protobuf")

	// Keep server running for demo
	select {}
}
