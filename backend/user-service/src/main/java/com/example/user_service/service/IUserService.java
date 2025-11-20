package com.example.user_service.service;

import com.example.user_service.dto.AuthResponse;
import com.example.user_service.dto.CreateUserRequest;
import com.example.user_service.dto.UserResponse;
import com.example.user_service.dto.LoginRequest;

import java.util.UUID;
import java.util.List;

public interface IUserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(UUID id);

    UserResponse getUserByEmail(String email);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser();

}
