package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/google/uuid"
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

// DTOs for HTTP endpoints
type CreateTripRequest struct {
	PassengerId    string `json:"passengerId"`
	PickupLocation string `json:"pickupLocation"`
	Destination    string `json:"destination"`
}

type CreateTripResponse struct {
	Success bool   `json:"success"`
	TripId  string `json:"tripId"`
	Message string `json:"message"`
}

type ValidateUserRequest struct {
	UserId string `json:"userId"`
}

type ValidateUserResponse struct {
	Valid    bool   `json:"valid"`
	UserType string `json:"userType"`
	Message  string `json:"message"`
}

type TripServer struct {
	springBootURL  string
	userServiceURL string
	httpClient     *http.Client
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

// HTTP endpoint handlers
func (s *TripServer) createTripHandler(w http.ResponseWriter, r *http.Request) {
	log.Println("🚗 Trip gRPC Service: Received createTrip request")

	if r.Method != "POST" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req CreateTripRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		log.Printf("❌ Failed to decode request: %v", err)
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	log.Printf("📍 Trip request: %s → %s for user %s", req.PickupLocation, req.Destination, req.PassengerId)

	// Step 1: Validate user via User Service
	validation, err := s.validateUser(req.PassengerId)
	if err != nil {
		log.Printf("❌ User validation failed: %v", err)
		response := CreateTripResponse{
			Success: false,
			Message: fmt.Sprintf("User validation failed: %v", err),
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response)
		return
	}

	if !validation.Valid {
		log.Printf("❌ User validation rejected: %s", validation.Message)
		response := CreateTripResponse{
			Success: false,
			Message: validation.Message,
		}
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response)
		return
	}

	log.Printf("✅ User validated: %s", validation.UserType)

	// Step 2: Create trip (mock implementation)
	tripId := uuid.New().String()

	log.Printf("✅ Trip created with ID: %s", tripId)

	response := CreateTripResponse{
		Success: true,
		TripId:  tripId,
		Message: "Trip created successfully",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func (s *TripServer) validateUser(userId string) (*ValidateUserResponse, error) {
	log.Printf("📞 Calling User Service to validate user: %s", userId)

	validateReq := ValidateUserRequest{
		UserId: userId,
	}

	jsonData, err := json.Marshal(validateReq)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %v", err)
	}

	url := fmt.Sprintf("%s/api/users/validate", s.userServiceURL)
	resp, err := s.httpClient.Post(url, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("failed to call user service: %v", err)
	}
	defer resp.Body.Close()

	var validateResp ValidateUserResponse
	if err := json.NewDecoder(resp.Body).Decode(&validateResp); err != nil {
		return nil, fmt.Errorf("failed to decode response: %v", err)
	}

	log.Printf("✅ User Service response: valid=%t, type=%s", validateResp.Valid, validateResp.UserType)
	return &validateResp, nil
}

func main() {
	// Load URLs from environment or use defaults
	springBootURL := "http://trip-service:8082"  // trip-service port
	userServiceURL := "http://user-service:8081" // user-service port

	// Initialize TripServer
	server := &TripServer{
		springBootURL:  springBootURL,
		userServiceURL: userServiceURL,
		httpClient:     &http.Client{Timeout: 10 * time.Second},
	}

	// Setup HTTP handlers
	http.HandleFunc("/createTrip", server.createTripHandler)

	log.Println("🚗 Trip gRPC Service starting on port :50052")
	log.Printf("📡 Will call User Service at: %s", userServiceURL)
	log.Printf("📡 Will proxy to Trip Service at: %s", springBootURL)

	// Start HTTP server
	go func() {
		if err := http.ListenAndServe(":50052", nil); err != nil {
			log.Fatalf("Failed to start HTTP server: %v", err)
		}
	}()

	log.Println("✅ Service running on port 50052...")

	// Keep server running
	select {}
}
