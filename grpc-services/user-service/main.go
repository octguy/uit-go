package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
)

// Simple structs for POC
type ValidateUserRequest struct {
	UserID string `json:"userId"`
}

type ValidateUserResponse struct {
	Valid    bool   `json:"valid"`
	UserName string `json:"userName,omitempty"`
	Message  string `json:"message,omitempty"`
}

func main() {
	log.Println("🚀 User gRPC Service (Intermediary) - Port :50051")

	// gRPC-style HTTP handler for validateUser
	http.HandleFunc("/validateUser", handleValidateUser)

	log.Println("✅ Ready to handle gRPC calls and forward to Spring User Service")
	log.Fatal(http.ListenAndServe(":50051", nil))
}

func handleValidateUser(w http.ResponseWriter, r *http.Request) {
	var req ValidateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	log.Printf("📞 gRPC call received: validateUser(userId=%s)", req.UserID)
	log.Printf("🔄 Forwarding to Spring User Service...")

	// Forward request to Spring User Service
	springUserServiceURL := getEnv("SPRING_USER_SERVICE_URL", "http://user-service-tf:8081")

	// Create request to Spring service
	reqBody, _ := json.Marshal(req)
	resp, err := http.Post(fmt.Sprintf("%s/api/users/validate", springUserServiceURL),
		"application/json", bytes.NewBuffer(reqBody))

	if err != nil {
		log.Printf("❌ Failed to call Spring User Service: %v", err)
		http.Error(w, "Failed to validate user", http.StatusInternalServerError)
		return
	}
	defer resp.Body.Close()

	// Forward the response back
	var response ValidateUserResponse
	if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
		log.Printf("❌ Failed to decode Spring service response: %v", err)
		http.Error(w, "Invalid response from user service", http.StatusInternalServerError)
		return
	}

	log.Printf("✅ Response from Spring User Service: valid=%v, userName=%s", response.Valid, response.UserName)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
