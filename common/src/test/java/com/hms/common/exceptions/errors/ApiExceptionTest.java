package com.hms.common.exceptions.errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApiException.
 * Tests exception creation with various error codes and field errors.
 */
@DisplayName("UC-CMN-006: ApiException Unit Tests")
class ApiExceptionTest {

    @Nested
    @DisplayName("Constructor: ApiException(ErrorCode)")
    class ConstructorWithErrorCodeTests {

        @Test
        @DisplayName("UC-CMN-006: Should create exception with error code")
        void constructor_withErrorCode_shouldCreateException() {
            // When
            ApiException exception = new ApiException(ErrorCode.RESOURCE_NOT_FOUND);

            // Then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
            assertThat(exception.getFieldErrors()).isNull();
        }

        @Test
        @DisplayName("Should handle validation error code")
        void constructor_withValidationError_shouldCreateException() {
            // When
            ApiException exception = new ApiException(ErrorCode.VALIDATION_ERROR);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(exception.getMessage()).isEqualTo("Validation Error");
        }
    }

    @Nested
    @DisplayName("Constructor: ApiException(ErrorCode, String)")
    class ConstructorWithCustomMessageTests {

        @Test
        @DisplayName("UC-CMN-006: Should create exception with custom message")
        void constructor_withCustomMessage_shouldOverrideDefaultMessage() {
            // Given
            String customMessage = "Patient with ID 123 not found";

            // When
            ApiException exception = new ApiException(ErrorCode.RESOURCE_NOT_FOUND, customMessage);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(customMessage);
            assertThat(exception.getFieldErrors()).isNull();
        }

        @Test
        @DisplayName("Should handle empty custom message")
        void constructor_withEmptyMessage_shouldAcceptEmptyMessage() {
            // When
            ApiException exception = new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "");

            // Then
            assertThat(exception.getMessage()).isEmpty();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("Constructor: ApiException(ErrorCode, Map<String, String>)")
    class ConstructorWithFieldErrorsTests {

        @Test
        @DisplayName("UC-CMN-006: Should create exception with field errors")
        void constructor_withFieldErrors_shouldIncludeValidationErrors() {
            // Given
            Map<String, String> fieldErrors = Map.of(
                    "email", "Email is required",
                    "password", "Password must be at least 8 characters"
            );

            // When
            ApiException exception = new ApiException(ErrorCode.VALIDATION_ERROR, fieldErrors);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(exception.getMessage()).isEqualTo(ErrorCode.VALIDATION_ERROR.getMessage());
            assertThat(exception.getFieldErrors()).isEqualTo(fieldErrors);
            assertThat(exception.getFieldErrors()).containsEntry("email", "Email is required");
        }

        @Test
        @DisplayName("Should handle empty field errors map")
        void constructor_withEmptyFieldErrors_shouldAcceptEmptyMap() {
            // Given
            Map<String, String> emptyErrors = Map.of();

            // When
            ApiException exception = new ApiException(ErrorCode.VALIDATION_ERROR, emptyErrors);

            // Then
            assertThat(exception.getFieldErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Constructor: ApiException(ErrorCode, String, Map<String, String>)")
    class ConstructorWithMessageAndFieldErrorsTests {

        @Test
        @DisplayName("UC-CMN-006: Should create exception with custom message and field errors")
        void constructor_withMessageAndFieldErrors_shouldIncludeBoth() {
            // Given
            String customMessage = "Validation failed for user registration";
            Map<String, String> fieldErrors = Map.of(
                    "username", "Username already exists",
                    "email", "Invalid email format"
            );

            // When
            ApiException exception = new ApiException(ErrorCode.VALIDATION_ERROR, customMessage, fieldErrors);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(exception.getMessage()).isEqualTo(customMessage);
            assertThat(exception.getFieldErrors()).isEqualTo(fieldErrors);
            assertThat(exception.getFieldErrors()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Exception Behavior")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("Should be throwable as RuntimeException")
        void exception_shouldBeThrowable() {
            // When & Then
            assertThatThrownBy(() -> {
                throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
            })
                    .isInstanceOf(RuntimeException.class)
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Resource Not Found");
        }

        @Test
        @DisplayName("Should preserve error code when caught")
        void exception_whenCaught_shouldPreserveErrorCode() {
            // When
            ApiException exception = null;
            try {
                throw new ApiException(ErrorCode.FORBIDDEN, "Access denied");
            } catch (ApiException e) {
                exception = e;
            }

            // Then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
            assertThat(exception.getMessage()).isEqualTo("Access denied");
        }
    }

    @Nested
    @DisplayName("Common Use Cases")
    class CommonUseCasesTests {

        @Test
        @DisplayName("Should handle resource not found scenario")
        void resourceNotFound_shouldCreateAppropriateException() {
            // When
            ApiException exception = new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "Employee with ID emp-123 not found"
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            assertThat(exception.getMessage()).contains("emp-123");
        }

        @Test
        @DisplayName("Should handle authentication failure scenario")
        void authenticationFailure_shouldCreateAppropriateException() {
            // When
            ApiException exception = new ApiException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Invalid username or password"
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
            assertThat(exception.getErrorCode().getHttpCode().value()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should handle business rule violation scenario")
        void businessRuleViolation_shouldCreateAppropriateException() {
            // Given
            Map<String, String> errors = Map.of(
                    "appointmentTime", "Cannot book appointment in the past",
                    "doctorId", "Doctor is not available at this time"
            );

            // When
            ApiException exception = new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Appointment booking failed",
                    errors
            );

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(exception.getFieldErrors()).containsKeys("appointmentTime", "doctorId");
        }
    }
}
