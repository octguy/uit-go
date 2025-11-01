package main

import (
	"context"
	"fmt"
	"log"
	"net"

	"google.golang.org/grpc"

	pb "github.com/uit-go/grpc-services/proto/driver"
)

type DriverServer struct {
	pb.UnimplementedDriverServiceServer
	springBootURL string
}

func (s *DriverServer) FindNearbyDrivers(ctx context.Context, req *pb.FindNearbyDriversRequest) (*pb.FindNearbyDriversResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/drivers/nearby?latitude=%f&longitude=%f&radiusKm=%f",
		s.springBootURL, req.Latitude, req.Longitude, req.RadiusKm)

	// TODO: Make HTTP GET request to Spring Boot
	// TODO: Parse JSON response
	// TODO: Convert to gRPC response format

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.FindNearbyDriversResponse{
		Drivers: []*pb.DriverInfo{},
	}, nil
}

func (s *DriverServer) GetDriverStatus(ctx context.Context, req *pb.GetDriverStatusRequest) (*pb.GetDriverStatusResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/drivers/%d", s.springBootURL, req.DriverId)

	log.Printf("TODO: Call %s", url)

	// Placeholder response
	return &pb.GetDriverStatusResponse{
		DriverId:         req.DriverId,
		Status:           "TODO",
		CurrentLatitude:  0.0,
		CurrentLongitude: 0.0,
	}, nil
}

func (s *DriverServer) UpdateDriverLocation(ctx context.Context, req *pb.UpdateDriverLocationRequest) (*pb.UpdateDriverLocationResponse, error) {
	// TODO: Call Spring Boot REST API
	url := fmt.Sprintf("%s/api/drivers/%d/location", s.springBootURL, req.DriverId)

	// TODO: Create HTTP PUT request with JSON body
	// TODO: Handle response

	log.Printf("TODO: PUT to %s", url)

	return &pb.UpdateDriverLocationResponse{
		Success: true,
		Message: "TODO: Implement",
	}, nil
}

func main() {
	// TODO: Load Spring Boot URL from environment
	springBootURL := "http://localhost:8083" // driver-service port

	server := &DriverServer{
		springBootURL: springBootURL,
	}

	lis, err := net.Listen("tcp", ":50053")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	pb.RegisterDriverServiceServer(grpcServer, server)

	log.Println("gRPC Driver Service running on :50053")
	log.Printf("Will proxy to Spring Boot at: %s", springBootURL)

	if err := grpcServer.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}
