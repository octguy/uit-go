package com.example.user_service.service;

import com.example.user_service.dto.*;
import com.example.user_service.entity.User;
import com.example.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        // TODO: Map entity fields to DTO
        return null;
    }
}