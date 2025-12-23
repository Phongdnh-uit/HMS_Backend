package com.hms.common.exceptions;

import java.util.Map;
import java.util.stream.Collectors;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(1) // High priority to ensure these handlers run before any others
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ex.getErrorCode().getCode());
        response.setMessage(ex.getMessage() != null ? ex.getMessage() : ex.getErrorCode().getMessage());
        response.setErrors(ex.getFieldErrors());
        return ResponseEntity.status(ex.getErrorCode().getHttpCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        Map<String, String> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.toMap(
                                        FieldError::getField,
                                        org.springframework.validation.FieldError::getDefaultMessage,
                                        (existing, _) -> existing));
        response.setCode(ErrorCode.VALIDATION_ERROR.getCode());
        response.setMessage(ErrorCode.VALIDATION_ERROR.getMessage());
        response.setErrors(fieldErrors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpCode()).body(response);
    }

    /**
     * Handle constraint violations from @Validated class-level validation.
     * Returns HTTP 400 BAD_REQUEST with validation error details.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        cv -> cv.getMessage(),
                        (existing, _) -> existing));
        response.setCode(ErrorCode.VALIDATION_ERROR.getCode());
        response.setMessage(ErrorCode.VALIDATION_ERROR.getMessage());
        response.setErrors(fieldErrors);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpCode()).body(response);
    }

    /**
     * Handle database constraint violations (duplicate key, foreign key violations).
     * Returns HTTP 409 CONFLICT for unique constraint violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.RESOURCE_EXISTS.getCode());
        
        // Extract a user-friendly message from the constraint violation
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            response.setMessage("Resource already exists: duplicate value detected");
        } else if (message != null && message.contains("foreign key constraint")) {
            response.setCode(ErrorCode.VALIDATION_ERROR.getCode());
            response.setMessage("Referenced resource does not exist");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } else {
            response.setMessage(ErrorCode.RESOURCE_EXISTS.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle all other uncaught exceptions.
     * Returns HTTP 500 INTERNAL_SERVER_ERROR.
     */
    @ExceptionHandler(Exception.class)
    @Order(Integer.MAX_VALUE) // Lowest priority - catch-all handler
    public ResponseEntity<ApiResponse<Void>> handleUncatchException(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        response.setMessage(
                ex.getMessage() == null || ex.getMessage().isEmpty() 
                    ? ErrorCode.INTERNAL_SERVER_ERROR.getMessage() 
                    : ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

