package com.hms.common.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.FeignHandledException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for FeignCustomErrorDecoder.
 * Tests error response decoding and exception creation.
 */
@DisplayName("UC-CMN-013: FeignCustomErrorDecoder Unit Tests")
class FeignCustomErrorDecoderTest {

    private FeignCustomErrorDecoder errorDecoder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        errorDecoder = new FeignCustomErrorDecoder();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Nested
    @DisplayName("Method: decode()")
    class DecodeTests {

        @Test
        @DisplayName("UC-CMN-013: Should decode ApiResponse error to FeignHandledException")
        void decode_withValidApiResponse_shouldCreateFeignHandledException() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2002);
            errorResponse.setMessage("Resource Not Found");
            errorResponse.setData(null);
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(404, responseBody);

            // When
            Exception exception = errorDecoder.decode("findById", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse()).isNotNull();
            assertThat(feignException.getResponse().getCode()).isEqualTo(2002);
            assertThat(feignException.getResponse().getMessage()).isEqualTo("Resource Not Found");
        }

        @Test
        @DisplayName("Should decode validation error with field errors")
        void decode_withValidationError_shouldIncludeFieldErrors() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2000);
            errorResponse.setMessage("Validation Error");
            errorResponse.setErrors(Map.of(
                    "email", "Email is required",
                    "password", "Password too short"
            ));
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(400, responseBody);

            // When
            Exception exception = errorDecoder.decode("create", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getCode()).isEqualTo(2000);
            assertThat(feignException.getResponse().getErrors()).containsEntry("email", "Email is required");
            assertThat(feignException.getResponse().getErrors()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle malformed JSON response")
        void decode_withMalformedJson_shouldReturnFallbackException() {
            // Given
            String malformedJson = "{invalid json content}";
            Response response = createMockResponse(500, malformedJson);

            // When
            Exception exception = errorDecoder.decode("someMethod", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse()).isNotNull();
            assertThat(feignException.getResponse().getCode()).isEqualTo(5000);
            assertThat(feignException.getResponse().getMessage()).contains("Service unavailable");
        }

        @Test
        @DisplayName("Should handle empty response body")
        void decode_withEmptyBody_shouldReturnFallbackException() {
            // Given
            Response response = createMockResponse(500, "");

            // When
            Exception exception = errorDecoder.decode("emptyMethod", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getCode()).isEqualTo(5000);
            assertThat(feignException.getResponse().getMessage()).contains("Service unavailable");
        }

        @Test
        @DisplayName("Should handle authentication error")
        void decode_withAuthenticationError_shouldDecodeAuthError() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2003);
            errorResponse.setMessage("Authentication Required");
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(401, responseBody);

            // When
            Exception exception = errorDecoder.decode("secureEndpoint", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getCode()).isEqualTo(2003);
            assertThat(feignException.getMessage()).isEqualTo("Authentication Required");
        }

        @Test
        @DisplayName("Should handle forbidden error")
        void decode_withForbiddenError_shouldDecodeForbiddenError() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2004);
            errorResponse.setMessage("Forbidden");
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(403, responseBody);

            // When
            Exception exception = errorDecoder.decode("adminEndpoint", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getCode()).isEqualTo(2004);
            assertThat(feignException.getResponse().getMessage()).isEqualTo("Forbidden");
        }

        @Test
        @DisplayName("Should handle conflict error")
        void decode_withConflictError_shouldDecodeConflictError() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2001);
            errorResponse.setMessage("Resource Exists");
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(409, responseBody);

            // When
            Exception exception = errorDecoder.decode("create", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getCode()).isEqualTo(2001);
            assertThat(feignException.getResponse().getMessage()).isEqualTo("Resource Exists");
        }
    }

    @Nested
    @DisplayName("Error Response Parsing")
    class ErrorResponseParsingTests {

        @Test
        @DisplayName("Should parse response with timestamp")
        void decode_withTimestamp_shouldParseTimestamp() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2099);
            errorResponse.setMessage("Internal Server Error");
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(500, responseBody);

            // When
            Exception exception = errorDecoder.decode("errorMethod", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should parse response without errors field")
        void decode_withoutErrorsField_shouldParseCorrectly() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = new ApiResponse<>();
            errorResponse.setCode(2002);
            errorResponse.setMessage("Not Found");
            errorResponse.setErrors(null);
            errorResponse.setTimestamp(Instant.now());

            String responseBody = objectMapper.writeValueAsString(errorResponse);
            Response response = createMockResponse(404, responseBody);

            // When
            Exception exception = errorDecoder.decode("findMethod", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getErrors()).isNull();
        }
    }

    @Nested
    @DisplayName("Fallback Scenarios")
    class FallbackScenariosTests {

        @Test
        @DisplayName("Should create fallback exception on parsing error")
        void decode_withParsingError_shouldCreateFallbackException() {
            // Given
            String invalidJson = "not a json";
            Response response = createMockResponse(500, invalidJson);

            // When
            Exception exception = errorDecoder.decode("failedMethod", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getCode()).isEqualTo(5000);
            assertThat(feignException.getResponse().getData()).isNull();
        }

        @Test
        @DisplayName("Should include exception message in fallback response")
        void decode_withException_shouldIncludeExceptionMessage() {
            // Given
            Response response = createMockResponse(500, "{");

            // When
            Exception exception = errorDecoder.decode("brokenMethod", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            FeignHandledException feignException = (FeignHandledException) exception;
            assertThat(feignException.getResponse().getMessage()).contains("Service unavailable");
        }
    }

    @Nested
    @DisplayName("HTTP Status Codes")
    class HttpStatusCodesTests {

        @Test
        @DisplayName("Should handle 400 Bad Request")
        void decode_with400Status_shouldDecodeCorrectly() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = createErrorResponse(2000, "Bad Request");
            Response response = createMockResponse(400, objectMapper.writeValueAsString(errorResponse));

            // When
            Exception exception = errorDecoder.decode("method400", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
        }

        @Test
        @DisplayName("Should handle 404 Not Found")
        void decode_with404Status_shouldDecodeCorrectly() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = createErrorResponse(2002, "Not Found");
            Response response = createMockResponse(404, objectMapper.writeValueAsString(errorResponse));

            // When
            Exception exception = errorDecoder.decode("method404", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            assertThat(((FeignHandledException) exception).getResponse().getMessage()).isEqualTo("Not Found");
        }

        @Test
        @DisplayName("Should handle 500 Internal Server Error")
        void decode_with500Status_shouldDecodeCorrectly() throws Exception {
            // Given
            ApiResponse<Void> errorResponse = createErrorResponse(2099, "Internal Server Error");
            Response response = createMockResponse(500, objectMapper.writeValueAsString(errorResponse));

            // When
            Exception exception = errorDecoder.decode("method500", response);

            // Then
            assertThat(exception).isInstanceOf(FeignHandledException.class);
            assertThat(((FeignHandledException) exception).getResponse().getCode()).isEqualTo(2099);
        }
    }

    // Helper methods
    private Response createMockResponse(int status, String body) {
        return Response.builder()
                .status(status)
                .reason("Test Reason")
                .request(Request.create(Request.HttpMethod.GET, "/test", new HashMap<>(), null, null, null))
                .headers(new HashMap<>())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    private ApiResponse<Void> createErrorResponse(int code, String message) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(Instant.now());
        return response;
    }
}
