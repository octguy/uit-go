package com.example.user_service.service;

import com.example.user_service.dto.response.AuthResponse;
import com.example.user_service.dto.request.CreateUserRequest;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.dto.request.LoginRequest;

import java.util.UUID;

public interface IUserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    UserResponse getUserByEmail(String email);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser();

}
