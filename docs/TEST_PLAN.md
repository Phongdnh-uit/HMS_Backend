# HMS Backend - Comprehensive Test Plan

## 📋 Project Overview

**Project:** Hospital Management System (HMS) Backend  
**Architecture:** Microservices with Spring Boot / Spring Cloud  
**Services:**
- `api-gateway` - API Gateway with JWT validation and routing
- `auth-service` - Authentication and account management
- `patient-service` - Patient profile management
- `appointment-service` - Appointment scheduling and queue management
- `medical-exam-service` - Medical examinations, prescriptions, lab orders
- `medicine-service` - Medicine and category management
- `hr-service` - HR, department, employee, and schedule management
- `billing-service` - Invoice and payment processing
- `common` - Shared utilities, DTOs, generic controllers
- `config-server` - Centralized configuration
- `discovery-service` - Eureka service discovery

---

## 🧪 Test Types Required

### 1. Unit Tests
Test individual components in isolation.

### 2. Integration Tests
Test component interactions with real or embedded dependencies.

### 3. API/Controller Tests
Test REST endpoints with MockMvc or WebTestClient.

### 4. Repository Tests
Test JPA repositories with embedded H2 database.

### 5. Service-to-Service (Feign Client) Tests
Test inter-service communication.

### 6. Security Tests
Test authentication, authorization, and security configurations.

### 7. End-to-End (E2E) Tests
Test complete user flows through API Gateway.

### 8. Performance/Load Tests
Test system behavior under load.

### 9. Contract Tests
Ensure API contracts are maintained between services.

---

## 📊 Test Coverage Matrix

| Service | Unit | Integration | API | Repository | Security | E2E |
|---------|------|-------------|-----|------------|----------|-----|
| auth-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| patient-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| appointment-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| medical-exam-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| medicine-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| hr-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| billing-service | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| api-gateway | ⬜ | ⬜ | ⬜ | N/A | ⬜ | ⬜ |
| common | ⬜ | ⬜ | N/A | ⬜ | N/A | N/A |
| config-server | ⬜ | ⬜ | ⬜ | N/A | N/A | N/A |
| discovery-service | ⬜ | ⬜ | ⬜ | N/A | N/A | N/A |

**Legend:** ⬜ Not Started | 🟡 In Progress | ✅ Complete

---

## 📝 Detailed Test Checklist

### Phase 1: Unit Tests (Isolation Testing)

#### 1.1 Auth Service Unit Tests
- [ ] **UC-AUTH-001:** `AuthServiceImpl.register()` - Valid registration
- [ ] **UC-AUTH-002:** `AuthServiceImpl.register()` - Duplicate email rejection
- [ ] **UC-AUTH-003:** `AuthServiceImpl.login()` - Valid credentials
- [ ] **UC-AUTH-004:** `AuthServiceImpl.login()` - Invalid credentials
- [ ] **UC-AUTH-005:** `AuthServiceImpl.refreshToken()` - Valid refresh token
- [ ] **UC-AUTH-006:** `AuthServiceImpl.refreshToken()` - Expired token handling
- [ ] **UC-AUTH-007:** `TokenProvider.generateToken()` - Token generation
- [ ] **UC-AUTH-008:** `TokenProvider.validateToken()` - Token validation
- [ ] **UC-AUTH-009:** `TokenProvider.validateToken()` - Expired token rejection
- [ ] **UC-AUTH-010:** `AccountMapper` - Entity to DTO mapping
- [ ] **UC-AUTH-011:** `AccountMapper` - DTO to Entity mapping
- [ ] **UC-AUTH-012:** `AccountHook.beforeCreate()` - Password encoding
- [ ] **UC-AUTH-013:** `CustomUserDetailsService.loadUserByUsername()` - User loading

#### 1.2 Patient Service Unit Tests
- [ ] **UC-PAT-001:** `PatientMapper` - Patient entity to response mapping
- [ ] **UC-PAT-002:** `PatientMapper` - Request to entity mapping
- [ ] **UC-PAT-003:** `PatientHook.beforeCreate()` - Pre-creation validation
- [ ] **UC-PAT-004:** `PatientHook.afterCreate()` - Post-creation processing
- [ ] **UC-PAT-005:** `PatientHelper` - Helper utility methods
- [ ] **UC-PAT-006:** `FileStorageService.uploadFile()` - File upload handling
- [ ] **UC-PAT-007:** `FileStorageService.deleteFile()` - File deletion

#### 1.3 Appointment Service Unit Tests
- [ ] **UC-APT-001:** `AppointmentMapper` - Appointment entity to response
- [ ] **UC-APT-002:** `AppointmentMapper` - Request to entity
- [ ] **UC-APT-003:** `AppointmentService.createAppointment()` - Valid appointment
- [ ] **UC-APT-004:** `AppointmentService.createAppointment()` - Conflict detection
- [ ] **UC-APT-005:** `AppointmentService.cancelAppointment()` - Cancellation logic
- [ ] **UC-APT-006:** `AppointmentService.getAvailableSlots()` - Time slot calculation
- [ ] **UC-APT-007:** `QueueService.addToQueue()` - Queue management
- [ ] **UC-APT-008:** `QueueService.getNextInQueue()` - Queue ordering
- [ ] **UC-APT-009:** `AppointmentHook.beforeCreate()` - Validation hooks
- [ ] **UC-APT-010:** `AppointmentHook.afterUpdate()` - Status change handling

#### 1.4 Medical Exam Service Unit Tests
- [ ] **UC-EXAM-001:** `MedicalExamMapper` - Exam entity to response
- [ ] **UC-EXAM-002:** `MedicalExamMapper` - Request to entity
- [ ] **UC-EXAM-003:** `PrescriptionMapper` - Prescription mapping
- [ ] **UC-EXAM-004:** `PrescriptionItemMapper` - Item mapping
- [ ] **UC-EXAM-005:** `LabOrderMapper` - Lab order mapping
- [ ] **UC-EXAM-006:** `LabTestMapper` - Lab test mapping
- [ ] **UC-EXAM-007:** `LabTestResultMapper` - Result mapping
- [ ] **UC-EXAM-008:** `MedicalExamHook.beforeCreate()` - Appointment validation
- [ ] **UC-EXAM-009:** `MedicalExamHook.beforeDelete()` - Delete prevention
- [ ] **UC-EXAM-010:** `MedicalExamHook.afterRead()` - Data enrichment
- [ ] **UC-EXAM-011:** `PrescriptionHook.beforeCreate()` - Exam validation
- [ ] **UC-EXAM-012:** `LabTestService` - Test CRUD operations
- [ ] **UC-EXAM-013:** `LabTestResultService` - Result CRUD operations

#### 1.5 Medicine Service Unit Tests
- [ ] **UC-MED-001:** `MedicineMapper` - Medicine entity to response
- [ ] **UC-MED-002:** `MedicineMapper` - Request to entity
- [ ] **UC-MED-003:** `CategoryMapper` - Category mapping
- [ ] **UC-MED-004:** `MedicineHook.beforeCreate()` - Validation
- [ ] **UC-MED-005:** `MedicineHook.beforeUpdate()` - Stock validation
- [ ] **UC-MED-006:** `CategoryHook.beforeDelete()` - Cascade prevention

#### 1.6 HR Service Unit Tests
- [ ] **UC-HR-001:** `EmployeeMapper` - Employee mapping
- [ ] **UC-HR-002:** `DepartmentMapper` - Department mapping
- [ ] **UC-HR-003:** `ScheduleMapper` - Schedule mapping
- [ ] **UC-HR-004:** `ScheduleService.createSchedule()` - Schedule creation
- [ ] **UC-HR-005:** `ScheduleService.cancelSchedule()` - Schedule cancellation
- [ ] **UC-HR-006:** `ScheduleService.getAvailableDoctors()` - Doctor availability
- [ ] **UC-HR-007:** `DepartmentHook.beforeDelete()` - Cascade prevention
- [ ] **UC-HR-008:** `EmployeeHook.beforeCreate()` - Account creation
- [ ] **UC-HR-009:** `ScheduleHook.beforeCreate()` - Conflict detection
- [ ] **UC-HR-010:** `FileStorageService` - Employee photo handling

#### 1.7 Billing Service Unit Tests
- [ ] **UC-BILL-001:** `InvoiceMapper` - Invoice mapping
- [ ] **UC-BILL-002:** `PaymentMapper` - Payment mapping
- [ ] **UC-BILL-003:** `VNPayService.createPaymentUrl()` - Payment URL generation
- [ ] **UC-BILL-004:** `VNPayService.verifyPayment()` - Payment verification
- [ ] **UC-BILL-005:** `InvoiceHook.beforeCreate()` - Amount calculation
- [ ] **UC-BILL-006:** `InvoiceHook.afterCreate()` - External service calls

#### 1.8 Common Module Unit Tests
- [ ] **UC-CMN-001:** `GenericController` - CRUD operations
- [ ] **UC-CMN-002:** `GenericService` - Service layer logic
- [ ] **UC-CMN-003:** `CrudService` - Base CRUD functionality
- [ ] **UC-CMN-004:** `GenericMapper` - Base mapping
- [ ] **UC-CMN-005:** `GenericHook` - Hook interface
- [ ] **UC-CMN-006:** `ApiException` - Exception handling
- [ ] **UC-CMN-007:** `GlobalExceptionHandler` - Error responses
- [ ] **UC-CMN-008:** `ApiResponse` - Response wrapping
- [ ] **UC-CMN-009:** `PageResponse` - Pagination handling
- [ ] **UC-CMN-010:** `UserContext` - User context parsing
- [ ] **UC-CMN-011:** `UserContextFilter` - Header extraction
- [ ] **UC-CMN-012:** `FeignHelper` - Feign utilities
- [ ] **UC-CMN-013:** `FeignCustomErrorDecoder` - Error decoding

#### 1.9 API Gateway Unit Tests
- [ ] **UC-GW-001:** `AuthFilter` - JWT validation
- [ ] **UC-GW-002:** `AuthFilter` - Header injection
- [ ] **UC-GW-003:** `SecurityConfig` - Route security rules
- [ ] **UC-GW-004:** `CorsConfig` - CORS configuration
- [ ] **UC-GW-005:** `SecurityConstant` - Public endpoints list

---

### Phase 2: Integration Tests

#### 2.1 Repository Integration Tests
- [ ] **IT-REPO-001:** `AccountRepository` - CRUD with H2
- [ ] **IT-REPO-002:** `AccountRepository.findByEmail()` - Custom query
- [ ] **IT-REPO-003:** `PatientRepository` - CRUD with H2
- [ ] **IT-REPO-004:** `PatientRepository.findByAccountId()` - Custom query
- [ ] **IT-REPO-005:** `AppointmentRepository` - CRUD with H2
- [ ] **IT-REPO-006:** `AppointmentRepository` - Complex queries (by date, status)
- [ ] **IT-REPO-007:** `MedicalExamRepository` - CRUD with H2
- [ ] **IT-REPO-008:** `MedicalExamRepository.existsByAppointmentId()` - Custom query
- [ ] **IT-REPO-009:** `PrescriptionRepository` - CRUD with H2
- [ ] **IT-REPO-010:** `LabOrderRepository` - CRUD with H2
- [ ] **IT-REPO-011:** `LabTestRepository` - CRUD with H2
- [ ] **IT-REPO-012:** `LabTestResultRepository` - CRUD with H2
- [ ] **IT-REPO-013:** `MedicineRepository` - CRUD with H2
- [ ] **IT-REPO-014:** `CategoryRepository` - CRUD with H2
- [ ] **IT-REPO-015:** `EmployeeRepository` - CRUD with H2
- [ ] **IT-REPO-016:** `DepartmentRepository` - CRUD with H2
- [ ] **IT-REPO-017:** `ScheduleRepository` - CRUD with H2
- [ ] **IT-REPO-018:** `InvoiceRepository` - CRUD with H2
- [ ] **IT-REPO-019:** `PaymentRepository` - CRUD with H2
- [ ] **IT-REPO-020:** `InvoiceItemRepository` - CRUD with H2

#### 2.2 Service Integration Tests
- [ ] **IT-SVC-001:** `AuthService` - Full registration flow
- [ ] **IT-SVC-002:** `AuthService` - Full login flow
- [ ] **IT-SVC-003:** `AuthService` - Token refresh flow
- [ ] **IT-SVC-004:** `PatientController + Repository` - CRUD integration
- [ ] **IT-SVC-005:** `AppointmentService + Repository` - Booking flow
- [ ] **IT-SVC-006:** `MedicalExamService + Hooks` - Exam creation
- [ ] **IT-SVC-007:** `PrescriptionService + MedicineClient` - Prescription flow
- [ ] **IT-SVC-008:** `LabOrderService + LabTestService` - Lab workflow
- [ ] **IT-SVC-009:** `ScheduleService + EmployeeRepository` - Scheduling
- [ ] **IT-SVC-010:** `InvoiceService + PaymentService` - Billing flow

#### 2.3 Feign Client Integration Tests (with WireMock)
- [ ] **IT-FEIGN-001:** `PatientClient` - Patient data fetching
- [ ] **IT-FEIGN-002:** `HrClient` - Employee/Doctor data fetching
- [ ] **IT-FEIGN-003:** `AppointmentClient` - Appointment data fetching
- [ ] **IT-FEIGN-004:** `BillingClient` - Invoice creation
- [ ] **IT-FEIGN-005:** `MedicalExamClient` - Exam data fetching
- [ ] **IT-FEIGN-006:** `AccountClient` - Account management
- [ ] **IT-FEIGN-007:** Feign error handling - 4xx errors
- [ ] **IT-FEIGN-008:** Feign error handling - 5xx errors
- [ ] **IT-FEIGN-009:** Feign timeout handling
- [ ] **IT-FEIGN-010:** Feign retry mechanism

---

### Phase 3: API/Controller Tests

#### 3.1 Auth Controller Tests
- [ ] **API-AUTH-001:** `POST /auth/login` - Valid credentials, returns JWT
- [ ] **API-AUTH-002:** `POST /auth/login` - Invalid credentials, returns 401
- [ ] **API-AUTH-003:** `POST /auth/register` - Valid registration
- [ ] **API-AUTH-004:** `POST /auth/register` - Duplicate email, returns 400
- [ ] **API-AUTH-005:** `POST /auth/register` - Invalid input validation
- [ ] **API-AUTH-006:** `POST /auth/refresh` - Valid refresh token
- [ ] **API-AUTH-007:** `POST /auth/refresh` - Expired token, returns 401
- [ ] **API-AUTH-008:** `GET /auth/me` - Returns current user profile
- [ ] **API-AUTH-009:** `POST /auth/logout` - Successful logout

#### 3.2 Account Controller Tests
- [ ] **API-ACC-001:** `GET /accounts` - List all accounts (admin)
- [ ] **API-ACC-002:** `GET /accounts/{id}` - Get account by ID
- [ ] **API-ACC-003:** `POST /accounts` - Create new account
- [ ] **API-ACC-004:** `PUT /accounts/{id}` - Update account
- [ ] **API-ACC-005:** `DELETE /accounts/{id}` - Delete account
- [ ] **API-ACC-006:** `GET /accounts` - Pagination support
- [ ] **API-ACC-007:** `GET /accounts` - Search/filter support

#### 3.3 Patient Controller Tests
- [ ] **API-PAT-001:** `GET /patients` - List all patients
- [ ] **API-PAT-002:** `GET /patients/{id}` - Get patient by ID
- [ ] **API-PAT-003:** `POST /patients` - Create new patient
- [ ] **API-PAT-004:** `PUT /patients/{id}` - Update patient
- [ ] **API-PAT-005:** `DELETE /patients/{id}` - Delete patient
- [ ] **API-PAT-006:** `GET /patients/me` - Get current patient profile
- [ ] **API-PAT-007:** `PATCH /patients/me` - Update own profile
- [ ] **API-PAT-008:** `GET /patients` - Search by name
- [ ] **API-PAT-009:** `GET /patients` - Pagination

#### 3.4 Appointment Controller Tests
- [ ] **API-APT-001:** `GET /appointments` - List appointments
- [ ] **API-APT-002:** `GET /appointments/{id}` - Get appointment by ID
- [ ] **API-APT-003:** `POST /appointments` - Book new appointment
- [ ] **API-APT-004:** `POST /appointments` - Walk-in appointment
- [ ] **API-APT-005:** `PUT /appointments/{id}` - Update appointment
- [ ] **API-APT-006:** `DELETE /appointments/{id}` - Cancel appointment
- [ ] **API-APT-007:** `GET /appointments/by-patient/{patientId}` - Patient appointments
- [ ] **API-APT-008:** `GET /appointments/by-doctor/{doctorId}` - Doctor appointments
- [ ] **API-APT-009:** `GET /appointments/available-slots` - Time slot availability
- [ ] **API-APT-010:** `POST /appointments/{id}/check-in` - Check-in
- [ ] **API-APT-011:** `POST /appointments/{id}/check-out` - Check-out
- [ ] **API-APT-012:** `GET /appointments/queue` - Queue status
- [ ] **API-APT-013:** `GET /appointments/stats` - Statistics

#### 3.5 Medical Exam Controller Tests
- [ ] **API-EXAM-001:** `GET /exams` - List medical exams
- [ ] **API-EXAM-002:** `GET /exams/{id}` - Get exam by ID
- [ ] **API-EXAM-003:** `POST /exams` - Create new exam
- [ ] **API-EXAM-004:** `PUT /exams/{id}` - Update exam
- [ ] **API-EXAM-005:** `GET /exams/by-appointment/{appointmentId}` - Get by appointment
- [ ] **API-EXAM-006:** `GET /exams/by-patient/{patientId}` - Patient exam history

#### 3.6 Prescription Controller Tests
- [ ] **API-PRESC-001:** `GET /prescriptions` - List prescriptions
- [ ] **API-PRESC-002:** `GET /prescriptions/{id}` - Get prescription by ID
- [ ] **API-PRESC-003:** `POST /prescriptions` - Create prescription
- [ ] **API-PRESC-004:** `PUT /prescriptions/{id}` - Update prescription
- [ ] **API-PRESC-005:** `DELETE /prescriptions/{id}` - Cancel prescription
- [ ] **API-PRESC-006:** `GET /prescriptions/by-exam/{examId}` - By exam

#### 3.7 Lab Order Controller Tests
- [ ] **API-LAB-001:** `GET /lab-orders` - List lab orders
- [ ] **API-LAB-002:** `GET /lab-orders/{id}` - Get lab order by ID
- [ ] **API-LAB-003:** `POST /lab-orders` - Create lab order
- [ ] **API-LAB-004:** `PUT /lab-orders/{id}` - Update lab order
- [ ] **API-LAB-005:** `DELETE /lab-orders/{id}` - Cancel lab order
- [ ] **API-LAB-006:** `PATCH /lab-orders/{id}/status` - Update status

#### 3.8 Lab Test Controller Tests
- [ ] **API-TEST-001:** `GET /lab-tests` - List available tests
- [ ] **API-TEST-002:** `GET /lab-tests/{id}` - Get test by ID
- [ ] **API-TEST-003:** `POST /lab-tests` - Create test type
- [ ] **API-TEST-004:** `PUT /lab-tests/{id}` - Update test type
- [ ] **API-TEST-005:** `DELETE /lab-tests/{id}` - Delete test type
- [ ] **API-TEST-006:** `GET /lab-tests/by-category/{category}` - By category

#### 3.9 Lab Test Result Controller Tests
- [ ] **API-RESULT-001:** `GET /lab-results` - List results
- [ ] **API-RESULT-002:** `GET /lab-results/{id}` - Get result by ID
- [ ] **API-RESULT-003:** `POST /lab-results` - Create result
- [ ] **API-RESULT-004:** `PUT /lab-results/{id}` - Update result
- [ ] **API-RESULT-005:** `GET /lab-results/by-order/{orderId}` - By order

#### 3.10 Medicine Controller Tests
- [ ] **API-MED-001:** `GET /medicines` - List medicines
- [ ] **API-MED-002:** `GET /medicines/{id}` - Get medicine by ID
- [ ] **API-MED-003:** `POST /medicines` - Create medicine
- [ ] **API-MED-004:** `PUT /medicines/{id}` - Update medicine
- [ ] **API-MED-005:** `DELETE /medicines/{id}` - Delete medicine
- [ ] **API-MED-006:** `PATCH /medicines/{id}/stock` - Update stock
- [ ] **API-MED-007:** `GET /medicines/low-stock` - Low stock alert

#### 3.11 Category Controller Tests
- [ ] **API-CAT-001:** `GET /categories` - List categories
- [ ] **API-CAT-002:** `GET /categories/{id}` - Get category by ID
- [ ] **API-CAT-003:** `POST /categories` - Create category
- [ ] **API-CAT-004:** `PUT /categories/{id}` - Update category
- [ ] **API-CAT-005:** `DELETE /categories/{id}` - Delete category

#### 3.12 Employee Controller Tests
- [ ] **API-EMP-001:** `GET /employees` - List employees
- [ ] **API-EMP-002:** `GET /employees/{id}` - Get employee by ID
- [ ] **API-EMP-003:** `POST /employees` - Create employee
- [ ] **API-EMP-004:** `PUT /employees/{id}` - Update employee
- [ ] **API-EMP-005:** `DELETE /employees/{id}` - Delete employee
- [ ] **API-EMP-006:** `GET /employees/doctors` - List doctors
- [ ] **API-EMP-007:** `GET /employees/by-department/{deptId}` - By department

#### 3.13 Department Controller Tests
- [ ] **API-DEPT-001:** `GET /departments` - List departments
- [ ] **API-DEPT-002:** `GET /departments/{id}` - Get department by ID
- [ ] **API-DEPT-003:** `POST /departments` - Create department
- [ ] **API-DEPT-004:** `PUT /departments/{id}` - Update department
- [ ] **API-DEPT-005:** `DELETE /departments/{id}` - Delete department

#### 3.14 Schedule Controller Tests
- [ ] **API-SCHED-001:** `GET /schedules` - List schedules
- [ ] **API-SCHED-002:** `GET /schedules/{id}` - Get schedule by ID
- [ ] **API-SCHED-003:** `POST /schedules` - Create schedule
- [ ] **API-SCHED-004:** `PUT /schedules/{id}` - Update schedule
- [ ] **API-SCHED-005:** `DELETE /schedules/{id}` - Cancel schedule
- [ ] **API-SCHED-006:** `GET /schedules/by-employee/{empId}` - By employee
- [ ] **API-SCHED-007:** `GET /schedules/doctors` - Available doctors

#### 3.15 Invoice Controller Tests
- [ ] **API-INV-001:** `GET /invoices` - List invoices
- [ ] **API-INV-002:** `GET /invoices/{id}` - Get invoice by ID
- [ ] **API-INV-003:** `POST /invoices` - Create invoice
- [ ] **API-INV-004:** `PUT /invoices/{id}` - Update invoice
- [ ] **API-INV-005:** `DELETE /invoices/{id}` - Cancel invoice
- [ ] **API-INV-006:** `GET /invoices/by-patient/{patientId}` - By patient
- [ ] **API-INV-007:** `GET /invoices/stats` - Statistics

#### 3.16 Payment Controller Tests
- [ ] **API-PAY-001:** `POST /payments/init` - Initialize VNPay payment
- [ ] **API-PAY-002:** `GET /payments/callback` - VNPay callback handling
- [ ] **API-PAY-003:** `GET /payments/{id}` - Get payment by ID
- [ ] **API-PAY-004:** `GET /payments/by-invoice/{invoiceId}` - By invoice

---

### Phase 4: Security Tests

#### 4.1 Authentication Tests
- [ ] **SEC-AUTH-001:** Valid JWT grants access
- [ ] **SEC-AUTH-002:** Missing JWT returns 401
- [ ] **SEC-AUTH-003:** Invalid JWT returns 401
- [ ] **SEC-AUTH-004:** Expired JWT returns 401
- [ ] **SEC-AUTH-005:** Malformed JWT returns 401

#### 4.2 Authorization Tests
- [ ] **SEC-AUTHZ-001:** Admin can access admin endpoints
- [ ] **SEC-AUTHZ-002:** Doctor can access doctor endpoints
- [ ] **SEC-AUTHZ-003:** Nurse can access nurse endpoints
- [ ] **SEC-AUTHZ-004:** Receptionist can access receptionist endpoints
- [ ] **SEC-AUTHZ-005:** Patient can access patient endpoints
- [ ] **SEC-AUTHZ-006:** Patient cannot access admin endpoints
- [ ] **SEC-AUTHZ-007:** Unauthorized role returns 403
- [ ] **SEC-AUTHZ-008:** Cross-patient data access prevented

#### 4.3 Gateway Security Tests
- [ ] **SEC-GW-001:** Public endpoints accessible without auth
- [ ] **SEC-GW-002:** Protected endpoints require auth
- [ ] **SEC-GW-003:** X-User-* headers injected correctly
- [ ] **SEC-GW-004:** Direct service access blocked in prod

#### 4.4 Input Validation Tests
- [ ] **SEC-VAL-001:** SQL injection prevention
- [ ] **SEC-VAL-002:** XSS prevention
- [ ] **SEC-VAL-003:** Request body size limits
- [ ] **SEC-VAL-004:** Path traversal prevention
- [ ] **SEC-VAL-005:** CORS configuration validation

---

### Phase 5: End-to-End (E2E) Tests

#### 5.1 Patient Registration Flow
- [ ] **E2E-REG-001:** Complete patient registration through gateway
- [ ] **E2E-REG-002:** Login after registration
- [ ] **E2E-REG-003:** View own profile

#### 5.2 Appointment Booking Flow
- [ ] **E2E-APT-001:** Patient books appointment with available doctor
- [ ] **E2E-APT-002:** Patient views their appointments
- [ ] **E2E-APT-003:** Patient cancels appointment
- [ ] **E2E-APT-004:** Receptionist creates walk-in appointment
- [ ] **E2E-APT-005:** Patient check-in flow

#### 5.3 Medical Examination Flow
- [ ] **E2E-EXAM-001:** Doctor creates exam for appointment
- [ ] **E2E-EXAM-002:** Doctor adds diagnosis and notes
- [ ] **E2E-EXAM-003:** Doctor creates prescription
- [ ] **E2E-EXAM-004:** Doctor orders lab tests
- [ ] **E2E-EXAM-005:** Lab technician enters results
- [ ] **E2E-EXAM-006:** Complete exam with billing

#### 5.4 Billing Flow
- [ ] **E2E-BILL-001:** Invoice created after exam
- [ ] **E2E-BILL-002:** Patient views invoice
- [ ] **E2E-BILL-003:** VNPay payment flow
- [ ] **E2E-BILL-004:** Payment confirmation

#### 5.5 HR Management Flow
- [ ] **E2E-HR-001:** Admin creates department
- [ ] **E2E-HR-002:** Admin creates employee/doctor
- [ ] **E2E-HR-003:** Admin creates schedule for doctor
- [ ] **E2E-HR-004:** View doctor availability

---

### Phase 6: Performance Tests

#### 6.1 Load Tests
- [ ] **PERF-LOAD-001:** 100 concurrent login requests
- [ ] **PERF-LOAD-002:** 50 concurrent appointment bookings
- [ ] **PERF-LOAD-003:** 200 concurrent patient list queries
- [ ] **PERF-LOAD-004:** Gateway routing under load

#### 6.2 Stress Tests
- [ ] **PERF-STRESS-001:** Database connection pool limits
- [ ] **PERF-STRESS-002:** Memory usage under sustained load
- [ ] **PERF-STRESS-003:** Service recovery after overload

#### 6.3 Endurance Tests
- [ ] **PERF-END-001:** 24-hour continuous operation
- [ ] **PERF-END-002:** Memory leak detection
- [ ] **PERF-END-003:** Connection leak detection

---

### Phase 7: Contract Tests

#### 7.1 API Contract Tests
- [ ] **CONTRACT-001:** Auth service API contract
- [ ] **CONTRACT-002:** Patient service API contract
- [ ] **CONTRACT-003:** Appointment service API contract
- [ ] **CONTRACT-004:** Medical exam service API contract
- [ ] **CONTRACT-005:** Medicine service API contract
- [ ] **CONTRACT-006:** HR service API contract
- [ ] **CONTRACT-007:** Billing service API contract

#### 7.2 Feign Client Contract Tests
- [ ] **CONTRACT-FEIGN-001:** PatientClient contract
- [ ] **CONTRACT-FEIGN-002:** HrClient contract
- [ ] **CONTRACT-FEIGN-003:** AppointmentClient contract
- [ ] **CONTRACT-FEIGN-004:** BillingClient contract
- [ ] **CONTRACT-FEIGN-005:** MedicalExamClient contract

---

## 🛠️ Test Infrastructure Setup Checklist

### Dependencies to Add
- [ ] JUnit 5 (Jupiter)
- [ ] Mockito
- [ ] AssertJ
- [ ] H2 Database (test scope)
- [ ] Spring Boot Test
- [ ] Spring Security Test
- [ ] Testcontainers (for integration tests)
- [ ] WireMock (for Feign client tests)
- [ ] JaCoCo (code coverage)
- [ ] Gatling/JMeter (performance tests)
- [ ] Rest-Assured (API tests)
- [ ] Pact (contract tests)

### Test Configuration Files
- [ ] `application-test.yml` for each service
- [ ] Test database configurations
- [ ] Mock configurations for external services
- [ ] Test data fixtures/factories

### CI/CD Integration
- [ ] GitHub Actions workflow for tests
- [ ] Coverage report generation
- [ ] Test result publishing
- [ ] Quality gate configuration

---

## 📈 Coverage Goals

| Category | Target Coverage |
|----------|-----------------|
| Unit Tests | 80%+ line coverage |
| Integration Tests | 70%+ critical paths |
| API Tests | 100% endpoints |
| Security Tests | 100% auth/authz scenarios |
| E2E Tests | Core business flows |

---

## 📅 Recommended Test Priority

### Week 1-2: Foundation
1. Set up test infrastructure
2. Unit tests for `common` module
3. Unit tests for `auth-service`

### Week 3-4: Core Services
4. Unit tests for remaining services
5. Repository integration tests
6. Service integration tests

### Week 5-6: API & Security
7. Controller/API tests for all services
8. Security tests
9. Feign client tests

### Week 7-8: E2E & Performance
10. End-to-End tests
11. Performance tests
12. Contract tests

---

## 📝 Notes

- All tests should disable Eureka client and Config Server for isolation
- Use `@SpringBootTest` with H2 for integration tests
- Mock Feign clients in unit tests using `@MockBean`
- Use `@WithMockUser` for security context in tests
- Generate test coverage reports with each PR

---

*Last Updated: 2026-01-07*
*Created by: Test Planning Assistant*
