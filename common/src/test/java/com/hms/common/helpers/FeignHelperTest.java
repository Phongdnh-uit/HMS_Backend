package com.hms.common.helpers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.FeignHandledException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FeignHelper.
 * Tests safe Feign call wrapping and exception handling.
 */
@DisplayName("UC-CMN-012: FeignHelper Unit Tests")
class FeignHelperTest {

    @Nested
    @DisplayName("Method: safeCall()")
    class SafeCallTests {

        @Test
        @DisplayName("UC-CMN-012: Should return successful response when call succeeds")
        void safeCall_withSuccessfulCall_shouldReturnResponse() {
            // Given
            ApiResponse<String> successResponse = ApiResponse.ok("test-data");

            // When
            ApiResponse<String> result = FeignHelper.safeCall(() -> successResponse);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(1000);
            assertThat(result.getMessage()).isEqualTo("success");
            assertThat(result.getData()).isEqualTo("test-data");
        }

        @Test
        @DisplayName("Should return response with null data")
        void safeCall_withNullData_shouldReturnResponseWithNullData() {
            // Given
            ApiResponse<String> nullDataResponse = ApiResponse.ok(null);

            // When
            ApiResponse<String> result = FeignHelper.safeCall(() -> nullDataResponse);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(1000);
            assertThat(result.getData()).isNull();
        }

        @Test
        @DisplayName("Should catch FeignHandledException and return error response")
        void safeCall_withFeignHandledException_shouldReturnErrorResponse() {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2002);
            errorResponse.setMessage("Resource Not Found");
            errorResponse.setData(null);

            FeignHandledException exception = new FeignHandledException(errorResponse);

            // When
            ApiResponse<String> result = FeignHelper.safeCall(() -> {
                throw exception;
            });

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(2002);
            assertThat(result.getMessage()).isEqualTo("Resource Not Found");
            assertThat(result.getData()).isNull();
        }

        @Test
        @DisplayName("Should rethrow unexpected exceptions")
        void safeCall_withUnexpectedException_shouldRethrowException() {
            // Given
            RuntimeException unexpectedException = new RuntimeException("Unexpected error");

            // When & Then
            assertThatThrownBy(() -> FeignHelper.safeCall(() -> {
                throw unexpectedException;
            }))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Unexpected error");
        }

        @Test
        @DisplayName("Should handle complex response objects")
        void safeCall_withComplexObject_shouldReturnComplexObject() {
            // Given
            record UserDto(String id, String name, String email) {}
            UserDto userData = new UserDto("user-123", "John Doe", "john@example.com");
            ApiResponse<UserDto> successResponse = ApiResponse.ok(userData);

            // When
            ApiResponse<UserDto> result = FeignHelper.safeCall(() -> successResponse);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().id()).isEqualTo("user-123");
            assertThat(result.getData().name()).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("Exception Scenarios")
    class ExceptionScenariosTests {

        @Test
        @DisplayName("Should handle validation error from Feign client")
        void safeCall_withValidationError_shouldReturnValidationErrorResponse() {
            // Given
            ApiResponse<Void> validationError = new ApiResponse<>();
            validationError.setCode(2000);
            validationError.setMessage("Validation Error");
            validationError.setErrors(java.util.Map.of("email", "Email is required"));

            FeignHandledException exception = new FeignHandledException(validationError);

            // When
            ApiResponse<Object> result = FeignHelper.safeCall(() -> {
                throw exception;
            });

            // Then
            assertThat(result.getCode()).isEqualTo(2000);
            assertThat(result.getMessage()).isEqualTo("Validation Error");
            assertThat(result.getErrors()).containsEntry("email", "Email is required");
        }

        @Test
        @DisplayName("Should handle authentication error from Feign client")
        void safeCall_withAuthenticationError_shouldReturnAuthErrorResponse() {
            // Given
            ApiResponse<Void> authError = new ApiResponse<>();
            authError.setCode(2003);
            authError.setMessage("Authentication Required");

            FeignHandledException exception = new FeignHandledException(authError);

            // When
            ApiResponse<Object> result = FeignHelper.safeCall(() -> {
                throw exception;
            });

            // Then
            assertThat(result.getCode()).isEqualTo(2003);
            assertThat(result.getMessage()).isEqualTo("Authentication Required");
        }

        @Test
        @DisplayName("Should handle service unavailable scenario")
        void safeCall_withServiceUnavailable_shouldReturnErrorResponse() {
            // Given
            ApiResponse<Void> serviceError = new ApiResponse<>();
            serviceError.setCode(5000);
            serviceError.setMessage("Service unavailable");

            FeignHandledException exception = new FeignHandledException(serviceError);

            // When
            ApiResponse<Object> result = FeignHelper.safeCall(() -> {
                throw exception;
            });

            // Then
            assertThat(result.getCode()).isEqualTo(5000);
            assertThat(result.getMessage()).isEqualTo("Service unavailable");
        }
    }

    @Nested
    @DisplayName("Type Safety")
    class TypeSafetyTests {

        @Test
        @DisplayName("Should maintain type safety for String responses")
        void safeCall_withStringType_shouldMaintainTypeSafety() {
            // Given
            ApiResponse<String> response = ApiResponse.ok("string-data");

            // When
            ApiResponse<String> result = FeignHelper.safeCall(() -> response);

            // Then
            assertThat(result.getData()).isInstanceOf(String.class);
            assertThat(result.getData()).isEqualTo("string-data");
        }

        @Test
        @DisplayName("Should maintain type safety for Integer responses")
        void safeCall_withIntegerType_shouldMaintainTypeSafety() {
            // Given
            ApiResponse<Integer> response = ApiResponse.ok(42);

            // When
            ApiResponse<Integer> result = FeignHelper.safeCall(() -> response);

            // Then
            assertThat(result.getData()).isInstanceOf(Integer.class);
            assertThat(result.getData()).isEqualTo(42);
        }

        @Test
        @DisplayName("Should maintain type safety for List responses")
        void safeCall_withListType_shouldMaintainTypeSafety() {
            // Given
            java.util.List<String> listData = java.util.List.of("item1", "item2", "item3");
            ApiResponse<java.util.List<String>> response = ApiResponse.ok(listData);

            // When
            ApiResponse<java.util.List<String>> result = FeignHelper.safeCall(() -> response);

            // Then
            assertThat(result.getData()).isInstanceOf(java.util.List.class);
            assertThat(result.getData()).hasSize(3);
            assertThat(result.getData()).containsExactly("item1", "item2", "item3");
        }
    }

    @Nested
    @DisplayName("Common Use Cases")
    class CommonUseCasesTests {

        @Test
        @DisplayName("Should support chaining multiple Feign calls")
        void safeCall_withChainedCalls_shouldSupportChaining() {
            // Given
            ApiResponse<String> firstResponse = ApiResponse.ok("first-call-result");
            ApiResponse<String> secondResponse = ApiResponse.ok("second-call-result");

            // When
            ApiResponse<String> result1 = FeignHelper.safeCall(() -> firstResponse);
            ApiResponse<String> result2 = FeignHelper.safeCall(() -> secondResponse);

            // Then
            assertThat(result1.getData()).isEqualTo("first-call-result");
            assertThat(result2.getData()).isEqualTo("second-call-result");
        }

        @Test
        @DisplayName("Should handle conditional error handling")
        void safeCall_withConditionalHandling_shouldAllowErrorChecking() {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2002);
            errorResponse.setMessage("Resource Not Found");

            FeignHandledException exception = new FeignHandledException(errorResponse);

            // When
            ApiResponse<String> result = FeignHelper.safeCall(() -> {
                throw exception;
            });

            // Then - Can check if call failed
            assertThat(result.getCode()).isNotEqualTo(1000);
            assertThat(result.getCode()).isEqualTo(2002);
            assertThat(result.getData()).isNull();
        }

        @Test
        @DisplayName("Should support fallback pattern")
        void safeCall_withFallback_shouldSupportFallbackPattern() {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(5000);
            errorResponse.setMessage("Service unavailable");

            FeignHandledException exception = new FeignHandledException(errorResponse);

            // When
            ApiResponse<String> result = FeignHelper.safeCall(() -> {
                throw exception;
            });

            // Fallback logic
            String finalData;
            if (result.getCode() != 1000) {
                finalData = "fallback-value";
            } else {
                finalData = result.getData();
            }

            // Then
            assertThat(finalData).isEqualTo("fallback-value");
        }
    }
}
