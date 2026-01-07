# HMS Backend - Test Progress Tracker

## 🎯 Quick Stats Dashboard

| Category          | Total   | Completed | Progress |
| ----------------- | ------- | --------- | -------- |
| Unit Tests        | 75      | 0         | 0%       |
| Integration Tests | 30      | 0         | 0%       |
| API Tests         | 87      | 0         | 0%       |
| Security Tests    | 22      | 0         | 0%       |
| E2E Tests         | 21      | 0         | 0%       |
| Performance Tests | 10      | 0         | 0%       |
| Contract Tests    | 12      | 0         | 0%       |
| **TOTAL**         | **257** | **0**     | **0%**   |

---

## ✅ How to Use This Tracker

Replace `[ ]` with `[x]` when you complete a test. Update the stats dashboard periodically.

---

## 📦 Phase 1: Unit Tests

### Auth Service (13 tests)

| ID          | Test Case                                                 | Status | Notes |
| ----------- | --------------------------------------------------------- | ------ | ----- |
| UC-AUTH-001 | `AuthServiceImpl.register()` - Valid registration         | [ ]    |       |
| UC-AUTH-002 | `AuthServiceImpl.register()` - Duplicate email rejection  | [ ]    |       |
| UC-AUTH-003 | `AuthServiceImpl.login()` - Valid credentials             | [ ]    |       |
| UC-AUTH-004 | `AuthServiceImpl.login()` - Invalid credentials           | [ ]    |       |
| UC-AUTH-005 | `AuthServiceImpl.refreshToken()` - Valid refresh token    | [ ]    |       |
| UC-AUTH-006 | `AuthServiceImpl.refreshToken()` - Expired token handling | [ ]    |       |
| UC-AUTH-007 | `TokenProvider.generateToken()` - Token generation        | [ ]    |       |
| UC-AUTH-008 | `TokenProvider.validateToken()` - Token validation        | [ ]    |       |
| UC-AUTH-009 | `TokenProvider.validateToken()` - Expired token rejection | [ ]    |       |
| UC-AUTH-010 | `AccountMapper` - Entity to DTO mapping                   | [ ]    |       |
| UC-AUTH-011 | `AccountMapper` - DTO to Entity mapping                   | [ ]    |       |
| UC-AUTH-012 | `AccountHook.beforeCreate()` - Password encoding          | [ ]    |       |
| UC-AUTH-013 | `CustomUserDetailsService.loadUserByUsername()`           | [ ]    |       |

### Patient Service (7 tests)

| ID         | Test Case                                                | Status | Notes |
| ---------- | -------------------------------------------------------- | ------ | ----- |
| UC-PAT-001 | `PatientMapper` - Patient entity to response mapping     | [ ]    |       |
| UC-PAT-002 | `PatientMapper` - Request to entity mapping              | [ ]    |       |
| UC-PAT-003 | `PatientHook.beforeCreate()` - Pre-creation validation   | [ ]    |       |
| UC-PAT-004 | `PatientHook.afterCreate()` - Post-creation processing   | [ ]    |       |
| UC-PAT-005 | `PatientHelper` - Helper utility methods                 | [ ]    |       |
| UC-PAT-006 | `FileStorageService.uploadFile()` - File upload handling | [ ]    |       |
| UC-PAT-007 | `FileStorageService.deleteFile()` - File deletion        | [ ]    |       |

### Appointment Service (10 tests)

| ID         | Test Case                                                     | Status | Notes |
| ---------- | ------------------------------------------------------------- | ------ | ----- |
| UC-APT-001 | `AppointmentMapper` - Entity to response                      | [ ]    |       |
| UC-APT-002 | `AppointmentMapper` - Request to entity                       | [ ]    |       |
| UC-APT-003 | `AppointmentService.createAppointment()` - Valid appointment  | [ ]    |       |
| UC-APT-004 | `AppointmentService.createAppointment()` - Conflict detection | [ ]    |       |
| UC-APT-005 | `AppointmentService.cancelAppointment()` - Cancellation logic | [ ]    |       |
| UC-APT-006 | `AppointmentService.getAvailableSlots()` - Time slot calc     | [ ]    |       |
| UC-APT-007 | `QueueService.addToQueue()` - Queue management                | [ ]    |       |
| UC-APT-008 | `QueueService.getNextInQueue()` - Queue ordering              | [ ]    |       |
| UC-APT-009 | `AppointmentHook.beforeCreate()` - Validation hooks           | [ ]    |       |
| UC-APT-010 | `AppointmentHook.afterUpdate()` - Status change handling      | [ ]    |       |

### Medical Exam Service (13 tests)

| ID          | Test Case                                                 | Status | Notes |
| ----------- | --------------------------------------------------------- | ------ | ----- |
| UC-EXAM-001 | `MedicalExamMapper` - Exam entity to response             | [ ]    |       |
| UC-EXAM-002 | `MedicalExamMapper` - Request to entity                   | [ ]    |       |
| UC-EXAM-003 | `PrescriptionMapper` - Prescription mapping               | [ ]    |       |
| UC-EXAM-004 | `PrescriptionItemMapper` - Item mapping                   | [ ]    |       |
| UC-EXAM-005 | `LabOrderMapper` - Lab order mapping                      | [ ]    |       |
| UC-EXAM-006 | `LabTestMapper` - Lab test mapping                        | [ ]    |       |
| UC-EXAM-007 | `LabTestResultMapper` - Result mapping                    | [ ]    |       |
| UC-EXAM-008 | `MedicalExamHook.beforeCreate()` - Appointment validation | [ ]    |       |
| UC-EXAM-009 | `MedicalExamHook.beforeDelete()` - Delete prevention      | [ ]    |       |
| UC-EXAM-010 | `MedicalExamHook.afterRead()` - Data enrichment           | [ ]    |       |
| UC-EXAM-011 | `PrescriptionHook.beforeCreate()` - Exam validation       | [ ]    |       |
| UC-EXAM-012 | `LabTestService` - Test CRUD operations                   | [ ]    |       |
| UC-EXAM-013 | `LabTestResultService` - Result CRUD operations           | [ ]    |       |

### Medicine Service (6 tests)

| ID         | Test Case                                          | Status | Notes |
| ---------- | -------------------------------------------------- | ------ | ----- |
| UC-MED-001 | `MedicineMapper` - Medicine entity to response     | [ ]    |       |
| UC-MED-002 | `MedicineMapper` - Request to entity               | [ ]    |       |
| UC-MED-003 | `CategoryMapper` - Category mapping                | [ ]    |       |
| UC-MED-004 | `MedicineHook.beforeCreate()` - Validation         | [ ]    |       |
| UC-MED-005 | `MedicineHook.beforeUpdate()` - Stock validation   | [ ]    |       |
| UC-MED-006 | `CategoryHook.beforeDelete()` - Cascade prevention | [ ]    |       |

### HR Service (10 tests)

| ID        | Test Case                                                     | Status | Notes |
| --------- | ------------------------------------------------------------- | ------ | ----- |
| UC-HR-001 | `EmployeeMapper` - Employee mapping                           | [ ]    |       |
| UC-HR-002 | `DepartmentMapper` - Department mapping                       | [ ]    |       |
| UC-HR-003 | `ScheduleMapper` - Schedule mapping                           | [ ]    |       |
| UC-HR-004 | `ScheduleService.createSchedule()` - Schedule creation        | [ ]    |       |
| UC-HR-005 | `ScheduleService.cancelSchedule()` - Cancellation             | [ ]    |       |
| UC-HR-006 | `ScheduleService.getAvailableDoctors()` - Doctor availability | [ ]    |       |
| UC-HR-007 | `DepartmentHook.beforeDelete()` - Cascade prevention          | [ ]    |       |
| UC-HR-008 | `EmployeeHook.beforeCreate()` - Account creation              | [ ]    |       |
| UC-HR-009 | `ScheduleHook.beforeCreate()` - Conflict detection            | [ ]    |       |
| UC-HR-010 | `FileStorageService` - Employee photo handling                | [ ]    |       |

### Billing Service (6 tests)

| ID          | Test Case                                                  | Status | Notes |
| ----------- | ---------------------------------------------------------- | ------ | ----- |
| UC-BILL-001 | `InvoiceMapper` - Invoice mapping                          | [ ]    |       |
| UC-BILL-002 | `PaymentMapper` - Payment mapping                          | [ ]    |       |
| UC-BILL-003 | `VNPayService.createPaymentUrl()` - Payment URL generation | [ ]    |       |
| UC-BILL-004 | `VNPayService.verifyPayment()` - Payment verification      | [ ]    |       |
| UC-BILL-005 | `InvoiceHook.beforeCreate()` - Amount calculation          | [ ]    |       |
| UC-BILL-006 | `InvoiceHook.afterCreate()` - External service calls       | [ ]    |       |

### Common Module (13 tests)

| ID         | Test Case                                  | Status | Notes |
| ---------- | ------------------------------------------ | ------ | ----- |
| UC-CMN-001 | `GenericController` - CRUD operations      | [ ]    |       |
| UC-CMN-002 | `GenericService` - Service layer logic     | [ ]    |       |
| UC-CMN-003 | `CrudService` - Base CRUD functionality    | [ ]    |       |
| UC-CMN-004 | `GenericMapper` - Base mapping             | [ ]    |       |
| UC-CMN-005 | `GenericHook` - Hook interface             | [ ]    |       |
| UC-CMN-006 | `ApiException` - Exception handling        | [ ]    |       |
| UC-CMN-007 | `GlobalExceptionHandler` - Error responses | [ ]    |       |
| UC-CMN-008 | `ApiResponse` - Response wrapping          | [ ]    |       |
| UC-CMN-009 | `PageResponse` - Pagination handling       | [ ]    |       |
| UC-CMN-010 | `UserContext` - User context parsing       | [ ]    |       |
| UC-CMN-011 | `UserContextFilter` - Header extraction    | [ ]    |       |
| UC-CMN-012 | `FeignHelper` - Feign utilities            | [ ]    |       |
| UC-CMN-013 | `FeignCustomErrorDecoder` - Error decoding | [ ]    |       |

### API Gateway (5 tests)

| ID        | Test Case                                  | Status | Notes |
| --------- | ------------------------------------------ | ------ | ----- |
| UC-GW-001 | `AuthFilter` - JWT validation              | [ ]    |       |
| UC-GW-002 | `AuthFilter` - Header injection            | [ ]    |       |
| UC-GW-003 | `SecurityConfig` - Route security rules    | [ ]    |       |
| UC-GW-004 | `CorsConfig` - CORS configuration          | [ ]    |       |
| UC-GW-005 | `SecurityConstant` - Public endpoints list | [ ]    |       |

---

## 📦 Phase 2: Integration Tests

### Repository Integration (20 tests)

| ID          | Test Case                                       | Status | Notes |
| ----------- | ----------------------------------------------- | ------ | ----- |
| IT-REPO-001 | `AccountRepository` - CRUD with H2              | [ ]    |       |
| IT-REPO-002 | `AccountRepository.findByEmail()`               | [ ]    |       |
| IT-REPO-003 | `PatientRepository` - CRUD with H2              | [ ]    |       |
| IT-REPO-004 | `PatientRepository.findByAccountId()`           | [ ]    |       |
| IT-REPO-005 | `AppointmentRepository` - CRUD with H2          | [ ]    |       |
| IT-REPO-006 | `AppointmentRepository` - Complex queries       | [ ]    |       |
| IT-REPO-007 | `MedicalExamRepository` - CRUD with H2          | [ ]    |       |
| IT-REPO-008 | `MedicalExamRepository.existsByAppointmentId()` | [ ]    |       |
| IT-REPO-009 | `PrescriptionRepository` - CRUD with H2         | [ ]    |       |
| IT-REPO-010 | `LabOrderRepository` - CRUD with H2             | [ ]    |       |
| IT-REPO-011 | `LabTestRepository` - CRUD with H2              | [ ]    |       |
| IT-REPO-012 | `LabTestResultRepository` - CRUD with H2        | [ ]    |       |
| IT-REPO-013 | `MedicineRepository` - CRUD with H2             | [ ]    |       |
| IT-REPO-014 | `CategoryRepository` - CRUD with H2             | [ ]    |       |
| IT-REPO-015 | `EmployeeRepository` - CRUD with H2             | [ ]    |       |
| IT-REPO-016 | `DepartmentRepository` - CRUD with H2           | [ ]    |       |
| IT-REPO-017 | `ScheduleRepository` - CRUD with H2             | [ ]    |       |
| IT-REPO-018 | `InvoiceRepository` - CRUD with H2              | [ ]    |       |
| IT-REPO-019 | `PaymentRepository` - CRUD with H2              | [ ]    |       |
| IT-REPO-020 | `InvoiceItemRepository` - CRUD with H2          | [ ]    |       |

### Service Integration (10 tests)

| ID         | Test Case                                    | Status | Notes |
| ---------- | -------------------------------------------- | ------ | ----- |
| IT-SVC-001 | `AuthService` - Full registration flow       | [ ]    |       |
| IT-SVC-002 | `AuthService` - Full login flow              | [ ]    |       |
| IT-SVC-003 | `AuthService` - Token refresh flow           | [ ]    |       |
| IT-SVC-004 | `PatientController + Repository` - CRUD      | [ ]    |       |
| IT-SVC-005 | `AppointmentService + Repository` - Booking  | [ ]    |       |
| IT-SVC-006 | `MedicalExamService + Hooks` - Exam creation | [ ]    |       |
| IT-SVC-007 | `PrescriptionService + MedicineClient`       | [ ]    |       |
| IT-SVC-008 | `LabOrderService + LabTestService`           | [ ]    |       |
| IT-SVC-009 | `ScheduleService + EmployeeRepository`       | [ ]    |       |
| IT-SVC-010 | `InvoiceService + PaymentService`            | [ ]    |       |

---

## 📦 Phase 3: API/Controller Tests (87 tests)

### Auth Controller (9 tests)

| ID           | Test Case                                  | Status | Notes |
| ------------ | ------------------------------------------ | ------ | ----- |
| API-AUTH-001 | `POST /auth/login` - Valid credentials     | [ ]    |       |
| API-AUTH-002 | `POST /auth/login` - Invalid credentials   | [ ]    |       |
| API-AUTH-003 | `POST /auth/register` - Valid registration | [ ]    |       |
| API-AUTH-004 | `POST /auth/register` - Duplicate email    | [ ]    |       |
| API-AUTH-005 | `POST /auth/register` - Invalid input      | [ ]    |       |
| API-AUTH-006 | `POST /auth/refresh` - Valid token         | [ ]    |       |
| API-AUTH-007 | `POST /auth/refresh` - Expired token       | [ ]    |       |
| API-AUTH-008 | `GET /auth/me` - Current user profile      | [ ]    |       |
| API-AUTH-009 | `POST /auth/logout` - Successful logout    | [ ]    |       |

### Account Controller (7 tests)

| ID          | Test Case                        | Status | Notes |
| ----------- | -------------------------------- | ------ | ----- |
| API-ACC-001 | `GET /accounts` - List all       | [ ]    |       |
| API-ACC-002 | `GET /accounts/{id}` - Get by ID | [ ]    |       |
| API-ACC-003 | `POST /accounts` - Create        | [ ]    |       |
| API-ACC-004 | `PUT /accounts/{id}` - Update    | [ ]    |       |
| API-ACC-005 | `DELETE /accounts/{id}` - Delete | [ ]    |       |
| API-ACC-006 | `GET /accounts` - Pagination     | [ ]    |       |
| API-ACC-007 | `GET /accounts` - Search/filter  | [ ]    |       |

### Patient Controller (9 tests)

| ID          | Test Case                                 | Status | Notes |
| ----------- | ----------------------------------------- | ------ | ----- |
| API-PAT-001 | `GET /patients` - List all                | [ ]    |       |
| API-PAT-002 | `GET /patients/{id}` - Get by ID          | [ ]    |       |
| API-PAT-003 | `POST /patients` - Create                 | [ ]    |       |
| API-PAT-004 | `PUT /patients/{id}` - Update             | [ ]    |       |
| API-PAT-005 | `DELETE /patients/{id}` - Delete          | [ ]    |       |
| API-PAT-006 | `GET /patients/me` - Current profile      | [ ]    |       |
| API-PAT-007 | `PATCH /patients/me` - Update own profile | [ ]    |       |
| API-PAT-008 | `GET /patients` - Search by name          | [ ]    |       |
| API-PAT-009 | `GET /patients` - Pagination              | [ ]    |       |

### Appointment Controller (13 tests)

| ID          | Test Case                            | Status | Notes |
| ----------- | ------------------------------------ | ------ | ----- |
| API-APT-001 | `GET /appointments` - List           | [ ]    |       |
| API-APT-002 | `GET /appointments/{id}` - Get by ID | [ ]    |       |
| API-APT-003 | `POST /appointments` - Book          | [ ]    |       |
| API-APT-004 | `POST /appointments` - Walk-in       | [ ]    |       |
| API-APT-005 | `PUT /appointments/{id}` - Update    | [ ]    |       |
| API-APT-006 | `DELETE /appointments/{id}` - Cancel | [ ]    |       |
| API-APT-007 | `GET /appointments/by-patient/{id}`  | [ ]    |       |
| API-APT-008 | `GET /appointments/by-doctor/{id}`   | [ ]    |       |
| API-APT-009 | `GET /appointments/available-slots`  | [ ]    |       |
| API-APT-010 | `POST /appointments/{id}/check-in`   | [ ]    |       |
| API-APT-011 | `POST /appointments/{id}/check-out`  | [ ]    |       |
| API-APT-012 | `GET /appointments/queue`            | [ ]    |       |
| API-APT-013 | `GET /appointments/stats`            | [ ]    |       |

### Medical Exam Controller (6 tests)

| ID           | Test Case                        | Status | Notes |
| ------------ | -------------------------------- | ------ | ----- |
| API-EXAM-001 | `GET /exams` - List              | [ ]    |       |
| API-EXAM-002 | `GET /exams/{id}` - Get by ID    | [ ]    |       |
| API-EXAM-003 | `POST /exams` - Create           | [ ]    |       |
| API-EXAM-004 | `PUT /exams/{id}` - Update       | [ ]    |       |
| API-EXAM-005 | `GET /exams/by-appointment/{id}` | [ ]    |       |
| API-EXAM-006 | `GET /exams/by-patient/{id}`     | [ ]    |       |

### Prescription Controller (6 tests)

| ID            | Test Case                             | Status | Notes |
| ------------- | ------------------------------------- | ------ | ----- |
| API-PRESC-001 | `GET /prescriptions` - List           | [ ]    |       |
| API-PRESC-002 | `GET /prescriptions/{id}` - Get by ID | [ ]    |       |
| API-PRESC-003 | `POST /prescriptions` - Create        | [ ]    |       |
| API-PRESC-004 | `PUT /prescriptions/{id}` - Update    | [ ]    |       |
| API-PRESC-005 | `DELETE /prescriptions/{id}` - Cancel | [ ]    |       |
| API-PRESC-006 | `GET /prescriptions/by-exam/{id}`     | [ ]    |       |

### Lab Order Controller (6 tests)

| ID          | Test Case                          | Status | Notes |
| ----------- | ---------------------------------- | ------ | ----- |
| API-LAB-001 | `GET /lab-orders` - List           | [ ]    |       |
| API-LAB-002 | `GET /lab-orders/{id}` - Get by ID | [ ]    |       |
| API-LAB-003 | `POST /lab-orders` - Create        | [ ]    |       |
| API-LAB-004 | `PUT /lab-orders/{id}` - Update    | [ ]    |       |
| API-LAB-005 | `DELETE /lab-orders/{id}` - Cancel | [ ]    |       |
| API-LAB-006 | `PATCH /lab-orders/{id}/status`    | [ ]    |       |

### Lab Test Controller (6 tests)

| ID           | Test Case                          | Status | Notes |
| ------------ | ---------------------------------- | ------ | ----- |
| API-TEST-001 | `GET /lab-tests` - List            | [ ]    |       |
| API-TEST-002 | `GET /lab-tests/{id}` - Get by ID  | [ ]    |       |
| API-TEST-003 | `POST /lab-tests` - Create         | [ ]    |       |
| API-TEST-004 | `PUT /lab-tests/{id}` - Update     | [ ]    |       |
| API-TEST-005 | `DELETE /lab-tests/{id}` - Delete  | [ ]    |       |
| API-TEST-006 | `GET /lab-tests/by-category/{cat}` | [ ]    |       |

### Lab Result Controller (5 tests)

| ID             | Test Case                           | Status | Notes |
| -------------- | ----------------------------------- | ------ | ----- |
| API-RESULT-001 | `GET /lab-results` - List           | [ ]    |       |
| API-RESULT-002 | `GET /lab-results/{id}` - Get by ID | [ ]    |       |
| API-RESULT-003 | `POST /lab-results` - Create        | [ ]    |       |
| API-RESULT-004 | `PUT /lab-results/{id}` - Update    | [ ]    |       |
| API-RESULT-005 | `GET /lab-results/by-order/{id}`    | [ ]    |       |

### Medicine Controller (7 tests)

| ID          | Test Case                         | Status | Notes |
| ----------- | --------------------------------- | ------ | ----- |
| API-MED-001 | `GET /medicines` - List           | [ ]    |       |
| API-MED-002 | `GET /medicines/{id}` - Get by ID | [ ]    |       |
| API-MED-003 | `POST /medicines` - Create        | [ ]    |       |
| API-MED-004 | `PUT /medicines/{id}` - Update    | [ ]    |       |
| API-MED-005 | `DELETE /medicines/{id}` - Delete | [ ]    |       |
| API-MED-006 | `PATCH /medicines/{id}/stock`     | [ ]    |       |
| API-MED-007 | `GET /medicines/low-stock`        | [ ]    |       |

### Category Controller (5 tests)

| ID          | Test Case                          | Status | Notes |
| ----------- | ---------------------------------- | ------ | ----- |
| API-CAT-001 | `GET /categories` - List           | [ ]    |       |
| API-CAT-002 | `GET /categories/{id}` - Get by ID | [ ]    |       |
| API-CAT-003 | `POST /categories` - Create        | [ ]    |       |
| API-CAT-004 | `PUT /categories/{id}` - Update    | [ ]    |       |
| API-CAT-005 | `DELETE /categories/{id}` - Delete | [ ]    |       |

### Employee Controller (7 tests)

| ID          | Test Case                               | Status | Notes |
| ----------- | --------------------------------------- | ------ | ----- |
| API-EMP-001 | `GET /employees` - List                 | [ ]    |       |
| API-EMP-002 | `GET /employees/{id}` - Get by ID       | [ ]    |       |
| API-EMP-003 | `POST /employees` - Create              | [ ]    |       |
| API-EMP-004 | `PUT /employees/{id}` - Update          | [ ]    |       |
| API-EMP-005 | `DELETE /employees/{id}` - Delete       | [ ]    |       |
| API-EMP-006 | `GET /employees/doctors` - List doctors | [ ]    |       |
| API-EMP-007 | `GET /employees/by-department/{id}`     | [ ]    |       |

### Department Controller (5 tests)

| ID           | Test Case                           | Status | Notes |
| ------------ | ----------------------------------- | ------ | ----- |
| API-DEPT-001 | `GET /departments` - List           | [ ]    |       |
| API-DEPT-002 | `GET /departments/{id}` - Get by ID | [ ]    |       |
| API-DEPT-003 | `POST /departments` - Create        | [ ]    |       |
| API-DEPT-004 | `PUT /departments/{id}` - Update    | [ ]    |       |
| API-DEPT-005 | `DELETE /departments/{id}` - Delete | [ ]    |       |

### Schedule Controller (7 tests)

| ID            | Test Case                         | Status | Notes |
| ------------- | --------------------------------- | ------ | ----- |
| API-SCHED-001 | `GET /schedules` - List           | [ ]    |       |
| API-SCHED-002 | `GET /schedules/{id}` - Get by ID | [ ]    |       |
| API-SCHED-003 | `POST /schedules` - Create        | [ ]    |       |
| API-SCHED-004 | `PUT /schedules/{id}` - Update    | [ ]    |       |
| API-SCHED-005 | `DELETE /schedules/{id}` - Cancel | [ ]    |       |
| API-SCHED-006 | `GET /schedules/by-employee/{id}` | [ ]    |       |
| API-SCHED-007 | `GET /schedules/doctors`          | [ ]    |       |

### Invoice Controller (7 tests)

| ID          | Test Case                        | Status | Notes |
| ----------- | -------------------------------- | ------ | ----- |
| API-INV-001 | `GET /invoices` - List           | [ ]    |       |
| API-INV-002 | `GET /invoices/{id}` - Get by ID | [ ]    |       |
| API-INV-003 | `POST /invoices` - Create        | [ ]    |       |
| API-INV-004 | `PUT /invoices/{id}` - Update    | [ ]    |       |
| API-INV-005 | `DELETE /invoices/{id}` - Cancel | [ ]    |       |
| API-INV-006 | `GET /invoices/by-patient/{id}`  | [ ]    |       |
| API-INV-007 | `GET /invoices/stats`            | [ ]    |       |

### Payment Controller (4 tests)

| ID          | Test Case                          | Status | Notes |
| ----------- | ---------------------------------- | ------ | ----- |
| API-PAY-001 | `POST /payments/init` - Initialize | [ ]    |       |
| API-PAY-002 | `GET /payments/callback` - VNPay   | [ ]    |       |
| API-PAY-003 | `GET /payments/{id}` - Get by ID   | [ ]    |       |
| API-PAY-004 | `GET /payments/by-invoice/{id}`    | [ ]    |       |

---

## 📦 Phase 4: Security Tests (22 tests)

### Authentication (5 tests)

| ID           | Test Case                 | Status | Notes |
| ------------ | ------------------------- | ------ | ----- |
| SEC-AUTH-001 | Valid JWT grants access   | [ ]    |       |
| SEC-AUTH-002 | Missing JWT returns 401   | [ ]    |       |
| SEC-AUTH-003 | Invalid JWT returns 401   | [ ]    |       |
| SEC-AUTH-004 | Expired JWT returns 401   | [ ]    |       |
| SEC-AUTH-005 | Malformed JWT returns 401 | [ ]    |       |

### Authorization (8 tests)

| ID            | Test Case                         | Status | Notes |
| ------------- | --------------------------------- | ------ | ----- |
| SEC-AUTHZ-001 | Admin access admin endpoints      | [ ]    |       |
| SEC-AUTHZ-002 | Doctor access doctor endpoints    | [ ]    |       |
| SEC-AUTHZ-003 | Nurse access nurse endpoints      | [ ]    |       |
| SEC-AUTHZ-004 | Receptionist access own endpoints | [ ]    |       |
| SEC-AUTHZ-005 | Patient access patient endpoints  | [ ]    |       |
| SEC-AUTHZ-006 | Patient cannot access admin       | [ ]    |       |
| SEC-AUTHZ-007 | Unauthorized role returns 403     | [ ]    |       |
| SEC-AUTHZ-008 | Cross-patient data prevented      | [ ]    |       |

### Gateway Security (4 tests)

| ID         | Test Case                     | Status | Notes |
| ---------- | ----------------------------- | ------ | ----- |
| SEC-GW-001 | Public endpoints no auth      | [ ]    |       |
| SEC-GW-002 | Protected require auth        | [ ]    |       |
| SEC-GW-003 | X-User-\* headers injected    | [ ]    |       |
| SEC-GW-004 | Direct service access blocked | [ ]    |       |

### Input Validation (5 tests)

| ID          | Test Case                 | Status | Notes |
| ----------- | ------------------------- | ------ | ----- |
| SEC-VAL-001 | SQL injection prevention  | [ ]    |       |
| SEC-VAL-002 | XSS prevention            | [ ]    |       |
| SEC-VAL-003 | Request body size limits  | [ ]    |       |
| SEC-VAL-004 | Path traversal prevention | [ ]    |       |
| SEC-VAL-005 | CORS configuration valid  | [ ]    |       |

---

## 📦 Phase 5: E2E Tests (21 tests)

### Patient Registration Flow (3 tests)

| ID          | Test Case                | Status | Notes |
| ----------- | ------------------------ | ------ | ----- |
| E2E-REG-001 | Complete registration    | [ ]    |       |
| E2E-REG-002 | Login after registration | [ ]    |       |
| E2E-REG-003 | View own profile         | [ ]    |       |

### Appointment Flow (5 tests)

| ID          | Test Case                   | Status | Notes |
| ----------- | --------------------------- | ------ | ----- |
| E2E-APT-001 | Patient books appointment   | [ ]    |       |
| E2E-APT-002 | Patient views appointments  | [ ]    |       |
| E2E-APT-003 | Patient cancels appointment | [ ]    |       |
| E2E-APT-004 | Receptionist walk-in        | [ ]    |       |
| E2E-APT-005 | Patient check-in flow       | [ ]    |       |

### Medical Exam Flow (6 tests)

| ID           | Test Case                   | Status | Notes |
| ------------ | --------------------------- | ------ | ----- |
| E2E-EXAM-001 | Doctor creates exam         | [ ]    |       |
| E2E-EXAM-002 | Doctor adds diagnosis       | [ ]    |       |
| E2E-EXAM-003 | Doctor creates prescription | [ ]    |       |
| E2E-EXAM-004 | Doctor orders lab tests     | [ ]    |       |
| E2E-EXAM-005 | Lab tech enters results     | [ ]    |       |
| E2E-EXAM-006 | Complete exam with billing  | [ ]    |       |

### Billing Flow (4 tests)

| ID           | Test Case                  | Status | Notes |
| ------------ | -------------------------- | ------ | ----- |
| E2E-BILL-001 | Invoice created after exam | [ ]    |       |
| E2E-BILL-002 | Patient views invoice      | [ ]    |       |
| E2E-BILL-003 | VNPay payment flow         | [ ]    |       |
| E2E-BILL-004 | Payment confirmation       | [ ]    |       |

### HR Management Flow (4 tests)

| ID         | Test Case                | Status | Notes |
| ---------- | ------------------------ | ------ | ----- |
| E2E-HR-001 | Admin creates department | [ ]    |       |
| E2E-HR-002 | Admin creates employee   | [ ]    |       |
| E2E-HR-003 | Admin creates schedule   | [ ]    |       |
| E2E-HR-004 | View doctor availability | [ ]    |       |

---

## 📦 Phase 6: Performance Tests (10 tests)

### Load Tests (4 tests)

| ID            | Test Case                  | Status | Notes |
| ------------- | -------------------------- | ------ | ----- |
| PERF-LOAD-001 | 100 concurrent logins      | [ ]    |       |
| PERF-LOAD-002 | 50 concurrent bookings     | [ ]    |       |
| PERF-LOAD-003 | 200 concurrent queries     | [ ]    |       |
| PERF-LOAD-004 | Gateway routing under load | [ ]    |       |

### Stress Tests (3 tests)

| ID              | Test Case                 | Status | Notes |
| --------------- | ------------------------- | ------ | ----- |
| PERF-STRESS-001 | DB connection pool limits | [ ]    |       |
| PERF-STRESS-002 | Memory usage under load   | [ ]    |       |
| PERF-STRESS-003 | Service recovery          | [ ]    |       |

### Endurance Tests (3 tests)

| ID           | Test Case                 | Status | Notes |
| ------------ | ------------------------- | ------ | ----- |
| PERF-END-001 | 24-hour operation         | [ ]    |       |
| PERF-END-002 | Memory leak detection     | [ ]    |       |
| PERF-END-003 | Connection leak detection | [ ]    |       |

---

## 📦 Phase 7: Contract Tests (12 tests)

### API Contracts (7 tests)

| ID           | Test Case                     | Status | Notes |
| ------------ | ----------------------------- | ------ | ----- |
| CONTRACT-001 | Auth service contract         | [ ]    |       |
| CONTRACT-002 | Patient service contract      | [ ]    |       |
| CONTRACT-003 | Appointment service contract  | [ ]    |       |
| CONTRACT-004 | Medical exam service contract | [ ]    |       |
| CONTRACT-005 | Medicine service contract     | [ ]    |       |
| CONTRACT-006 | HR service contract           | [ ]    |       |
| CONTRACT-007 | Billing service contract      | [ ]    |       |

### Feign Client Contracts (5 tests)

| ID                 | Test Case                  | Status | Notes |
| ------------------ | -------------------------- | ------ | ----- |
| CONTRACT-FEIGN-001 | PatientClient contract     | [ ]    |       |
| CONTRACT-FEIGN-002 | HrClient contract          | [ ]    |       |
| CONTRACT-FEIGN-003 | AppointmentClient contract | [ ]    |       |
| CONTRACT-FEIGN-004 | BillingClient contract     | [ ]    |       |
| CONTRACT-FEIGN-005 | MedicalExamClient contract | [ ]    |       |

---

## 📝 Test Session Log

Use this section to track your testing sessions:

| Date       | Session   | Tests Completed | Notes       |
| ---------- | --------- | --------------- | ----------- |
| YYYY-MM-DD | Session 1 | 0               | Starting... |

---

_Last Updated: 2026-01-07_
