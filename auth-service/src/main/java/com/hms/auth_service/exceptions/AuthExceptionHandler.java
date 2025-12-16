package com.hms.auth_service.exceptions;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Auth-service specific exception handler.
 * Handles authentication-related exceptions that require spring-security dependency.
 * This is separate from GlobalExceptionHandler (in common module) because common 
 * doesn't have spring-security dependency.
 */
@RestControllerAdvice
@Order(0) // Higher priority than GlobalExceptionHandler (@Order(1))
public class AuthExceptionHandler {

    /**
     * Handle bad credentials (wrong email or password).
     * Returns HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.INVALID_CREDENTIALS.getCode());
        response.setMessage("Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle internal authentication errors (e.g., user not found during authentication).
     * Returns HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalAuthError(InternalAuthenticationServiceException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.INVALID_CREDENTIALS.getCode());
        response.setMessage("Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle all other authentication exceptions (parent class).
     * This catches any authentication failure that isn't specifically handled above.
     * Returns HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.INVALID_CREDENTIALS.getCode());
        response.setMessage("Authentication failed: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}

