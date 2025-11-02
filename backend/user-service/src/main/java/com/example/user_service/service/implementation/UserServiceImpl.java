package com.example.user_service.service.implementation;

import com.example.user_service.dto.CreateUserRequest;
import com.example.user_service.dto.UpdateUserRequest;
import com.example.user_service.dto.UserResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.entity.User;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.IUserService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();

        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPassword(request.getPassword());
        user.setUserType(request.getUserType());

        user = userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPassword() != null) {
            user.setPassword(request.getPassword());
        }
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }

        user = userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse login(LoginRequest request) {
        Optional<User> maybeUser = userRepository.findByEmail(request.getEmail());
        if (maybeUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = maybeUser.get();
        if (user.getPassword() == null || !user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Successful login -> return UserResponse
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
