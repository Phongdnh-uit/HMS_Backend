# Data Models - 3-Week MVP (Final Implementation)

**Project:** HMS Backend  
**Scope:** Complete data model specifications for 3-week MVP  
**Database:** MySQL 8.0 (Database-per-Service Pattern)  
**ORM:** Spring Data JPA with Hibernate  
**Date:** December 2, 2025

---

## 🎯 Overview

This document defines the **final database schemas** for all 8 microservices in the 3-week MVP.

**Services & Databases:**
- **auth-service** → `auth_db` (1 entity: Account)
- **patient-service** → `patient_db` (1 entity: Patient)
- **medicine-service** → `medicine_db` (2 entities: Category, Medicine)
- **hr-service** → `hr_db` (3 entities: Department, Employee, EmployeeSchedule)
- **appointment-service** → `appointment_db` (1 entity: Appointment)
- **medical-exam-service** → `medical_exam_db` (3 entities: MedicalExam, Prescription, PrescriptionItem)
- **billing-service** → `billing_db` (3 entities: Invoice, InvoiceItem, Payment)
- **reports-service** → `reports_db` (1 entity: ReportCache)

**Total:** 8 databases, 15 entities

---

## 📊 Database Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Service Discovery (Eureka)                   │
│                      Config Server (Spring Cloud Config)            │
│                      API Gateway (Spring Cloud Gateway)             │
└─────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────────┐
        │                           │                               │
┌───────▼────────┐        ┌─────────▼─────────┐         ┌──────────▼──────────┐
│  auth_db       │        │   patient_db      │         │    medicine_db      │
│  ----------    │        │   ------------    │         │    -------------    │
│  - accounts    │◄───FK──│   - patients      │         │    - categories     │
│                │        │     (accountId)   │         │    - medicines      │
└────────────────┘        └───────────────────┘         └─────────────────────┘
                                    
┌──────────────────┐      ┌─────────────────────┐      ┌─────────────────────┐
│    hr_db         │      │  appointment_db     │      │  medical_exam_db    │
│    ------        │      │  ---------------    │      │  ----------------   │
│  - departments   │      │  - appointments     │◄─FK──│  - medical_exams    │
│  - employees     │◄─FK──│    (doctorId)       │      │    (appointmentId)  │
│  - employee_     │      │    (patientId FK)   │      │  - prescriptions    │
│    schedules     │      │                     │      │  - prescription_    │
│                  │      │                     │      │    items            │
└──────────────────┘      └─────────────────────┘      │                     │
                                                       └───────────┬─────────┘
                                                                   │
                          ┌────────────────────┐        ┌─────────▼──────────┐
                          │   reports_db       │        │    billing_db      │
                          │   -----------      │        │    -----------     │
                          │  - report_cache    │        │    - invoices      │
                          │    (aggregations)  │◄───────┼──► - invoice_items │
                          │                    │        │    - payments      │
                          └────────────────────┘        └────────────────────┘
```

**Cross-Service Relationships (Soft FKs via IDs):**
- `patients.accountId` → `accounts.id` (auth-service)
- `employees.accountId` → `accounts.id` (auth-service)
- `employees.departmentId` → `departments.id` (hr-service)
- `appointments.patientId` → `patients.id` (patient-service)
- `appointments.doctorId` → `employees.id` (hr-service)
- `medical_exams.appointmentId` → `appointments.id` (appointment-service)
- `prescription_items.medicineId` → `medicines.id` (medicine-service)
- `invoices.patientId` → `patients.id` (patient-service)
- `invoices.appointmentId` → `appointments.id` (appointment-service)

---

## 1️⃣ Auth Service Database (`auth_db`)

### Entity 1.1: `Account`

**Table Name:** `accounts`  
**Purpose:** User authentication and authorization

**Entity Definition:**
```java
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid format")
    private String email;
    
    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    private String password; // BCrypt hashed (validation happens in service layer before hashing)
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleEnum role; // ADMIN, PATIENT, DOCTOR, NURSE, EMPLOYEE
    
    private String refreshToken; // JWT refresh token
    private Instant refreshTokenExpiresAt;
    private boolean emailVerified = false;
}

public enum RoleEnum {
    ADMIN, PATIENT, DOCTOR, NURSE, EMPLOYEE
}
```

**MySQL Schema:**
```sql
CREATE TABLE accounts (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    refresh_token TEXT,
    refresh_token_expires_at DATETIME(6),
    email_verified BOOLEAN DEFAULT FALSE,
    
    INDEX idx_accounts_email (email),
    INDEX idx_accounts_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key (auto-generated)
- `email`: Unique user email for login
- `password`: BCrypt hashed password (NEVER store plaintext)
- `role`: User role enum (ADMIN, PATIENT, DOCTOR, NURSE, EMPLOYEE)
- `refreshToken`: JWT refresh token for session management
- `refreshTokenExpiresAt`: Refresh token expiration timestamp
- `emailVerified`: Email verification status (default: false)

**Constraints & Business Rules:**
- `email`: Unique constraint, valid email format required
- `password`: BCrypt hashed (strength: 10 rounds minimum)
- `role`: Must be one of enum values (ADMIN, PATIENT, DOCTOR, NURSE, EMPLOYEE)
- `refreshToken`: Expires after 7 days (configurable)
- Email verification required before full account access

**Sample Data:**
```sql
INSERT INTO accounts (id, email, password, role, email_verified) VALUES
('550e8400-e29b-41d4-a716-446655440001', 'admin@hms.com', '$2a$10$...', 'ADMIN', TRUE),
('550e8400-e29b-41d4-a716-446655440002', 'patient1@gmail.com', '$2a$10$...', 'PATIENT', TRUE),
('550e8400-e29b-41d4-a716-446655440003', 'doctor1@hms.com', '$2a$10$...', 'DOCTOR', TRUE),
('550e8400-e29b-41d4-a716-446655440004', 'nurse1@hms.com', '$2a$10$...', 'NURSE', TRUE);
```

---

## 2️⃣ Patient Service Database (`patient_db`)

### Entity 2.1: `Patient`

**Table Name:** `patients`  
**Purpose:** Patient demographic and health profile

**Entity Definition:**
```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private Long accountId; // FK to accounts.id (soft FK)
    
    @Column(nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @Email(message = "Email must be valid format")
    private String email;
    
    @Past(message = "Date of birth must be in the past")
    private Instant dateOfBirth;
    
    private Gender gender; // Enum: MALE, FEMALE, OTHER
    
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;
    private String address;
    private String identificationNumber; // National ID
    private String healthInsuranceNumber;
    
    // Emergency Contact
    private String relativeFullName;
    private String relativePhoneNumber;
    private String relativeRelationship; // e.g., "Spouse", "Parent", "Sibling"
    
    // Health Information
    @Column(length = 5)
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood type must be A+, A-, B+, B-, AB+, AB-, O+, or O-")
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-
    
    @Column(columnDefinition = "TEXT")
    private String allergies; // Comma-separated or JSON string
    
    // Soft delete tracking
    private Instant deletedAt; // Nullable - tracks when patient was soft deleted
    private String deletedBy; // User ID who performed deletion
    
    // Audit fields
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}

public enum Gender {
    MALE, FEMALE, OTHER
}
```

**MySQL Schema:**
```sql
CREATE TABLE patients (
    id VARCHAR(36) PRIMARY KEY,
    account_id BIGINT,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    date_of_birth DATETIME(6),
    gender VARCHAR(20),
    phone_number VARCHAR(50),
    address TEXT,
    identification_number VARCHAR(50),
    health_insurance_number VARCHAR(50),
    
    -- Emergency Contact
    relative_full_name VARCHAR(255),
    relative_phone_number VARCHAR(50),
    relative_relationship VARCHAR(50),
    
    -- Health Information
    blood_type VARCHAR(5),
    allergies TEXT,
    
    -- Soft delete tracking
    deleted_at DATETIME(6),
    deleted_by VARCHAR(36),
    
    -- Audit
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_patients_account_id (account_id),
    INDEX idx_patients_full_name (full_name),
    INDEX idx_patients_phone (phone_number),
    INDEX idx_patients_email (email),
    INDEX idx_patients_blood_type (blood_type),
    INDEX idx_patients_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `accountId`: Soft FK to accounts.id (nullable - staff can register patients without accounts)
- `fullName`: Patient's full legal name
- `email`: Contact email
- `dateOfBirth`: Birth date for age calculation
- `gender`: MALE, FEMALE, OTHER
- `phoneNumber`: Primary contact number
- `address`: Full residential address
- `identificationNumber`: National ID/SSN
- `healthInsuranceNumber`: Insurance policy number
- `relativeFullName`: Emergency contact name
- `relativePhoneNumber`: Emergency contact phone
- `relativeRelationship`: Relationship (Spouse, Parent, Sibling, Child, Friend, Other)
- `bloodType`: A+, A-, B+, B-, AB+, AB-, O+, O-
- `allergies`: Known allergies (comma-separated or JSON)
- `deletedAt`: Soft delete timestamp (nullable)
- `deletedBy`: User ID who performed deletion (nullable)

**Constraints & Business Rules:**
- `accountId`: Can be NULL (walk-in patients without accounts)
- `bloodType`: Must be one of 8 standard types or NULL
- `allergies`: Stored as comma-separated string (e.g., "Penicillin, Peanuts")
- `relativeRelationship`: Recommended values: Spouse, Parent, Sibling, Child, Friend, Other
- `fullName`: Required field for all patients
- **Soft Delete Policy (Pattern Note):**
  - NEVER hard delete patients (preserves medical history and audit trail)
  - Set `deletedAt` timestamp and `deletedBy` user ID
  - Before deletion, check for future appointments (prevent orphan records)
  - All queries must filter `WHERE deleted_at IS NULL`
  - `deletedBy` is nullable (allows testing without authentication)


**Sample Data:**
```sql
INSERT INTO patients (id, account_id, full_name, email, date_of_birth, gender, phone_number, 
                      blood_type, allergies, relative_full_name, relative_phone_number, relative_relationship) VALUES
('p001', 2, 'Nguyen Van A', 'patient1@gmail.com', '1990-01-15', 'MALE', '0901234567', 
 'O+', 'Penicillin, Peanuts', 'Nguyen Thi B', '0907654321', 'Spouse'),
('p002', NULL, 'Tran Thi C', 'patient2@gmail.com', '1985-05-20', 'FEMALE', '0912345678',
 'A+', NULL, 'Tran Van D', '0908765432', 'Father');
```

---

## 3️⃣ Medicine Service Database (`medicine_db`)

### Entity 3.1: `Category`

**Table Name:** `categories`  
**Purpose:** Medicine classification (Antibiotics, Painkillers, etc.)

**Entity Definition:**
```java
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Category name is required")
    private String name;
    
    private String description;
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}
```

**MySQL Schema:**
```sql
CREATE TABLE categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `name`: Unique category name (e.g., "Antibiotics", "Painkillers")
- `description`: Detailed category description
- Standard audit fields (createdAt, updatedAt, createdBy, updatedBy)

**Constraints:**
- `name`: UNIQUE constraint (case-sensitive)

**Sample Data:**
```sql
INSERT INTO categories (id, name, description) VALUES
('cat001', 'Antibiotics', 'Medications that fight bacterial infections'),
('cat002', 'Painkillers', 'Pain relief medications'),
('cat003', 'Vitamins', 'Nutritional supplements'),
('cat004', 'Antihistamines', 'Allergy relief medications');
```

---

### Entity 3.2: `Medicine`

**Table Name:** `medicines`  
**Purpose:** Medicine master catalog with pricing and inventory

**Entity Definition:**
```java
@Entity
@Table(name = "medicines")
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Medicine name is required")
    private String name;
    
    @Column(nullable = false)
    @NotBlank(message = "Active ingredient is required")
    private String activeIngredient;
    
    @Column(nullable = false)
    @NotBlank(message = "Unit is required")
    private String unit; // e.g., "tablet", "ml", "capsule"
    
    private String description;
    
    @Column(nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Long quantity; // Stock quantity
    
    private String concentration; // e.g., "500mg"
    private String packaging; // e.g., "Box of 20 tablets"
    
    @Column(nullable = false)
    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Purchase price must be non-negative")
    private BigDecimal purchasePrice;
    
    @Column(nullable = false)
    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price must be non-negative")
    private BigDecimal sellingPrice;
    
    @Column(nullable = false)
    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    private Instant expiresAt;

    @Column(length = 255)
    private String manufacturer; // e.g., "Pfizer", "Sanofi"
    
    @Column(columnDefinition = "TEXT")
    private String sideEffects; // Common side effects
    
    @Column(length = 255)
    private String storageConditions; // e.g., "Store at room temperature (15-30°C)"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}
```

**MySQL Schema:**
```sql
CREATE TABLE medicines (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    active_ingredient VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    description TEXT,
    quantity BIGINT NOT NULL,
    concentration VARCHAR(50),
    packaging VARCHAR(100),
    purchase_price DECIMAL(12,2) NOT NULL,
    selling_price DECIMAL(12,2) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    category_id VARCHAR(36),
    manufacturer VARCHAR(255),
    side_effects TEXT,
    storage_conditions VARCHAR(255),
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_medicines_name (name),
    INDEX idx_medicines_category (category_id),
    INDEX idx_medicines_expires_at (expires_at),
    INDEX idx_medicines_manufacturer (manufacturer)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `name`: Medicine commercial name
- `activeIngredient`: Active pharmaceutical ingredient
- `unit`: Measurement unit (tablet, ml, capsule, etc.)
- `description`: Detailed medicine description
- `quantity`: Current stock quantity
- `concentration`: Dosage strength (e.g., "500mg", "250ml")
- `packaging`: Package details (e.g., "Box of 20 tablets")
- `purchasePrice`: Cost price from supplier
- `sellingPrice`: Retail price to patients
- `expiresAt`: Expiration date timestamp
- `categoryId`: FK to categories.id (nullable with ON DELETE SET NULL)
- `manufacturer`: Pharmaceutical company (e.g., "Pfizer", "GSK")
- `sideEffects`: Common adverse reactions
- `storageConditions`: Storage requirements (temperature, humidity)

**Constraints & Business Rules:**
- **Pricing Rule:** `sellingPrice` must be >= `purchasePrice` (validation in service layer before save/update)
  - Prevents selling medicine at a loss
  - Validation error message: "Selling price must be greater than or equal to purchase price"
- `expiresAt` > current date (validation on create/update)
- `quantity` decrements when used in prescriptions (stock management)
- Price snapshot stored in `prescription_items` (not live reference)
- **Pattern Note:** Price snapshot prevents historical data corruption when prices change

**Sample Data:**
```sql
INSERT INTO medicines (id, name, active_ingredient, unit, quantity, concentration, packaging, purchase_price, selling_price, expires_at, category_id) VALUES
('med001', 'Amoxicillin 500mg', 'Amoxicillin', 'capsule', 1000, '500mg', 'Box of 20 capsules',
 5000, 8000, '2026-12-31', 'cat001'),
('med002', 'Paracetamol 500mg', 'Paracetamol', 'tablet', 5000, '500mg', 'Box of 100 tablets',
 1000, 1500, '2027-06-30', 'cat002');
```

---

## 4️⃣ HR Service Database (`hr_db`)

### Entity 4.1: `Department`

**Table Name:** `departments`  
**Purpose:** Hospital departments/specializations organization

**Entity Definition:**
```java
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Department name is required")
    private String name; // e.g., "Cardiology", "Pediatrics", "Emergency"
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String headDoctorId; // FK to employees.id (optional)
    
    private String location; // e.g., "Building A - Floor 3"
    
    private String phoneExtension; // Internal phone number
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DepartmentStatus status; // ACTIVE, INACTIVE
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}

public enum DepartmentStatus {
    ACTIVE, INACTIVE
}
```

**MySQL Schema:**
```sql
CREATE TABLE departments (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    head_doctor_id VARCHAR(36),
    location VARCHAR(255),
    phone_extension VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_departments_name (name),
    INDEX idx_departments_status (status),
    INDEX idx_departments_head_doctor (head_doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `name`: Unique department name (e.g., "Cardiology", "Pediatrics")
- `description`: Detailed department description
- `headDoctorId`: Soft FK to employees.id (department head, nullable)
- `location`: Physical location (e.g., "Building A - Floor 3")
- `phoneExtension`: Internal phone number
- `status`: ACTIVE or INACTIVE

**Constraints & Business Rules:**
- `name`: UNIQUE constraint
- `headDoctorId`: Can be NULL (assigned later)
- Only ACTIVE departments can accept new appointments
- Departments can be deactivated but not deleted (historical data integrity)

**Sample Data:**
```sql
INSERT INTO departments (id, name, description, location, phone_extension, status) VALUES
('dept001', 'Cardiology', 'Heart and cardiovascular diseases', 'Building A - Floor 3', '301', 'ACTIVE'),
('dept002', 'Pediatrics', 'Child healthcare and treatment', 'Building B - Floor 2', '202', 'ACTIVE'),
('dept003', 'Emergency', '24/7 emergency medical services', 'Building A - Ground Floor', '911', 'ACTIVE'),
('dept004', 'Orthopedics', 'Bone and joint conditions', 'Building C - Floor 1', '401', 'ACTIVE');
```

---

### Entity 4.2: `Employee`

**Table Name:** `employees`  
**Purpose:** Staff information (doctors, nurses, receptionists, admin)

**Entity Definition:**
```java
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String accountId; // FK to accounts.id (soft FK)
    
    @Column(nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @Column(nullable = false)
    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    private EmployeeRole role; // DOCTOR, NURSE, RECEPTIONIST, ADMIN
    
    private String departmentId; // FK to departments.id (soft FK)
    
    private String specialization; // For doctors: additional specialization details
    
    @Pattern(regexp = "^[A-Z]{2}-[0-9]{5}$", message = "License number must be format XX-12345")
    private String licenseNumber; // Medical license
    
    @Email(message = "Email must be valid format")
    private String email;
    
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;
    private String address;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status; // ACTIVE, ON_LEAVE, RESIGNED
    
    private Instant hiredAt;
    
    // Soft delete tracking
    private Instant deletedAt; // Nullable - tracks when employee was soft deleted
    private String deletedBy; // User ID who performed deletion
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}

public enum EmployeeRole {
    DOCTOR, NURSE, RECEPTIONIST, ADMIN
}

public enum EmployeeStatus {
    ACTIVE, ON_LEAVE, RESIGNED
}
```

**MySQL Schema:**
```sql
CREATE TABLE employees (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36),
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department_id VARCHAR(36),
    specialization VARCHAR(100),
    license_number VARCHAR(50),
    email VARCHAR(255),
    phone_number VARCHAR(50),
    address TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    hired_at DATETIME(6),
    
    -- Soft delete tracking
    deleted_at DATETIME(6),
    deleted_by VARCHAR(36),
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_employees_account_id (account_id),
    INDEX idx_employees_role (role),
    INDEX idx_employees_department_id (department_id),
    INDEX idx_employees_status (status),
    INDEX idx_employees_specialization (specialization),
    INDEX idx_employees_deleted_at (deleted_at),
    UNIQUE KEY uk_employees_license (license_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `accountId`: Soft FK to accounts.id (nullable - staff without login access)
- `fullName`: Employee's full name
- `role`: DOCTOR, NURSE, RECEPTIONIST, ADMIN
- `departmentId`: Soft FK to departments.id (required for DOCTOR/NURSE)
- `specialization`: Additional detail beyond department (for doctors)
- `licenseNumber`: Medical license number (required for DOCTOR/NURSE, UNIQUE)
- `email`: Employee contact email
- `phoneNumber`: Employee phone
- `address`: Residential address
- `status`: ACTIVE, ON_LEAVE, RESIGNED
- `hiredAt`: Employment start date
- `deletedAt`: Soft delete timestamp (nullable)
- `deletedBy`: User ID who performed deletion (nullable)

**Constraints & Business Rules:**
- `departmentId`: Required for DOCTOR and NURSE roles
- `licenseNumber`: Required for DOCTOR and NURSE, must be unique
- `accountId`: Can be NULL (staff without login access)
- `status = RESIGNED`: Cannot be assigned to new appointments
- **Soft Delete Policy (Pattern Note):**
  - NEVER hard delete employees (preserves audit trail)
  - Set `deletedAt` timestamp and `deletedBy` user ID
  - Before deletion, check for future appointments (prevent orphan records)
  - All queries must filter `WHERE deleted_at IS NULL`
  - `deletedBy` is nullable (allows testing without authentication)

**Sample Data:**
```sql
INSERT INTO employees (id, account_id, full_name, role, department_id, specialization, license_number, email, phone_number, status, hired_at) VALUES
('emp001', '550e8400-e29b-41d4-a716-446655440003', 'Dr. Nguyen Van Hung', 'DOCTOR', 'dept001', 'Interventional Cardiology', 
 'MD-12345', 'dr.hung@hms.com', '0901111111', 'ACTIVE', '2020-01-15'),
('emp002', '550e8400-e29b-41d4-a716-446655440004', 'Nurse Tran Thi Mai', 'NURSE', 'dept001', NULL,
 'RN-67890', 'nurse.mai@hms.com', '0902222222', 'ACTIVE', '2021-03-20');
```

---

### Entity 4.3: `EmployeeSchedule`

**Table Name:** `employee_schedules`  
**Purpose:** Employee availability calendar for all roles (doctors, nurses, receptionists, admin)

**Entity Definition:**
```java
@Entity
@Table(name = "employee_schedules")
public class EmployeeSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Employee ID is required")
    private String employeeId; // FK to employees.id (all roles)
    
    @Column(nullable = false)
    @NotNull(message = "Work date is required")
    @FutureOrPresent(message = "Work date cannot be in the past")
    private LocalDate workDate; // e.g., 2025-12-05
    
    @Column(nullable = false)
    @NotNull(message = "Start time is required")
    private LocalTime startTime; // e.g., 08:00
    
    @Column(nullable = false)
    @NotNull(message = "End time is required")
    private LocalTime endTime; // e.g., 17:00
    
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private ScheduleStatus status; // AVAILABLE, BOOKED, CANCELLED
    
    private String notes; // e.g., "Conference 2PM-4PM"
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}

public enum ScheduleStatus {
    AVAILABLE, BOOKED, CANCELLED
}
```

**MySQL Schema:**
```sql
CREATE TABLE employee_schedules (
    id VARCHAR(36) PRIMARY KEY,
    employee_id VARCHAR(36) NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    notes TEXT,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_schedules_employee_id (employee_id),
    INDEX idx_schedules_work_date (work_date),
    INDEX idx_schedules_status (status),
    UNIQUE KEY uk_schedules_employee_date (employee_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `employeeId`: Soft FK to employees.id (all roles: DOCTOR, NURSE, RECEPTIONIST, ADMIN)
- `workDate`: Schedule date (DATE type)
- `startTime`: Work start time (TIME type)
- `endTime`: Work end time (TIME type)
- `status`: AVAILABLE, BOOKED, CANCELLED
- `notes`: Optional notes (e.g., "Conference 2PM-4PM")

**Constraints & Business Rules:**
- UNIQUE constraint on (employee_id, work_date) - one schedule per employee per day
- `startTime` < `endTime` (validation in service layer)
- Cannot create schedules for past dates
- `status = BOOKED` when all time slots filled (updated by appointment-service)
- **Pattern Note:** Generic scheduling for all employee types
  - DOCTOR: Patient appointments (consultation, follow-up)
  - NURSE: Shift schedules, patient care assignments
  - RECEPTIONIST: Front desk coverage shifts
  - ADMIN: Office hours, administrative tasks
  - Role-specific filtering in business logic (only DOCTOR schedules in appointment booking)

**Sample Data:**
```sql
INSERT INTO employee_schedules (id, employee_id, work_date, start_time, end_time, status) VALUES
('sch001', 'emp001', '2025-12-05', '08:00', '12:00', 'AVAILABLE'),  -- Doctor
('sch002', 'emp001', '2025-12-05', '13:00', '17:00', 'AVAILABLE'),  -- Doctor
('sch003', 'emp001', '2025-12-06', '08:00', '17:00', 'BOOKED'),     -- Doctor
('sch004', 'emp002', '2025-12-05', '07:00', '15:00', 'AVAILABLE'),  -- Nurse
('sch005', 'emp003', '2025-12-05', '08:00', '17:00', 'AVAILABLE');  -- Receptionist
```

---

## 5️⃣ Appointment Service Database (`appointment_db`)

### Entity 5.1: `Appointment`

**Table Name:** `appointments`  
**Purpose:** Patient appointment bookings

**Entity Definition:**
```java
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Patient ID is required")
    private String patientId; // FK to patients.id (patient-service)
    
    @Column(nullable = false)
    @NotBlank(message = "Doctor ID is required")
    private String doctorId; // FK to employees.id (hr-service)
    
    @Column(nullable = false)
    @NotNull(message = "Appointment time is required")
    @Future(message = "Appointment time must be in the future")
    private LocalDateTime appointmentTime; // e.g., 2025-12-05 09:00
    
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status; // SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    
    @Enumerated(EnumType.STRING)
    private AppointmentType type; // CONSULTATION, FOLLOW_UP, EMERGENCY
    
    private String reason; // Chief complaint
    private String notes; // Admin notes
    
    private Instant cancelledAt;
    private String cancelReason;
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}

public enum AppointmentStatus {
    SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
}

public enum AppointmentType {
    CONSULTATION, FOLLOW_UP, EMERGENCY
}
```

**MySQL Schema:**
```sql
CREATE TABLE appointments (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    doctor_id VARCHAR(36) NOT NULL,
    appointment_time DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    type VARCHAR(50),
    reason TEXT,
    notes TEXT,
    cancelled_at DATETIME(6),
    cancel_reason TEXT,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_appointments_patient_id (patient_id),
    INDEX idx_appointments_doctor_id (doctor_id),
    INDEX idx_appointments_time (appointment_time),
    INDEX idx_appointments_status (status),
    INDEX idx_appointments_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `patientId`: Soft FK to patients.id (patient-service)
- `doctorId`: Soft FK to employees.id where role=DOCTOR (hr-service)
- `appointmentTime`: Scheduled datetime
- `status`: SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
- `type`: CONSULTATION, FOLLOW_UP, EMERGENCY
- `reason`: Chief complaint
- `notes`: Administrative notes
- `cancelledAt`: Cancellation timestamp
- `cancelReason`: Reason for cancellation

**Constraints & Business Rules:**
- Cannot book appointments in the past
- Cannot double-book doctors (validate no overlapping appointments)
- Doctor must have available schedule for appointment date
- **Appointment duration: 30 minutes (configurable)**
  - **Implementation:** Service layer constant, NOT a database field
  - **Purpose:** Calculate time slots, prevent double-booking, validate overlaps
  - **Configuration:** Externalized to `application.yml`:
    ```yaml
    appointment:
      duration-minutes: 30
      buffer-minutes: 0  # Optional gap between appointments
    ```
  - **Usage Example:**
    ```java
    LocalDateTime appointmentEnd = appointmentTime.plusMinutes(30);
    // Check for overlapping appointments within this time window
    ```
  - **Design Decision:** Fixed duration for MVP simplicity; can add `durationMinutes` field to Appointment entity later if different appointment types need different durations (e.g., EMERGENCY = 60 min)
- `status = COMPLETED` required before creating medical exam
- **Immutability Rules (Audit Integrity):**
  - COMPLETED appointments: Cannot be modified (medical exam already created)
  - CANCELLED appointments: Cannot be modified (cancellation is final decision)
  - NO_SHOW appointments: Cannot be modified (attendance recorded, penalty/rescheduling handled separately)
  - Only SCHEDULED appointments can be updated or cancelled
- **Pattern Note:** Cross-service validation with hr-service for doctor availability

**Sample Data:**
```sql
INSERT INTO appointments (id, patient_id, doctor_id, appointment_time, status, type, reason) VALUES
('apt001', 'p001', 'emp001', '2025-12-05 09:00', 'SCHEDULED', 'CONSULTATION', 'Chest pain'),
('apt002', 'p002', 'emp001', '2025-12-05 10:00', 'COMPLETED', 'FOLLOW_UP', 'Blood pressure check'),
('apt003', 'p001', 'emp001', '2025-12-06 14:00', 'CANCELLED', 'CONSULTATION', 'Headache');
```

---

## 6️⃣ Medical Exam Service Database (`medical_exam_db`)

### Entity 6.1: `MedicalExam`

**Table Name:** `medical_exams`  
**Purpose:** Exam records after appointment completion  

**Entity Definition:**
```java
@Entity
@Table(name = "medical_exams")
public class MedicalExam {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Appointment ID is required")
    private String appointmentId; // FK to appointments.id (one-to-one)
    
    private String patientId; // Denormalized for query performance
    private String doctorId; // Denormalized for query performance
    
    @Column(columnDefinition = "TEXT")
    private String diagnosis; // Primary diagnosis
    
    @Column(columnDefinition = "TEXT")
    private String symptoms; // Patient symptoms
    
    @Column(columnDefinition = "TEXT")
    private String treatment; // Treatment plan
    
    // Vitals
    private Double temperature; // Celsius
    private Integer bloodPressureSystolic; // mmHg
    private Integer bloodPressureDiastolic; // mmHg
    private Integer heartRate; // bpm
    private Double weight; // kg
    private Double height; // cm
    
    @Column(columnDefinition = "TEXT")
    private String notes; // Doctor's notes
    
    private Instant examDate;
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}
```

**MySQL Schema:**
```sql
CREATE TABLE medical_exams (
    id VARCHAR(36) PRIMARY KEY,
    appointment_id VARCHAR(36) NOT NULL UNIQUE,
    patient_id VARCHAR(36),
    doctor_id VARCHAR(36),
    diagnosis TEXT,
    symptoms TEXT,
    treatment TEXT,
    
    -- Vitals
    temperature DECIMAL(4,1),
    blood_pressure_systolic INT,
    blood_pressure_diastolic INT,
    heart_rate INT,
    weight DECIMAL(5,2),
    height DECIMAL(5,2),
    
    notes TEXT,
    exam_date DATETIME(6),
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_exams_appointment_id (appointment_id),
    INDEX idx_exams_patient_id (patient_id),
    INDEX idx_exams_doctor_id (doctor_id),
    INDEX idx_exams_exam_date (exam_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `appointmentId`: Soft FK to appointments.id (UNIQUE - one exam per appointment)
- `patientId`: Denormalized from appointment (query performance)
- `doctorId`: Denormalized from appointment (query performance)
- `diagnosis`: Primary diagnosis/medical condition
- `symptoms`: Patient-reported symptoms
- `treatment`: Prescribed treatment plan
- `temperature`: Body temperature in Celsius
- `bloodPressureSystolic`: Systolic BP in mmHg
- `bloodPressureDiastolic`: Diastolic BP in mmHg
- `heartRate`: Heart rate in bpm
- `weight`: Patient weight in kg
- `height`: Patient height in cm
- `notes`: Doctor's additional notes
- `examDate`: Exam completion timestamp

**Constraints & Business Rules:**
- UNIQUE constraint on `appointmentId` (one exam per appointment)
- Can only create exam for `status = COMPLETED` appointments
- Vitals (temperature, BP, heart rate, etc.) are optional but recommended
- `examDate` defaults to appointment time
- **Pattern Note:** Denormalization of patientId/doctorId improves query performance

**Sample Data:
```sql
INSERT INTO medical_exams (id, appointment_id, patient_id, doctor_id, diagnosis, symptoms, temperature, blood_pressure_systolic, blood_pressure_diastolic, heart_rate, exam_date) VALUES
('exam001', 'apt002', 'p002', 'emp001', 'Hypertension Stage 1', 'Headache, dizziness',
 36.8, 145, 95, 78, '2025-12-05 10:30');
```

---

### Entity 6.2: `Prescription`

**Table Name:** `prescriptions`  
**Purpose:** Medicine prescriptions linked to exams  

**Entity Definition:**
```java
@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Medical exam ID is required")
    private String medicalExamId; // FK to medical_exams.id
    
    private String patientId; // Denormalized
    private String doctorId; // Denormalized
    
    @Column(nullable = false)
    @NotNull(message = "Prescription date is required")
    private Instant prescribedAt; // Defaults to Instant.now() in service layer
    
    @Column(columnDefinition = "TEXT")
    private String notes; // Usage instructions
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private List<PrescriptionItem> items;
}
```

**MySQL Schema:**
```sql
CREATE TABLE prescriptions (
    id VARCHAR(36) PRIMARY KEY,
    medical_exam_id VARCHAR(36) NOT NULL,
    patient_id VARCHAR(36),
    doctor_id VARCHAR(36),
    prescribed_at DATETIME(6) NOT NULL,
    notes TEXT,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_prescriptions_exam_id (medical_exam_id),
    INDEX idx_prescriptions_patient_id (patient_id),
    INDEX idx_prescriptions_prescribed_at (prescribed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `medicalExamId`: Soft FK to medical_exams.id
- `patientId`: Denormalized (query performance)
- `doctorId`: Denormalized (query performance)
- `prescribedAt`: Prescription timestamp (defaults to current timestamp when created)
- `notes`: General usage instructions

**Constraints & Business Rules:**
- One prescription per medical exam (business logic validation)
- Contains multiple prescription items (one-to-many relationship)
- Cannot modify after 24 hours (immutable for audit)

---

### Entity 6.3: `PrescriptionItem`

**Table Name:** `prescription_items`  
**Purpose:** Individual medicines in a prescription (with price snapshot)  

**Entity Definition:**
```java
@Entity
@Table(name = "prescription_items")
public class PrescriptionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    @NotNull(message = "Prescription is required")
    private Prescription prescription;
    
    @Column(nullable = false)
    @NotBlank(message = "Medicine ID is required")
    private String medicineId; // FK to medicines.id (soft FK)
    
    // Medicine snapshot (price at prescription time)
    @Column(nullable = false)
    @NotBlank(message = "Medicine name is required")
    private String medicineName;
    
    @Column(nullable = false)
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice; // Selling price at prescription time
    
    @Column(nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @Column(nullable = false)
    @NotBlank(message = "Dosage is required")
    private String dosage; // e.g., "1 tablet twice daily"
    
    private Integer durationDays; // e.g., 7 days
    
    @Column(columnDefinition = "TEXT")
    private String instructions; // Special instructions
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
```

**MySQL Schema:**
```sql
CREATE TABLE prescription_items (
    id VARCHAR(36) PRIMARY KEY,
    prescription_id VARCHAR(36) NOT NULL,
    medicine_id VARCHAR(36) NOT NULL,
    
    -- Snapshot at prescription time
    medicine_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    
    quantity INT NOT NULL,
    dosage VARCHAR(255) NOT NULL,
    duration_days INT,
    instructions TEXT,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE,
    INDEX idx_prescription_items_prescription_id (prescription_id),
    INDEX idx_prescription_items_medicine_id (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `prescriptionId`: FK to prescriptions.id (CASCADE delete)
- `medicineId`: Soft FK to medicines.id (reference only)
- `medicineName`: Medicine name snapshot (prevents data loss if medicine deleted)
- `unitPrice`: Selling price snapshot at prescription time (prevents price change impact)
- `quantity`: Number of units prescribed
- `dosage`: Dosage instructions (e.g., "1 tablet twice daily")
- `durationDays`: Treatment duration (e.g., 7 days)
- `instructions`: Special instructions (e.g., "Take with food")

**Constraints & Business Rules:**
- `unitPrice` snapshot prevents price changes from affecting historical prescriptions
- `unitPrice` must be greater than 0 (prevents zero-price prescriptions)
- `quantity` triggers stock decrement in medicine-service (via event/API call)
  - **Stock Validation (CRITICAL):** Before decrement, verify `medicine.quantity >= prescription_item.quantity`
  - If insufficient stock, reject prescription creation with error: "Insufficient stock for {medicineName}. Available: {currentStock}, Required: {requestedQuantity}"
  - Stock decrement is atomic operation (prevent race conditions in concurrent prescriptions)
- Total prescription cost = SUM(unitPrice × quantity) across all items
- Cannot modify after 24 hours (immutable for audit)
- **Pattern Note:** Price snapshot pattern ensures billing accuracy regardless of future price changes

**Sample Data:
```sql
INSERT INTO prescriptions (id, medical_exam_id, patient_id, doctor_id, prescribed_at) VALUES
('rx001', 'exam001', 'p002', 'emp001', '2025-12-05 10:30');

INSERT INTO prescription_items (id, prescription_id, medicine_id, medicine_name, unit_price, 
                                 quantity, dosage, duration_days, instructions) VALUES
('rxi001', 'rx001', 'med001', 'Amoxicillin 500mg', 8000, 20, '1 capsule twice daily', 10, 
 'Take with food'),
('rxi002', 'rx001', 'med002', 'Paracetamol 500mg', 1500, 10, '1 tablet as needed', 5,
 'Maximum 4 tablets per day');
```

---

## 7️⃣ Billing Service Database (`billing_db`)

### Entity 7.1: `Invoice`

**Table Name:** `invoices`  
**Purpose:** Patient invoices (consultation + medicine costs)  

**Entity Definition:**
```java
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Patient ID is required")
    private String patientId; // FK to patients.id
    
    private String appointmentId; // FK to appointments.id (optional)
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Invoice number is required")
    @Pattern(regexp = "^INV-\\d{8}-\\d{4}$", message = "Invoice number must be format INV-YYYYMMDD-XXXX")
    private String invoiceNumber; // Auto-generated: INV-20251205-0001
    
    @Column(nullable = false)
    @NotNull(message = "Invoice date is required")
    private Instant invoiceDate;
    
    @FutureOrPresent(message = "Due date must be in the future or present")
    private Instant dueDate; // Payment deadline (nullable for immediate payment, or 7+ days for installments/insurance)
    
    @Column(nullable = false)
    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Subtotal must be greater than 0")
    private BigDecimal subtotal; // Before discounts/tax
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount must be non-negative")
    private BigDecimal discount; // Discount amount (can be 0)
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Tax must be non-negative")
    private BigDecimal tax; // Tax amount (VAT 10%, can be 0)
    
    @Column(nullable = false)
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
    private BigDecimal totalAmount; // Final amount = subtotal - discount + tax
    
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status; // UNPAID, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<InvoiceItem> items;
}

public enum InvoiceStatus {
    UNPAID, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
}
```

**MySQL Schema:**
```sql
CREATE TABLE invoices (
    id VARCHAR(36) PRIMARY KEY,
    patient_id VARCHAR(36) NOT NULL,
    appointment_id VARCHAR(36),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_date DATETIME(6) NOT NULL,
    due_date DATETIME(6),
    
    subtotal DECIMAL(15,2) NOT NULL,
    discount DECIMAL(15,2) DEFAULT 0,
    tax DECIMAL(15,2) DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL,
    
    status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    notes TEXT,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_invoices_patient_id (patient_id),
    INDEX idx_invoices_appointment_id (appointment_id),
    INDEX idx_invoices_number (invoice_number),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_invoice_date (invoice_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `patientId`: Soft FK to patients.id
- `appointmentId`: Soft FK to appointments.id (nullable)
- `invoiceNumber`: Auto-generated unique invoice number (format: INV-YYYYMMDD-XXXX)
- `invoiceDate`: Invoice creation timestamp
- `dueDate`: Payment deadline (nullable for immediate payment, or invoiceDate + 7-30 days for installments/insurance)
- `subtotal`: Total before discounts/tax (must be > 0)
- `discount`: Discount amount (nullable, can be 0)
- `tax`: Tax amount (e.g., VAT 10%, can be 0)
- `totalAmount`: Final amount (subtotal - discount + tax, must be > 0)
- `status`: UNPAID, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
- `notes`: Additional notes

**Constraints & Business Rules:**
- `invoiceNumber`: UNIQUE constraint, auto-generated format INV-YYYYMMDD-XXXX
- **Payment Models:**
  - **Immediate payment**: `dueDate = null` or `dueDate = invoiceDate` (same-day payment required)
  - **Payment plan**: `dueDate = invoiceDate + 7-30 days` (installment payments for expensive prescriptions)
  - **Insurance billing**: `dueDate = invoiceDate + 30-60 days` (insurer payment deadline)
- **Validation Rules:**
  - `subtotal` and `totalAmount` must be **strictly greater than 0** (prevents zero-amount invoices)
  - `discount` and `tax` can be **0 or positive** (optional discounts/taxes)
- `totalAmount = subtotal - discount + tax` (calculated field)
- Status auto-updates based on payment records:
  - UNPAID: total payments = 0
  - PARTIALLY_PAID: 0 < total payments < totalAmount
  - PAID: total payments >= totalAmount
  - OVERDUE: dueDate < current date AND status != PAID
- Auto-generate invoice when appointment status = COMPLETED

**Sample Data:
```sql
INSERT INTO invoices (id, patient_id, appointment_id, invoice_number, invoice_date, due_date,
                      subtotal, discount, tax, total_amount, status) VALUES
('inv001', 'p002', 'apt002', 'INV-20251205-0001', '2025-12-05', '2025-12-12',
 500000, 0, 50000, 550000, 'PAID');
```

---

### Entity 7.2: `InvoiceItem`

**Table Name:** `invoice_items`  
**Purpose:** Line items in invoice (consultation fee, medicines, tests)  

**Entity Definition:**
```java
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    @NotNull(message = "Invoice is required")
    private Invoice invoice;
    
    @Column(nullable = false)
    @NotNull(message = "Item type is required")
    @Enumerated(EnumType.STRING)
    private ItemType type; // CONSULTATION, MEDICINE, TEST, OTHER
    
    @Column(nullable = false)
    @NotBlank(message = "Description is required")
    private String description; // e.g., "Consultation - Cardiology", "Amoxicillin 500mg"
    
    private String referenceId; // prescription_item.id or other reference
    
    @Column(nullable = false)
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @Column(nullable = false)
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;
    
    @Column(nullable = false)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount; // quantity * unitPrice
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}

public enum ItemType {
    CONSULTATION, MEDICINE, TEST, OTHER
}
```

**MySQL Schema:**
```sql
CREATE TABLE invoice_items (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    reference_id VARCHAR(36),
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    INDEX idx_invoice_items_invoice_id (invoice_id),
    INDEX idx_invoice_items_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `invoiceId`: FK to invoices.id (CASCADE delete)
- `type`: CONSULTATION, MEDICINE, TEST, OTHER
- `description`: Line item description (e.g., "Cardiology Consultation", "Amoxicillin 500mg")
- `referenceId`: Optional reference to prescription_item.id or other source
- `quantity`: Number of units
- `unitPrice`: Price per unit
- `amount`: Total line item amount (quantity × unitPrice)

**Constraints & Business Rules:**
- `amount = quantity × unitPrice` (calculated field)
- Invoice subtotal = SUM(amount) across all items
- **Consultation Fee Configuration:**
  - Consultation fees vary by department/specialization (e.g., Cardiology: 200,000 VND, Pediatrics: 150,000 VND)
  - **Implementation Options:**
    1. **Config File Approach:** Store in `application.yml` or external config service
       ```yaml
       consultation-fees:
         cardiology: 200000
         pediatrics: 150000
         emergency: 300000
       ```
    2. **Database Approach:** Add `consultationFee` field to Department entity (hr-service)
       - Query hr-service API to get fee when generating invoice
       - Allows dynamic updates without redeployment
  - **MVP Recommendation:** Config file (simpler), migrate to database if fees change frequently
  - Fee is snapshot at invoice creation time (immune to future price changes)
- Medicine prices copied from `prescription_items.unitPrice` (snapshot)

**Sample Data:
```sql
INSERT INTO invoice_items (id, invoice_id, type, description, quantity, unit_price, amount) VALUES
('ini001', 'inv001', 'CONSULTATION', 'Cardiology Consultation', 1, 200000, 200000),
('ini002', 'inv001', 'MEDICINE', 'Amoxicillin 500mg', 20, 8000, 160000),
('ini003', 'inv001', 'MEDICINE', 'Paracetamol 500mg', 10, 1500, 15000);
```

---

### Entity 7.3: `Payment`

**Table Name:** `payments`  
**Purpose:** Payment transactions for invoices  

**Entity Definition:**
```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotBlank(message = "Invoice ID is required")
    private String invoiceId; // FK to invoices.id
    
    @Column(unique = true)
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", message = "Idempotency key must be valid UUID")
    private String idempotencyKey; // UUID - prevents duplicate payments (CRITICAL)
    
    @Column(nullable = false)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Column(nullable = false)
    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    private PaymentMethod method; // CASH, CREDIT_CARD, BANK_TRANSFER, INSURANCE
    
    @Column(nullable = false)
    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, COMPLETED, FAILED, REFUNDED
    
    private String transactionId; // External payment gateway transaction ID
    
    @Column(nullable = false)
    @NotNull(message = "Payment date is required")
    private Instant paymentDate;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @CreatedBy private String createdBy;
    @LastModifiedBy private String updatedBy;
}

public enum PaymentMethod {
    CASH, CREDIT_CARD, BANK_TRANSFER, INSURANCE
}

public enum PaymentStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}
```

**MySQL Schema:**
```sql
CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    invoice_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(36) UNIQUE, -- Prevents duplicate payments
    amount DECIMAL(15,2) NOT NULL,
    method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    payment_date DATETIME(6) NOT NULL,
    notes TEXT,
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    
    INDEX idx_payments_invoice_id (invoice_id),
    INDEX idx_payments_method (method),
    INDEX idx_payments_status (status),
    INDEX idx_payments_payment_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `invoiceId`: Soft FK to invoices.id
- `idempotencyKey`: Client-generated UUID (UNIQUE - prevents duplicate payments)
- `amount`: Payment amount
- `method`: CASH, CREDIT_CARD, BANK_TRANSFER, INSURANCE
- `status`: PENDING, COMPLETED, FAILED, REFUNDED
- `transactionId`: External payment gateway transaction ID (nullable)
- `paymentDate`: Payment timestamp
- `notes`: Additional payment notes

**Constraints & Business Rules:**
- **Idempotency Pattern (CRITICAL):**
  - **Client-Side UUID Generation:**
    - **Frontend (JavaScript):** `crypto.randomUUID()` or `uuidv4()` library
    - **Mobile (Java/Kotlin):** `UUID.randomUUID().toString()`
    - **Mobile (Swift):** `UUID().uuidString`
  - **Idempotency Key Lifecycle:**
    1. Client generates NEW UUID **per payment attempt** (not per user, not per invoice)
    2. Store UUID in client state/memory before API call
    3. Send UUID in request body: `{"idempotencyKey": "550e8400-e29b-41d4-a716-446655440000", "invoiceId": "inv001", "amount": 550000}`
    4. **On Network Failure/Timeout:** Retry with SAME UUID (prevents duplicate charge)
    5. **On Success:** Discard UUID, generate new one for next payment
  - **Server-Side Validation:**
    - Check if `idempotencyKey` exists in database before processing
    - If exists: Return existing payment record (HTTP 200 + existing payment data)
    - If not exists: Process payment, save with idempotency key
  - **Example Scenario (Prevents Double Charge):**
    ```
    User clicks "Pay" → Client generates UUID "abc-123"
    Request sent → Network timeout (no response received)
    User clicks "Pay" again → Client retries with SAME UUID "abc-123"
    Server checks: "abc-123" exists → Returns original payment (no duplicate charge)
    ```
  - UNIQUE constraint enforced at database level (prevents race conditions)
- Total payments for invoice = SUM(amount WHERE status = COMPLETED)
- Invoice status auto-updates based on total payments:
  - UNPAID: total = 0
  - PARTIALLY_PAID: 0 < total < invoice.totalAmount
  - PAID: total >= invoice.totalAmount
- Partial payments allowed (installments)
- Refunds create negative payment records (status = REFUNDED)
  - **PENDING**: Payment initiated, awaiting confirmation (e.g., bank transfer pending, credit card processing)
  - **COMPLETED**: Payment successfully processed and confirmed
  - **FAILED**: Payment rejected (insufficient funds, card declined, network error)
  - **REFUNDED**: Payment reversed and returned to patient
    - **Refund Scenarios:**
      1. **Appointment Cancellation (Post-Payment)**: Patient paid in advance, then cancels appointment, refund issued
      2. **Billing Error**: Overcharged amount corrected (e.g., wrong consultation fee applied, incorrect medicine price)
      3. **Insurance Adjustment**: Insurance covers more than expected after claim processed, refund excess payment to patient
      4. **Duplicate Payment**: Idempotency failure workaround (manual refund if duplicate charge occurred despite safeguards)
      5. **Medical Dispute**: Service quality dispute resolved in patient's favor (e.g., incorrect treatment, medication error)
    - **Refund Implementation**: Create new payment record with negative amount and status = REFUNDED
      - Example: Original payment +550,000 VND → Refund payment -550,000 VND
      - Invoice status recalculates: total payments = original - refund
- **Pattern Note:** Idempotency key prevents financial data corruption from duplicate requests

**Sample Data:
```sql
INSERT INTO payments (id, invoice_id, idempotency_key, amount, method, status, payment_date) VALUES
('pay001', 'inv001', '550e8400-e29b-41d4-a716-446655440000', 550000, 'CASH', 'COMPLETED', '2025-12-05 11:00');
```

---

## 8️⃣ Reports Service Database (`reports_db`)

### Entity 8.1: `ReportCache`

**Table Name:** `report_cache`  
**Purpose:** Cache aggregated reports for performance  

**Entity Definition:**
```java
@Entity
@Table(name = "report_cache")
public class ReportCache {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    @NotNull(message = "Report type is required")
    @Enumerated(EnumType.STRING)
    private ReportType reportType; // REVENUE, APPOINTMENTS, DOCTOR_PERFORMANCE, PATIENT_ACTIVITY
    
    @Column(nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @Column(nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    @Column(columnDefinition = "TEXT")
    private String filters; // JSON: {"doctorId": "emp001", "status": "COMPLETED"}
    
    @Column(columnDefinition = "LONGTEXT")
    @NotBlank(message = "Report data is required")
    private String data; // JSON cached result
    
    @Column(nullable = false)
    @NotNull(message = "Generated timestamp is required")
    private Instant generatedAt;
    
    private Instant expiresAt; // TTL for cache invalidation
    
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}

public enum ReportType {
    REVENUE, APPOINTMENTS, DOCTOR_PERFORMANCE, PATIENT_ACTIVITY
}
```

**MySQL Schema:**
```sql
CREATE TABLE report_cache (
    id VARCHAR(36) PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    filters TEXT,
    data LONGTEXT NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6),
    
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    INDEX idx_report_cache_type_dates (report_type, start_date, end_date),
    INDEX idx_report_cache_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Field Descriptions:**
- `id`: UUID primary key
- `reportType`: REVENUE, APPOINTMENTS, DOCTOR_PERFORMANCE, PATIENT_ACTIVITY
- `startDate`: Report period start date
- `endDate`: Report period end date
- `filters`: JSON filters (e.g., {"doctorId": "emp001", "status": "COMPLETED"})
- `data`: JSON cached report result (LONGTEXT)
- `generatedAt`: Cache generation timestamp
- `expiresAt`: Cache expiration timestamp (TTL)

**Constraints & Business Rules:**
- Composite index on (report_type, start_date, end_date) for fast lookups
- Cache TTL: 12 hours (configurable)
- Reports regenerate on-demand if cache expired (`expiresAt < current timestamp`)
- `data` stored as JSON for flexibility
- `filters` allow parameterized caching (e.g., per doctor, per department)
- **Pattern Note:** Caching pattern reduces database load for expensive aggregation queries

**Sample Data:
```sql
INSERT INTO report_cache (id, report_type, start_date, end_date, filters, data, generated_at, expires_at) VALUES
('rc001', 'REVENUE', '2025-12-01', '2025-12-31', '{}', 
 '{"totalRevenue": 50000000, "paidInvoices": 45, "pendingInvoices": 5}',
 '2025-12-05 08:00', '2025-12-05 20:00');
```

---

## 🔄 Cross-Service Data Flow

### 1. Patient Registration → Appointment → Exam → Invoice Workflow

```
1. Auth Service: Create Account
   POST /api/auth/register
   → accounts.id = "acc123"

2. Patient Service: Create Patient Profile
   POST /api/patients
   → patients.id = "p001", patients.accountId = "acc123"

3. HR Service: Create Department
   POST /api/departments
   → departments.id = "dept001", departments.name = "Cardiology"

4. HR Service: Create Employee (Doctor)
   POST /api/employees
   → employees.id = "emp001", employees.role = "DOCTOR", employees.departmentId = "dept001"

5. HR Service: Create Employee Schedule
   POST /api/schedules
   → employee_schedules.employeeId = "emp001", status = "AVAILABLE"

6. Appointment Service: Book Appointment
   POST /api/appointments
   → appointments.patientId = "p001", appointments.doctorId = "emp001"
   → Call HR Service: Update schedule status = "BOOKED"

7. Appointment Service: Complete Appointment
   PATCH /api/appointments/{id}/complete
   → appointments.status = "COMPLETED"

8. Medical Exam Service: Create Exam
   POST /api/medical-exams
   → medical_exams.appointmentId = "apt001"

9. Medical Exam Service: Add Prescription
   POST /api/prescriptions
   → prescriptions.medicalExamId = "exam001"
   → prescription_items (multiple medicines)
   → Call Medicine Service: Decrement stock

10. Billing Service: Auto-Generate Invoice (Triggered by Prescription Creation)
    POST /api/invoices/generate
    → invoices.appointmentId = "apt001"
    → invoice_items (consultation + medicines from prescription)
    → Triggered after prescription is saved

11. Billing Service: Record Payment
    POST /api/payments
    → payments.invoiceId = "inv001"
    → Update invoices.status = "PAID"

12. Reports Service: Generate Revenue Report
    GET /api/reports/revenue?startDate=2025-12-01&endDate=2025-12-31
    → Query billing_db for invoices + payments
    → Cache result in report_cache
```

---

## 🎯 Design Patterns & Best Practices

**1. Database-per-Service Pattern:**
- Each microservice has isolated database (8 services = 8 databases)
- Ensures loose coupling and independent deployment
- No shared database dependencies

**2. Soft Foreign Keys:**
- Cross-service relationships use IDs only (no DB-level FK constraints)
- Examples: `appointments.patientId` → `patients.id`, `appointments.doctorId` → `employees.id`
- Enables service autonomy and prevents cascading failures

**3. Soft Delete Pattern:**
- **Entities:** Employee
- **Implementation:** `deletedAt` timestamp + `deletedBy` user ID (both nullable)
- **Purpose:** Preserve audit trail, prevent orphan records
- **Query Rule:** Always filter `WHERE deleted_at IS NULL`
- **Testing:** `deletedBy` nullable allows testing without authentication

**4. Price Snapshot Pattern:**
- **Entities:** PrescriptionItem, InvoiceItem
- **Implementation:** Copy price at transaction time (not live reference)
- **Purpose:** Prevents historical data corruption when prices change
- **Example:** `prescription_items.unitPrice` stores medicine price at prescription time

**5. Idempotency Pattern:**
- **Entities:** Payment
- **Implementation:** Client-generated `idempotencyKey` (UUID, UNIQUE constraint)
- **Purpose:** Prevents duplicate payments on network retry/timeout
- **Critical for:** Financial transactions, payment gateways

**6. Denormalization for Performance:**
- **Entities:** MedicalExam (patientId, doctorId), Prescription (patientId, doctorId)
- **Purpose:** Improve query performance, reduce cross-service calls
- **Trade-off:** Data duplication vs query speed

**7. Generic Scheduling Pattern:**
- **Entity:** EmployeeSchedule
- **Implementation:** Single table for all employee roles (DOCTOR, NURSE, RECEPTIONIST, ADMIN)
- **Purpose:** Flexibility for different schedule types (appointments, shifts, coverage)
- **Filtering:** Role-specific logic in business layer

**8. Cache Pattern:**
- **Entity:** ReportCache
- **Implementation:** Store expensive aggregation results as JSON with TTL
- **Purpose:** Reduce database load for complex reports
- **TTL:** 12 hours (configurable)

**9. Audit Trail:**
- **All Entities:** createdAt, updatedAt, createdBy, updatedBy
- **Purpose:** Track all data changes for compliance and debugging
- **Integration:** Spring Data JPA @CreatedDate, @CreatedBy annotations

**10. UUID Primary Keys:**
- **All Entities:** String-based UUID (not auto-increment)
- **Purpose:** Distributed ID generation, no coordination required
- **Format:** VARCHAR(36) for MySQL compatibility

---

## 📊 Entity Summary

| Service | Database | Entities | Purpose |
|---------|----------|----------|---------|
| auth-service | auth_db | Account (1) | Authentication & authorization |
| patient-service | patient_db | Patient (1) | Patient demographics & health info |
| medicine-service | medicine_db | Category, Medicine (2) | Medicine catalog & inventory |
| hr-service | hr_db | Department, Employee, EmployeeSchedule (3) | Staff & scheduling management |
| appointment-service | appointment_db | Appointment (1) | Appointment bookings |
| medical-exam-service | medical_exam_db | MedicalExam, Prescription, PrescriptionItem (3) | Medical records & prescriptions |
| billing-service | billing_db | Invoice, InvoiceItem, Payment (3) | Billing & payment processing |
| reports-service | reports_db | ReportCache (1) | Report caching & aggregation |

**Total:** 8 services, 8 databases, 15 entities

---

## ✅ Anti-Pattern Fixes Applied

- ✅ **Soft Delete Tracking:** Employee entity has deletedAt/deletedBy (audit trail + orphan prevention)
- ✅ **Idempotency Keys:** Payment entity has idempotencyKey UNIQUE (prevents duplicate charges)
- ✅ **Proper Relational Tables:** EmployeeSchedule as dedicated table (NOT JSONB - enables querying/indexing)
- ✅ **Generic Scheduling:** Supports all employee types (DOCTOR, NURSE, RECEPTIONIST, ADMIN)
- ✅ **Price Snapshots:** PrescriptionItem stores unitPrice at prescription time (prevents historical corruption)
- ✅ **Denormalization:** Strategic duplication for query performance (patientId/doctorId in exams)

---

## 🚀 Ready for Development

All schemas are production-ready with microservices best practices:
- ✅ Service isolation (database-per-service)
- ✅ Data integrity (soft FKs, unique constraints)
- ✅ Audit compliance (audit fields, soft delete)
- ✅ Performance optimization (indexes, denormalization, caching)
- ✅ Financial safety (idempotency, price snapshots)
- ✅ Scalability (UUID PKs, stateless design)
