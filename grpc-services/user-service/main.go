package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"

	_ "github.com/lib/pq"
)

// Simple structs for POC
type ValidateUserRequest struct {
	UserID string `json:"userId"`
}

type ValidateUserResponse struct {
	Valid    bool   `json:"valid"`
	UserType string `json:"userType,omitempty"`
	Message  string `json:"message,omitempty"`
}

var db *sql.DB

func main() {
	log.Println("🚀 User gRPC Service (Pattern 2 POC) - Port :50051")

	// Connect to database
	initDB()

	// gRPC-style HTTP handler for validateUser
	http.HandleFunc("/validateUser", handleValidateUser)

	log.Println("✅ Ready to handle gRPC calls from Trip Service")
	log.Fatal(http.ListenAndServe(":50051", nil))
}

func initDB() {
	dbHost := getEnv("DB_HOST", "user-service-db")
	dbUser := getEnv("DB_USER", "user_service_user")
	dbPassword := getEnv("DB_PASSWORD", "user_service_pass")
	dbName := getEnv("DB_NAME", "user_service_db")

	connStr := fmt.Sprintf("host=%s user=%s password=%s dbname=%s sslmode=disable",
		dbHost, dbUser, dbPassword, dbName)

	var err error
	db, err = sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("❌ Failed to connect to database: %v", err)
	}

	log.Println("✅ Connected to User database")
}

func handleValidateUser(w http.ResponseWriter, r *http.Request) {
	var req ValidateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	log.Printf("📞 gRPC call: validateUser(userId=%s)", req.UserID)

	// Query database
	var userType string
	err := db.QueryRow("SELECT user_type FROM users WHERE id = $1", req.UserID).Scan(&userType)

	var response ValidateUserResponse
	if err == sql.ErrNoRows {
		response = ValidateUserResponse{
			Valid:   false,
			Message: "User not found",
		}
		log.Printf("❌ User %s not found", req.UserID)
	} else if err != nil {
		log.Printf("❌ Database error: %v", err)
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	} else {
		response = ValidateUserResponse{
			Valid:    true,
			UserType: userType,
			Message:  "User validated successfully",
		}
		log.Printf("✅ User %s validated: %s", req.UserID, userType)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
