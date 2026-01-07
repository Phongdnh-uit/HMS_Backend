package com.hms.auth_service.controllers;

import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.enums.RoleEnum;
import com.hms.common.test.MockMvcTestUtils;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller/API integration tests for AuthController.
 * Tests the full HTTP request/response cycle with MockMvc.
 * 
 * These tests verify:
 * - HTTP status codes are correct
 * - Response body structure matches expectations
 * - Request validation works properly
 * - Error handling returns appropriate responses
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.private-key=MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC5nFlclEp78Bj+pMzF0R0YT0O2FDIwWOVj3pBGr1G1jZ/s3BU0jz7bPmM0Qr7pP/kLc5OJY1x2V3R2H7kBLaZtNpJfMmPH2vP+nZ0rXmLOV+rZCkP+jLkCmL0TzP8M5q8hR2kL3qP7L8N2VpKzL9pR+kM3qL5V+L1N2RpGzJ9pR+kN3qLzV+L2N2VqGzL9pR+kO3qL3V+L3N2VrGzM9pR+kP3qL7V+L5N2VsGzN9pR+kQ3qL+V+L7N2VtGzO9pR+kR3qMBV+L9N2VuGzP9pR+kS3qMFV+MBN2VvGzQ9pR+kT3qMJV+MCN2VwGzR9pR+kU3qMNV+MEN2VxGzS9pR+kV3qMRV+MGN2VyGzTAgMBAAECggEAGxQ8CY2f0G0j1j9CZX6L2mK1b3l7L0WfX8P3vL5R0Z+P1L6V7rN/P5R0X8O3uL5Q0Y+N1K6U7qM/O5Q0W8N3tL5P0X+M1J6T7pL/N5P0V8M3sL5O0W+L1I6S7oK/M5O0U8L3rL5N0V+K1H6R7nJ/L5N0T8K3qL5M0U+J1G6Q7mI/K5M0S8J3pL5L0T+I1F6P7lH/J5L0R8I3oL5K0S+H1E6O7kG/I5K0Q8H3nL5J0R+G1D6N7jF/H5J0P8G3mL5I0Q+F1C6M7iE/G5I0O8F3lL5H0P+E1B6L7hD/F5H0N8E3kL5G0O+D1A6K7gC/E5G0M8D3jL5F0N+C196J7fB/D5F0L8C3iL5QKBgQDpRd0S9Z+P1H6R3rI5M0U+I1G6Q7nJ/K5M0S8J3pL5L0T+J1H6R7nJ/L5N0T8K3pL5M0T+I1F6P7lH/J5L0R8I3nL5K0S+H1E6O7kG/I5K0Q8H3mL5J0R+G1D6N7jF/H5J0P8G3lL5I0Q+F1C6M7iE/GwKBgQDLqd0T+Z9O1I7R4sI6M1V+J2H7R8oK/L6N1T9K4qL6M1U+K2I7R8oK/L6N1T9K4pL6L1T+J2H7Q8nJ/K6M1S9J4oL6K1S+I2G7P8mI/J6L1R9I4nL6J1R+H2F7O8lH/I6K1Q9H4mL6I1Q+G2E7N8kG/H6J1P9G4lL6H1P+F2D7M8jF/G6I1NQKBgBLqe1U+a+Q2J8T9rL7O2W+L3I8S9pL/M7O2T+L8rM7N2V+K3H8R9oK/L7N2S+K8qL7M2U+J3G8Q9nJ/K7M2R+J8pL7L2T+I3F8P9mI/J7L2Q+I8oL7K2S+H3E8O9lH/I7K2P+H8nL7J2R+G3D8N9kG/H7J2O+G8mL7I2QKBgFMrf2V+b+R3K9U+sM8P3X+M4J9T+qM/N8P3U+M9sN8O3W+L4I9S+pL/M8O3T+L9rM8N3V+K4H9R+oK/L8N3S+K9qL8M3U+J4G9Q+nJ/K8M3R+J9pL8L3T+I4F9P+mI/J8L3Q+I9oL8K3SQKBgGNsg3W+c+S4L+V+tN9Q4Y+N5K+U+rN/O9Q4V+N+tO9P4X+M5J+T+qM/N9P4U+M+sN9O4W+L5I+S+pL/M9O4T+L+rM9N4V+K5H+R+oK/L9N4S+K+qL9M4U+J5G+Q+nJ/K9M4R+J+pL9L4T+I5F+P+mI/J9L4Q",
    "jwt.public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuZxZXJRKe/AY/qTMxdEdGE9DthQyMFjlY96QRq9RtY2f7NwVNI8+2z5jNEK+6T/5C3OTiWNcdld0dh+5AS2mbTaSXzJjx9rz/p2dK15izlfq2QpD/oy5Api9E8z/DOavIUdpC96j+y/DdlaSsy/aUfpDN6i+Vfi9TdkaRsyfaUfpDd6i81fi9jdlahsy/aUfpDt6i91fi9zdlaxszPaUfpD96i+1fi+TdlbBszfaUfpEN6i/lfi/TdlbRszvaaUfpEd6jAVfi/TdlbhszfaUfpEt6jBVfjATdlbxs0PaUfpE96jCVfjAjdlcBs0faUfpFN6jDVfjBDdlcRs0vaUfpFd6jEVfjBjdlchsUwIDAQAB",
    "jwt.access-token.expiration=3600",
    "jwt.refresh-token.expiration=86400"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API-AUTH: AuthController API Tests")
class AuthControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("API-AUTH-001: Should register new user with valid data")
        void register_withValidData_shouldReturn200() throws Exception {
            // Given
            String email = TestDataFactory.uniqueEmail();
            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, TestDataFactory.simplePassword());

            // When & Then
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.data.email", is(email)));
        }

        @Test
        @DisplayName("API-AUTH-002: Should reject empty email")
        void register_withEmptyEmail_shouldReturn400() throws Exception {
            // Given
            String requestBody = """
                {
                    "email": "",
                    "password": "Test@123456"
                }
                """;

            // When & Then
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("API-AUTH-003: Should reject missing password")
        void register_withMissingPassword_shouldReturn400() throws Exception {
            // Given
            String requestBody = """
                {
                    "email": "test@example.com"
                }
                """;

            // When & Then
            mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("API-AUTH-004: Should login successfully with valid credentials")
        void login_withValidCredentials_shouldReturn200() throws Exception {
            // Given - Create an account first
            String email = TestDataFactory.uniqueEmail();
            String rawPassword = "Test@123456";
            
            Account account = new Account();
            account.setEmail(email);
            account.setPassword(passwordEncoder.encode(rawPassword));
            account.setRole(RoleEnum.PATIENT);
            account.setEmailVerified(true);
            accountRepository.save(account);

            String requestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, rawPassword);

            // When & Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.account.email", is(email)));
        }

        @Test
        @DisplayName("API-AUTH-005: Should reject invalid credentials")
        void login_withInvalidCredentials_shouldReturn401() throws Exception {
            // Given
            String requestBody = """
                {
                    "email": "nonexistent@test.com",
                    "password": "wrongPassword"
                }
                """;

            // When & Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("API-AUTH-006: Should reject wrong password")
        void login_withWrongPassword_shouldReturn401() throws Exception {
            // Given - Create an account first
            String email = TestDataFactory.uniqueEmail();
            
            Account account = new Account();
            account.setEmail(email);
            account.setPassword(passwordEncoder.encode("correctPassword"));
            account.setRole(RoleEnum.PATIENT);
            accountRepository.save(account);

            String requestBody = """
                {
                    "email": "%s",
                    "password": "wrongPassword"
                }
                """.formatted(email);

            // When & Then
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("API-AUTH-007: Should reject invalid refresh token")
        void refresh_withInvalidToken_shouldReturn401Or400() throws Exception {
            // Given
            String requestBody = """
                {
                    "refreshToken": "invalid-refresh-token"
                }
                """;

            // When & Then
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("API-AUTH-008: Should reject logout with invalid token")
        void logout_withInvalidToken_shouldReturn4xx() throws Exception {
            // Given
            String requestBody = """
                {
                    "refreshToken": "invalid-refresh-token"
                }
                """;

            // When & Then
            mockMvc.perform(post("/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andDo(print())
                .andExpect(status().is4xxClientError());
        }
    }
}
