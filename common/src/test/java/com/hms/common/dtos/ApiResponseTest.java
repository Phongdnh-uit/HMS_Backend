package com.hms.common.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApiResponse.
 * Tests response creation and data wrapping.
 */
@DisplayName("UC-CMN-008: ApiResponse Unit Tests")
class ApiResponseTest {

    @Nested
    @DisplayName("Method: ok(T data)")
    class OkWithDataTests {

        @Test
        @DisplayName("UC-CMN-008: Should create successful response with data")
        void ok_withData_shouldCreateSuccessResponse() {
            // Given
            String testData = "test-data";

            // When
            ApiResponse<String> response = ApiResponse.ok(testData);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(1000);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getErrors()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getTimestamp()).isBefore(Instant.now().plusSeconds(1));
        }

        @Test
        @DisplayName("Should handle null data")
        void ok_withNullData_shouldCreateResponseWithNullData() {
            // When
            ApiResponse<String> response = ApiResponse.ok(null);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(1000);
            assertThat(response.getMessage()).isEqualTo("success");
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("Should handle complex object data")
        void ok_withComplexObject_shouldCreateResponse() {
            // Given
            Map<String, Object> complexData = Map.of(
                    "id", "123",
                    "name", "Test User",
                    "active", true
            );

            // When
            ApiResponse<Map<String, Object>> response = ApiResponse.ok(complexData);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getData()).isEqualTo(complexData);
            assertThat(response.getData()).containsEntry("id", "123");
        }
    }

    @Nested
    @DisplayName("Method: ok(String message, T data)")
    class OkWithCustomMessageTests {

        @Test
        @DisplayName("UC-CMN-008: Should create response with custom message")
        void ok_withCustomMessage_shouldCreateResponseWithMessage() {
            // Given
            String customMessage = "Operation completed successfully";
            String testData = "result-data";

            // When
            ApiResponse<String> response = ApiResponse.ok(customMessage, testData);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCode()).isEqualTo(1000);
            assertThat(response.getMessage()).isEqualTo(customMessage);
            assertThat(response.getData()).isEqualTo(testData);
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty custom message")
        void ok_withEmptyMessage_shouldAcceptEmptyMessage() {
            // Given
            String emptyMessage = "";

            // When
            ApiResponse<String> response = ApiResponse.ok(emptyMessage, "data");

            // Then
            assertThat(response.getMessage()).isEmpty();
            assertThat(response.getCode()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGettersTests {

        @Test
        @DisplayName("Should allow manual field setting")
        void setters_shouldAllowManualConfiguration() {
            // Given
            ApiResponse<String> response = new ApiResponse<>();
            Instant timestamp = Instant.now();

            // When
            response.setCode(2000);
            response.setMessage("Custom error");
            response.setData("error-data");
            response.setErrors(Map.of("field", "error message"));
            response.setTimestamp(timestamp);

            // Then
            assertThat(response.getCode()).isEqualTo(2000);
            assertThat(response.getMessage()).isEqualTo("Custom error");
            assertThat(response.getData()).isEqualTo("error-data");
            assertThat(response.getErrors()).containsEntry("field", "error message");
            assertThat(response.getTimestamp()).isEqualTo(timestamp);
        }
    }
}
