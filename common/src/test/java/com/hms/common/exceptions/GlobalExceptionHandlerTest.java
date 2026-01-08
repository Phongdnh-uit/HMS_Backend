package com.hms.common.exceptions;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests exception handling and error response formatting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-CMN-007: GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Nested
    @DisplayName("Method: handleApiException()")
    class HandleApiExceptionTests {

        @Test
        @DisplayName("UC-CMN-007: Should handle ApiException with error code")
        void handleApiException_withErrorCode_shouldReturnFormattedResponse() {
            // Given
            ApiException exception = new ApiException(ErrorCode.RESOURCE_NOT_FOUND);

            // When
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleApiException(exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
            assertThat(response.getBody().getMessage()).isEqualTo("Resource Not Found");
            assertThat(response.getBody().getErrors()).isNull();
        }

        @Test
        @DisplayName("Should handle ApiException with custom message")
        void handleApiException_withCustomMessage_shouldUseCustomMessage() {
            // Given
            String customMessage = "Patient with ID 123 not found";
            ApiException exception = new ApiException(ErrorCode.RESOURCE_NOT_FOUND, customMessage);

            // When
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleApiException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody().getMessage()).isEqualTo(customMessage);
        }

        @Test
        @DisplayName("Should handle ApiException with field errors")
        void handleApiException_withFieldErrors_shouldIncludeFieldErrors() {
            // Given
            Map<String, String> fieldErrors = Map.of(
                    "email", "Email is required",
                    "password", "Password too short"
            );
            ApiException exception = new ApiException(ErrorCode.VALIDATION_ERROR, fieldErrors);

            // When
            ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleApiException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
            assertThat(response.getBody().getErrors()).isEqualTo(fieldErrors);
            assertThat(response.getBody().getErrors()).containsEntry("email", "Email is required");
        }

        @Test
        @DisplayName("Should handle different error codes with correct HTTP status")
        void handleApiException_withDifferentErrorCodes_shouldMapCorrectHttpStatus() {
            // Test UNAUTHORIZED
            ApiException unauthorizedException = new ApiException(ErrorCode.AUTHENTICATION_REQUIRED);
            ResponseEntity<ApiResponse<Void>> unauthorizedResponse = 
                    exceptionHandler.handleApiException(unauthorizedException);
            assertThat(unauthorizedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

            // Test FORBIDDEN
            ApiException forbiddenException = new ApiException(ErrorCode.FORBIDDEN);
            ResponseEntity<ApiResponse<Void>> forbiddenResponse = 
                    exceptionHandler.handleApiException(forbiddenException);
            assertThat(forbiddenResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            // Test CONFLICT
            ApiException conflictException = new ApiException(ErrorCode.RESOURCE_EXISTS);
            ResponseEntity<ApiResponse<Void>> conflictResponse = 
                    exceptionHandler.handleApiException(conflictException);
            assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("Method: handleValidationException()")
    class HandleValidationExceptionTests {

        @Test
        @DisplayName("UC-CMN-007: Should handle MethodArgumentNotValidException")
        void handleValidationException_withFieldErrors_shouldReturnValidationErrors() {
            // Given
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            org.springframework.validation.BindingResult bindingResult = 
                    mock(org.springframework.validation.BindingResult.class);
            
            FieldError fieldError1 = new FieldError("user", "email", "Email is required");
            FieldError fieldError2 = new FieldError("user", "age", "Age must be positive");
            
            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(java.util.List.of(fieldError1, fieldError2));

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleValidationException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
            assertThat(response.getBody().getMessage()).isEqualTo("Validation Error");
            assertThat(response.getBody().getErrors()).hasSize(2);
            assertThat(response.getBody().getErrors()).containsEntry("email", "Email is required");
            assertThat(response.getBody().getErrors()).containsEntry("age", "Age must be positive");
        }

        @Test
        @DisplayName("Should handle duplicate field errors by keeping first occurrence")
        void handleValidationException_withDuplicateFields_shouldKeepFirstError() {
            // Given
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            org.springframework.validation.BindingResult bindingResult = 
                    mock(org.springframework.validation.BindingResult.class);
            
            FieldError error1 = new FieldError("user", "email", "First error");
            FieldError error2 = new FieldError("user", "email", "Second error");
            
            given(exception.getBindingResult()).willReturn(bindingResult);
            given(bindingResult.getFieldErrors()).willReturn(java.util.List.of(error1, error2));

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleValidationException(exception);

            // Then
            assertThat(response.getBody().getErrors()).containsEntry("email", "First error");
            assertThat(response.getBody().getErrors()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Method: handleConstraintViolation()")
    class HandleConstraintViolationTests {

        @Test
        @DisplayName("UC-CMN-007: Should handle ConstraintViolationException")
        void handleConstraintViolation_withViolations_shouldReturnValidationErrors() {
            // Given
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            
            ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
            Path path1 = mock(Path.class);
            given(violation1.getPropertyPath()).willReturn(path1);
            given(path1.toString()).willReturn("email");
            given(violation1.getMessage()).willReturn("must be a valid email");
            
            ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
            Path path2 = mock(Path.class);
            given(violation2.getPropertyPath()).willReturn(path2);
            given(path2.toString()).willReturn("age");
            given(violation2.getMessage()).willReturn("must be at least 18");
            
            violations.add(violation1);
            violations.add(violation2);
            
            ConstraintViolationException exception = new ConstraintViolationException(violations);

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleConstraintViolation(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
            assertThat(response.getBody().getErrors()).hasSize(2);
            assertThat(response.getBody().getErrors()).containsEntry("email", "must be a valid email");
        }
    }

    @Nested
    @DisplayName("Method: handleDataIntegrityViolation()")
    class HandleDataIntegrityViolationTests {

        @Test
        @DisplayName("UC-CMN-007: Should handle duplicate entry constraint violation")
        void handleDataIntegrityViolation_withDuplicateEntry_shouldReturnConflict() {
            // Given
            Exception cause = new Exception("Duplicate entry 'test@example.com' for key 'email'");
            DataIntegrityViolationException exception = new DataIntegrityViolationException("", cause);

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleDataIntegrityViolation(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_EXISTS.getCode());
            assertThat(response.getBody().getMessage()).contains("duplicate value detected");
        }

        @Test
        @DisplayName("Should handle foreign key constraint violation")
        void handleDataIntegrityViolation_withForeignKey_shouldReturnBadRequest() {
            // Given
            Exception cause = new Exception("foreign key constraint fails");
            DataIntegrityViolationException exception = new DataIntegrityViolationException("", cause);

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleDataIntegrityViolation(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
            assertThat(response.getBody().getMessage()).contains("Referenced resource does not exist");
        }

        @Test
        @DisplayName("Should handle generic data integrity violation")
        void handleDataIntegrityViolation_withGenericViolation_shouldReturnConflict() {
            // Given
            Exception cause = new Exception("Some other constraint violation");
            DataIntegrityViolationException exception = new DataIntegrityViolationException("", cause);

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleDataIntegrityViolation(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_EXISTS.getCode());
        }
    }

    @Nested
    @DisplayName("Method: handleUncatchException()")
    class HandleUncatchExceptionTests {

        @Test
        @DisplayName("UC-CMN-007: Should handle unexpected exceptions")
        void handleUncatchException_withGenericException_shouldReturnInternalServerError() {
            // Given
            Exception exception = new RuntimeException("Unexpected error occurred");

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleUncatchException(exception);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error occurred");
        }

        @Test
        @DisplayName("Should handle exception with null message")
        void handleUncatchException_withNullMessage_shouldUseDefaultMessage() {
            // Given
            Exception exception = new RuntimeException();

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleUncatchException(exception);

            // Then
            assertThat(response.getBody().getMessage()).isEqualTo("Internal Server Error");
        }

        @Test
        @DisplayName("Should handle exception with empty message")
        void handleUncatchException_withEmptyMessage_shouldUseDefaultMessage() {
            // Given
            Exception exception = new RuntimeException("");

            // When
            ResponseEntity<ApiResponse<Void>> response = 
                    exceptionHandler.handleUncatchException(exception);

            // Then
            assertThat(response.getBody().getMessage()).isEqualTo("Internal Server Error");
        }
    }
}
