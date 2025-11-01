package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"math"
	"net"
	"os"

	"github.com/joho/godotenv"
	_ "github.com/lib/pq"
	"google.golang.org/grpc"

	pb "github.com/uit-go/grpc-services/proto/driver"
)

type DriverServer struct {
	pb.UnimplementedDriverServiceServer
	db *sql.DB
}

func (s *DriverServer) FindNearbyDrivers(ctx context.Context, req *pb.FindNearbyDriversRequest) (*pb.FindNearbyDriversResponse, error) {
	// Using simplified distance calculation for nearby drivers
	query := `
		SELECT id, user_id, vehicle_plate, vehicle_model, status, 
		       current_latitude, current_longitude
		FROM drivers 
		WHERE status = 'AVAILABLE'
		AND current_latitude IS NOT NULL 
		AND current_longitude IS NOT NULL
	`

	rows, err := s.db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("failed to query drivers: %v", err)
	}
	defer rows.Close()

	var drivers []*pb.DriverInfo

	for rows.Next() {
		var driver pb.DriverInfo
		var lat, lng float64

		err := rows.Scan(&driver.Id, &driver.UserId, &driver.VehiclePlate,
			&driver.VehicleModel, &driver.Status, &lat, &lng)
		if err != nil {
			continue
		}

		// Calculate distance using Haversine formula
		distance := calculateDistance(req.Latitude, req.Longitude, lat, lng)

		if distance <= req.RadiusKm {
			driver.CurrentLatitude = lat
			driver.CurrentLongitude = lng
			drivers = append(drivers, &driver)
		}
	}

	return &pb.FindNearbyDriversResponse{
		Drivers: drivers,
	}, nil
}

func (s *DriverServer) GetDriverStatus(ctx context.Context, req *pb.GetDriverStatusRequest) (*pb.GetDriverStatusResponse, error) {
	var status string
	var lat, lng float64

	query := `SELECT status, current_latitude, current_longitude FROM drivers WHERE id = $1`
	row := s.db.QueryRow(query, req.DriverId)

	err := row.Scan(&status, &lat, &lng)
	if err != nil {
		return nil, fmt.Errorf("driver not found: %v", err)
	}

	return &pb.GetDriverStatusResponse{
		DriverId:         req.DriverId,
		Status:           status,
		CurrentLatitude:  lat,
		CurrentLongitude: lng,
	}, nil
}

func (s *DriverServer) UpdateDriverLocation(ctx context.Context, req *pb.UpdateDriverLocationRequest) (*pb.UpdateDriverLocationResponse, error) {
	query := `UPDATE drivers SET current_latitude = $1, current_longitude = $2, last_updated = NOW() WHERE id = $3`

	result, err := s.db.Exec(query, req.Latitude, req.Longitude, req.DriverId)
	if err != nil {
		return &pb.UpdateDriverLocationResponse{
			Success: false,
			Message: fmt.Sprintf("Failed to update location: %v", err),
		}, nil
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return &pb.UpdateDriverLocationResponse{
			Success: false,
			Message: "Driver not found",
		}, nil
	}

	return &pb.UpdateDriverLocationResponse{
		Success: true,
		Message: "Location updated successfully",
	}, nil
}

// calculateDistance calculates the distance between two points using Haversine formula
func calculateDistance(lat1, lon1, lat2, lon2 float64) float64 {
	const R = 6371 // Earth's radius in kilometers

	dLat := (lat2 - lat1) * math.Pi / 180
	dLon := (lon2 - lon1) * math.Pi / 180

	a := math.Sin(dLat/2)*math.Sin(dLat/2) +
		math.Cos(lat1*math.Pi/180)*math.Cos(lat2*math.Pi/180)*
			math.Sin(dLon/2)*math.Sin(dLon/2)

	c := 2 * math.Atan2(math.Sqrt(a), math.Sqrt(1-a))

	return R * c
}

func main() {
	// Load environment variables
	godotenv.Load()

	// Database connection
	dbHost := getEnv("DB_HOST", "localhost")
	dbPort := getEnv("DB_PORT", "5434")
	dbUser := getEnv("DB_USER", "postgres")
	dbPassword := getEnv("DB_PASSWORD", "password")
	dbName := getEnv("DB_NAME", "driver_service")

	connStr := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		dbHost, dbPort, dbUser, dbPassword, dbName)

	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	// Test database connection
	if err := db.Ping(); err != nil {
		log.Fatalf("Failed to ping database: %v", err)
	}

	// Create gRPC server
	lis, err := net.Listen("tcp", ":50052")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterDriverServiceServer(s, &DriverServer{db: db})

	log.Println("Driver gRPC server starting on port 50052...")
	if err := s.Serve(lis); err != nil {
		log.Fatalf("Failed to serve: %v", err)
	}
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
