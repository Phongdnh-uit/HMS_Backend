package com.hms.medical_exam_service.securities;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts user information from gateway-forwarded headers
 * and stores it in thread-local UserContext for audit purposes.
 * 
 * Headers expected:
 * - X-User-ID: User's unique identifier
 * - X-User-Role: User's role (ADMIN, DOCTOR, NURSE, etc.)
 * - X-User-Email: User's email address
 */
@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-ID");
        String userRole = request.getHeader("X-User-Role");
        String userEmail = request.getHeader("X-User-Email");
        
        if (userId != null) {
            UserContext.User user = new UserContext.User();
            user.setId(userId);
            user.setRole(userRole);
            user.setEmail(userEmail);
            UserContext.setUser(user);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
