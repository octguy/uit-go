package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"net"
	"os"

	"github.com/joho/godotenv"
	_ "github.com/lib/pq"
	"google.golang.org/grpc"

	pb "github.com/uit-go/grpc-services/proto/trip"
)

type TripServer struct {
	pb.UnimplementedTripServiceServer
	db *sql.DB
}

func (s *TripServer) GetTrip(ctx context.Context, req *pb.GetTripRequest) (*pb.TripResponse, error) {
	var trip pb.TripInfo

	query := `
		SELECT id, passenger_id, driver_id, status, pickup_location, destination,
		       pickup_latitude, pickup_longitude, destination_latitude, destination_longitude,
		       fare, created_at, updated_at
		FROM trips WHERE id = $1
	`

	row := s.db.QueryRow(query, req.TripId)

	var driverID sql.NullInt64
	var fare sql.NullFloat64
	var createdAt, updatedAt sql.NullTime

	err := row.Scan(&trip.Id, &trip.PassengerId, &driverID, &trip.Status,
		&trip.PickupLocation, &trip.Destination,
		&trip.PickupLatitude, &trip.PickupLongitude,
		&trip.DestinationLatitude, &trip.DestinationLongitude,
		&fare, &createdAt, &updatedAt)
	if err != nil {
		return nil, fmt.Errorf("trip not found: %v", err)
	}

	if driverID.Valid {
		trip.DriverId = driverID.Int64
	}
	if fare.Valid {
		trip.Fare = fare.Float64
	}
	if createdAt.Valid {
		trip.CreatedAt = createdAt.Time.Format("2006-01-02T15:04:05Z")
	}
	if updatedAt.Valid {
		trip.UpdatedAt = updatedAt.Time.Format("2006-01-02T15:04:05Z")
	}

	return &pb.TripResponse{
		Trip: &trip,
	}, nil
}

func (s *TripServer) GetTripsByUser(ctx context.Context, req *pb.GetTripsByUserRequest) (*pb.GetTripsByUserResponse, error) {
	var query string
	var args []interface{}

	if req.UserType == "PASSENGER" {
		query = `
			SELECT id, passenger_id, driver_id, status, pickup_location, destination,
			       pickup_latitude, pickup_longitude, destination_latitude, destination_longitude,
			       fare, created_at, updated_at
			FROM trips WHERE passenger_id = $1
			ORDER BY created_at DESC
		`
		args = []interface{}{req.UserId}
	} else if req.UserType == "DRIVER" {
		query = `
			SELECT id, passenger_id, driver_id, status, pickup_location, destination,
			       pickup_latitude, pickup_longitude, destination_latitude, destination_longitude,
			       fare, created_at, updated_at
			FROM trips WHERE driver_id = $1
			ORDER BY created_at DESC
		`
		args = []interface{}{req.UserId}
	} else {
		return nil, fmt.Errorf("invalid user type: %s", req.UserType)
	}

	rows, err := s.db.Query(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to query trips: %v", err)
	}
	defer rows.Close()

	var trips []*pb.TripInfo

	for rows.Next() {
		var trip pb.TripInfo
		var driverID sql.NullInt64
		var fare sql.NullFloat64
		var createdAt, updatedAt sql.NullTime

		err := rows.Scan(&trip.Id, &trip.PassengerId, &driverID, &trip.Status,
			&trip.PickupLocation, &trip.Destination,
			&trip.PickupLatitude, &trip.PickupLongitude,
			&trip.DestinationLatitude, &trip.DestinationLongitude,
			&fare, &createdAt, &updatedAt)
		if err != nil {
			continue
		}

		if driverID.Valid {
			trip.DriverId = driverID.Int64
		}
		if fare.Valid {
			trip.Fare = fare.Float64
		}
		if createdAt.Valid {
			trip.CreatedAt = createdAt.Time.Format("2006-01-02T15:04:05Z")
		}
		if updatedAt.Valid {
			trip.UpdatedAt = updatedAt.Time.Format("2006-01-02T15:04:05Z")
		}

		trips = append(trips, &trip)
	}

	return &pb.GetTripsByUserResponse{
		Trips: trips,
	}, nil
}

func (s *TripServer) UpdateTripStatus(ctx context.Context, req *pb.UpdateTripStatusRequest) (*pb.UpdateTripStatusResponse, error) {
	query := `UPDATE trips SET status = $1, updated_at = NOW() WHERE id = $2`

	result, err := s.db.Exec(query, req.Status, req.TripId)
	if err != nil {
		return &pb.UpdateTripStatusResponse{
			Success: false,
			Message: fmt.Sprintf("Failed to update trip status: %v", err),
		}, nil
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return &pb.UpdateTripStatusResponse{
			Success: false,
			Message: "Trip not found",
		}, nil
	}

	return &pb.UpdateTripStatusResponse{
		Success: true,
		Message: "Trip status updated successfully",
	}, nil
}

func main() {
	// Load environment variables
	godotenv.Load()

	// Database connection
	dbHost := getEnv("DB_HOST", "localhost")
	dbPort := getEnv("DB_PORT", "5433")
	dbUser := getEnv("DB_USER", "postgres")
	dbPassword := getEnv("DB_PASSWORD", "password")
	dbName := getEnv("DB_NAME", "trip_service")

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
	lis, err := net.Listen("tcp", ":50053")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterTripServiceServer(s, &TripServer{db: db})

	log.Println("Trip gRPC server starting on port 50053...")
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
