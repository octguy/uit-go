package com.example.trip_service.security;

import com.example.trip_service.client.UserClient;
import com.example.trip_service.dto.response.UserValidationResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final UserClient userClient;

    public AuthFilter(UserClient userClient) {
        this.userClient = userClient;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = request.getHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);

                try {
                    UserValidationResponse res = userClient.validate(token);
                    if (res.isValid()) {
                        UserContext.setUserId(res.getUserId());
                        UserContext.setUserRole(res.getRole());
                    }
                } catch (Exception e) {
                    System.err.println("Invalid JWT Token: " + e.getMessage());
                }
            }

            filterChain.doFilter(request, response);
        }
        finally {
            UserContext.clear(); // important to clear the context after the request is processed
        }
    }
}
