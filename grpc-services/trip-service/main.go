package main

import (
	"context"
	"fmt"
	"log"
	"net"

	"google.golang.org/grpc"

	pb "github.com/uit-go/grpc-services/proto/trip"
)

type TripServer struct {
	pb.UnimplementedTripServiceServer
	springBootURL string
}

func (s *TripServer) GetTrip(ctx context.Context, req *pb.GetTripRequest) (*pb.TripResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/trips/%d", s.springBootURL, req.TripId)

	// TODO: Make HTTP GET request to Spring Boot
	// TODO: Parse JSON response
	// TODO: Convert to gRPC response format

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.TripResponse{
		Trip: &pb.TripInfo{
			Id:          req.TripId,
			PassengerId: 0,
			DriverId:    0,
			Status:      "TODO",
		},
	}, nil
}

func (s *TripServer) GetTripsByUser(ctx context.Context, req *pb.GetTripsByUserRequest) (*pb.GetTripsByUserResponse, error) {
	// TODO: Call Spring Boot REST API
	var url string
	if req.UserType == "PASSENGER" {
		url = fmt.Sprintf("%s/api/trips/passenger/%d", s.springBootURL, req.UserId)
	} else if req.UserType == "DRIVER" {
		url = fmt.Sprintf("%s/api/trips/driver/%d", s.springBootURL, req.UserId)
	}

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.GetTripsByUserResponse{
		Trips: []*pb.TripInfo{},
	}, nil
}

func (s *TripServer) UpdateTripStatus(ctx context.Context, req *pb.UpdateTripStatusRequest) (*pb.UpdateTripStatusResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/trips/%d/status", s.springBootURL, req.TripId)

	// TODO: Create HTTP PUT request with JSON body
	// TODO: Handle response

	log.Printf("TODO: PUT to %s", url)

	return &pb.UpdateTripStatusResponse{
		Success: true,
		Message: "TODO: Implement",
	}, nil
}

func main() {
	// TODO: Load Spring Boot URL from environment
	springBootURL := "http://localhost:8082" // trip-service port

	server := &TripServer{
		springBootURL: springBootURL,
	}

	lis, err := net.Listen("tcp", ":50052")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterTripServiceServer(grpcServer, server)

	log.Println("gRPC Trip Service running on :50052")
	log.Printf("Will proxy to Spring Boot at: %s", springBootURL)

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
