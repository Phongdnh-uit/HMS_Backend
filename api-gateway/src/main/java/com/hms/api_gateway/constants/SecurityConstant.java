package com.hms.api_gateway.constants;

public interface SecurityConstant {
    String[] PUBLIC_URLS = {
            // Frontend-friendly API paths (gateway adds /api prefix via routes)
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            // Password reset endpoints (no auth required - for forgot password flow)
            "/api/auth/send-password-reset-token",
            "/api/auth/reset-password",
            // Email verification endpoints (no auth required - for email verification flow)
            "/api/auth/send-verification-email",
            "/api/auth/verify-email",
            // Direct service access paths (for service-to-service communication)
            "/auth-service/auth/login",
            "/auth-service/auth/register",
            "/auth-service/auth/refresh",
            "/auth-service/auth/logout",
            "/auth-service/auth/send-password-reset-token",
            "/auth-service/auth/reset-password",
            "/auth-service/auth/send-verification-email",
            "/auth-service/auth/verify-email",
            // Actuator health endpoints for monitoring
            "/actuator/health",
            "/actuator/health/**",
            // Lab test image downloads - public so browser can access without JWT
            "/api/exams/lab-results/images/*/download",
            // Swagger UI and OpenAPI documentation endpoints
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            // Service-specific OpenAPI docs (for aggregation)
            "/api/*/v3/api-docs",
            "/*/v3/api-docs",
    };
}


