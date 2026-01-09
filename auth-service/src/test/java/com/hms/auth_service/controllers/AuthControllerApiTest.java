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
    "jwt.private-key=MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCzq5b75OdtR+xmK1RyPVOw5e7JMEQ/oCqIbpGSmNpZw1wo/V2MLioXdfGpSypz1Wx7cuvIH3ghQLtC/ekFb7MFu+8r5b0pi/g3lG7Rpiyg3x6gAcRlttvbAzgFuTAwqbzBtOjEAfYcMi//i1epbeu83IZ4lMVHVYa+KpFZylSikZs+Lp3Czvqxku2lUw4F6QNXeVaHEy7+TqJkn85j0xp0RhKufUWvXKakhrQL+ixU9+fgAkRjCGgNWrhZ4FmQAu/8Z62Ou4Xp/X4zM+HrDdpUDVzfjCZXKEJRtljv7+lPhvibA3HW7KEcuFI/H4/NfXQdZf9QiA4KdNAUTwcOBnWDAgMBAAECggEAAKrlCzeNApNvVevYSvpeWrxn+DhlMIVUUyByCwTlXssChyZ12AT95Atso+/jDv4egpDpPlubkygN6hBOctY46I8LsvKOEt7xw6caCE5hQRHDJ94J0ETvKS+oYCERhETFLkURTlUhiHZJRl5Kq5b8dr2tBDqtRvgS4Zb/5kEBsn7taBH5XubCwXWaePMgv0OWOqvgbqBqrn1oKUAo2tgnzb5Z8iqRbU8sKmnaWg2d0N3UlU1PcakgaTRUsMedkFFRK7wm7EnOx7T7mabq0pH+pNiJRjcXkaZiZyKFTQylqVkhkiMz+Kr7hQOR999vWknD/5WJ3yqZbf46qWvWW/lO8QKBgQDc4kUUqD6lihrwxZzco5DhqtiMiBZTrRjIlyTQVkzoUf75T5vBgw9dV8KOvAyfAIhPqZy/K3tJE5h0Y2qdSX4YHFHnpFaUeAc6jA9VN38jvV++mJioCORMSyvcdme3bwKZwPH4bdyhpXXvDu8WgMqtxuA1qnSIrW5kj2YeT2xVUwKBgQDQO/jXjx+stYwI6IrhmKmd/GsP6ARq7DBvc8ehAWxIUWy/doVtLDKA7iBiY2k4txuKZ+vVJiFIrlWQEXkN0v9V+xSOQfAsTfCvReA9VrTcmrGyg4sczYaqTynUtpBiPk3JR6USg+uHnhB3v8xcJ0BnWaNFfaFZosWZnqscdVupEQKBgD2326WR/S80D6MHFHVyHqFvo2JhBBwNWcdytA6ijoq7nor6+4JupHRoeSx4zu3+sBfSRMIF3ETm/MpInRml113VS+11tdt2Tk7Fo+Mjxpqt53rSGJWbJm9K8c6AJyrSEd/0Uaggym1AVludyKB5DCjSUQOtxTILg7UFfHsrphGRAoGBALwrVYTFCuRw8egi601NPQlMzhqDmWpdoK82OtEdjhITsmeZ3Jx48fJcPNtr5eugW7zIh+4HFNCu6RbVE0UQxDeYOk0K/NOsYrCgPjBvp+QpziX4hL80YlqISs6zYunET1px6ZM/rpjQlyT/JtaniaVa9RWYnzN2yYU2Qn3m9zthAoGBANw3mDYQTrquF2/dSqyVtPhiWMfQHEsvFqWWPc5xmGC4EgS6YErBUBYmEkETP1rDR/jnbCsKwvqstRiL6P5AeKjaah3LfjDWT2TGJVXUlXA9oBCm2d8Gh2E+l+CYHUj5BgbJ/0osYMUoLPOEgJBS/2JuBUft0bZ2B39pLdW3YoOQ",
    "jwt.public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs6uW++TnbUfsZitUcj1TsOXuyTBEP6AqiG6RkpjaWcNcKP1djC4qF3XxqUsqc9Vse3LryB94IUC7Qv3pBW+zBbvvK+W9KYv4N5Ru0aYsoN8eoAHEZbbb2wM4BbkwMKm8wbToxAH2HDIv/4tXqW3rvNyGeJTFR1WGviqRWcpUopGbPi6dws76sZLtpVMOBekDV3lWhxMu/k6iZJ/OY9MadEYSrn1Fr1ympIa0C/osVPfn4AJEYwhoDVq4WeBZkALv/GetjruF6f1+MzPh6w3aVA1c34wmVyhCUbZY7+/pT4b4mwNx1uyhHLhSPx+PzX10HWX/UIgOCnTQFE8HDgZ1gwIDAQAB",
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
