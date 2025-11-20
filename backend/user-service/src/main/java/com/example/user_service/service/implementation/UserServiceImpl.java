package com.example.user_service.service.implementation;

import com.example.user_service.dto.CreateUserRequest;
import com.example.user_service.dto.UserResponse;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.entity.User;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.IUserService;
import com.example.user_service.util.SecurityUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        User user = new User();

        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user = userRepository.save(user);

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
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
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserResponse getCurrentUser() {
        User user = SecurityUtil.getCurrentUser();

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
