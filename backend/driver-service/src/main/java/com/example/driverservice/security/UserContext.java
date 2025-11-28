package com.example.driverservice.security;

import java.util.UUID;

public class UserContext {

    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserRole = new ThreadLocal<>();

    public static void setUserId(UUID id) {
        currentUserId.set(id);
    }

    public static void setUserRole(String role) {
        currentUserRole.set(role);
    }

    public static UUID getUserId() {
        return currentUserId.get();
    }

    public static String getUserRole() {
        return currentUserRole.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentUserRole.remove();
    }
}
