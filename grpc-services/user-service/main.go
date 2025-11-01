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

	pb "github.com/uit-go/grpc-services/proto/user"
)

type UserServer struct {
	pb.UnimplementedUserServiceServer
	db *sql.DB
}

func (s *UserServer) GetUser(ctx context.Context, req *pb.GetUserRequest) (*pb.UserResponse, error) {
	var user pb.UserResponse

	query := `SELECT id, email, name, user_type, phone, created_at FROM users WHERE id = $1`
	row := s.db.QueryRow(query, req.UserId)

	var createdAt sql.NullTime
	var phone sql.NullString

	err := row.Scan(&user.Id, &user.Email, &user.Name, &user.UserType, &phone, &createdAt)
	if err != nil {
		return nil, fmt.Errorf("user not found: %v", err)
	}

	if phone.Valid {
		user.Phone = phone.String
	}
	if createdAt.Valid {
		user.CreatedAt = createdAt.Time.Format("2006-01-02T15:04:05Z")
	}

	return &user, nil
}

func (s *UserServer) ValidateUser(ctx context.Context, req *pb.ValidateUserRequest) (*pb.ValidateUserResponse, error) {
	var userID int64
	var userType string

	query := `SELECT id, user_type FROM users WHERE email = $1`
	row := s.db.QueryRow(query, req.Email)

	err := row.Scan(&userID, &userType)
	if err != nil {
		return &pb.ValidateUserResponse{
			Valid: false,
		}, nil
	}

	return &pb.ValidateUserResponse{
		Valid:    true,
		UserId:   userID,
		UserType: userType,
	}, nil
}

func (s *UserServer) GetUsersByType(ctx context.Context, req *pb.GetUsersByTypeRequest) (*pb.GetUsersByTypeResponse, error) {
	query := `SELECT id, email, name, user_type, phone, created_at FROM users WHERE user_type = $1`
	rows, err := s.db.Query(query, req.UserType)
	if err != nil {
		return nil, fmt.Errorf("failed to query users: %v", err)
	}
	defer rows.Close()

	var users []*pb.UserResponse

	for rows.Next() {
		var user pb.UserResponse
		var createdAt sql.NullTime
		var phone sql.NullString

		err := rows.Scan(&user.Id, &user.Email, &user.Name, &user.UserType, &phone, &createdAt)
		if err != nil {
			continue
		}

		if phone.Valid {
			user.Phone = phone.String
		}
		if createdAt.Valid {
			user.CreatedAt = createdAt.Time.Format("2006-01-02T15:04:05Z")
		}

		users = append(users, &user)
	}

	return &pb.GetUsersByTypeResponse{
		Users: users,
	}, nil
}

func main() {
	// Load environment variables
	godotenv.Load()

	// Database connection
	dbHost := getEnv("DB_HOST", "localhost")
	dbPort := getEnv("DB_PORT", "5435")
	dbUser := getEnv("DB_USER", "postgres")
	dbPassword := getEnv("DB_PASSWORD", "password")
	dbName := getEnv("DB_NAME", "user_service")

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
	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("Failed to listen: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterUserServiceServer(s, &UserServer{db: db})

	log.Println("User gRPC server starting on port 50051...")
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
