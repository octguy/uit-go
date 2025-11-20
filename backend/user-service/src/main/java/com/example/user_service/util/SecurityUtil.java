package com.example.user_service.util;

import com.example.user_service.entity.CustomUserDetails;
import com.example.user_service.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    private SecurityUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        return customUserDetails.getUser();
    }
}
