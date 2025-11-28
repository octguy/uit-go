package com.example.driverservice.utils;

import com.example.driverservice.exception.UnauthorizedException;
import com.example.driverservice.security.UserContext;

import java.util.UUID;

public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static UUID getCurrentUserId() {
        UUID userId = UserContext.getUserId();
        if (userId == null) {
            System.err.println("In SecurityUtil, user id is null");
            throw new UnauthorizedException("User is not authenticated");
        }
        return userId;
    }
}
