package com.example.user_service.service;

import com.example.user_service.dto.CreateUserRequest;
import com.example.user_service.dto.UpdateUserRequest;
import com.example.user_service.dto.UserResponse;
import com.example.user_service.dto.LoginRequest;

import java.util.UUID;

public interface IUserService {

    UserResponse createUser(CreateUserRequest request);

    // New: get user by id
    UserResponse getUserById(UUID id);

    // New: get user by email
    UserResponse getUserByEmail(String email);

    // New: update user by id
    UserResponse updateUser(UUID id, UpdateUserRequest request);

    // Modified: login now returns UserResponse (raw email/password comparison)
    UserResponse login(LoginRequest request);
}
