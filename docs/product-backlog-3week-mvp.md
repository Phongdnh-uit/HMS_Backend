# Product Backlog - 3-Week MVP (Brownfield)

**Project:** Hospital Management System - Sprint to MVP  
**Timeline:** 3 weeks (15 working days)  
**Current Date:** December 2, 2025  
**Target Completion:** December 20, 2025  
**Team:** 8 developers (3 Backend + 5 Frontend)  
**Target Scope:** 125 story points (Backend) + FE work in parallel  
**Approach:** Parallel development - BE builds services, FE builds UI with mocks/integration  
**Status:** 📋 Ready for Implementation

---

## 👥 Team Structure

**Backend Team (3 developers):**
- Build microservices following existing patterns
- Focus: REST APIs, business logic, database, integration
- Work mode: Sequential dependencies (services depend on each other)

**Frontend Team (5 developers):**
- Build Next.js application with shadcn/ui components
- Focus: Pages, forms, API integration, state management
- Work mode: Parallel development (can work with mocked APIs initially)
- **Note:** Detailed FE backlog and specifications will be created separately

---

## 🎯 Strategic Goals

> **Note:** This backlog focuses on **Backend development**. Frontend backlog with detailed screen designs, user flows, and component specifications will be created as a separate document.

### Primary Objective
Build a **minimum viable hospital management system** that supports:
1. ✅ User authentication with role-based access (already working)
2. ✅ Medicine catalog management (already working)
3. ✅ Basic patient records (already working)
4. 🆕 **Employee/doctor management** (NEW - HR Service)
5. 🆕 **Appointment booking** (NEW - Appointment Service)
6. 🆕 **Basic medical exam records** (NEW - Medical Exam Service)
7. 🆕 **Billing & invoicing** (NEW - Billing Service)
8. 🆕 **Reports & statistics** (NEW - Reports Service)

### Extensibility Principles
🔐 **Future-Proof Design:**
- All data models designed with FULL schema (implement fields progressively)
- Core workflows complete (add features without changing flows)
- Services remain independent (add Billing/Reports later without refactoring)
- RBAC with all 4 roles (ADMIN, PATIENT, DOCTOR, NURSE) - enforcement added progressively

### What's OUT of Scope (Post-MVP)
- ❌ Payment gateway integration (Stripe, PayPal)
- ❌ Advanced billing: insurance claims, payment plans, discounts
- ❌ Custom report builder with dynamic queries
- ❌ Real-time dashboards with charts
- ❌ Advanced features: vital signs, lab tests, imaging
- ❌ Document uploads
- ❌ Email/SMS notifications
- ❌ Advanced RBAC (permission matrix - use simple role checks for MVP)

---

## ✅ Lecture Requirements Coverage

This backlog ensures **100% coverage** of all mandatory lecture requirements:

| # | Requirement Category | MVP Coverage | Epic(s) | Stories |
|---|---------------------|--------------|---------|---------|
| **1** | **Patient Management** | | | |
| 1a | Patient registration & profile | ✅ Complete | Epic 0 (FE-02) | Patient entity with full fields |
| 1b | Patient search & filtering | ✅ Complete | Existing | Patient service RSQL |
| 1c | Electronic medical records | ✅ Complete | Epic 3 | Medical Exam + Prescription management |
| **2** | **Appointment Management** | | | |
| 2a | Appointment booking | ✅ Complete | Epic 2 | AP-03 with full validation |
| 2b | Doctor schedule management | ✅ Complete | Epic 1, 2 | HR-03 availability + AP-08 schedule view |
| 2c | Appointment cancellation | ✅ Complete | Epic 2 | AP-05 cancellation workflow |
| 2d | Conflict prevention | ✅ Complete | Epic 2 | AP-06 advanced validation |
| **3** | **Medical Examination** | | | |
| 3a | Exam record creation | ✅ Complete | Epic 3 | ME-02, ME-03 |
| 3b | Diagnosis & treatment plans | ✅ Complete | Epic 3 | MedicalExam entity fields |
| 3c | Prescription management | ✅ Complete | Epic 3 | ME-04 full CRUD |
| 3d | Medical history tracking | ✅ Complete | Epic 3 | ME-05 exam listing by patient |
| **4** | **Billing Management** | | | |
| 4a | Invoice generation | ✅ Complete | Epic 5 | BL-03 auto-generate from exam |
| 4b | Hospital fee tracking | ✅ Complete | Epic 5 | BL-02 invoice items (consultation + medicines) |
| 4c | Payment recording | ✅ Complete | Epic 5 | BL-04 payment entity & workflow |
| 4d | Payment status tracking | ✅ Complete | Epic 5 | BL-05 invoice listing with payment summary |
| **5** | **HR Management** | | | |
| 5a | Employee records | ✅ Complete | Epic 1 | HR-02 full employee CRUD |
| 5b | Doctor profiles | ✅ Complete | Epic 1 | HR-02 with specialization, license |
| 5c | Doctor availability | ✅ Complete | Epic 1 | HR-03 weekly schedule + overrides |
| 5d | Department management | ✅ Complete | Epic 1 | Department enum in Employee |
| **6** | **Reporting & Statistics** | | | |
| 6a | Financial reports | ✅ Complete | Epic 6 | RP-02 revenue report |
| 6b | Appointment statistics | ✅ Complete | Epic 6 | RP-03 appointment analytics |
| 6c | Doctor performance | ✅ Complete | Epic 6 | RP-04 workload & revenue metrics |
| 6d | Patient activity | ✅ Complete | Epic 6 | RP-05 visit frequency |

**Coverage Summary:**
- ✅ **24/24 requirements covered (100%)**
- 6 major categories fully implemented
- All core workflows complete
- Ready for academic evaluation

---

## ⚠️ Risk Assessment & Mitigation

| Risk | Impact | Probability | Mitigation Strategy |
|------|--------|-------------|---------------------|
| **123 BE points in 3 weeks** | 🟡 Medium | 🟢 Low | 3 BE developers (41 pts each), proven patterns, 80% code reuse from GenericController |
| **Billing integration complexity** | 🟡 Medium | 🟡 Medium | Auto-generate only (no payment gateway), simple manual payment recording |
| **Reports service queries slow** | 🟡 Medium | 🟢 Low | Query existing services (no complex joins), add caching if needed |
| **Service integration delays** | 🟡 Medium | 🟡 Medium | RestTemplate pattern proven in existing code, mock services for testing |
| **BE-FE coordination** | 🟡 Medium | 🟡 Medium | API contracts published early, FE uses mocks initially, integration in Week 3 |
| **Team coordination** | 🟡 Medium | 🟢 Low | Clear epic ownership (BE team), daily standups, Postman collection ready |
| **Scope creep** | 🔴 High | 🟢 Low | Strict adherence to backlog, Phase 2 features clearly marked as OUT |

**Contingency Plans:**
1. **If behind schedule by Week 2:**
   - Defer Epic 6 (Reports) to Phase 2 - still meets lecture requirements with manual SQL queries
   - Simplify BL-03 (skip auto-generate, create invoices manually)
   
2. **If 1 developer unavailable:**
   - Reduce Reports to 2 stories (RP-02, RP-03 only - covers financial + appointments)
   - Extend BL-04 deadline (payments can be added post-demo)

3. **If integration issues arise:**
   - Use service mocks with hardcoded responses
   - Build services in isolation, integrate in final 2 days

**Success Factors:**
- ✅ Existing foundation (3 services working)
- ✅ Proven architecture patterns
- ✅ Clear acceptance criteria per story
- ✅ Extensible data models (no refactoring needed later)

---

## 📊 Epic Overview

| Epic | Service | Priority | Week | Points | Status |
|------|---------|----------|------|--------|--------|
| Epic 0 | Foundation Enhancement | P0 | Week 1 | 13 | 🔧 Enhance existing |
| Epic 1 | HR Service | P0 | Week 1 | 14 | 🆕 New service |
| Epic 2 | Appointment Service | P1 | Week 2 | 30 | 🆕 New service |
| Epic 3 | Medical Exam Service | P1 | Week 3 | 18 | 🆕 New service |
| Epic 5 | Billing Service | P1 | Week 2-3 | 23 | 🆕 New service |
| Epic 6 | Reports Service | P2 | Week 3 | 18 | 🆕 New service |
| Epic 4 | Integration & Testing | P0 | Week 3 | 9 | 🔗 Cross-cutting |
| **TOTAL** | **6 services + foundation** | | **3 weeks** | **125** | |

---

## 📅 Weekly Breakdown (Backend + Frontend Parallel Tracks)

### Backend Stream (Sequential - 3 Developers)

| Week | Epic Focus | BE Points | BE Deliverables |
|------|------------|-----------|-----------------|
| **Week 1** | Foundation + HR Service | 27 | RBAC complete, Patient/Medicine enriched, Employee CRUD, Doctor availability, Postman v1.0 |
| **Week 2** | Appointment + Billing/Exam Setup | 38 | Appointment booking + scheduling, Invoice/Payment entities, Medical Exam entities, Postman v2.0 |
| **Week 3** | Medical Exam + Billing + Reports + Integration | 58 | Exam records + prescriptions, Invoice auto-generation, Payment tracking, 4 Reports, E2E tests, Docker, Postman v3.0 |

### Frontend Stream (Parallel - 5 Developers)

| Week | FE Focus | FE Integration Point | FE Deliverables |
|------|----------|----------------------|-----------------|
| **Week 1** | Setup + Core Screens (Mocked) | Day 5: Auth + Patient + Medicine APIs | Login/Register, Patient UI, Medicine UI, HR calendar mockups |
| **Week 2** | Feature Screens (Mocked) | Day 10: HR + Appointment APIs | Appointment booking flow, Medical Exam form, Billing screens, Reports mockups |
| **Week 3** | Integration + Testing | Day 15-21: All APIs live | Replace mocks, Full integration, E2E testing, Polish |

**Combined Progress Milestones:**
- **Day 5:** BE delivers Auth/Patient/Medicine APIs → FE integrates core CRUD
- **Day 10:** BE delivers HR/Appointment APIs → FE integrates booking flow
- **Day 15:** BE delivers Billing/Reports APIs → FE begins full integration
- **Day 21:** Both teams deliver production-ready system

---

## 🏗️ Epic 0: Foundation Enhancement (Week 1)

**Purpose:** Enhance existing services to support new features without breaking current functionality.

**Business Value:** Provides authentication, authorization, and patient data foundation for all new services.

### Stories

#### **FE-01: RBAC Enhancement** 
- **Points:** 5  
- **Priority:** P0 - Critical  
- **Description:** Add missing roles and update authorization guards

**Current State:**
- ✅ JWT authentication working
- ✅ RoleEnum has: ADMIN, PATIENT, EMPLOYEE
- ❌ Missing: DOCTOR, NURSE roles
- ❌ No role-based endpoint protection (beyond basic auth)

**Acceptance Criteria:**
- Update `RoleEnum` to: ADMIN, PATIENT, DOCTOR, NURSE
- Create `@Roles()` annotation for Spring Security
- Create `RolesGuard` to check user roles
- Update Account entity registration to support all roles
- Document role permission matrix (simple version for MVP)
- Apply role guards to existing endpoints:
  - `/accounts/*` → ADMIN only
  - `/medicines/*`, `/categories/*` → ADMIN, DOCTOR (read for NURSE)
  - `/patients/*` → ADMIN, DOCTOR, NURSE (create/update), PATIENT (read own only)

**Database Changes:**
- Modify `RoleEnum` values only (no schema change - enum stored as string)

**Dependencies:** None  
**Integration Points:** All services will use this for authorization

---

#### **FE-02: Patient Service Completion**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** Add missing patient fields needed for medical workflows

**Current State:**
- ✅ Basic patient CRUD working
- ✅ Fields: fullName, email, dateOfBirth, gender, phone, address, etc.
- ✅ **Emergency contact already implemented:** relativeFullName, relativePhoneNumber
- ❌ Missing: Blood type, allergies (critical for medical safety)
- ❌ Missing: Emergency contact relationship field

**Acceptance Criteria:**
- Add fields to Patient entity:
  - `bloodType` (enum: A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, AB_POSITIVE, AB_NEGATIVE, O_POSITIVE, O_NEGATIVE)
  - `allergies` (String, text field - simple comma-separated for MVP)
  - `relativeRelationship` (String) - relationship to patient (e.g., "Spouse", "Parent", "Sibling")
- Update PatientRequest/Response DTOs
- Migration script to add columns (nullable, for backward compatibility)
- Update existing patient records (can be null for MVP)
- Add validation: phone format, blood type enum

**Database Changes:**
```sql
ALTER TABLE patient ADD COLUMN blood_type VARCHAR(20);
ALTER TABLE patient ADD COLUMN allergies TEXT;
ALTER TABLE patient ADD COLUMN relative_relationship VARCHAR(50);
```

**Note:** Emergency contact fields (`relativeFullName`, `relativePhoneNumber`) are already implemented in the existing Patient entity. Only need to add relationship field for completeness.

**Dependencies:** None  
**Integration Points:** Medical Exam Service will read these fields

---

#### **FE-03: API Gateway Route Updates**
- **Points:** 2  
- **Priority:** P0 - Critical  
- **Description:** Add routing for new HR, Appointment, Medical Exam, Billing, Reports services

**Acceptance Criteria:**
- Add routes to `api-gateway.yml`:
  ```yaml
  - id: hr-service
    uri: lb://hr-service
    predicates:
      - Path=/hr-service/**
    filters:
      - StripPrefix=1
  - id: appointment-service
    uri: lb://appointment-service
    predicates:
      - Path=/appointment-service/**
    filters:
      - StripPrefix=1
  - id: medical-exam-service
    uri: lb://medical-exam-service
    predicates:
      - Path=/medical-exam-service/**
    filters:
      - StripPrefix=1
  - id: billing-service
    uri: lb://billing-service
    predicates:
      - Path=/billing-service/**
    filters:
      - StripPrefix=1
  - id: reports-service
    uri: lb://reports-service
    predicates:
      - Path=/reports-service/**
    filters:
      - StripPrefix=1
  ```
- Update AuthFilter to pass user context headers
- Test routing with mock services (health checks)

**Dependencies:** None  
**Integration Points:** All new services route through gateway

---

#### **FE-04: Common Library Enhancements**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** Add shared enums and utilities for new services

**Acceptance Criteria:**
- Add to `common` module:
  - `AppointmentStatus` enum (SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)
  - `ExamStatus` enum (IN_PROGRESS, COMPLETED, CANCELLED)
  - `Department` enum (CARDIOLOGY, PEDIATRICS, EMERGENCY, RADIOLOGY, SURGERY, GENERAL_PRACTICE, ORTHOPEDICS, NEUROLOGY)
  - `DayOfWeek` enum (MONDAY to SUNDAY - if not using Java's built-in)
  - Date/time utility classes (for appointment slot validation)
- Update all services to use latest common module

**Dependencies:** None  
**Integration Points:** HR, Appointment, Medical Exam services

---

**Epic 0 Total: 13 points**

---

## 🏥 Epic 1: HR Service - Employee & Doctor Management (Week 1)

**Purpose:** Manage hospital staff (doctors, nurses, admin) and doctor availability schedules.

**Business Value:** Essential for appointment booking. Links employees to user accounts and provides doctor availability data.

**Data Model Design (Future-Proof):**
```
Employee Entity (implement NOW):
- id: String (UUID)
- accountId: String (FK to Account - nullable for non-system staff)
- employeeCode: String (unique, e.g., "DOC001")
- fullName: String (required)
- email: String
- phoneNumber: String
- department: Department enum (required)
- position: String (e.g., "Senior Cardiologist")
- specialization: String (e.g., "Interventional Cardiology")
- licenseNumber: String (for doctors)
- dateOfBirth: Instant
- hireDate: Instant
- status: EmployeeStatus enum (ACTIVE, ON_LEAVE, TERMINATED)
- availability: JSONB (weekly schedule + overrides)
- createdAt/updatedAt/createdBy/updatedBy (audit fields)

Future fields (design now, add later):
- salary: BigDecimal (add when Payroll module built)
- performanceRating: Integer (add when Review module built)
- certificates: JSONB (add when Certification tracking needed)
```

### Stories

#### **HR-01: HR Service Infrastructure**
- **Points:** 2  
- **Priority:** P0 - Critical  
- **Description:** Scaffold HR service following existing patterns

**Acceptance Criteria:**
- Create `hr-service` module in gradle
- Copy structure from `patient-service` (proven pattern)
- Setup dependencies: Spring Data JPA, MySQL, Eureka Client, Config Client
- Create application config in config-server: `hr-service.yml`
- Database: `hr_db` (separate MySQL instance or schema)
- Docker Compose config: `infrastructure/dev/hr-service/compose.yaml`
- Health check endpoint working
- Register with Eureka
- Basic test: GET /actuator/health returns UP

**Database Setup:**
```yaml
# config-server/configuration/hr-service.yml
spring:
  datasource:
    url: jdbc:mysql://${HR_DB_HOST}:${HR_DB_PORT}/${HR_DB_NAME}
    username: ${HR_DB_USERNAME}
    password: ${HR_DB_PASSWORD}
  jpa:
    hibernate.ddl-auto: update
```

**Dependencies:** FE-03 (API Gateway routes)  
**Integration Points:** Config Server, Discovery Service, API Gateway

---

#### **HR-02: Employee Entity & CRUD**
- **Points:** 5  
- **Priority:** P0 - Critical  
- **Description:** Complete employee management with all fields

**Acceptance Criteria:**
- Create `Employee` entity with ALL fields (see data model above)
- Create `EmployeeRequest`/`EmployeeResponse` DTOs
- Create `EmployeeMapper` (MapStruct)
- Create `EmployeeRepository` extends `SimpleRepository`
- Create `EmployeeService` extends `GenericService`
- Create `EmployeeController` extends `GenericController`
- Add soft delete tracking field to Employee entity:
  ```java
  Employee {
    ...
    status: EmployeeStatus enum (ACTIVE, ON_LEAVE, RESIGNED)
    deletedAt: Instant (nullable - tracks soft delete timestamp)
    deletedBy: String (user who performed deletion)
  }
  ```
- Implement endpoints:
  - `GET /employees/all` - Paginated list with RSQL filter (auto-filter deletedAt IS NULL)
  - `GET /employees/{id}` - Get by ID (only if deletedAt IS NULL)
  - `POST /employees` - Create (ADMIN only)
  - `PUT /employees/{id}` - Update (ADMIN only)
  - `DELETE /employees/{id}` - Soft delete:
    ```java
    // Before soft delete, check cross-service dependencies
    int futureAppointments = appointmentService.countFutureByDoctor(employeeId);
    if (futureAppointments > 0) {
      throw new BusinessException("Cannot delete doctor with scheduled appointments");
    }
    employee.setDeletedAt(Instant.now());
    employee.setDeletedBy(currentUserId);
    employee.setStatus(RESIGNED);
    ```
- Filter support: `?filter=department==CARDIOLOGY;status==ACTIVE`
- Validation:
  - employeeCode unique
  - email format
  - phoneNumber format
  - department must be valid enum
  - hireDate cannot be future

**Sample Data (seed for testing):**
```java
// Create 5 doctors across different departments
Doctor 1: Dr. Nguyen Van A, Cardiology, DOC001
Doctor 2: Dr. Tran Thi B, Pediatrics, DOC002
Doctor 3: Dr. Le Van C, Emergency, DOC003
Doctor 4: Dr. Pham Thi D, Radiology, DOC004
Doctor 5: Dr. Hoang Van E, Surgery, DOC005
```

**Dependencies:** HR-01, FE-01 (RBAC)  
**Integration Points:** Auth Service (accountId FK)

---

#### **HR-03: Doctor Availability Management**
- **Points:** 5  
- **Priority:** P0 - Critical  
- **Description:** Weekly schedule + date-specific overrides for doctors

**Acceptance Criteria:**
- Create separate `DoctorSchedule` entity (NOT JSONB - proper table design):
  ```java
  DoctorSchedule {
    id: String (UUID)
    doctorId: String (FK to Employee)
    workDate: LocalDate (e.g., 2025-12-05)
    startTime: LocalTime (e.g., 08:00)
    endTime: LocalTime (e.g., 17:00)
    status: ScheduleStatus enum (AVAILABLE, BOOKED, CANCELLED)
    notes: String (e.g., "Conference 2PM-4PM")
  }
  ```
- Database: Unique index on (doctorId, workDate) to prevent duplicate schedules per day
- Endpoints:
  - `POST /hr/schedules` - Create schedule for specific date (ADMIN, DOCTOR self)
  - `GET /hr/schedules?doctorId={id}&startDate={date}&endDate={date}` - Get schedules in range
  - `PUT /hr/schedules/{id}` - Update schedule status
  - `GET /hr/schedules/{doctorId}/available-slots?date=2025-12-15` - Get available 30-min slots for specific date
- Business rules:
  - Only employees with department in MEDICAL departments can have availability
  - Slots must be 30-min intervals (09:00, 09:30, 10:00...)
  - Weekly schedule: 08:00-18:00 range only
  - Overrides take precedence over weekly schedule
- Response format for available-slots:
  ```json
  ["09:00", "09:30", "10:00", "10:30", "11:00", "14:00", "14:30"...]
  ```

**Dependencies:** HR-02  
**Integration Points:** Appointment Service will call available-slots endpoint

---

#### **HR-04: Employee Search**
- **Points:** 2  
- **Priority:** P1 - High  
- **Description:** Search employees by multiple criteria

**Acceptance Criteria:**
- Endpoint: `GET /employees/search?q={query}`
- Search fields: name, email, employeeCode, department
- Support filters: `&department=CARDIOLOGY&status=ACTIVE`
- Return paginated results
- Highlight matching fields in response (optional for MVP)
- Use RSQL for filtering (leverage existing library)

**Dependencies:** HR-02  
**Integration Points:** Frontend will use for doctor selection

---

**Epic 1 Total: 14 points**

---

## 📅 Epic 2: Appointment Service - Booking & Management (Week 2)

**Purpose:** Enable patients to book appointments with doctors, manage schedules, prevent conflicts.

**Business Value:** Core patient-facing feature. Primary workflow for outpatient services.

**Data Model Design (Future-Proof):**
```
Appointment Entity (implement NOW):
- id: String (UUID)
- patientId: String (FK to Patient)
- doctorId: String (FK to Employee)
- appointmentDate: LocalDate (required)
- timeSlot: String (HH:MM format, e.g., "09:30")
- reason: String (patient's reason for visit)
- status: AppointmentStatus enum (SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)
- cancellationReason: String (if status=CANCELLED)
- cancelledAt: Instant
- cancelledBy: String (user ID)
- createdAt/updatedAt/createdBy/updatedBy (audit)

Future fields (design now, add later):
- notes: String (doctor's notes - add when needed)
- examId: String (FK to MedicalExam - nullable, link after exam created)
- followUpDate: LocalDate (add when follow-up workflow built)
- reminderSent: Boolean (add when notification system built)
- checkInTime: Instant (add when check-in kiosk built)
```

### Stories

#### **AP-01: Appointment Service Infrastructure**
- **Points:** 3  
- **Priority:** P0 - Critical  
- **Description:** Scaffold Appointment service

**Acceptance Criteria:**
- Create `appointment-service` module
- Setup: JPA, MySQL, Eureka, Config, RestTemplate/WebClient for service calls
- Database: `appointment_db`
- Docker Compose config
- Config file: `appointment-service.yml`
- Health check working
- Register with Eureka

**Dependencies:** FE-03  
**Integration Points:** Config, Discovery, Gateway

---

#### **AP-02: Appointment Entity & Basic CRUD**
- **Points:** 5  
- **Priority:** P0 - Critical  
- **Description:** Create appointment entity and basic operations

**Acceptance Criteria:**
- Create `Appointment` entity with ALL fields
- Create DTOs: `AppointmentRequest`, `AppointmentResponse`
- Create Mapper, Repository, Service, Controller (follow pattern)
- Endpoints (no validation yet, just CRUD):
  - `POST /appointments` - Create
  - `GET /appointments/all` - List (paginated, RSQL filter)
  - `GET /appointments/{id}` - Get by ID
  - `PUT /appointments/{id}` - Update
  - `DELETE /appointments/{id}` - Delete (soft delete via status)
- Basic validation:
  - Required fields: patientId, doctorId, appointmentDate, timeSlot
  - Date format validation
  - Time format validation (HH:MM)

**Dependencies:** AP-01  
**Integration Points:** None yet (stubs for services)

---

#### **AP-03: Appointment Booking with Validation**
- **Points:** 8  
- **Priority:** P0 - Critical  
- **Description:** Full booking workflow with business rules

**Acceptance Criteria:**
- Enhance `POST /appointments` with full validation:
  
**Validation Flow (with Circuit Breaker & Parallel Async Calls):**
  1. DTO validation (required fields, formats)
  2. **Parallel Async Validation (reduce 250ms → 50ms):**
     ```java
     CompletableFuture<Patient> patientFuture = patientServiceClient.getPatient(patientId); // Circuit breaker
     CompletableFuture<Employee> doctorFuture = hrServiceClient.getEmployee(doctorId); // Circuit breaker
     CompletableFuture<List<String>> slotsFuture = hrServiceClient.getAvailableSlots(doctorId, date); // Circuit breaker
     
     CompletableFuture.allOf(patientFuture, doctorFuture, slotsFuture).join();
     ```
  3. Verify doctor role (must have position containing "Doctor" or department medical)
  4. Verify timeSlot in available slots
  5. Check for conflicts: No existing appointment with same doctor + date + timeSlot with status != CANCELLED
  6. Verify appointment date not in past
  7. Verify timeSlot format (must be 30-min interval: 09:00, 09:30...)
  8. Create appointment with status=SCHEDULED
  9. Set createdBy from JWT context (X-User-ID header)

**Circuit Breaker Configuration:**
  - Use Resilience4j with fallback to cached data
  - Timeout: 3 seconds per service call
  - Failure threshold: 50% in 10 requests
  - Fallback: Return last known patient/doctor data from local cache

**Business Rules:**
  - Cannot book past appointments (date >= today)
  - Time slots: 30-minute intervals only (09:00, 09:30, 10:00...)
  - Business hours: 08:00-18:00 (last slot 17:30)
  - No double-booking (same doctor + date + time)
  - Patient can book max 5 future appointments (prevent spam)

**Error Responses:**
  - 400: Invalid input (DTO validation)
  - 404: Patient or Doctor not found
  - 409: Time slot conflict (already booked)
  - 422: Doctor not available at requested time
  - 422: Cannot book past appointments

**Service Integration:**
  - Use `RestTemplate` or `WebClient` for service calls
  - Handle service timeout (circuit breaker optional, fallback for MVP)
  - Cache doctor availability (5 min TTL) to reduce HR service load

**Dependencies:** AP-02, HR-03 (doctor availability), FE-02 (patient service)  
**Integration Points:** Patient Service, HR Service

---

#### **AP-04: Appointment Listing & Filtering**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** View appointments with role-based filtering

**Acceptance Criteria:**
- Endpoint: `GET /appointments/all`
- RSQL filters:
  - By patient: `?filter=patientId=={id}`
  - By doctor: `?filter=doctorId=={id}`
  - By date range: `?filter=appointmentDate>='2025-12-01';appointmentDate<='2025-12-31'`
  - By status: `?filter=status==SCHEDULED`
  - Combined: `?filter=doctorId=={id};status==SCHEDULED;appointmentDate>=today()`

**Role-Based Access:**
  - PATIENT: Can only see own appointments (auto-filter by patientId from JWT)
  - DOCTOR: Can see appointments where doctorId = self
  - NURSE: Can see all appointments (for scheduling assistance)
  - ADMIN: Can see all appointments

- Enrich response with patient name, doctor name (join or separate calls)
- Sort by appointmentDate, timeSlot (default: ascending)
- Pagination support

**Dependencies:** AP-03, FE-01 (RBAC)  
**Integration Points:** Auth Service (JWT context)

---

#### **AP-05: Appointment Cancellation**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** Cancel appointments with business rules

**Acceptance Criteria:**
- Endpoint: `PUT /appointments/{id}/cancel`
- Request body: `{ "reason": "string" }`
- Business rules:
  - Cannot cancel if appointment date/time is within 2 hours
  - Cannot cancel if status = COMPLETED or CANCELLED
  - Only PATIENT (own appointment) or ADMIN can cancel
  - DOCTOR can mark as NO_SHOW (different endpoint)
- Update fields:
  - status = CANCELLED
  - cancellationReason = request.reason
  - cancelledAt = now()
  - cancelledBy = current user ID
- Response: Updated appointment

**Role-Based:**
  - PATIENT: Can cancel own appointments only (check patientId = JWT patientId)
  - ADMIN: Can cancel any appointment
  - DOCTOR/NURSE: Cannot cancel (only mark NO_SHOW)

**Error Responses:**
  - 403: Not authorized to cancel this appointment
  - 422: Cannot cancel within 2 hours of appointment
  - 422: Appointment already completed/cancelled

**Dependencies:** AP-04  
**Integration Points:** Auth Service (role check)

---

#### **AP-06: Conflict Detection & Prevention**
- **Points:** 3  
- **Priority:** P0 - Critical  
- **Description:** Advanced conflict validation

**Acceptance Criteria:**
- Enhance booking validation with:
  1. Check patient doesn't have overlapping appointments (same date, within 30 min window)
  2. Check doctor doesn't have overlapping appointments
  3. Validate weekend/holiday handling (configurable, simple for MVP):
     - Weekends: Check doctor's weekly schedule (may work Saturdays)
     - Holidays: Check override with status=OFF
  4. Validate time slot format strictly (must match available slots exactly)

- Conflict check query:
  ```sql
  SELECT COUNT(*) FROM appointments 
  WHERE doctorId = ? 
    AND appointmentDate = ? 
    AND timeSlot = ? 
    AND status IN ('SCHEDULED', 'CONFIRMED')
  ```

- Return clear error message: "Time slot 09:30 on 2025-12-15 is already booked"

**Dependencies:** AP-03  
**Integration Points:** HR Service (availability)

---

#### **AP-07: Appointment Status Management**
- **Points:** 2  
- **Priority:** P2 - Medium  
- **Description:** Update appointment status (doctor workflow)

**Acceptance Criteria:**
- Endpoint: `PUT /appointments/{id}/status`
- Request: `{ "status": "COMPLETED" | "NO_SHOW" | "CONFIRMED" }`
- Role-based:
  - DOCTOR: Can update own appointments (COMPLETED, NO_SHOW)
  - NURSE: Can confirm appointments (SCHEDULED → CONFIRMED)
  - ADMIN: Can update any status
- Business rules:
  - Cannot mark COMPLETED until appointment date/time has passed
  - COMPLETED → links to Medical Exam (future integration)
  - NO_SHOW → appointment still counted but patient didn't arrive

**Dependencies:** AP-05  
**Integration Points:** Medical Exam Service (future)

---

#### **AP-08: Doctor Schedule View**
- **Points:** 3  
- **Priority:** P2 - Medium  
- **Description:** View doctor's daily schedule

**Acceptance Criteria:**
- Endpoint: `GET /appointments/schedule?doctorId={id}&date={date}`
- Response: Timeline view of appointments
  ```json
  {
    "doctorId": "uuid",
    "doctorName": "Dr. Nguyen Van A",
    "date": "2025-12-15",
    "slots": [
      {"time": "09:00", "status": "BOOKED", "patientName": "John Doe", "appointmentId": "uuid"},
      {"time": "09:30", "status": "AVAILABLE"},
      {"time": "10:00", "status": "BOOKED", "patientName": "Jane Smith", "appointmentId": "uuid"},
      ...
    ]
  }
  ```
- Merge doctor availability + existing appointments
- DOCTOR can view own schedule
- NURSE/ADMIN can view any doctor's schedule

**Dependencies:** AP-04, HR-03  
**Integration Points:** HR Service (availability)

---

**Epic 2 Total: 30 points**

---

## 🏥 Epic 3: Medical Exam Service - Clinical Records (Week 3)

**Purpose:** Record medical examinations, diagnoses, treatment plans, and basic prescriptions.

**Business Value:** Core clinical workflow. Creates medical record documentation.

**Data Model Design (Future-Proof):**
```
MedicalExam Entity (implement NOW):
- id: String (UUID)
- patientId: String (FK to Patient)
- doctorId: String (FK to Employee)
- appointmentId: String (FK to Appointment, nullable)
- examDate: Instant (required)
- chiefComplaint: String (patient's main concern)
- physicalExamination: String (doctor's findings)
- diagnosis: String (medical diagnosis)
- treatmentPlan: String (recommended treatment)
- status: ExamStatus enum (IN_PROGRESS, COMPLETED, CANCELLED)
- createdAt/updatedAt/createdBy/updatedBy

Prescription Entity (implement NOW):
- id: String (UUID)
- examId: String (FK to MedicalExam)
- medicineId: String (FK to Medicine)
- medicineName: String (snapshot at prescription time)
- dosage: String (e.g., "500mg")
- frequency: String (e.g., "Twice daily")
- duration: String (e.g., "7 days")
- quantity: Integer (total pills/units)
- instructions: String (e.g., "Take after meals")
- unitPrice: BigDecimal (snapshot from Medicine)
- createdAt/createdBy

Future fields (design now, add later):
MedicalExam:
- vitalSigns: JSONB (BP, HR, temp - add when vital signs module built)
- labTests: JSONB (ordered tests - add when lab integration built)
- imagingStudies: JSONB (X-ray, CT - add when imaging integration built)
- followUpDate: LocalDate
- billingId: String (FK to Invoice - add when Billing built)
```

### Stories

#### **ME-01: Medical Exam Service Infrastructure**
- **Points:** 2  
- **Priority:** P0 - Critical  
- **Description:** Scaffold Medical Exam service

**Acceptance Criteria:**
- Create `medical-exam-service` module
- Setup: JPA, MySQL, Eureka, Config
- Database: `medical_exam_db`
- Docker Compose config
- Config file: `medical-exam-service.yml`
- Health check working

**Dependencies:** FE-03  
**Integration Points:** Config, Discovery, Gateway

---

#### **ME-02: Medical Exam Entity & CRUD**
- **Points:** 5  
- **Priority:** P0 - Critical  
- **Description:** Create exam records with full fields

**Acceptance Criteria:**
- Create `MedicalExam` entity with ALL fields
- Create DTOs: `MedicalExamRequest`, `MedicalExamResponse`
- Create Mapper, Repository, Service, Controller
- Endpoints:
  - `POST /medical-exams` - Create (DOCTOR only)
  - `GET /medical-exams/all` - List (paginated, RSQL)
  - `GET /medical-exams/{id}` - Get by ID
  - `PUT /medical-exams/{id}` - Update (DOCTOR only, own exams)
  - `DELETE /medical-exams/{id}` - Soft delete (status=CANCELLED)

**Validation:**
  - Required: patientId, doctorId, examDate
  - examDate cannot be future
  - Auto-set: createdBy from JWT (must be DOCTOR role)

**Role-Based Access:**
  - DOCTOR: Can create/update own exams, view own exams
  - PATIENT: Can view own exams (read-only)
  - NURSE: Can view all exams (read-only)
  - ADMIN: Full access

**Dependencies:** ME-01  
**Integration Points:** None yet (stubs)

---

#### **ME-03: Medical Exam with Patient Validation**
- **Points:** 3  
- **Priority:** P0 - Critical  
- **Description:** Validate patient and link to appointment

**Acceptance Criteria:**
- Enhance `POST /medical-exams` with **Saga Pattern** for distributed transaction:
  1. Validate patient exists: `GET /patient-service/patients/{patientId}` (with circuit breaker)
  2. Validate doctor exists: `GET /hr-service/employees/{doctorId}` (must be DOCTOR role, with circuit breaker)
  3. If appointmentId provided - **SAGA ORCHESTRATION:**
     ```java
     try {
       // Step 1: Validate appointment
       appointment = appointmentService.getAppointment(appointmentId);
       validateAppointment(appointment, patientId, doctorId);
       
       // Step 2: Create exam
       exam = examRepository.save(newExam);
       
       // Step 3: Update appointment status
       appointmentService.updateStatus(appointmentId, COMPLETED);
       
       return exam;
     } catch (Exception ex) {
       // COMPENSATING TRANSACTION: Rollback exam if appointment update fails
       if (exam != null) {
         examRepository.delete(exam);
       }
       throw new SagaException("Failed to complete exam creation", ex);
     }
     ```
  4. Enrich response with patient name, doctor name (cached for performance)

**Business Rules:**
  - Can create exam without appointment (walk-in patients)
  - If linked to appointment, appointment must be SCHEDULED or CONFIRMED (status validation)
  - Doctor creating exam must match appointmentId.doctorId (if provided)
  - **Idempotency:** If exam already exists for appointmentId, return existing exam (prevent duplicates)

**Dependencies:** ME-02, AP-07  
**Integration Points:** Patient Service, HR Service, Appointment Service

---

#### **ME-04: Prescription Management**
- **Points:** 5  
- **Priority:** P1 - High  
- **Description:** Create prescriptions linked to exams

**Acceptance Criteria:**
- Create `Prescription` entity
- Endpoints:
  - `POST /medical-exams/{examId}/prescriptions` - Add prescription (DOCTOR only)
  - `GET /medical-exams/{examId}/prescriptions` - List prescriptions for exam
  - `GET /prescriptions/{id}` - Get prescription details
  - `PUT /prescriptions/{id}` - Update prescription (DOCTOR only, within 24h)
  - `DELETE /prescriptions/{id}` - Delete prescription (DOCTOR only, within 24h)

**Request Body:**
  ```json
  {
    "medicineId": "uuid",
    "dosage": "500mg",
    "frequency": "Twice daily",
    "duration": "7 days",
    "quantity": 14,
    "instructions": "Take after meals with water"
  }
  ```

**Validation & Business Rules:**
  1. Validate medicineId exists: `GET /medicine-service/medicines/{id}`
  2. Snapshot medicine name and unit price at prescription time (for billing)
  3. Validate examId exists and status = COMPLETED
  4. Auto-calculate quantity suggestion based on frequency + duration
  5. Only exam creator (doctor) can prescribe
  6. Can add multiple medicines to one exam

**Response Enrichment:**
  - Include medicine details (name, active ingredient from snapshot)
  - Calculate total cost (quantity × unitPrice)

**Dependencies:** ME-03  
**Integration Points:** Medicine Service

---

#### **ME-05: Medical Exam Listing with Filters**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** View exams with role-based filtering

**Acceptance Criteria:**
- Endpoint: `GET /medical-exams/all`
- RSQL filters:
  - By patient: `?filter=patientId=={id}`
  - By doctor: `?filter=doctorId=={id}`
  - By date range: `?filter=examDate>='2025-12-01';examDate<='2025-12-31'`
  - By status: `?filter=status==COMPLETED`

**Role-Based Auto-Filtering:**
  - PATIENT: Auto-filter by patientId from JWT (can only see own exams)
  - DOCTOR: Auto-filter by doctorId from JWT (can only see own exams)
  - NURSE/ADMIN: Can see all exams

- Enrich response with patient name, doctor name
- Include prescription count in list view
- Sort by examDate (default: descending, newest first)

**Dependencies:** ME-04  
**Integration Points:** Auth Service (JWT context)

---

**Epic 3 Total: 18 points**

---

## 🧾 Epic 5: Billing Service - Invoice & Payment Management (Week 2-3)

**Purpose:** Generate invoices from medical exams, track payments, manage hospital fees.

**Business Value:** Revenue tracking, financial record-keeping, patient billing transparency.

**Data Model Design (Future-Proof):**
```
Invoice Entity (implement NOW):
- id: String (UUID)
- invoiceNumber: String (auto-generated: INV-20251202-001)
- patientId: String (FK to Patient)
- examId: String (FK to MedicalExam)
- invoiceDate: Instant (required)
- dueDate: Instant (invoiceDate + 30 days)
- totalAmount: BigDecimal (calculated from items)
- status: InvoiceStatus enum (UNPAID, PAID, PARTIALLY_PAID, CANCELLED)
- notes: String
- createdAt/updatedAt/createdBy/updatedBy

InvoiceItem Entity (implement NOW):
- id: String (UUID)
- invoiceId: String (FK to Invoice)
- itemType: ItemType enum (CONSULTATION, MEDICINE, PROCEDURE, LAB_TEST)
- description: String (e.g., "Consultation - Dr. Nguyen", "Medicine: Paracetamol")
- quantity: Integer
- unitPrice: BigDecimal
- totalPrice: BigDecimal (quantity × unitPrice)
- referenceId: String (medicineId or examId)

Payment Entity (implement NOW):
- id: String (UUID)
- invoiceId: String (FK to Invoice)
- **idempotencyKey: String (UUID, UNIQUE - prevents duplicate payments)**
- paymentDate: Instant
- amount: BigDecimal
- paymentMethod: PaymentMethod enum (CASH, CARD, BANK_TRANSFER, INSURANCE)
- transactionReference: String (optional, for tracking)
- notes: String
- createdAt/createdBy

**Idempotency Implementation:**
```java
@PostMapping("/payments")
public Payment recordPayment(@RequestBody PaymentRequest request) {
  // Check if payment already processed
  Payment existing = paymentRepo.findByIdempotencyKey(request.getIdempotencyKey());
  if (existing != null) {
    return existing; // Return existing payment, no duplicate charge
  }
  
  // Process new payment
  Payment payment = new Payment();
  payment.setIdempotencyKey(request.getIdempotencyKey());
  return paymentRepo.save(payment);
}
```

Future fields (design now, add later):
Invoice:
- discountAmount: BigDecimal (add when discount module built)
- taxAmount: BigDecimal (add when tax calculation needed)
- insuranceClaimId: String (add when insurance integration built)

Payment:
- gatewayTransactionId: String (add when payment gateway integrated)
- gatewayStatus: String (add when Stripe/PayPal integrated)
```

### Stories

#### **BL-01: Billing Service Infrastructure**
- **Points:** 2  
- **Priority:** P1 - High  
- **Description:** Scaffold Billing service

**Acceptance Criteria:**
- Create `billing-service` module
- Setup: JPA, MySQL, Eureka, Config
- Database: `billing_db`
- Docker Compose config
- Config file: `billing-service.yml`
- Health check working
- Register with Eureka

**Dependencies:** FE-03  
**Integration Points:** Config, Discovery, Gateway

---

#### **BL-02: Invoice Entity & CRUD**
- **Points:** 5  
- **Priority:** P1 - High  
- **Description:** Invoice and invoice items management

**Acceptance Criteria:**
- Create `Invoice` and `InvoiceItem` entities
- Create DTOs: `InvoiceRequest`, `InvoiceResponse`, `InvoiceItemDTO`
- Create Mappers, Repositories, Services, Controllers
- Endpoints:
  - `POST /invoices` - Create invoice (ADMIN only)
  - `GET /invoices/all` - List (paginated, RSQL)
  - `GET /invoices/{id}` - Get by ID with items
  - `PUT /invoices/{id}` - Update invoice (ADMIN only)
  - `DELETE /invoices/{id}` - Cancel invoice (set status=CANCELLED)
  - `POST /invoices/{id}/items` - Add item to invoice
  - `DELETE /invoices/{id}/items/{itemId}` - Remove item

**Validation:**
  - Invoice number must be unique and auto-generated
  - totalAmount = SUM(invoiceItems.totalPrice)
  - Cannot modify PAID invoices
  - Cannot add items to PAID/CANCELLED invoices

**Role-Based Access:**
  - ADMIN: Full CRUD access
  - PATIENT: Can view own invoices (read-only)
  - DOCTOR: Can view invoices for their exams (read-only)

**Dependencies:** BL-01  
**Integration Points:** None yet (stubs)

---

#### **BL-03: Auto-Generate Invoice from Medical Exam**
- **Points:** 8  
- **Priority:** P0 - Critical  
- **Description:** Automatically create invoice when medical exam completed

**Acceptance Criteria:**
- Endpoint: `POST /invoices/generate-from-exam`
- Request: `{ "examId": "uuid" }`
- Workflow:
  1. Fetch exam details: `GET /medical-exam-service/medical-exams/{examId}`
  2. Verify exam status = COMPLETED
  3. Check if invoice already exists for this exam (prevent duplicates)
  4. Fetch prescriptions: `GET /medical-exam-service/medical-exams/{examId}/prescriptions`
  5. Fetch patient details: `GET /patient-service/patients/{patientId}`
  6. Fetch doctor details: `GET /hr-service/employees/{doctorId}`
  7. Calculate invoice items:
     - Item 1: CONSULTATION - "Consultation with Dr. {doctorName}" - Fixed fee $50
     - Item 2-N: MEDICINE - "Medicine: {medicineName} - {quantity} units @ ${unitPrice}" each
  8. Calculate totalAmount = $50 + SUM(prescription totals)
  9. Generate invoice number (format: INV-YYYYMMDD-XXX)
  10. Set dueDate = invoiceDate + 30 days
  11. Create Invoice with status=UNPAID
  12. Link invoice to exam (update MedicalExam.billingId if field exists)

**Business Rules:**
  - Consultation fee: $50 (configurable constant for MVP)
  - Medicine prices from prescription snapshots (unitPrice already stored)
  - Invoice date = current timestamp
  - Due date = invoice date + 30 days
  - Only ADMIN or system can generate invoices

**Response:**
  ```json
  {
    "invoiceId": "uuid",
    "invoiceNumber": "INV-20251202-001",
    "patientName": "John Doe",
    "totalAmount": 125.50,
    "items": [
      {"description": "Consultation - Dr. Nguyen Van A", "amount": 50.00},
      {"description": "Medicine: Paracetamol - 14 units", "amount": 75.50}
    ]
  }
  ```

**Dependencies:** BL-02, ME-04 (prescriptions must exist)  
**Integration Points:** Medical Exam Service, Patient Service, HR Service

---

#### **BL-04: Payment Entity & Recording**
- **Points:** 5  
- **Priority:** P1 - High  
- **Description:** Record payments against invoices

**Acceptance Criteria:**
- Create `Payment` entity
- Endpoints:
  - `POST /invoices/{id}/payments` - Record payment (ADMIN only)
  - `GET /invoices/{id}/payments` - List payments for invoice
  - `GET /payments/{id}` - Get payment details
  - `DELETE /payments/{id}` - Void payment (ADMIN only, within 24h)

**Request Body:**
  ```json
  {
    "amount": 125.50,
    "paymentMethod": "CASH",
    "transactionReference": "optional-ref-123",
    "notes": "Paid in full"
  }
  ```

**Business Logic:**
  1. Validate invoiceId exists and status != CANCELLED
  2. Validate amount > 0 and amount <= invoice remaining balance
  3. Create Payment record
  4. Calculate total paid: SUM(payments.amount)
  5. Update invoice status:
     - If total paid >= totalAmount → status = PAID
     - If total paid > 0 and < totalAmount → status = PARTIALLY_PAID
     - If total paid = 0 → status = UNPAID
  6. Set invoice paidAt timestamp if fully paid

**Response Enrichment:**
  - Include invoice details
  - Show remaining balance
  - List all payments for this invoice

**Dependencies:** BL-03  
**Integration Points:** None

---

#### **BL-05: Invoice Listing with Filters**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** View invoices with role-based filtering

**Acceptance Criteria:**
- Endpoint: `GET /invoices/all`
- RSQL filters:
  - By patient: `?filter=patientId=={id}`
  - By status: `?filter=status==UNPAID`
  - By date range: `?filter=invoiceDate>='2025-12-01';invoiceDate<='2025-12-31'`
  - By amount: `?filter=totalAmount>=100.00`
  - Overdue: `?filter=dueDate<today();status==UNPAID`

**Role-Based Access:**
  - PATIENT: Auto-filter by patientId from JWT (can only see own invoices)
  - ADMIN: Can see all invoices
  - DOCTOR: Can see invoices related to their exams

- Enrich response with patient name, exam details
- Include payment summary (total paid, remaining balance)
- Sort by invoiceDate (default: descending)

**Dependencies:** BL-04  
**Integration Points:** Auth Service (JWT context)

---

**Epic 5 Total: 23 points**

---

## 📊 Epic 6: Reports Service - Analytics & Statistics (Week 3)

**Purpose:** Provide financial and operational reports for hospital management.

**Business Value:** Data-driven decision making, financial tracking, performance monitoring.

**Data Model Design (Future-Proof):**
```
No dedicated entities for MVP (query-based reports)

Future considerations:
- ReportDefinition: Store custom report configurations
- ReportSchedule: Automated report generation
- ReportCache: Cache frequently accessed reports
```

### Stories

#### **RP-01: Reports Service Infrastructure**
- **Points:** 2  
- **Priority:** P2 - Medium  
- **Description:** Scaffold Reports service

**Acceptance Criteria:**
- Create `reports-service` module
- Setup: JPA, MySQL, Eureka, Config, WebClient for service calls
- Database: `reports_db` (minimal, for caching if needed)
- Docker Compose config
- Config file: `reports-service.yml`
- Health check working
- Register with Eureka

**Dependencies:** FE-03  
**Integration Points:** Config, Discovery, Gateway

---

#### **RP-02: Revenue Report (Financial)**
- **Points:** 5  
- **Priority:** P1 - High  
- **Description:** Daily/weekly/monthly revenue and payment tracking

**Acceptance Criteria:**
- Endpoint: `GET /reports/revenue?startDate={date}&endDate={date}`
- Query Billing Service for invoice and payment data
- Calculate metrics:
  - Total invoices generated
  - Total amount billed
  - Total amount paid
  - Total amount unpaid
  - Payment breakdown by method (CASH, CARD, etc.)
  - Daily revenue trend

**Response:**
  ```json
  {
    "period": {"start": "2025-12-01", "end": "2025-12-31"},
    "summary": {
      "totalInvoices": 250,
      "totalBilled": 150000.00,
      "totalPaid": 120000.00,
      "totalUnpaid": 30000.00,
      "collectionRate": 80.0
    },
    "paymentMethods": {
      "CASH": 60000.00,
      "CARD": 40000.00,
      "BANK_TRANSFER": 15000.00,
      "INSURANCE": 5000.00
    },
    "dailyRevenue": [
      {"date": "2025-12-01", "billed": 5000.00, "paid": 4000.00, "invoices": 10},
      {"date": "2025-12-02", "billed": 4500.00, "paid": 4500.00, "invoices": 9}
    ]
  }
  ```

**Business Logic:**
  - Call: `GET /billing-service/invoices/all?filter=invoiceDate>={start};invoiceDate<={end}`
  - Call: `GET /billing-service/invoices/{id}/payments` for each invoice
  - Aggregate data by date, payment method
  - Calculate collection rate: (totalPaid / totalBilled) × 100

**Role-Based Access:**
  - ADMIN only (financial data is sensitive)

**Dependencies:** RP-01, BL-05 (invoice listing)  
**Integration Points:** Billing Service

---

#### **RP-03: Appointment Statistics Report**
- **Points:** 5  
- **Priority:** P1 - High  
- **Description:** Appointment volume, status distribution, completion rates

**Acceptance Criteria:**
- Endpoint: `GET /reports/appointments?startDate={date}&endDate={date}`
- Query Appointment Service for appointment data
- Calculate metrics:
  - Total appointments
  - Status breakdown (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW)
  - Completion rate
  - No-show rate
  - Cancellation rate
  - Average appointments per day
  - Peak days/hours

**Response:**
  ```json
  {
    "period": {"start": "2025-12-01", "end": "2025-12-31"},
    "summary": {
      "totalAppointments": 500,
      "completed": 350,
      "cancelled": 50,
      "noShow": 30,
      "scheduled": 70,
      "completionRate": 70.0,
      "noShowRate": 6.0,
      "cancellationRate": 10.0
    },
    "statusBreakdown": {
      "COMPLETED": 350,
      "SCHEDULED": 70,
      "CANCELLED": 50,
      "NO_SHOW": 30,
      "CONFIRMED": 0
    },
    "dailyTrend": [
      {"date": "2025-12-01", "total": 20, "completed": 15, "cancelled": 2, "noShow": 1},
      {"date": "2025-12-02", "total": 18, "completed": 16, "cancelled": 1, "noShow": 0}
    ]
  }
  ```

**Business Logic:**
  - Call: `GET /appointment-service/appointments/all?filter=appointmentDate>={start};appointmentDate<={end}`
  - Group by status, date
  - Calculate rates based on totals

**Role-Based Access:**
  - ADMIN, NURSE (for scheduling insights)

**Dependencies:** RP-01, AP-04 (appointment listing)  
**Integration Points:** Appointment Service

---

#### **RP-04: Doctor Performance Report**
- **Points:** 4  
- **Priority:** P1 - High  
- **Description:** Doctor workload, patient count, revenue contribution

**Acceptance Criteria:**
- Endpoint: `GET /reports/doctors?startDate={date}&endDate={date}&doctorId={id}`
- Query multiple services for comprehensive doctor metrics
- Calculate per doctor:
  - Total appointments
  - Total exams completed
  - Total patients seen (unique)
  - Revenue generated (from invoices)
  - Average appointments per day
  - Average revenue per appointment

**Response:**
  ```json
  {
    "period": {"start": "2025-12-01", "end": "2025-12-31"},
    "doctors": [
      {
        "doctorId": "uuid",
        "doctorName": "Dr. Nguyen Van A",
        "department": "CARDIOLOGY",
        "metrics": {
          "totalAppointments": 120,
          "completedExams": 110,
          "uniquePatients": 95,
          "revenueGenerated": 12000.00,
          "avgAppointmentsPerDay": 6.0,
          "avgRevenuePerAppointment": 100.00,
          "completionRate": 91.67
        }
      }
    ]
  }
  ```

**Business Logic:**
  - Call Appointment Service: count appointments by doctor
  - Call Medical Exam Service: count exams by doctor
  - Call Patient Service: count unique patients
  - Call Billing Service: sum invoice amounts where exam.doctorId = doctor
  - Calculate averages and rates

**Role-Based Access:**
  - ADMIN only (performance data is sensitive)
  - DOCTOR can view own statistics only

**Dependencies:** RP-01, AP-04, ME-05, BL-05  
**Integration Points:** Appointment Service, Medical Exam Service, Billing Service, HR Service

---

#### **RP-05: Patient Activity Report**
- **Points:** 2  
- **Priority:** P2 - Medium  
- **Description:** Patient visit frequency and engagement metrics

**Acceptance Criteria:**
- Endpoint: `GET /reports/patients?startDate={date}&endDate={date}`
- Calculate:
  - Total unique patients
  - New patients (first visit in period)
  - Returning patients
  - Total visits
  - Average visits per patient
  - Most frequent patients (top 10)

**Response:**
  ```json
  {
    "period": {"start": "2025-12-01", "end": "2025-12-31"},
    "summary": {
      "totalPatients": 200,
      "newPatients": 50,
      "returningPatients": 150,
      "totalVisits": 500,
      "avgVisitsPerPatient": 2.5
    },
    "topPatients": [
      {"patientId": "uuid", "patientName": "John Doe", "visits": 12},
      {"patientId": "uuid", "patientName": "Jane Smith", "visits": 10}
    ]
  }
  ```

**Business Logic:**
  - Call Appointment Service: get all appointments in period
  - Call Medical Exam Service: verify completed visits
  - Call Patient Service: check patient registration dates (new vs returning)
  - Group by patient, count visits

**Role-Based Access:**
  - ADMIN only

**Dependencies:** RP-01, AP-04, ME-05  
**Integration Points:** Appointment Service, Medical Exam Service, Patient Service

---

**Epic 6 Total: 18 points**

---

## 🔗 Epic 4: Integration, Testing & Documentation (Week 3)

**Purpose:** Ensure all services work together, document APIs, prepare for deployment.

**Business Value:** System quality, reliability, developer experience.

### Stories

#### **IT-01: End-to-End Workflow Testing**
- **Points:** 3  
- **Priority:** P0 - Critical  
- **Description:** Test complete user journeys

**Test Scenarios:**
1. **Patient Registration → Appointment → Exam → Billing Flow:**
   - Register patient account
   - Login as patient
   - Search for doctor
   - Book appointment
   - Login as doctor
   - View appointments
   - Create medical exam
   - Add prescription
   - Auto-generate invoice from exam
   - Record payment
   - Patient views exam record and invoice

2. **Doctor Availability → Booking Flow:**
   - Admin creates doctor employee
   - Doctor sets weekly availability
   - Doctor adds holiday override
   - Patient books appointment at available slot
   - Verify conflict prevention (try double-booking)

3. **Role-Based Access Testing:**
   - Test PATIENT can't access other patients' data
   - Test DOCTOR can't modify other doctors' exams
   - Test ADMIN has full access
   - Test NURSE has read-only access
   - Test PATIENT can view own invoices only
   - Test ADMIN can access all financial reports

4. **Billing & Reports Workflow:**
   - Complete exam → generate invoice → record payment
   - Run revenue report to verify payment appears
   - Run appointment statistics report
   - Run doctor performance report
   - Verify all aggregations are correct

**Deliverables:**
  - Test cases documented
  - Postman collection updated with all endpoints (including Billing & Reports)
  - Integration test suite (JUnit - optional for MVP)

**Dependencies:** All epics  
**Integration Points:** All services

---

#### **IT-02: API Documentation (Swagger/OpenAPI)**
- **Points:** 3  
- **Priority:** P1 - High  
- **Description:** Complete API documentation for all endpoints

**Acceptance Criteria:**
- Add Swagger/Springdoc dependencies to all services
- Configure Swagger UI: `/api-docs` endpoint
- Document all endpoints with:
  - Operation summary & description
  - Request/Response schemas
  - Example requests/responses
  - Error codes documentation
  - Authentication requirements
- Centralize Swagger docs via API Gateway (optional)
- Export OpenAPI 3.0 spec JSON file

**Deliverables:**
  - Swagger UI accessible at `http://localhost:8080/{service}/api-docs`
  - OpenAPI spec files in `/docs` folder

**Dependencies:** All services implemented  
**Integration Points:** API Gateway

---

#### **IT-03: Deployment & Environment Setup**
- **Points:** 1  
- **Priority:** P1 - High  
- **Description:** Update Docker Compose for all services

**Acceptance Criteria:**
- Update `infrastructure/pro/compose.yaml`:
  - Add MySQL for hr-service
  - Add MySQL for appointment-service
  - Add MySQL for medical-exam-service
  - Add MySQL for billing-service
  - Add MySQL for reports-service
  - Add service containers for hr, appointment, medical-exam, billing, reports
  - Configure dependencies (wait for config-server, databases)
  - Health checks for all services

- Update `.env.example` with all new environment variables:
  - HR_SERVICE_PORT, HR_DB_*
  - APPOINTMENT_SERVICE_PORT, APPOINTMENT_DB_*
  - MEDICAL_EXAM_SERVICE_PORT, MEDICAL_EXAM_DB_*
  - BILLING_SERVICE_PORT, BILLING_DB_*
  - REPORTS_SERVICE_PORT, REPORTS_DB_*

- Test full stack startup: `docker-compose up`

**Deliverables:**
  - Updated Docker Compose files
  - Updated environment variable templates
  - Deployment README with startup order

**Dependencies:** All services implemented  
**Integration Points:** All services

---

**Epic 4 Total: 9 points**

---

## 📊 Summary Tables

### Total Points by Epic

| Epic | Points | Percentage |
|------|--------|------------|
| Epic 0: Foundation Enhancement | 13 | 11% |
| Epic 1: HR Service | 14 | 11% |
| Epic 2: Appointment Service | 30 | 24% |
| Epic 3: Medical Exam Service | 18 | 14% |
| Epic 5: Billing Service | 23 | 18% |
| Epic 6: Reports Service | 18 | 14% |
| Epic 4: Integration & Testing | 9 | 7% |
| **TOTAL** | **125** | **100%** |

### Points by Week

| Week | Focus | Points | Stories |
|------|-------|--------|---------|
| Week 1 | Foundation + HR | 27 | 9 stories |
| Week 2 | Appointment Service | 30 | 8 stories |
| Week 3 | Medical Exam + Polish | 25 | 8 stories |
| **TOTAL** | | **84** | **25 stories** |

### Services Status After MVP

| Service | Status | Key Features |
|---------|--------|--------------|
| Config Server | ✅ Existing | Centralized configuration |
| Discovery Service | ✅ Existing | Service registry (Eureka) |
| API Gateway | ✅ Enhanced | JWT validation, routing for 8 services |
| Common Library | ✅ Enhanced | Shared enums, utilities |
| Auth Service | ✅ Enhanced | RBAC with 4 roles |
| Medicine Service | ✅ Existing | Medicine catalog |
| Patient Service | ✅ Enhanced | Complete patient records |
| HR Service | 🆕 **NEW** | Employees, doctors, availability |
| Appointment Service | 🆕 **NEW** | Booking, cancellation, scheduling |
| Medical Exam Service | 🆕 **NEW** | Exam records, prescriptions |
| Billing Service | 🆕 **NEW** | Invoices, payments, fee tracking |
| Reports Service | 🆕 **NEW** | Financial & operational analytics |

---

## 👥 Team Allocation

### Backend Team (3 Developers)

| BE Dev | Skill Level | Week 1 (27 pts) | Week 2 (38 pts) | Week 3 (58 pts) | Total Points |
|--------|-------------|-----------------|-----------------|-----------------|--------------|
| **BE-1 (Senior)** | Backend Lead | Epic 0 (13 pts) + HR-01, HR-02 (7 pts) | AP-01 to AP-04 (19 pts) | RP-01 to RP-05 (18 pts) + IT-02 (3 pts) | 60 pts |
| **BE-2 (Mid-Senior)** | Services Expert | HR-03, HR-04 (7 pts) | AP-05 to AP-08 (11 pts) + BL-01, BL-02 (7 pts) | BL-03 (8 pts) + ME-03 (3 pts) + IT-01 (3 pts) | 39 pts |
| **BE-3 (Mid)** | Core Developer | - | ME-01, ME-02 (7 pts) | ME-04, ME-05 (8 pts) + BL-04, BL-05 (8 pts) + IT-03 (1 pt) | 24 pts |

**Total Backend:** 123 story points across 3 developers (avg 41 pts/dev)

**Backend Weekly Distribution:**

**Week 1: Foundation + HR (27 points)**
- BE-1: FE-01 (5 pts), FE-02 (3 pts), FE-03 (2 pts), FE-04 (3 pts), HR-01 (2 pts), HR-02 (5 pts)
- BE-2: HR-03 (5 pts), HR-04 (2 pts)
- BE-3: Code review, environment setup, testing support

**Week 2: Appointments + Billing Start (38 points)**
- BE-1: AP-01 (3 pts), AP-02 (5 pts), AP-03 (8 pts), AP-04 (3 pts)
- BE-2: AP-05 (3 pts), AP-06 (3 pts), AP-07 (2 pts), AP-08 (3 pts), BL-01 (2 pts), BL-02 (5 pts)
- BE-3: ME-01 (2 pts), ME-02 (5 pts)

**Week 3: Medical Exam + Billing + Reports (58 points)**
- BE-1: RP-01 (2 pts), RP-02 (5 pts), RP-03 (5 pts), RP-04 (4 pts), RP-05 (2 pts), IT-02 (3 pts)
- BE-2: BL-03 (8 pts), ME-03 (3 pts), IT-01 (3 pts)
- BE-3: ME-04 (5 pts), ME-05 (3 pts), BL-04 (5 pts), BL-05 (3 pts), IT-03 (1 pt)

---

### Frontend Team (5 Developers) - High-Level Plan

> **Detailed FE backlog with screens, components, and user flows will be created separately.**

**FE Work Mode:** Parallel development with 3 integration points

**Week 1: Setup + Static Screens (FE works ahead)**
- **FE-1 (Lead):** Project setup, auth screens (login/register), routing, API client setup
- **FE-2:** Patient module (list, create, edit forms)
- **FE-3:** Medicine module (list, create, edit forms)
- **FE-4:** Shared components (tables, forms, modals) + design system
- **FE-5:** HR module mockups (employee list, doctor availability calendar)

**Week 2: Core Features + Mock Integration**
- **FE-1:** Appointment booking flow (multi-step form, doctor selection, time slots)
- **FE-2:** Medical exam module (exam form, prescription builder)
- **FE-3:** Billing module (invoice view, payment forms)
- **FE-4:** Reports & dashboards (charts, filters, date pickers)
- **FE-5:** Integration layer (API hooks, state management)

**Week 3: Integration + Testing**
- **All FE:** Replace mocks with real APIs, bug fixes, E2E testing, polish

**FE Integration Points:**
- **Day 5 (Week 1):** Auth + Patient + Medicine APIs ready → FE integrates
- **Day 10 (Week 2):** HR + Appointment APIs ready → FE integrates booking flow
- **Day 15 (Week 3):** Billing + Reports APIs ready → FE completes integration

**FE Deliverables (Separate Document):**
- Screen designs for all modules
- Component library documentation
- API integration guide
- User flow diagrams
- Form validation rules

---

## 🚀 Getting Started

### Prerequisites
- Java 23
- Docker & Docker Compose
- 4 developers (recommended)
- 3 weeks dedicated time

### Development Approach
1. **Week 1:** Focus on foundation (RBAC, patient enhancement) + complete HR Service
2. **Week 2:** Build Appointment Service end-to-end + start Billing Service (invoice generation)
3. **Week 3:** Build Medical Exam Service + complete Billing (payments) + Reports Service + testing + documentation

### Key Success Factors
✅ Follow existing patterns (`GenericController`, `GenericService`)  
✅ Design full data models now (add fields later without schema changes)  
✅ Service-to-service calls via RestTemplate/WebClient  
✅ Role-based authorization on all endpoints  
✅ Integration testing for critical flows  
✅ Document as you build (Swagger annotations)  

---

## 📝 Notes for Future Expansion

### Post-MVP Features (Phase 2+)
- Payment gateway integration (Stripe, PayPal, VNPay)
- Advanced billing: insurance claims, payment plans, discounts/promotions, tax calculation
- Custom report builder with dynamic SQL queries
- Real-time dashboards with interactive charts (Chart.js, D3.js)
- Report scheduling and email delivery
- Vital signs tracking (blood pressure, heart rate, temperature)
- Lab test integration and result management
- Imaging study management (X-ray, CT, MRI)
- Document uploads (prescriptions, lab reports, images)
- Email/SMS notifications (appointment reminders, payment reminders)
- Advanced RBAC (permission matrix, resource-level access control)
- Patient portal dashboard
- Doctor performance analytics with benchmarking
- Inventory management for medicines

### Data Model Extensibility
All entities designed with future fields in mind. Adding features won't require:
- Schema migrations (fields already designed, just add columns)
- Breaking API changes (add fields to DTOs, backward compatible)
- Service refactoring (independent services can evolve separately)

---

**Document Status:** ✅ READY FOR IMPLEMENTATION  
**Created:** December 2, 2025  
**Version:** 2.1 (3-Week MVP - Anti-Pattern Fixes Applied)  
**Total Scope:** 125 story points, 34 stories, 6 new services, 8 developers  
**Next Steps:** Generate Data Models document → API Contracts → Feature Specifications

---

## 📈 Project Metrics Summary

### Scope Overview
- **Total Story Points:** 125
- **Total Stories:** 34
- **Total Epics:** 7 (including Epic 0 and Epic 4)
- **New Services:** 6 (HR, Appointment, Medical Exam, Billing, Reports + enhanced existing 4)
- **Total Endpoints:** ~80+ REST endpoints
- **Database Tables:** ~15 new tables

### Team Capacity
- **Team Size:** 4 developers
- **Duration:** 3 weeks (15 working days)
- **Average Points per Developer:** 31.25 points
- **Average Points per Week:** 41.67 points
- **Velocity Target:** ~10 points per developer per week

### Technical Stack
- **Backend:** Spring Boot 3.5.8, Java 23
- **Architecture:** Microservices (12 total services including infrastructure)
- **Database:** MySQL 8.0 (10 databases: 1 per business service + infrastructure)
- **Service Discovery:** Eureka
- **API Gateway:** Spring Cloud Gateway
- **Configuration:** Spring Cloud Config Server
- **Authentication:** JWT with OAuth2 Resource Server
- **Containerization:** Docker + Docker Compose

### Complexity Breakdown
| Epic | Complexity | Primary Challenge |
|------|-----------|-------------------|
| Epic 0 | 🟢 Low | Enhancing existing code |
| Epic 1 | 🟡 Medium | JSONB availability schedule |
| Epic 2 | 🔴 High | Multi-service validation + conflict detection |
| Epic 3 | 🟡 Medium | Prescription snapshot logic |
| Epic 5 | 🟡 Medium | Auto-invoice generation workflow |
| Epic 6 | 🟢 Low | Aggregation queries only |
| Epic 4 | 🟢 Low | Testing + documentation |

### Business Value Priority
1. **Epic 2 (Appointment)** - Core patient-facing feature
2. **Epic 3 (Medical Exam)** - Core clinical workflow
3. **Epic 5 (Billing)** - Revenue tracking (mandatory)
4. **Epic 1 (HR)** - Enables appointments
5. **Epic 6 (Reports)** - Management insights (mandatory)
6. **Epic 0 (Foundation)** - Prerequisite for all
7. **Epic 4 (Testing)** - Quality assurance

### Lecture Requirement Compliance
- ✅ 100% coverage (24/24 requirements)
- ✅ All 6 categories implemented
- ✅ No gaps in mandatory features
- ✅ Demonstrates full-stack expertise
- ✅ Production-ready architecture

---

## 🎯 Success Criteria

**Definition of Done (Backend MVP):**
- [ ] All 33 backend stories completed and tested
- [ ] 100% of lecture requirements covered
- [ ] All 6 new services deployed via Docker Compose
- [ ] Postman collection with 80+ endpoints working
- [ ] API contracts published (OpenAPI/Swagger)
- [ ] End-to-end backend workflow tested (patient → appointment → exam → invoice → payment)
- [ ] All 4 reports generating accurate data
- [ ] E2E integration tests passing
- [ ] Service-to-service communication validated
- [ ] JWT authorization working across all services

**Definition of Done (Frontend Integration):**
- [ ] All UI screens completed (Auth, Patient, Medicine, HR, Appointment, Medical Exam, Billing, Reports)
- [ ] Full API integration (no mocks remaining)
- [ ] User flows tested (patient registration → appointment booking → exam → billing)
- [ ] Reports dashboard with filters/charts working
- [ ] Authentication flow (login, JWT refresh, logout) working
- [ ] Error handling and validation UX complete
- [ ] Responsive design (desktop + mobile)

**Combined System Ready For:**
- ✅ Lecture demonstration (all 24 requirements visible)
- ✅ Production deployment (Docker Compose)
- ✅ Live demo (patient → appointment → exam → invoice workflow)
- ✅ Code review (clean architecture, best practices)

---

## 📋 Next Steps (Documentation Package)

**Immediate Priority (Week 1 Prep):**
1. **Data Models Complete Document** - All 9 entities with fields, relationships, migrations (BE team needs this Day 1)
2. **API Contracts Complete Document** - OpenAPI specs for all 80+ endpoints (FE team can start building with mocks)
3. **Backend Feature Specifications** - 6 epic files with field-level detail, logic flows, business rules

**Secondary Priority (Parallel Work):**
4. **Frontend Backlog & User Flows** - Separate document with screens, components, state management
5. **Frontend Component Specifications** - shadcn/ui integration, form validation, API client patterns

**Timeline:**
- Day 0 (Today): Complete Data Models + API Contracts
- Day 1: BE team starts Epic 0, FE team starts Auth screens
- Day 2-3: Complete BE Feature Specs + FE User Flows
- Day 5: First integration checkpoint (Auth + Patient + Medicine)

---

## 📞 Communication Protocol

**Daily Standups (15 min):**
- BE team: Service progress, blockers, API readiness
- FE team: Screen progress, integration issues, mock requirements
- Sync: Integration checkpoint updates

**Integration Checkpoints:**
- **Day 5:** BE demo Auth/Patient/Medicine APIs → FE switches from mocks
- **Day 10:** BE demo HR/Appointment APIs → FE tests booking flow
- **Day 15:** BE demo Billing/Reports APIs → FE begins full integration

**Communication Channels:**
- Slack: Real-time questions, quick decisions
- GitHub: Code reviews, PR discussions
- Postman: API contract changes, endpoint testing
- Confluence/Notion: Spec updates, design decisions

**Risk Escalation:**
- Any blocker >4 hours → escalate to team lead
- Integration failure → immediate sync meeting
- Scope creep detected → backlog review meeting

---
- [ ] API documentation complete (Swagger)
- [ ] No P0/P1 bugs blocking workflows
- [ ] Code reviewed and merged to main branch
- [ ] Demo-ready for academic presentation

**Stretch Goals (if time permits):**
- [ ] Invoice PDF export (BL-06 - 2 pts)
- [ ] Report scheduling (future epic)
- [ ] Email notifications (future epic)
- [ ] Advanced caching (Redis)

---

**🚀 Ready to proceed with implementation! All features included, no simplification needed.**

