package com.hms.api_gateway.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for End-to-End tests that test complete user flows through the API Gateway.
 * E2E tests simulate real user interactions by making HTTP requests through the gateway
 * and verifying responses across multiple microservices.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "jwt.public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs/yJcHARVAGn1xzVbndvyuj1spOinx4IXLP+r73zRpd9Aqjq8gKDnJolEPHXM55wdLxkvjXMEv3p665tbcRXUyHUWI6/p/UIeB6pEZyNUqUj6BiiE0lhv3nINmlAXeXibigMaOzUXBAIl7fuw29oG5bCWjjoUBWgVMFRkGAJfOBZDnqG1OgX1ibjCP4qDI5RgBuv32xAP3/n3eUmNel3+kxAj/ETVkmRbJfiJh61qc4n3bUxXXpJkPnAdGyNxcVPXArASLkKDlb1PSQOJ1x83s75yvXgLMxzt1hgThArM4vlFZRx8IjTbWIpJAjAqil0PLRIX6L3FzoRW6xuHRmMGwIDAQAB",
        "jwt.private-key=MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCz/IlwcBFUAafXHNVud2/K6PWyk6KfHghcs/6vvfNGl30CqOryAoOcmiUQ8dcznnB0vGS+NcwS/enrrm1txFdTIdRYjr+n9Qh4HqkRnI1SpSPoGKITSWG/ecg2aUBd5eJuKAxo7NRcEAiXt+7Db2gblsJaOOhQFaBUwVGQYAl84FkOeobU6BfWJuMI/ioMjlGAG6/fbEA/f+fd5SY16Xf6TECP8RNWSZFsl+ImHrWpzifdtTFdekmQ+cB0bI3FxU9cCsBIuQoOVvU9JA4nXHzezvnK9eAszHO3WGBOECszi+UVlHHwiNNtYikkCMCqKXQ8tEhfovcXOhFbrG4dGYwbAgMBAAECggEARy9aLF4mfFMrTqjYwRf73wTEKBy79LKsG/4UfO1jikff6W1lftH/u7A7eLgtpPzE/WaFcbVVQS2rlssl/IPrBbYbhervYj5HWzrFI3IHcuhdiy4y6+3Z+yDvTpXSH1EMfQTZ46XHV6H/xAG7LRCi3EaEF6rqNsJW5y6OU8un9PsbLs/Gmpi25f983qZsoDdXfHL3xEF/ESzYiCf1x9Gnm9rRNJx8NgrxjLlJxzfAToKlPsY4M6vYaL/Rqyyp1hAFRVYN5ZC2ju75yAKqhR+2u4aoTE2NPDZJNFn4HvwIcb7FzCjKQn3qgCCOyYbfCnt0E6ICbsTYP/6yvoidRxB6AQKBgQDyfZG3F3IOMEoO0eoIh9MdUQ85nfhhbCzyYJy/3dMf98ADrFcS8x7OaJxjzpfIOB4m2ELCbysfBOIil6wlhrzUDoxsSwl+cfxUE8nYVEC6YTAxGQpCKUrPlSCnFjOwN34+AZpSrWy0R6DztpLFoNnCRNcD9rQOHfOZ/hw0gPAJjQKBgQC+A4bw0qSj5IkMLJ3RLSV6d6MEkIgQk64ziy8i3qagQHgyn4QHKE+qIUSYQMGjbTvRmY+ZV4OC1nDMNoatwAq9dk9c4jQCIfz+K0ik/0EYL+XbeH/ogUJgtcgI7qegdVhS5qNNnCev6tw93H8PuWe6qymIgscEvP/NW7aCJYH+RwKBgH1xZiMwVsluQ7F8+DPnh9gfqd+lj+teGbZdMlmzOFfOQ5/i1Lyx1pry1QxwwGZMWZTAxXBuMAGA9jbs/ZoAJMkSqaQQAV5POziHcCCgHUgNpO+RQ5RMZi4SuGyXeK/NVVpgW+QvYQ+2ClZpeW4RMvVjxVOAmU7AQdfE8/RZS1O1AoGARdwIHbxkObmJRYeV0lUV5Wvc7I2y6N1S+7JddyWC/4IUbxBEu1jvyS4ICS4tw0ci6hHaQNdzC4SJ3hrh8zma+UNpNE4aLvqOCGijgb4zEZByovkvla0IWYQb8mTEDnN2MKyJN7yEvuHLbZcGxCI3Z+MMFxt1zmEqbzcb58egfnsCgYEA1dBSsNNJJVSuj+HcrxCKRb1BmgIpD4Mbr7M/sp4OZQ5jDqBOjyWdBHnZfpzMVLbqgWwunsDTjA0tFYqziunpI/MmzTfsy5oPEu05GeIQ8X+8Aqw7RCBP+dv8T8Wur+eMWS9wDcObV3nLaBad6G+DIZJwaPdoZJpQ2NQruQbGOm0=",
        // Disable Eureka and Config Server for E2E tests
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
    }
)
@ActiveProfiles("test")
public abstract class E2ETestBase {

    @LocalServerPort
    protected int port;

    protected String baseUrl;
    protected String authToken;
    protected Map<String, String> testUsers = new HashMap<>();

    @BeforeEach
    void setUpBase() {
        baseUrl = "http://localhost:" + port;
        RestAssured.baseURI = baseUrl;
        RestAssured.port = port;
    }

    /**
     * Create a base request specification with common headers
     */
    protected RequestSpecification given() {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON);
    }

    /**
     * Create an authenticated request with JWT token
     */
    protected RequestSpecification givenAuth(String token) {
        return given()
            .header("Authorization", "Bearer " + token);
    }

    /**
     * Helper method to register a new user
     */
    protected Response registerUser(String email, String password, String role) {
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("email", email);
        registerRequest.put("password", password);
        registerRequest.put("role", role);

        return given()
            .body(registerRequest)
            .post("/auth/register");
    }

    /**
     * Helper method to login and get JWT token
     */
    protected String loginAndGetToken(String email, String password) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", password);

        Response response = given()
            .body(loginRequest)
            .post("/auth/login");

        if (response.statusCode() == 200) {
            return response.jsonPath().getString("data.token");
        }
        return null;
    }

    /**
     * Helper method to register and login a user in one step
     */
    protected String registerAndLogin(String email, String password, String role) {
        registerUser(email, password, role);
        return loginAndGetToken(email, password);
    }

    /**
     * Generate unique email for testing
     */
    protected String generateUniqueEmail() {
        return "test." + System.currentTimeMillis() + "@hms-e2e-test.com";
    }

    /**
     * Generate unique email with prefix
     */
    protected String generateUniqueEmail(String prefix) {
        return prefix + "." + System.currentTimeMillis() + "@hms-e2e-test.com";
    }

    /**
     * Extract data from ApiResponse wrapper
     */
    protected <T> T extractData(Response response, Class<T> dataClass) {
        return response.jsonPath().getObject("data", dataClass);
    }

    /**
     * Verify response is successful (2xx status code)
     */
    protected void assertSuccess(Response response) {
        response.then()
            .statusCode(org.hamcrest.Matchers.lessThan(300));
    }

    /**
     * Verify response has ApiResponse structure with success=true
     */
    protected void assertApiSuccess(Response response) {
        response.then()
            .statusCode(200)
            .body("success", org.hamcrest.Matchers.equalTo(true));
    }
}
