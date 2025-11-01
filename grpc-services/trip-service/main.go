package main

import (
	"context"
	"fmt"
	"log"
	"time"
)

// TODO: Replace with actual protobuf generated code
type GetTripRequest struct {
	TripId int64
}

type TripResponse struct {
	Trip *TripInfo
}

type TripInfo struct {
	Id                   int64
	PassengerId          int64
	DriverId             int64
	Status               string
	PickupLocation       string
	Destination          string
	PickupLatitude       float64
	PickupLongitude      float64
	DestinationLatitude  float64
	DestinationLongitude float64
	Fare                 float64
	CreatedAt            string
	UpdatedAt            string
}

type GetTripsByUserRequest struct {
	UserId   int64
	UserType string
}

type GetTripsByUserResponse struct {
	Trips []*TripInfo
}

type UpdateTripStatusRequest struct {
	TripId int64
	Status string
}

type UpdateTripStatusResponse struct {
	Success bool
	Message string
}

type TripServer struct {
	springBootURL string
}

func (s *TripServer) GetTrip(ctx context.Context, req *GetTripRequest) (*TripResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/trips/%d", s.springBootURL, req.TripId)

	// TODO: Make HTTP GET request to Spring Boot
	// TODO: Parse JSON response
	// TODO: Convert to gRPC response format

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &TripResponse{
		Trip: &TripInfo{
			Id:          req.TripId,
			PassengerId: 0,
			DriverId:    0,
			Status:      "TODO",
		},
	}, nil
}

func (s *TripServer) GetTripsByUser(ctx context.Context, req *GetTripsByUserRequest) (*GetTripsByUserResponse, error) {
	// TODO: Call Spring Boot REST API
	var url string
	if req.UserType == "PASSENGER" {
		url = fmt.Sprintf("%s/api/trips/passenger/%d", s.springBootURL, req.UserId)
	} else if req.UserType == "DRIVER" {
		url = fmt.Sprintf("%s/api/trips/driver/%d", s.springBootURL, req.UserId)
	}

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &GetTripsByUserResponse{
		Trips: []*TripInfo{},
	}, nil
}

func (s *TripServer) UpdateTripStatus(ctx context.Context, req *UpdateTripStatusRequest) (*UpdateTripStatusResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/trips/%d/status", s.springBootURL, req.TripId)

	// TODO: Create HTTP PUT request with JSON body
	// TODO: Handle response

	log.Printf("TODO: PUT to %s", url)

	return &UpdateTripStatusResponse{
		Success: true,
		Message: "TODO: Implement",
	}, nil
}

func main() {
	// TODO: Load Spring Boot URL from environment
	springBootURL := "http://localhost:8082" // trip-service port

	// TODO: Initialize TripServer
	_ = &TripServer{
		springBootURL: springBootURL,
	}

	// TODO: Setup gRPC server with protobuf
	// TODO: Register TripServiceServer
	// TODO: Listen on port 50052

	log.Println("gRPC Trip Service (Interface Only) - Port :50052")
	log.Printf("Will proxy to Spring Boot at: %s", springBootURL)

	// TODO: Implement actual gRPC server
	log.Println("TODO: Implement gRPC server with protobuf")

	// Keep server running for demo - create a blocking channel
	done := make(chan bool)
	go func() {
		log.Println("Service running... Press Ctrl+C to stop")
		// Simulate some work
		for {
			log.Println("Service heartbeat...")
			time.Sleep(30 * time.Second)
		}
	}()
	<-done // This will block forever until something sends to done channel
}
