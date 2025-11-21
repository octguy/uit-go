package com.example.trip_service.utility;

import com.example.trip_service.aop.RequireUser;
import com.example.trip_service.security.UserContext;

import java.util.UUID;

public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    @RequireUser
    public static UUID getCurrentUserId() {
        return UserContext.getUserId();
    }
}
