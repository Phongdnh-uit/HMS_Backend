# End-to-End (E2E) Tests - HMS Backend

## 📋 Overview

This directory contains End-to-End tests for the Hospital Management System Backend. These tests verify complete user workflows through the API Gateway, ensuring all microservices work together correctly.

## 🗂️ Test Files

### 1. E2ETestBase.java

Base class providing common utilities for all E2E tests:

- REST Assured configuration
- Authentication helpers (register, login, get token)
- Request builders (with and without auth)
- Response assertion helpers
- Unique email generation

### 2. PatientRegistrationFlowE2ETest.java

**Test Coverage:** E2E-REG-001 to E2E-REG-003

Tests the complete patient registration and authentication flow:

- ✅ E2E-REG-001: Complete patient registration through gateway
  - Valid registration with email, password, role
  - Duplicate email rejection
  - Required field validation
  - Password strength validation
- ✅ E2E-REG-002: Login after registration
  - Successful login with registered credentials
  - JWT token generation and format validation
  - Incorrect password rejection
  - Non-existent email rejection
  - Refresh token functionality
- ✅ E2E-REG-003: View own profile
  - Authenticated profile retrieval
  - Unauthenticated request rejection
  - Invalid token rejection
  - Expired token handling
  - Complete registration-to-profile workflow

### 3. AppointmentBookingFlowE2ETest.java

**Test Coverage:** E2E-APT-001 to E2E-APT-005

Tests the complete appointment booking and management flow:

- ✅ E2E-APT-001: Patient books appointment with available doctor
  - Successful appointment creation
  - Authentication requirement
  - Future date validation
  - Time conflict detection
- ✅ E2E-APT-002: Patient views their appointments
  - List patient's appointments
  - Get appointment by ID
  - Cross-patient access prevention
- ✅ E2E-APT-003: Patient cancels appointment
  - Cancel own appointment
  - Prevent canceling other patient's appointments
- ✅ E2E-APT-004: Receptionist creates walk-in appointment
  - Walk-in appointment creation
  - Immediate appointment support
- ✅ E2E-APT-005: Patient check-in flow
  - Check-in for appointment
  - Queue position tracking
  - Check-out after examination

### 4. MedicalExaminationFlowE2ETest.java

**Test Coverage:** E2E-EXAM-001 to E2E-EXAM-003

Tests the complete medical examination and prescription workflow:

- ✅ E2E-EXAM-001: Doctor creates exam for appointment
  - Medical exam creation for checked-in appointment
  - Appointment requirement validation
  - Role-based access control
  - Exam retrieval by ID
- ✅ E2E-EXAM-002: Doctor adds diagnosis and notes
  - Add diagnosis to exam
  - Update diagnosis and notes
  - Patient exam history retrieval
- ✅ E2E-EXAM-003: Doctor creates prescription
  - Create prescription with multiple medications
  - Retrieve prescription by exam ID
  - Prescription items validation
  - Patient prescription access
  - Complete examination workflow (exam → diagnosis → prescription)

## 🚀 Running E2E Tests

### Run All E2E Tests

```bash
# From project root
./gradlew :api-gateway:test --tests "*E2ETest"

# Or run specific test class
./gradlew :api-gateway:test --tests "PatientRegistrationFlowE2ETest"
./gradlew :api-gateway:test --tests "AppointmentBookingFlowE2ETest"
./gradlew :api-gateway:test --tests "MedicalExaminationFlowE2ETest"
```

### Run Specific Test Methods

```bash
# Run specific nested class
./gradlew :api-gateway:test --tests "PatientRegistrationFlowE2ETest\$CompletePatientRegistrationTest"

# Run specific test method
./gradlew :api-gateway:test --tests "PatientRegistrationFlowE2ETest.shouldRegisterNewPatientSuccessfully"
```

### From IDE

- **IntelliJ IDEA:** Right-click on test class/method → Run
- **VS Code:** Use Testing sidebar or click run icons

## 📊 Test Statistics

| Flow Category        | Test Cases | Status |
| -------------------- | ---------- | ------ |
| Patient Registration | 13         | ✅     |
| Appointment Booking  | 15         | ✅     |
| Medical Examination  | 14         | ✅     |
| **Total**            | **42**     | ✅     |

## 🔧 Technical Details

### Technology Stack

- **Testing Framework:** JUnit 5
- **HTTP Client:** REST Assured
- **Assertions:** Hamcrest + AssertJ
- **Test Environment:** Spring Boot Test with Random Port

### Test Environment Configuration

- Uses embedded H2 database
- Random port for API Gateway
- JWT keys configured in test properties
- Eureka and Config Server disabled

### Authentication Flow

1. Register user via `/auth/register`
2. Login via `/auth/login` to get JWT token
3. Use token in `Authorization: Bearer <token>` header
4. Token includes user ID and role for authorization

## 📝 Test Data Management

### User Creation

Tests create unique users for each test run using timestamp-based emails:

```java
String email = generateUniqueEmail("patient");
// Results in: patient.1704635847123@hms-e2e-test.com
```

### Roles Used in Tests

- `PATIENT` - End users booking appointments
- `DOCTOR` - Medical professionals conducting exams
- `NURSE` - Supporting medical staff
- `RECEPTIONIST` - Administrative staff managing walk-ins
- `ADMIN` - System administrators (not yet covered in E2E)

## 🎯 Coverage Status

### Completed Flows ✅

- [x] Patient Registration (E2E-REG-001 to E2E-REG-003)
- [x] Appointment Booking (E2E-APT-001 to E2E-APT-005)
- [x] Medical Examination (E2E-EXAM-001 to E2E-EXAM-003)

### Remaining Flows (From TEST_PLAN.md) 📋

- [ ] Lab Order Flow (E2E-LAB-001 to E2E-LAB-003)
- [ ] Lab Test Results (E2E-RESULT-001 to E2E-RESULT-002)
- [ ] Billing and Payment (E2E-BILL-001 to E2E-BILL-003)
- [ ] Medicine Management (E2E-MED-001 to E2E-MED-002)
- [ ] HR and Schedule (E2E-HR-001 to E2E-HR-002)
- [ ] Report Generation (E2E-REP-001)
- [ ] Admin Operations (E2E-ADMIN-001 to E2E-ADMIN-003)

## 🐛 Troubleshooting

### Tests Failing with 401 Unauthorized

- Check JWT keys in test configuration
- Verify token is being generated correctly
- Check `Authorization` header format

### Tests Failing with Connection Refused

- Ensure Spring Boot test is starting properly
- Check for port conflicts
- Verify `@SpringBootTest(webEnvironment = RANDOM_PORT)` is set

### Tests Flaking on CI/CD

- Add appropriate waits for async operations
- Use test data isolation (unique emails/IDs)
- Check for race conditions in appointment booking

### Service Communication Failures

- Verify Feign clients are configured for test environment
- Check service URLs in test properties
- Ensure all required services are available in test context

## 📚 Best Practices

### 1. Test Independence

Each test should be independent and not rely on other tests:

```java
@BeforeEach
void setUp() {
    // Create fresh test data for each test
    patientToken = registerAndLogin(...);
}
```

### 2. Descriptive Test Names

Use BDD-style naming:

```java
@DisplayName("Should successfully register a new patient account with valid data")
void shouldRegisterNewPatientSuccessfully() { ... }
```

### 3. Clear Test Structure

Follow AAA (Arrange-Act-Assert) pattern:

```java
// Given: Setup test data
String email = generateUniqueEmail();

// When: Perform action
Response response = registerUser(email, password, role);

// Then: Verify results
response.then().statusCode(200);
```

### 4. Flexible Assertions

Account for different valid implementations:

```java
response.then()
    .statusCode(anyOf(equalTo(200), equalTo(201)))
    .body("data.status", anyOf(equalTo("SCHEDULED"), equalTo("PENDING")));
```

## 🔗 Related Documentation

- [TEST_PLAN.md](../../docs/TEST_PLAN.md) - Complete test plan
- [TEST_INFRASTRUCTURE_GUIDE.md](../../docs/TEST_INFRASTRUCTURE_GUIDE.md) - Test setup guide
- [TEST_PROGRESS_TRACKER.md](../../docs/TEST_PROGRESS_TRACKER.md) - Test completion tracking

---

**Last Updated:** 2026-01-08  
**Status:** Phase 5 E2E Tests - Patient Registration, Appointment Booking, and Medical Examination flows completed ✅
