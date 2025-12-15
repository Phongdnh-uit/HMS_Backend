package com.hms.medical_exam_service.securities;

import lombok.Getter;
import lombok.Setter;

/**
 * Thread-local holder for user context information.
 * Populated by UserContextFilter from gateway-forwarded headers.
 */
public class UserContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void setUser(User user) {
        currentUser.set(user);
    }

    public static User getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }

    @Getter
    @Setter
    public static class User {
        private String id;
        private String role;
        private String email;
    }
}
