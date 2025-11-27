package com.hms.auth_service.securities;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
    public static String getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getId() : null;
    }

    public static boolean isRealAuthenticated() {
        return getCurrentUserDetails() != null;
    }

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return (CustomUserDetails) principal;
        }
        return null;
    }
}
