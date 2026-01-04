package com.hms.api_gateway.constants;

public interface SecurityConstant {
    String[] PUBLIC_URLS = {
            // Frontend-friendly API paths (gateway adds /api prefix via routes)
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            // Direct service access paths (for service-to-service communication)
            "/auth-service/auth/login",
            "/auth-service/auth/register",
            "/auth-service/auth/refresh",
            "/auth-service/auth/logout",
            // Actuator health endpoints for monitoring
            "/actuator/health",
            "/actuator/health/**",
            // Lab test image downloads - public so browser can access without JWT
            "/api/exams/lab-results/images/*/download",
    };
}


