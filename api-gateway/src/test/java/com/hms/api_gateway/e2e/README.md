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

### 5. BillingPaymentFlowE2ETest.java

**Test Coverage:** E2E-BILL-001 to E2E-BILL-004

Tests the complete billing and payment workflow through VNPay:

- ✅ E2E-BILL-001: Invoice created after exam
  - Automatic invoice generation after exam
  - Manual invoice creation for appointment
  - Invoice retrieval by exam ID
  - Patient invoice listing
- ✅ E2E-BILL-002: Patient views invoice
  - Invoice retrieval by ID
  - Invoice details with line items
  - Cross-patient access prevention
  - Payment status viewing
- ✅ E2E-BILL-003: VNPay payment flow
  - VNPay payment initialization
  - Payment amount validation
  - Payment retrieval by invoice
  - Payment retrieval by ID
  - Complete payment workflow
- ✅ E2E-BILL-004: Payment confirmation
  - Payment status verification
  - VNPay callback handling
  - Invoice status update after payment

### 6. MedicineManagementFlowE2ETest.java

**Test Coverage:** E2E-MED-001 to E2E-MED-003

Tests the complete medicine and prescription dispensing workflow:

- ✅ E2E-MED-001: Pharmacist views and manages prescriptions
  - Prescription viewing by ID
  - Prescription items with medicine details
  - Prescriptions listing by patient
  - Prescription filtering by status
  - Prescription retrieval by exam ID
- ✅ E2E-MED-002: Prescription dispensing and stock updates
  - Initial medicine stock retrieval
  - Prescription dispensing
  - Stock update verification after dispensing
  - Double dispensing prevention
  - Prescription status tracking
  - Dispensed timestamp tracking
- ✅ E2E-MED-003: Medicine inventory management
  - Medicine listing
  - Medicine filtering by category
  - Medicine search by name
  - Medicine details viewing
  - Stock updates
  - Category listing
  - Low stock handling
  - Complete medicine management workflow

### 7. HRManagementFlowE2ETest.java

**Test Coverage:** E2E-HR-001 to E2E-HR-004

Tests the complete HR and schedule management workflow:

- ✅ E2E-HR-001: Admin creates department
  - Department creation with valid data
  - Required field validation
  - Duplicate department name prevention
  - Department listing
  - Department retrieval by ID
  - Non-admin authorization rejection
- ✅ E2E-HR-002: Admin creates employee/doctor
  - Doctor employee creation with account linking
  - Nurse employee creation
  - Invalid license number format rejection
  - Employee listing
  - Employee retrieval by ID
  - Self-service employee profile viewing
- ✅ E2E-HR-003: Admin creates schedule for doctor
  - Doctor schedule creation
  - Multiple schedules for work week
  - Required field validation
  - Schedule status updates
  - Schedule retrieval by ID
- ✅ E2E-HR-004: View doctor availability
  - Patient viewing doctor schedules
  - Schedule filtering by status
  - Schedule filtering by doctor
  - Schedule filtering by department
  - Schedule lookup by doctor and date
  - Doctor viewing own schedules
  - Empty result handling for future dates

### 8. LabTestFlowE2ETest.java

**Test Coverage:** E2E-LAB-001 to E2E-LAB-003

Tests the complete laboratory test workflow:

- ✅ E2E-LAB-001: Doctor orders lab tests for examination
  - Create lab order with multiple tests
  - Lab order with single test
  - Required field validation (medical exam ID)
  - Retrieve lab order by ID
  - Retrieve lab orders for medical exam
  - Priority handling (NORMAL, URGENT)
- ✅ E2E-LAB-002: Lab technician views pending tests
  - List all lab orders for technician
  - View lab test results for an exam
  - View patient's lab test history
  - Retrieve all available lab tests
- ✅ E2E-LAB-003: Lab technician enters test results
  - Update lab test result with values
  - Flag abnormal test results
  - Retrieve lab result by ID after entry
  - Allow partial result updates
  - Complete full lab test workflow (order → perform → complete → view)
  - Allow doctor to interpret completed results

## 🚀 Running E2E Tests

### Run All E2E Tests

```bash
# From project root
./gradlew :api-gateway:test --tests "*E2ETest"

# Or run specific test class
./gradlew :api-gateway:test --tests "PatientRegistrationFlowE2ETest"
./gradlew :api-gateway:test --tests "AppointmentBookingFlowE2ETest"
./gradlew :api-gateway:test --tests "MedicalExaminationFlowE2ETest"
./gradlew :api-gateway:test --tests "BillingPaymentFlowE2ETest"
./gradlew :api-gateway:test --tests "MedicineManagementFlowE2ETest"
./gradlew :api-gateway:test --tests "HRManagementFlowE2ETest"
./gradlew :api-gateway:test --tests "LabTestFlowE2ETest"
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
| Billing and Payment  | 18         | ✅     |
| Medicine Management  | 18         | ✅     |
| HR and Schedule      | 24         | ✅     |
| Lab Test Flow        | 18         | ✅     |
| **Total**            | **120**    | ✅     |
| **Total**            | **102**    | ✅     |

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
- `DOCTOR` - Medical professionals conducting exams and ordering lab tests
- `PHARMACIST` - Pharmacy staff dispensing medications
- `LAB_TECHNICIAN` - Lab technicians entering test results (or DOCTOR role)
- `ADMIN` - System administrators managing medicines, categories, departments, employees
- `RECEPTIONIST` - Administrative staff managing walk-ins

## 🎯 Coverage Status

### Completed Flows ✅

- [x] Patient Registration (E2E-REG-001 to E2E-REG-003) - 13 tests
- [x] Appointment Booking (E2E-APT-001 to E2E-APT-005) - 15 tests
- [x] Medical Examination (E2E-EXAM-001 to E2E-EXAM-003) - 14 tests
- [x] Billing and Payment (E2E-BILL-001 to E2E-BILL-004) - 18 tests
- [x] Medicine Management (E2E-MED-001 to E2E-MED-003) - 18 tests
- [x] HR and Schedule Management (E2E-HR-001 to E2E-HR-004) - 24 tests
- [x] Lab Test Flow (E2E-LAB-001 to E2E-LAB-003) - 18 tests

**Total: 120 E2E tests implemented** ✅

### Remaining Flows (From TEST_PLAN.md) 📋

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

**Summary:** 120 tests implemented across 7 major flows ✅

**Implemented Flows:**

- ✅ Patient Registration Flow (13 tests)
- ✅ Appointment Booking Flow (15 tests)
- ✅ Medical Examination Flow (14 tests)
- ✅ Billing and Payment Flow (18 tests)
- ✅ Medicine Management Flow (18 tests)
- ✅ HR and Schedule Management Flow (24 tests)
- ✅ Lab Test Flow (18 tests)

**Last Updated:** 2026-01-08  
**Status:** Phase 5 E2E Tests - 7 major flows completed ✅
