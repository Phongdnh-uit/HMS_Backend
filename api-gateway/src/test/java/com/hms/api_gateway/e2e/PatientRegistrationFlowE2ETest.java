package com.hms.api_gateway.e2e;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E-REG: Patient Registration Flow End-to-End Tests
 * 
 * Tests complete patient registration workflow through the API Gateway:
 * 1. User registers an account
 * 2. User logs in to get JWT token
 * 3. User views their profile
 */
@DisplayName("E2E-REG: Patient Registration Flow")
class PatientRegistrationFlowE2ETest extends E2ETestBase {

    @Nested
    @DisplayName("E2E-REG-001: Complete patient registration through gateway")
    class CompletePatientRegistrationTest {

        @Test
        @DisplayName("Should successfully register a new patient account with valid data")
        void shouldRegisterNewPatientSuccessfully() {
            // Given: Valid patient registration data
            String email = generateUniqueEmail("patient");
            String password = "SecurePassword123!";
            String role = "PATIENT";

            Map<String, Object> registerRequest = new HashMap<>();
            registerRequest.put("email", email);
            registerRequest.put("password", password);
            registerRequest.put("role", role);

            // When: User submits registration through gateway
            Response response = given()
                .body(registerRequest)
                .post("/auth/register");

            // Then: Registration is successful
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("successfully"))
                .body("data.id", notNullValue())
                .body("data.email", equalTo(email))
                .body("data.role", equalTo(role));

            // Verify password is not returned in response
            response.then()
                .body("data.password", nullValue());
        }

        @Test
        @DisplayName("Should reject registration with duplicate email")
        void shouldRejectDuplicateEmail() {
            // Given: An already registered email
            String email = generateUniqueEmail("duplicate");
            String password = "SecurePassword123!";
            registerUser(email, password, "PATIENT");

            // When: Another user tries to register with same email
            Map<String, Object> duplicateRequest = new HashMap<>();
            duplicateRequest.put("email", email);
            duplicateRequest.put("password", "DifferentPassword123!");
            duplicateRequest.put("role", "PATIENT");

            Response response = given()
                .body(duplicateRequest)
                .post("/auth/register");

            // Then: Registration is rejected
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(409)))
                .body("success", equalTo(false))
                .body("message", containsStringIgnoringCase("email"));
        }

        @Test
        @DisplayName("Should validate required fields during registration")
        void shouldValidateRequiredFields() {
            // Given: Invalid registration data (missing email)
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("password", "SecurePassword123!");
            invalidRequest.put("role", "PATIENT");

            // When: User submits incomplete registration
            Response response = given()
                .body(invalidRequest)
                .post("/auth/register");

            // Then: Validation error is returned
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should validate password strength")
        void shouldValidatePasswordStrength() {
            // Given: Weak password
            String email = generateUniqueEmail("weakpass");
            Map<String, Object> weakPasswordRequest = new HashMap<>();
            weakPasswordRequest.put("email", email);
            weakPasswordRequest.put("password", "123");
            weakPasswordRequest.put("role", "PATIENT");

            // When: User submits registration with weak password
            Response response = given()
                .body(weakPasswordRequest)
                .post("/auth/register");

            // Then: Validation error is returned
            response.then()
                .statusCode(anyOf(equalTo(400), equalTo(422)))
                .body("success", equalTo(false));
        }
    }

    @Nested
    @DisplayName("E2E-REG-002: Login after registration")
    class LoginAfterRegistrationTest {

        @Test
        @DisplayName("Should successfully login with registered credentials")
        void shouldLoginWithRegisteredCredentials() {
            // Given: A registered patient account
            String email = generateUniqueEmail("login-test");
            String password = "SecurePassword123!";
            registerUser(email, password, "PATIENT");

            // When: User logs in with correct credentials
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("email", email);
            loginRequest.put("password", password);

            Response response = given()
                .body(loginRequest)
                .post("/auth/login");

            // Then: Login is successful and JWT token is returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.token", notNullValue())
                .body("data.token", not(emptyString()))
                .body("data.refreshToken", notNullValue())
                .body("data.email", equalTo(email))
                .body("data.role", equalTo("PATIENT"));

            // Verify JWT token format
            String token = response.jsonPath().getString("data.token");
            assertThat(token).matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$");
        }

        @Test
        @DisplayName("Should reject login with incorrect password")
        void shouldRejectIncorrectPassword() {
            // Given: A registered patient account
            String email = generateUniqueEmail("wrong-pass");
            String correctPassword = "SecurePassword123!";
            registerUser(email, correctPassword, "PATIENT");

            // When: User logs in with incorrect password
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("email", email);
            loginRequest.put("password", "WrongPassword123!");

            Response response = given()
                .body(loginRequest)
                .post("/auth/login");

            // Then: Login is rejected
            response.then()
                .statusCode(anyOf(equalTo(401), equalTo(403)))
                .body("success", equalTo(false))
                .body("data.token", nullValue());
        }

        @Test
        @DisplayName("Should reject login with non-existent email")
        void shouldRejectNonExistentEmail() {
            // Given: Non-existent email
            String email = "nonexistent." + System.currentTimeMillis() + "@hms-test.com";
            
            // When: User tries to login
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("email", email);
            loginRequest.put("password", "SomePassword123!");

            Response response = given()
                .body(loginRequest)
                .post("/auth/login");

            // Then: Login is rejected
            response.then()
                .statusCode(anyOf(equalTo(401), equalTo(404)))
                .body("success", equalTo(false));
        }

        @Test
        @DisplayName("Should be able to use refresh token to get new access token")
        void shouldRefreshToken() {
            // Given: A logged-in user with refresh token
            String email = generateUniqueEmail("refresh-test");
            String password = "SecurePassword123!";
            registerUser(email, password, "PATIENT");
            
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("email", email);
            loginRequest.put("password", password);
            
            Response loginResponse = given()
                .body(loginRequest)
                .post("/auth/login");
            
            String refreshToken = loginResponse.jsonPath().getString("data.refreshToken");

            // When: User uses refresh token to get new access token
            Map<String, String> refreshRequest = new HashMap<>();
            refreshRequest.put("refreshToken", refreshToken);

            Response response = given()
                .body(refreshRequest)
                .post("/auth/refresh");

            // Then: New access token is returned
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.token", notNullValue())
                .body("data.token", not(emptyString()));
        }
    }

    @Nested
    @DisplayName("E2E-REG-003: View own profile")
    class ViewOwnProfileTest {

        @Test
        @DisplayName("Should successfully retrieve authenticated user profile")
        void shouldGetOwnProfile() {
            // Given: A registered and logged-in patient
            String email = generateUniqueEmail("profile-test");
            String password = "SecurePassword123!";
            String token = registerAndLogin(email, password, "PATIENT");

            // When: User requests their own profile
            Response response = givenAuth(token)
                .get("/auth/me");

            // Then: Profile is returned successfully
            response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.email", equalTo(email))
                .body("data.role", equalTo("PATIENT"))
                .body("data.id", notNullValue());

            // Password should not be included in profile
            response.then()
                .body("data.password", nullValue());
        }

        @Test
        @DisplayName("Should reject profile request without authentication")
        void shouldRejectUnauthenticatedProfileRequest() {
            // When: User requests profile without token
            Response response = given()
                .get("/auth/me");

            // Then: Request is rejected with 401 Unauthorized
            response.then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Should reject profile request with invalid token")
        void shouldRejectInvalidToken() {
            // Given: Invalid JWT token
            String invalidToken = "invalid.jwt.token";

            // When: User requests profile with invalid token
            Response response = givenAuth(invalidToken)
                .get("/auth/me");

            // Then: Request is rejected with 401 Unauthorized
            response.then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Should reject profile request with expired token")
        void shouldRejectExpiredToken() {
            // Given: An expired JWT token (simulated with malformed token)
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjF9.invalid";

            // When: User requests profile with expired token
            Response response = givenAuth(expiredToken)
                .get("/auth/me");

            // Then: Request is rejected with 401 Unauthorized
            response.then()
                .statusCode(401);
        }

        @Test
        @DisplayName("Should complete full registration and profile verification flow")
        void shouldCompleteFullRegistrationFlow() {
            // Given: New patient data
            String email = generateUniqueEmail("full-flow");
            String password = "SecurePassword123!";
            String role = "PATIENT";

            // Step 1: Register
            Response registerResponse = registerUser(email, password, role);
            registerResponse.then()
                .statusCode(200)
                .body("success", equalTo(true));
            
            Long accountId = registerResponse.jsonPath().getLong("data.id");

            // Step 2: Login
            String token = loginAndGetToken(email, password);
            assertThat(token).isNotNull().isNotEmpty();

            // Step 3: Verify profile
            Response profileResponse = givenAuth(token)
                .get("/auth/me");
            
            profileResponse.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(accountId.intValue()))
                .body("data.email", equalTo(email))
                .body("data.role", equalTo(role));

            // Step 4: Verify token works for protected endpoints
            Response accountsResponse = givenAuth(token)
                .get("/accounts/" + accountId);
            
            // Should be able to access own account data
            accountsResponse.then()
                .statusCode(anyOf(equalTo(200), equalTo(403))); // May be 403 if role-based access is enforced
        }
    }
}
