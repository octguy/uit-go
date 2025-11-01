package com.example.user_service.service;

import com.example.user_service.dto.*;
import com.example.user_service.entity.User;
import com.example.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserResponse createUser(CreateUserRequest request) {
        // TODO: Validate user data
        // TODO: Check if email already exists
        // TODO: Create new user entity
        // TODO: Save to database
        // TODO: Return user response
        return null;
    }

    public UserResponse getUserById(Long userId) {
        // TODO: Find user by ID
        // TODO: Handle not found case
        // TODO: Convert to response
        return null;
    }

    public UserResponse getUserById(UUID userId) {
        System.out.println("🔍 UserService: Looking up user with UUID: " + userId);
        
        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                System.out.println("✅ User found: " + user.getEmail());
                return convertToResponse(user);
            } else {
                System.out.println("❌ User not found with UUID: " + userId);
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Error fetching user: " + e.getMessage());
            return null;
        }
    }

    public UserResponse getUserByEmail(String email) {
        // TODO: Find user by email
        // TODO: Handle not found case
        // TODO: Convert to response
        return null;
    }

    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        // TODO: Find user by ID
        // TODO: Update fields if provided
        // TODO: Save changes
        // TODO: Return updated user
        return null;
    }

    public List<UserResponse> getUsersByType(String userType) {
        // TODO: Query users by type (PASSENGER or DRIVER)
        // TODO: Convert to response DTOs
        return null;
    }

    private UserResponse convertToResponse(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setUserType(user.getUserType());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}