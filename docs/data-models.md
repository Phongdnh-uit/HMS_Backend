# Data Models Documentation

**Project:** Hospital Management System (HMS)  
**Pattern:** Database-per-Service (Microservices)  
**DBMS:** MySQL 8+  
**ORM:** Spring Data JPA + Hibernate

---

## Overview

The HMS backend implements a **database-per-service pattern**, where each microservice manages its own isolated database. This provides:

- ✅ **Service autonomy** - Each service owns its data
- ✅ **Technology flexibility** - Different schemas per service
- ✅ **Fault isolation** - Database failures don't cascade
- ✅ **Independent scaling** - Scale databases based on service load

**Total Entities:** 17 core entities across 7 databases  
**Relationship Strategy:** Denormalized references (service IDs instead of foreign keys across services)

---

## Common Patterns

### Audit Fields (All Entities)

Every entity includes JPA auditing via `@EntityListeners(AuditingEntityListener.class)`:

```java
@CreatedDate
private Instant createdAt;

@LastModifiedDate
private Instant updatedAt;

@CreatedBy
private String createdBy;

@LastModifiedBy
private String updatedBy;
```

### Primary Keys

**UUID Generation Strategy:**
```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private String id;
```

All entities use UUID strings for distributed system compatibility and avoiding ID collisions.

### Denormalization Pattern

Cross-service references use **snapshot denormalization** for historical accuracy and query performance:

```java
// Instead of foreign key to patient-service
private String patientId;           // Reference for querying
private String patientName;         // Snapshot at creation time
```

This ensures:
- Historical data remains intact if patient updates profile
- No cross-service joins needed for displaying data
- Resilience to service unavailability

---

## Auth Service Database (auth_db)

### Account Entity

**Table:** `accounts`  
**Purpose:** User authentication and authorization

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Unique account identifier |
| email | VARCHAR | UNIQUE, NOT NULL | Login email |
| password | VARCHAR | NOT NULL | BCrypt hashed password |
| role | ENUM | NOT NULL | RoleEnum: ADMIN, DOCTOR, NURSE, RECEPTIONIST, PATIENT |
| refreshToken | VARCHAR | - | JWT refresh token |
| refreshTokenExpiresAt | TIMESTAMP | - | Refresh token expiry |
| emailVerified | BOOLEAN | DEFAULT false | Email verification status |

**Relationships:** None (isolated service)

**Notes:**
- Passwords stored with BCrypt hashing
- Role-based access control (RBAC) via `RoleEnum`
- Refresh token rotation for security
- No soft delete (accounts are permanent once created)

---

## Patient Service Database (patient_db)

### Patient Entity

**Table:** `patients`  
**Purpose:** Patient demographic and medical information

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Patient identifier |
| accountId | VARCHAR | - | Reference to auth-service account |
| fullName | VARCHAR | NOT NULL | Patient full name |
| email | VARCHAR | - | Contact email |
| dateOfBirth | DATE | - | Birth date |
| gender | ENUM | - | Gender (MALE, FEMALE, OTHER) |
| phoneNumber | VARCHAR | - | Contact phone |
| address | VARCHAR | - | Residential address |
| identificationNumber | VARCHAR | - | National ID / passport |
| healthInsuranceNumber | VARCHAR | - | Insurance policy number |
| relativeFullName | VARCHAR | - | Emergency contact name |
| relativePhoneNumber | VARCHAR | - | Emergency contact phone |
| relativeRelationship | VARCHAR | - | Relationship to patient |
| bloodType | VARCHAR | - | Blood type (A+, B+, O-, etc.) |
| allergies | VARCHAR | - | Known allergies |
| profileImageUrl | VARCHAR | - | Profile photo URL |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:** None (references accountId in auth-service)

**Indexes:** Likely on accountId for login lookups

**Notes:**
- Comprehensive patient demographics
- Emergency contact information
- Medical metadata (blood type, allergies)
- Profile image storage (URL to external storage)

---

## Appointment Service Database (appointment_db)

### Appointment Entity

**Table:** `appointments`  
**Purpose:** Scheduled and walk-in appointments with queue management

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Appointment identifier |
| patientId | VARCHAR | - | Reference to patient-service |
| patientName | VARCHAR | - | Patient name snapshot |
| doctorId | VARCHAR | - | Reference to hr-service employee |
| doctorName | VARCHAR | - | Doctor name snapshot |
| doctorDepartment | VARCHAR | - | Department snapshot |
| appointmentTime | TIMESTAMP | - | Scheduled time |
| status | ENUM | - | PENDING, CONFIRMED, COMPLETED, CANCELLED, IN_PROGRESS, WAITING |
| type | ENUM | - | SCHEDULED, WALK_IN |
| reason | VARCHAR | - | Chief complaint |
| notes | VARCHAR | - | Additional notes |
| cancelledAt | TIMESTAMP | - | Cancellation timestamp |
| cancelReason | VARCHAR | - | Why cancelled |
| queueNumber | INT | - | Daily queue position (1, 2, 3...) |
| priority | INT | - | Priority level (10=Emergency, 100=Normal) |
| priorityReason | VARCHAR | - | EMERGENCY, ELDERLY, PREGNANT, APPOINTMENT |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:** None (denormalized references)

**Business Rules:**
- Queue numbers reset daily
- Lower priority = higher urgency
- Status transitions: PENDING → CONFIRMED → WAITING → IN_PROGRESS → COMPLETED
- Walk-ins automatically get queue numbers

**Notes:**
- Supports both scheduled and walk-in workflows
- Priority queue system for waiting room management
- Historical accuracy via snapshot fields

---

## Medical Exam Service Database (exam_db)

### 1. MedicalExam Entity

**Table:** `medical_exams`  
**Purpose:** Core medical examination records

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Exam identifier |
| appointmentId | VARCHAR | UNIQUE, NOT NULL | Links to appointment (1:1) |
| patientId | VARCHAR | - | Patient reference |
| patientName | VARCHAR | - | Patient snapshot |
| doctorId | VARCHAR | - | Doctor reference |
| doctorName | VARCHAR | - | Doctor snapshot |
| diagnosis | TEXT | - | Medical diagnosis |
| symptoms | TEXT | - | Presented symptoms |
| treatment | TEXT | - | Treatment plan |
| temperature | DOUBLE | - | Body temperature (°C) |
| bloodPressureSystolic | INT | - | Systolic BP (mmHg) |
| bloodPressureDiastolic | INT | - | Diastolic BP (mmHg) |
| heartRate | INT | - | Heart rate (bpm) |
| weight | DOUBLE | - | Weight (kg) |
| height | DOUBLE | - | Height (cm) |
| notes | TEXT | - | Additional notes |
| hasPrescription | BOOLEAN | NOT NULL | Has prescription flag (billing workflow) |
| examDate | TIMESTAMP | - | Exam date |
| followUpDate | DATE | - | Scheduled follow-up |
| followUpNotificationSent | BOOLEAN | NOT NULL | Follow-up reminder sent flag |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:** 
- 1:1 with Appointment (appointmentId)
- 1:N with Prescription (via prescription.medicalExamId)
- 1:N with LabOrder (via labOrder.medicalExamId)

**Notes:**
- Complete vital signs recording
- Follow-up notification automation
- Billing integration via `hasPrescription` flag

### 2. Prescription Entity

**Table:** `prescriptions`  
**Purpose:** Medicine prescriptions with lifecycle management

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Prescription identifier |
| medicalExamId | VARCHAR | NOT NULL | Medical exam reference |
| status | ENUM | NOT NULL | ACTIVE, CANCELLED, DISPENSED |
| cancelledAt | TIMESTAMP | - | Cancellation time |
| cancelledBy | VARCHAR | - | Who cancelled |
| cancelReason | VARCHAR | - | Why cancelled |
| dispensedAt | TIMESTAMP | - | Dispensed timestamp |
| dispensedBy | VARCHAR | - | Pharmacist who dispensed |
| patientId | VARCHAR | - | Patient reference |
| patientName | VARCHAR | - | Patient snapshot |
| doctorId | VARCHAR | - | Doctor reference |
| doctorName | VARCHAR | - | Doctor snapshot |
| prescribedAt | TIMESTAMP | NOT NULL | Prescription time |
| notes | TEXT | - | Prescription notes |
| + audit fields | - | - | createdAt, createdBy |

**Relationships:**
- 1:N with PrescriptionItem (cascade ALL, orphan removal)

**Lifecycle:**
1. **ACTIVE** - Created by doctor, medicine stock reserved
2. **CANCELLED** - Doctor cancels, stock restored (saga compensation)
3. **DISPENSED** - Pharmacy dispenses, triggers invoice generation

### 3. PrescriptionItem Entity

**Table:** `prescription_items`  
**Purpose:** Individual medicines in a prescription

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Item identifier |
| prescription | FK | NOT NULL | Prescription reference (ManyToOne LAZY) |
| medicineId | VARCHAR | - | Medicine reference (medicine-service) |
| medicineName | VARCHAR | - | Medicine snapshot |
| quantity | INT | - | Quantity prescribed |
| dosage | VARCHAR | - | Dosage instructions |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:**
- N:1 with Prescription (cascade via parent)

### 4. LabOrder Entity

**Table:** `lab_orders`  
**Purpose:** Laboratory test orders

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Lab order identifier |
| medicalExamId | VARCHAR | - | Medical exam reference |
| patientId | VARCHAR | - | Patient reference |
| patientName | VARCHAR | - | Patient snapshot |
| doctorId | VARCHAR | - | Ordering doctor |
| doctorName | VARCHAR | - | Doctor snapshot |
| status | ENUM | - | PENDING, COMPLETED, CANCELLED |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:**
- 1:N with LabTestResult (cascade ALL)

### 5. LabTest Entity

**Table:** `lab_tests`  
**Purpose:** Lab test catalog/template

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Test identifier |
| testName | VARCHAR | - | Test name |
| testCode | VARCHAR | - | Lab code |
| category | VARCHAR | - | Test category |
| description | TEXT | - | Description |
| price | DECIMAL | - | Test price |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Notes:** Template/catalog for available lab tests

### 6. LabTestResult Entity

**Table:** `lab_test_results`  
**Purpose:** Individual test results in a lab order

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Result identifier |
| labOrder | FK | - | Lab order reference (ManyToOne LAZY) |
| labTestId | VARCHAR | - | Test template reference |
| testName | VARCHAR | - | Test name snapshot |
| testValue | VARCHAR | - | Result value |
| unit | VARCHAR | - | Measurement unit |
| normalRange | VARCHAR | - | Reference range |
| notes | TEXT | - | Interpretation notes |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:**
- N:1 with LabOrder

### 7. DiagnosticImage Entity

**Table:** `diagnostic_images`  
**Purpose:** Medical images (X-ray, CT, MRI, etc.)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Image identifier |
| labTestResultId | VARCHAR | - | Associated lab result |
| imageUrl | VARCHAR | - | Image storage URL |
| imageType | VARCHAR | - | X-RAY, CT_SCAN, MRI, ULTRASOUND |
| description | TEXT | - | Image description |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Notes:** Images stored externally (S3/cloud storage), URL reference only

---

## Billing Service Database (billing_db)

### 1. Invoice Entity

**Table:** `invoices`  
**Purpose:** Patient billing records

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Invoice identifier |
| invoiceNumber | VARCHAR | UNIQUE, NOT NULL | Human-readable invoice number |
| medicalExamId | VARCHAR | NOT NULL | Medical exam reference |
| appointmentId | VARCHAR | NOT NULL | Appointment reference |
| patientId | VARCHAR | NOT NULL | Patient reference |
| patientName | VARCHAR | NOT NULL | Patient snapshot |
| invoiceDate | TIMESTAMP | NOT NULL | Invoice creation date |
| dueDate | TIMESTAMP | - | Payment due date |
| subtotal | DECIMAL(12,2) | - | Sum of items |
| discount | DECIMAL(12,2) | - | Applied discount |
| tax | DECIMAL(12,2) | - | Tax amount |
| totalAmount | DECIMAL(12,2) | NOT NULL | Final amount |
| paidAmount | DECIMAL(12,2) | - | Amount paid so far |
| status | ENUM | NOT NULL | UNPAID, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED |
| cancelReason | VARCHAR(500) | - | Cancellation reason |
| cancelledAt | TIMESTAMP | - | Cancelled timestamp |
| cancelledBy | VARCHAR | - | Who cancelled |
| notes | VARCHAR(1000) | - | Invoice notes |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:**
- 1:N with InvoiceItem (cascade ALL, orphan removal, EAGER fetch)

**Notes:**
- Auto-generated invoiceNumber for tracking
- Supports partial payments (PARTIALLY_PAID status)
- Cancellation tracking with reason

### 2. InvoiceItem Entity

**Table:** `invoice_items`  
**Purpose:** Line items in an invoice

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Item identifier |
| invoice | FK | - | Invoice reference (ManyToOne LAZY) |
| description | VARCHAR | - | Item description |
| quantity | INT | - | Quantity |
| unitPrice | DECIMAL | - | Price per unit |
| amount | DECIMAL | - | Total (quantity × unitPrice) |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:**
- N:1 with Invoice (cascade via parent)

### 3. Payment Entity

**Table:** `payments`  
**Purpose:** Payment transaction records

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Payment identifier |
| invoice | FK | - | Invoice reference (ManyToOne LAZY) |
| paymentDate | TIMESTAMP | - | Payment timestamp |
| amount | DECIMAL | - | Payment amount |
| paymentMethod | ENUM | - | CASH, VNPAY, CREDIT_CARD |
| transactionId | VARCHAR | - | External transaction ID (VNPay) |
| status | ENUM | - | PENDING, SUCCESS, FAILED |
| notes | VARCHAR | - | Payment notes |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Indexes:** On invoiceId for payment history queries

**Notes:**
- VNPay integration for online payments
- Transaction ID for payment reconciliation
- Supports multiple payments per invoice (partial payments)

---

## HR Service Database (hr_db)

### 1. Employee Entity

**Table:** `employees`  
**Purpose:** Staff management (doctors, nurses, admin, etc.)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Employee identifier |
| accountId | VARCHAR | - | Auth service account reference |
| fullName | VARCHAR | NOT NULL | Full name |
| role | ENUM | NOT NULL | DOCTOR, NURSE, ADMIN, RECEPTIONIST |
| departmentId | VARCHAR | - | Department reference |
| specialization | VARCHAR | - | Medical specialization (for doctors) |
| licenseNumber | VARCHAR | - | Medical license number |
| phoneNumber | VARCHAR | - | Contact phone |
| address | VARCHAR | - | Address |
| status | ENUM | NOT NULL | ACTIVE, ON_LEAVE, INACTIVE |
| hiredAt | TIMESTAMP | - | Hire date |
| profileImageUrl | VARCHAR | - | Profile photo URL |
| deletedAt | TIMESTAMP | - | Soft delete timestamp |
| deletedBy | VARCHAR | - | Who deleted |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:** None (departmentId reference to Department)

**Soft Delete:** `@SoftDelete` annotation - employees are marked deleted, not removed

**Notes:**
- Links to auth-service for login credentials
- Specialization for doctors (Cardiology, Neurology, etc.)
- Profile images stored externally

### 2. Department Entity

**Table:** `departments`  
**Purpose:** Hospital departments/units

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Department identifier |
| name | VARCHAR | NOT NULL | Department name |
| description | TEXT | - | Department description |
| location | VARCHAR | - | Physical location |
| phoneNumber | VARCHAR | - | Department contact |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:** 1:N with Employee (via employee.departmentId)

### 3. EmployeeSchedule Entity

**Table:** `employee_schedules`  
**Purpose:** Work schedules for staff

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Schedule identifier |
| employeeId | VARCHAR | - | Employee reference |
| dayOfWeek | VARCHAR | - | MONDAY, TUESDAY, etc. |
| shiftStartTime | TIME | - | Shift start |
| shiftEndTime | TIME | - | Shift end |
| isAvailable | BOOLEAN | - | Available for appointments |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Notes:**
- Weekly recurring schedules
- Availability flag for appointment booking system

---

## Medicine Service Database (medicine_db)

### 1. Medicine Entity

**Table:** `medicines`  
**Purpose:** Medicine inventory and catalog

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Medicine identifier |
| name | VARCHAR | NOT NULL | Medicine name |
| activeIngredient | VARCHAR | NOT NULL | Active ingredient |
| unit | VARCHAR | NOT NULL | Unit (tablet, ml, mg, etc.) |
| description | VARCHAR | - | Description |
| quantity | LONG | NOT NULL | Stock quantity |
| concentration | VARCHAR | - | Concentration (500mg, 10%, etc.) |
| packaging | VARCHAR | - | Packaging type |
| purchasePrice | DECIMAL | NOT NULL | Cost price |
| sellingPrice | DECIMAL | NOT NULL | Selling price |
| manufacturer | VARCHAR | - | Manufacturer name |
| sideEffects | VARCHAR | - | Known side effects |
| storageConditions | VARCHAR | - | Storage requirements |
| expiresAt | TIMESTAMP | NOT NULL | Expiry date |
| category | FK | - | Category reference (ManyToOne LAZY) |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:**
- N:1 with Category

**Notes:**
- Inventory tracking with quantity
- Expiry date management
- Purchase/selling price for profit tracking

### 2. Category Entity

**Table:** `categories`  
**Purpose:** Medicine categorization

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK | Category identifier |
| name | VARCHAR | NOT NULL | Category name |
| description | TEXT | - | Category description |
| + audit fields | - | - | createdAt, updatedAt, createdBy, updatedBy |

**Relationships:** 1:N with Medicine

**Examples:** Antibiotics, Painkillers, Antihistamines, etc.

---

## Report Service Database (report_db)

**Note:** Likely uses **cached aggregates** rather than persistent entities.

Based on API contracts, report service generates:
- Revenue reports (daily, weekly, monthly, yearly)
- Appointment statistics
- Patient demographics

**Data Strategy:** Queries other services or uses materialized views/cache for performance.

---

## Entity Relationship Overview

### Cross-Service References (Denormalized)

```
auth-service.Account (id)
    ↓ (referenced by accountId)
├── patient-service.Patient
└── hr-service.Employee

hr-service.Employee (id, role=DOCTOR)
    ↓ (referenced by doctorId)
├── appointment-service.Appointment
├── medical-exam-service.MedicalExam
└── medical-exam-service.Prescription

patient-service.Patient (id)
    ↓ (referenced by patientId)
├── appointment-service.Appointment
├── medical-exam-service.MedicalExam
├── medical-exam-service.Prescription
├── billing-service.Invoice
└── medical-exam-service.LabOrder

appointment-service.Appointment (id)
    ↓ (referenced by appointmentId - 1:1)
├── medical-exam-service.MedicalExam
└── billing-service.Invoice

medical-exam-service.MedicalExam (id)
    ↓ (referenced by medicalExamId)
├── medical-exam-service.Prescription
├── medical-exam-service.LabOrder
└── billing-service.Invoice

medicine-service.Medicine (id)
    ↓ (referenced by medicineId)
└── medical-exam-service.PrescriptionItem
```

### Within-Service Relationships (JPA)

**Medical Exam Service:**
```
Prescription (1) → (N) PrescriptionItem
LabOrder (1) → (N) LabTestResult
```

**Billing Service:**
```
Invoice (1) → (N) InvoiceItem
Invoice (1) → (N) Payment
```

**Medicine Service:**
```
Category (1) → (N) Medicine
```

**HR Service:**
```
Department (1) → (N) Employee
```

---

## Database Schema Best Practices

### ✅ Implemented

1. **UUID Primary Keys** - Distributed system friendly, no collisions
2. **Audit Fields** - Full audit trail via JPA annotations
3. **Soft Delete** - Employee records marked deleted, not removed
4. **Denormalization** - Snapshot pattern for cross-service data
5. **Enum Types** - Type-safe status/role fields
6. **Decimal for Money** - DECIMAL(12,2) for financial accuracy
7. **Cascade Operations** - Parent-child relationships managed via JPA
8. **Lazy Loading** - @ManyToOne(LAZY) for performance
9. **Indexes** - On foreign keys and unique fields
10. **Constraints** - NOT NULL, UNIQUE where required

### Data Integrity Strategies

**Within Service:**
- Foreign key constraints (JPA relationships)
- Cascade operations (ALL, orphan removal)
- Validation annotations (@NotBlank, @NotNull)

**Cross-Service:**
- No foreign keys (different databases)
- Eventual consistency via saga pattern
- Denormalized snapshots for historical accuracy
- OpenFeign for data validation (e.g., check patient exists)

---

## Schema Evolution Strategy

**Migration Tool:** Likely Flyway or Liquibase (not confirmed in codebase)

**Versioning:** Each service manages own schema independently

**Backward Compatibility:**
- Additive changes (new columns with defaults)
- Non-breaking renames (add new, deprecate old)
- Data migrations via service layer

---

## Database Connections

**Configuration:** Application properties per service

**Connection Pooling:** HikariCP (Spring Boot default)

**Typical Configuration:**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/{service}_db
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

---

## Performance Considerations

### Denormalization Benefits
- ✅ No cross-service joins
- ✅ Fast query performance
- ✅ Service isolation

### Denormalization Trade-offs
- ⚠️ Data duplication
- ⚠️ Manual consistency management
- ⚠️ Snapshot updates not propagated

### Optimization Strategies
- Indexes on foreign key fields (patientId, doctorId, appointmentId)
- Eager fetch for small collections (InvoiceItems)
- Lazy fetch for large collections (Prescription items)
- Caching for read-heavy data (medicines, departments)

---

## Summary

**Total Entities:** 17  
**Total Databases:** 7 (auth_db, patient_db, appointment_db, exam_db, billing_db, hr_db, medicine_db)

**Entity Distribution:**
- Auth Service: 1 entity (Account)
- Patient Service: 1 entity (Patient)
- Appointment Service: 1 entity (Appointment)
- Medical Exam Service: 7 entities (MedicalExam, Prescription, PrescriptionItem, LabOrder, LabTest, LabTestResult, DiagnosticImage)
- Billing Service: 3 entities (Invoice, InvoiceItem, Payment)
- HR Service: 3 entities (Employee, Department, EmployeeSchedule)
- Medicine Service: 2 entities (Medicine, Category)

**Key Design Principles:**
1. Database per service for autonomy
2. UUID primary keys for distributed systems
3. Denormalized snapshots for cross-service data
4. Comprehensive auditing on all entities
5. Enum types for type safety
6. Decimal types for financial accuracy
7. Soft deletes where appropriate
8. JPA relationships within service boundaries

---

**Generated:** 2026-01-03  
**Source:** Entity class analysis from HMS_Backend/*/src/main/java/**/entities/
