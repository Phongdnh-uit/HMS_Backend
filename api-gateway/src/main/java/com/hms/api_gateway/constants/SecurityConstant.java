package com.hms.api_gateway.constants;

public interface SecurityConstant {
    String[] PUBLIC_URLS = {
            // Frontend-friendly API paths (gateway adds /api prefix via routes)
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            // Direct service access paths (for service-to-service communication)
            "/auth-service/auth/login",
            "/auth-service/auth/register",
            "/auth-service/auth/refresh",
            // Public read-only access to medicines and categories
            "/api/medicines",
            "/api/medicines/**",
            "/api/medicines/categories",
            "/api/medicines/categories/**",
    };
}
