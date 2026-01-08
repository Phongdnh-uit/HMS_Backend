# HMS Backend - Test Infrastructure Guide

## 📋 Overview

This guide explains how to use the test infrastructure set up for the HMS Backend project. The infrastructure provides reusable utilities, annotations, and base classes to make writing tests faster and more consistent.

---

## 🔧 Quick Start

### Running All Tests

```bash
# Run all tests
./gradlew test

# Run tests for a specific service
./gradlew :auth-service:test
./gradlew :patient-service:test

# Run tests with coverage report
./gradlew test jacocoTestReport
```

### Viewing Coverage Reports

After running tests, find HTML coverage reports at:

```
{service}/build/reports/jacoco/test/html/index.html
```

---

## 📦 Test Dependencies (Already Configured)

All services have access to these testing libraries:

| Library        | Purpose                | Version |
| -------------- | ---------------------- | ------- |
| JUnit 5        | Test framework         | 5.11.4  |
| Mockito        | Mocking framework      | 5.14.2  |
| AssertJ        | Fluent assertions      | 3.27.2  |
| H2 Database    | In-memory database     | 2.3.232 |
| WireMock       | HTTP mocking for Feign | 3.10.0  |
| Testcontainers | Docker-based testing   | 1.20.4  |
| DataFaker      | Test data generation   | 2.4.2   |
| REST Assured   | API testing            | 5.5.0   |
| Awaitility     | Async testing          | 4.2.2   |

---

## 🛠️ Test Utilities (from common module)

### 1. TestDataFactory - Generate Test Data

```java
import com.hms.common.test.TestDataFactory;

// Generate unique email
String email = TestDataFactory.uniqueEmail(); // "test.1704635847123@hms-test.com"

// Generate random patient data
String fullName = TestDataFactory.fullName();
LocalDate birthDate = TestDataFactory.adultBirthDate();
String bloodType = TestDataFactory.bloodType();
String allergies = TestDataFactory.allergies();

// Generate appointment data
LocalDateTime appointmentTime = TestDataFactory.futureAppointmentTime(30);

// Generate medical data
String diagnosis = TestDataFactory.diagnosis();
String symptoms = TestDataFactory.symptoms();
String medicine = TestDataFactory.medicineName();

// Generate HR data
String department = TestDataFactory.uniqueDepartmentName();
String jobTitle = TestDataFactory.jobTitle();

// Generate billing data
double amount = TestDataFactory.amount();

// Use underlying Faker for custom data
String custom = TestDataFactory.faker().medical().hospitalName();
```

### 2. MockMvcTestUtils - API Testing Helpers

```java
import com.hms.common.test.MockMvcTestUtils;

@Autowired
private MockMvc mockMvc;

@Test
void testCreatePatient() throws Exception {
    PatientRequest request = new PatientRequest();
    request.setName("John Doe");

    // POST with JSON body
    MockMvcTestUtils.post(mockMvc, "/patients", request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("John Doe"));

    // POST with user context (simulating authenticated request)
    MockMvcTestUtils.postWithUser(mockMvc, "/patients", request, "user-123", "ADMIN")
        .andExpect(status().isOk());

    // GET with query params
    MockMvcTestUtils.get(mockMvc, "/patients",
        MockMvcTestUtils.params()
            .add("page", 0)
            .add("size", 10)
            .build())
        .andExpect(status().isOk());
}
```

### 3. WireMockTest - Testing Feign Clients

```java
import com.hms.common.test.WireMockTest;

class PatientClientTest extends WireMockTest {

    @Test
    void getPatient_shouldReturnPatient() {
        // Stub external service response
        stubGetJson("/patients/1", 200, """
            {"id": 1, "name": "John Doe"}
        """);

        // Call your Feign client
        PatientResponse response = patientClient.getPatient(1L);

        // Verify
        assertThat(response.getName()).isEqualTo("John Doe");
        verifyGet("/patients/1");
    }

    @Test
    void getPatient_shouldHandleError() {
        // Stub error response
        stubError("/patients/999", 404, "Patient not found");

        // Test error handling
        assertThatThrownBy(() -> patientClient.getPatient(999L))
            .isInstanceOf(FeignException.class);
    }

    @Test
    void testTimeout() {
        // Simulate slow response
        stubTimeout("/patients/1", 5000);

        // Test timeout handling
        // ...
    }
}
```

### 4. ApiResponseAssert - Custom Assertions

```java
import com.hms.common.test.ApiResponseAssert;

@Test
void testApiResponse() {
    ApiResponse<PatientResponse> response = patientService.getPatient(1L);

    ApiResponseAssert.assertThat(response)
        .isSuccessful()
        .hasData()
        .dataMatches(patient -> patient.getName().equals("John"));
}
```

---

## 📝 Test Annotations

### @IntegrationTest - Full Integration Tests

```java
import com.hms.common.test.IntegrationTest;

@IntegrationTest
class PatientServiceIntegrationTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void createPatient_shouldPersistToDatabase() {
        // Full integration test with H2
        // Eureka and Config Server are disabled
    }
}
```

### @ControllerTest - API/Controller Tests

```java
import com.hms.common.test.ControllerTest;

@ControllerTest
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPatient_shouldReturn200() throws Exception {
        mockMvc.perform(get("/patients/1"))
            .andExpect(status().isOk());
    }
}
```

### @JpaRepositoryTest - Repository Tests

```java
import com.hms.common.test.JpaRepositoryTest;

@JpaRepositoryTest
class PatientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PatientRepository repository;

    @Test
    void findByEmail_shouldReturnPatient() {
        // Test with H2 in-memory database
    }
}
```

---

## 📁 Test Class Templates

### Unit Test Template

```java
package com.hms.{service}.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UC-XXX: ServiceName Unit Tests")
class MyServiceTest {

    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyServiceImpl service;

    @BeforeEach
    void setUp() {
        // Common setup
    }

    @Nested
    @DisplayName("Method: myMethod()")
    class MyMethodTests {

        @Test
        @DisplayName("TC-001: Should do something when valid input")
        void myMethod_withValidInput_shouldSucceed() {
            // Given
            given(repository.findById(any())).willReturn(Optional.of(new Entity()));

            // When
            var result = service.myMethod("input");

            // Then
            assertThat(result).isNotNull();
            verify(repository).findById(any());
        }

        @Test
        @DisplayName("TC-002: Should throw exception when invalid input")
        void myMethod_withInvalidInput_shouldThrowException() {
            // Given
            given(repository.findById(any())).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.myMethod("bad"))
                .isInstanceOf(NotFoundException.class);
        }
    }
}
```

### Repository Test Template

```java
package com.hms.{service}.repositories;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("IT-REPO: MyRepository Integration Tests")
class MyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MyRepository repository;

    @Test
    @DisplayName("IT-REPO-001: Should save and find entity")
    void save_shouldPersistEntity() {
        // Given
        MyEntity entity = new MyEntity();
        entity.setName("Test");

        // When
        entityManager.persistAndFlush(entity);
        entityManager.clear();

        // Then
        Optional<MyEntity> found = repository.findById(entity.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test");
    }
}
```

### Controller Test Template

```java
package com.hms.{service}.controllers;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API-XXX: MyController API Tests")
class MyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /my-resource")
    class GetResourceTests {

        @Test
        @DisplayName("API-XXX-001: Should return 200 with valid request")
        void get_shouldReturn200() throws Exception {
            mockMvc.perform(get("/my-resource")
                    .header("X-User-ID", "test-user")
                    .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        }
    }
}
```

---

## 🏃 Running Tests

### Common Commands

```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :auth-service:test

# Run specific test class
./gradlew :auth-service:test --tests "AuthServiceImplTest"

# Run specific test method
./gradlew :auth-service:test --tests "AuthServiceImplTest.login_withValidCredentials*"

# Run tests with specific tag
./gradlew test -PincludeTags="unit"

# Run tests in parallel
./gradlew test --parallel

# Generate coverage report
./gradlew jacocoTestReport

# Skip tests during build
./gradlew build -x test
```

### IDE Integration

**IntelliJ IDEA:**

1. Right-click on test class/method → Run
2. Use gutter icons (green play button)
3. Coverage: Run with Coverage (Ctrl+Shift+F10)

**VS Code:**

1. Install Java Test Runner extension
2. Use Testing sidebar
3. Click run icons in test files

---

## 📊 Test Coverage

Coverage reports are generated at:

```
{service}/build/reports/jacoco/test/html/index.html
```

### Coverage Goals

| Category        | Target |
| --------------- | ------ |
| Line Coverage   | 80%+   |
| Branch Coverage | 70%+   |
| Critical Paths  | 100%   |

### Enforcing Coverage (Optional)

Uncomment in `build.gradle.kts`:

```kotlin
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    violationRules {
        rule {
            limit {
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}
```

---

## 📝 Best Practices

### 1. Test Naming Convention

```
methodName_stateUnderTest_expectedBehavior
```

Example: `login_withValidCredentials_shouldReturnToken`

### 2. Use Nested Classes for Organization

```java
@Nested
@DisplayName("POST /patients")
class CreatePatientTests {
    // All POST-related tests
}

@Nested
@DisplayName("GET /patients/{id}")
class GetPatientTests {
    // All GET-related tests
}
```

### 3. Use @DisplayName for Readability

```java
@Test
@DisplayName("TC-AUTH-001: Should authenticate user with valid email and password")
void testLogin() { ... }
```

### 4. Follow AAA Pattern

```java
@Test
void exampleTest() {
    // Arrange (Given)
    var input = createTestInput();

    // Act (When)
    var result = service.process(input);

    // Assert (Then)
    assertThat(result).isNotNull();
}
```

### 5. Use BDD-style Mockito

```java
// Instead of: when(mock.method()).thenReturn(value);
given(mock.method()).willReturn(value);

// Instead of: verify(mock).method();
then(mock).should().method();
```

---

## 🐛 Troubleshooting

### "Eureka client not found" errors

Ensure your test uses `@ActiveProfiles("test")` or sets:

```java
@SpringBootTest(properties = {"eureka.client.enabled=false"})
```

### "Config Server connection refused"

Add property:

```java
@SpringBootTest(properties = {"spring.cloud.config.enabled=false"})
```

### H2 compatibility issues

Use MySQL mode:

```yaml
spring.datasource.url: jdbc:h2:mem:testdb;MODE=MySQL
```

### Slow tests

- Use `@DataJpaTest` instead of `@SpringBootTest` for repository tests
- Use `@WebMvcTest` for controller-only tests
- Enable parallel execution in build.gradle.kts

---

## 📁 Project Structure

```
{service}/
├── src/
│   ├── main/java/...
│   └── test/
│       ├── java/com/hms/{service}/
│       │   ├── controllers/     # API tests
│       │   ├── services/        # Unit tests
│       │   ├── repositories/    # Repository tests
│       │   ├── mappers/         # Mapper tests
│       │   └── hooks/           # Hook tests
│       └── resources/
│           └── application-test.yml
```

---

_Last Updated: 2026-01-07_
