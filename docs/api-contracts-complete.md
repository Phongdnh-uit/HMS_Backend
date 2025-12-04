# API Contracts Complete - 3-Week MVP

**Project:** HMS Backend  
**Scope:** 3-Week MVP Implementation (6 New Services + Foundation Enhancement)  
**API Gateway:** Spring Cloud Gateway (Port 8080)  
**Architecture:** RESTful APIs with JWT Authentication  
**Date:** December 2, 2025

---

## 🎯 Overview

This document defines ALL API contracts for the 3-week MVP, including:
- **Total Services:** 8 services (3 existing enhanced + 5 new)
- **Total Endpoints:** ~95 REST endpoints
- **Authentication:** JWT Bearer tokens via API Gateway
- **Response Format:** JSON with consistent structure
- **Error Handling:** Standard HTTP status codes + error response schema

---

## 📊 API Gateway Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway (Port 8080)                 │
│                   JWT Validation + Routing                  │
│                                                             │
│  Headers Injected to All Services:                          │
│  - X-User-ID: {userId}                                      │
│  - X-User-Role: {ADMIN|PATIENT|DOCTOR|NURSE|EMPLOYEE}       │
│  - X-User-Email: {email}                                    │
└──────────────────────────────┬──────────────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
┌───────▼─────────┐  ┌─────────▼──────────┐  ┌──────▼──────────┐
│  Auth Service   │  │  Patient Service   │  │ Medicine Service│
│  Port: 8081     │  │  Port: 8082        │  │ Port: 8083      │
│  /api/auth/**   │  │  /api/patients/**  │  │ /api/medicines/**│
└─────────────────┘  └────────────────────┘  └─────────────────┘

┌─────────────────┐  ┌─────────────────────┐  ┌──────────────────┐
│   HR Service    │  │ Appointment Service │  │ Medical Exam Svc │
│  Port: 8084     │  │  Port: 8085         │  │ Port: 8086       │
│  /api/hr/**     │  │  /api/appointments/**│  │ /api/exams/**    │
└─────────────────┘  └─────────────────────┘  └──────────────────┘

┌─────────────────┐  ┌─────────────────────┐
│ Billing Service │  │  Reports Service    │
│  Port: 8087     │  │  Port: 8088         │
│  /api/billing/**│  │  /api/reports/**    │
└─────────────────┘  └─────────────────────┘
```

**Base URL:** `http://localhost:8080` (API Gateway)  
**All requests routed through API Gateway**

---

## 🔐 Authentication & Authorization

### Global Headers

**All authenticated requests must include:**
```http
Authorization: Bearer {JWT_TOKEN}
```

**API Gateway automatically injects these headers to downstream services:**
```http
X-User-ID: 550e8400-e29b-41d4-a716-446655440001
X-User-Role: DOCTOR
X-User-Email: doctor1@hms.com
```

### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| **ADMIN** | Full access to all endpoints |
| **DOCTOR** | Read/Write patients, appointments, exams, prescriptions; Read HR, medicines, billing |
| **NURSE** | Read/Write patients, appointments; Read exams, medicines |
| **PATIENT** | Read own data (patients, appointments, exams, invoices); Create appointments |
| **EMPLOYEE** | Read patients, appointments, medicines |

---

## 📋 Standard Response Schemas

### Success Response
```json
{
  "status": "success",
  "data": {
    // Response data here
  },
  "timestamp": "2025-12-02T10:30:00Z"
}
```

### Error Response
```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed for one or more fields",
    "details": [
      {
        "field": "email",
        "message": "Email is required"
      }
    ]
  },
  "timestamp": "2025-12-02T10:30:00Z"
}
```

### Paginated Response
```json
{
  "status": "success",
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "timestamp": "2025-12-02T10:30:00Z"
}
```

---

## 🔑 1. Auth Service API (`/api/auth`)

**Base Path:** `/api/auth`  
**Service Port:** 8081  
**Purpose:** User authentication and token management

### 1.1 Register Account

**Endpoint:** `POST /api/auth/register`  
**Access:** Public  
**Description:** Create new user account

**Request Body:**
```json
{
  "email": "patient1@gmail.com",
  "password": "SecurePass123!",
  "passwordConfirm": "SecurePass123!",
  "role": "PATIENT"
}
```

**Validation:**
- `email`: Required, valid email format, unique
- `password`: Required, min 8 chars, must include uppercase, lowercase, number, special char
- `passwordConfirm`: Required, must match password
- `role`: Required, one of [ADMIN, PATIENT, EMPLOYEE, DOCTOR, NURSE]

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "email": "patient1@gmail.com",
    "role": "PATIENT",
    "emailVerified": false,
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • email is required
  • email must be valid format (user@domain.com)
  • password is required
  • password must be at least 8 characters
  • password must contain uppercase letter
  • password must contain lowercase letter
  • password must contain number
  • password must contain special character
  • passwordConfirm is required
  • passwordConfirm must match password
  • role is required
  • role must be one of [ADMIN, PATIENT, EMPLOYEE, DOCTOR, NURSE]
- `409 EMAIL_ALREADY_EXISTS`: Email already registered

---

### 1.2 Login

**Endpoint:** `POST /api/auth/login`  
**Access:** Public  
**Description:** Authenticate user and get JWT tokens

**Request Body:**
```json
{
  "email": "patient1@gmail.com",
  "password": "SecurePass123!"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "email": "patient1@gmail.com",
      "role": "PATIENT"
    }
  }
}
```

**Error Codes:**
- `401 INVALID_CREDENTIALS`: Wrong email/password

---

### 1.3 Refresh Token

**Endpoint:** `POST /api/auth/refresh`  
**Access:** Requires valid refresh token (no Authorization header needed)  
**Description:** Get new access token using refresh token

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 3600
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: refreshToken is required
- `401 INVALID_TOKEN`: Refresh token expired/invalid/revoked

---

### 1.4 Logout

**Endpoint:** `POST /api/auth/logout`  
**Access:** Authenticated  
**Description:** Revoke refresh token

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "message": "Logged out successfully"
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token

---

### 1.5 Get Current User

**Endpoint:** `GET /api/auth/me`  
**Access:** Authenticated  
**Description:** Get current authenticated user info

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "email": "patient1@gmail.com",
    "role": "PATIENT",
    "emailVerified": true
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token

---

## 👤 2. Patient Service API (`/api/patients`)

**Base Path:** `/api/patients`  
**Service Port:** 8082  
**Purpose:** Patient profile and health information management

### 2.1 Create Patient

**Endpoint:** `POST /api/patients`  
**Access:** ADMIN, DOCTOR, NURSE  
**Description:** Create new patient profile

**Request Body:**
```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440002",
  "fullName": "Nguyen Van A",
  "email": "patient1@gmail.com",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "phoneNumber": "0901234567",
  "address": "123 Main St, Ho Chi Minh City",
  "identificationNumber": "079090001234",
  "healthInsuranceNumber": "VN123456789",
  "bloodType": "O+",
  "allergies": "Penicillin, Peanuts",
  "relativeFullName": "Nguyen Thi B",
  "relativePhoneNumber": "0907654321",
  "relativeRelationship": "Spouse"
}
```

**Validation:**
- `fullName`: Required, max 255 chars
- `dateOfBirth`: Required, ISO 8601 date format
- `gender`: One of [MALE, FEMALE, OTHER]
- `bloodType`: One of [A+, A-, B+, B-, AB+, AB-, O+, O-] or null
- `phoneNumber`: Valid Vietnamese phone format

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "p001",
    "accountId": "550e8400-e29b-41d4-a716-446655440002",
    "fullName": "Nguyen Van A",
    "email": "patient1@gmail.com",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "phoneNumber": "0901234567",
    "bloodType": "O+",
    "allergies": "Penicillin, Peanuts",
    "relativeFullName": "Nguyen Thi B",
    "relativePhoneNumber": "0907654321",
    "relativeRelationship": "Spouse",
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • fullName is required
  • fullName exceeds maximum length (255 characters)
  • dateOfBirth is required
  • dateOfBirth must be valid ISO 8601 date (YYYY-MM-DD)
  • dateOfBirth cannot be in the future
  • gender must be one of [MALE, FEMALE, OTHER]
  • bloodType must be one of [A+, A-, B+, B-, AB+, AB-, O+, O-] or null
  • phoneNumber must be valid Vietnamese format (10 digits starting with 0)
  • email must be valid format
  • identificationNumber must be 12 digits
- `403 FORBIDDEN`: User role not authorized (requires ADMIN, DOCTOR, or NURSE)
- `404 ACCOUNT_NOT_FOUND`: Account ID doesn't exist

---

### 2.2 Get Patient by ID

**Endpoint:** `GET /api/patients/{id}`  
**Access:** Authenticated (own data) | ADMIN, DOCTOR, NURSE (all)  
**Description:** Retrieve patient by ID

**Path Parameters:**
- `id` (string): Patient UUID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "p001",
    "accountId": "550e8400-e29b-41d4-a716-446655440002",
    "fullName": "Nguyen Van A",
    "email": "patient1@gmail.com",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "phoneNumber": "0901234567",
    "address": "123 Main St, Ho Chi Minh City",
    "identificationNumber": "079090001234",
    "healthInsuranceNumber": "VN123456789",
    "bloodType": "O+",
    "allergies": "Penicillin, Peanuts",
    "relativeFullName": "Nguyen Thi B",
    "relativePhoneNumber": "0907654321",
    "relativeRelationship": "Spouse",
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Not authorized to view this patient
- `404 PATIENT_NOT_FOUND`: Patient doesn't exist

---

### 2.3 List Patients (with filters)

**Endpoint:** `GET /api/patients`  
**Access:** ADMIN, DOCTOR, NURSE  
**Description:** List all patients with pagination and filters

**Query Parameters:**
- `page` (int, default=0): Page number
- `size` (int, default=20, max=100): Page size
- `search` (string): RSQL search query
- `sort` (string, default=createdAt,desc): Sort field and direction

**RSQL Search Examples:**
- `fullName=like='Nguyen*'`
- `bloodType==O+`
- `gender==MALE;bloodType==O+` (AND)
- `bloodType==O+,bloodType==A+` (OR)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "p001",
        "fullName": "Nguyen Van A",
        "gender": "MALE",
        "bloodType": "O+",
        "phoneNumber": "0901234567"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: User role not authorized (requires ADMIN, DOCTOR, or NURSE)

---

### 2.4 Update Patient

**Endpoint:** `PUT /api/patients/{id}`  
**Access:** ADMIN, DOCTOR, NURSE  
**Description:** Update patient information

**Request Body:** (Same as Create, all fields optional)
```json
{
  "fullName": "Nguyen Van A Updated",
  "allergies": "Penicillin, Peanuts, Latex"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "p001",
    "fullName": "Nguyen Van A Updated",
    "allergies": "Penicillin, Peanuts, Latex",
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed (same rules as Create Patient)
- `403 FORBIDDEN`: Not authorized to update this patient
- `404 PATIENT_NOT_FOUND`: Patient doesn't exist

---

### 2.5 Delete Patient (Soft Delete)

**Endpoint:** `DELETE /api/patients/{id}`  
**Access:** ADMIN only  
**Description:** Soft delete patient (marks as deleted, preserves audit trail)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "p001",
    "deletedAt": "2025-12-05T14:00:00Z",
    "deletedBy": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

**Business Rules:**
- Patient record is NOT permanently deleted (soft delete only)
- Sets `deletedAt` timestamp and `deletedBy` user ID
- Validates no future appointments exist before deletion
- Deleted patients excluded from all list queries by default

**Error Codes:**
- `404 PATIENT_NOT_FOUND`: Patient doesn't exist
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `409 HAS_FUTURE_APPOINTMENTS`: Cannot delete patient with scheduled future appointments

---

## 💊 3. Medicine Service API (`/api/medicines`)

**Base Path:** `/api/medicines`  
**Service Port:** 8083  
**Purpose:** Medicine catalog and inventory management

### 3.1 Create Medicine

**Endpoint:** `POST /api/medicines`  
**Access:** ADMIN  
**Description:** Add new medicine to catalog

**Request Body:**
```json
{
  "name": "Amoxicillin 500mg",
  "activeIngredient": "Amoxicillin",
  "unit": "capsule",
  "description": "Antibiotic for bacterial infections",
  "quantity": 1000,
  "concentration": "500mg",
  "packaging": "Box of 20 capsules",
  "purchasePrice": 5000,
  "sellingPrice": 8000,
  "expiresAt": "2026-12-31",
  "categoryId": "cat001",
  "manufacturer": "GSK",
  "sideEffects": "Nausea, diarrhea, allergic reactions",
  "storageConditions": "Store below 25°C, keep dry"
}
```

**Validation:**
- `name`: Required, max 255 chars
- `sellingPrice`: Must be >= purchasePrice
- `quantity`: Non-negative integer
- `expiresAt`: Must be future date

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "med001",
    "name": "Amoxicillin 500mg",
    "activeIngredient": "Amoxicillin",
    "unit": "capsule",
    "quantity": 1000,
    "sellingPrice": 8000,
    "expiresAt": "2026-12-31",
    "manufacturer": "GSK",
    "category": {
      "id": "cat001",
      "name": "Antibiotics"
    },
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • name is required
  • name exceeds maximum length (255 characters)
  • purchasePrice must be positive (> 0)
  • sellingPrice must be positive (> 0)
  • sellingPrice must be >= purchasePrice
  • quantity cannot be negative
  • expiresAt must be valid ISO 8601 date
  • expiresAt must be future date
  • unit is required
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 CATEGORY_NOT_FOUND`: Category ID doesn't exist

---

### 3.2 Get Medicine by ID

**Endpoint:** `GET /api/medicines/{id}`  
**Access:** Authenticated  
**Description:** Retrieve medicine details

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "med001",
    "name": "Amoxicillin 500mg",
    "activeIngredient": "Amoxicillin",
    "unit": "capsule",
    "description": "Antibiotic for bacterial infections",
    "concentration": "500mg",
    "packaging": "Box of 20 capsules",
    "quantity": 1000,
    "purchasePrice": 5000,
    "sellingPrice": 8000,
    "expiresAt": "2026-12-31",
    "manufacturer": "GSK",
    "sideEffects": "Nausea, diarrhea, allergic reactions",
    "storageConditions": "Store below 25°C, keep dry",
    "category": {
      "id": "cat001",
      "name": "Antibiotics",
      "description": "Medications that fight bacterial infections"
    },
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T10:30:00Z"
  }
}
```

**Field Visibility by Role:**
- 
- **All roles**: name, activeIngredient, unit, concentration, sellingPrice, sideEffects, storageConditions, category, manufacturer
- **ADMIN, DOCTOR, NURSE**: All fields above + purchasePrice, quantity, expiresAt, packaging, description
- **ADMIN**: All fields (full inventory details)
- **DOCTOR, NURSE**: All fields except purchasePrice (clinical + stock info)

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token
- `404 MEDICINE_NOT_FOUND`: Medicine doesn't exist

---

### 3.3 List Medicines

**Endpoint:** `GET /api/medicines`  
**Access:** Authenticated  
**Description:** List medicines with filters

**Query Parameters:**
- `page` (int, default=0): Page number
- `size` (int, default=20, max=100): Page size
- `sort` (string, default=createdAt,desc): Sort field and direction
- `search` (string): RSQL search query
- `categoryId` (string): Filter by category ID

**RSQL Search Examples:**
- `name=like='*cillin*'` (search by name)
- `quantity<100` (low stock - ADMIN only)
- `expiresAt<'2026-01-01'` (expiring soon - ADMIN only)
- `category.name=='Antibiotics'` (filter by category name)
- `sellingPrice>5000;sellingPrice<10000` (price range)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "med001",
        "name": "Amoxicillin 500mg",
        "activeIngredient": "Amoxicillin",
        "unit": "capsule",
        "concentration": "500mg",
        "packaging": "Box of 20 capsules",
        "quantity": 1000,
        "purchasePrice": 5000,
        "sellingPrice": 8000,
        "expiresAt": "2026-12-31",
        "manufacturer": "GSK",
        "category": {
          "id": "cat001",
          "name": "Antibiotics"
        },
        "createdAt": "2025-12-02T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

**Field Visibility by Role:**
- **ADMIN**: All fields (full inventory details)
- **DOCTOR, NURSE**: All fields except purchasePrice (clinical + stock info)
- **EMPLOYEE**: name, activeIngredient, unit, concentration, sellingPrice, manufacturer, category only (basic info)

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token
- `403 FORBIDDEN`: User role not authorized (requires ADMIN, DOCTOR, NURSE, or EMPLOYEE)
- `404 CATEGORY_NOT_FOUND`: Category ID doesn't exist (if categoryId filter applied)

---

### 3.4 Update Medicine

**Endpoint:** `PATCH /api/medicines/{id}`  
**Access:** ADMIN  
**Description:** Partial update medicine information

**Request Body:** (All fields optional - only include fields to update)
```json
{
  "name": "Amoxicillin 500mg Updated",
  "activeIngredient": "Amoxicillin",
  "unit": "capsule",
  "description": "Antibiotic for bacterial infections - updated",
  "concentration": "500mg",
  "packaging": "Box of 30 capsules",
  "purchasePrice": 5500,
  "sellingPrice": 8500,
  "expiresAt": "2027-06-30",
  "categoryId": "cat001",
  "manufacturer": "GSK",
  "sideEffects": "Nausea, diarrhea, allergic reactions",
  "storageConditions": "Store below 25°C, keep dry"
}
```

**Validation:**
- `sellingPrice`: Must be >= purchasePrice (if both provided)
- `expiresAt`: Must be future date (if provided)
- `name`: Max 255 chars (if provided)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "med001",
    "name": "Amoxicillin 500mg Updated",
    "activeIngredient": "Amoxicillin",
    "unit": "capsule",
    "description": "Antibiotic for bacterial infections - updated",
    "concentration": "500mg",
    "packaging": "Box of 30 capsules",
    "quantity": 1000,
    "purchasePrice": 5500,
    "sellingPrice": 8500,
    "expiresAt": "2027-06-30",
    "manufacturer": "GSK",
    "sideEffects": "Nausea, diarrhea, allergic reactions",
    "storageConditions": "Store below 25°C, keep dry",
    "category": {
      "id": "cat001",
      "name": "Antibiotics",
      "description": "Medications that fight bacterial infections"
    },
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Note:** This endpoint can update `quantity` directly. However, for stock adjustments with audit trail (prescription deductions, restocking), use the dedicated `PATCH /api/medicines/{id}/stock` endpoint which tracks reason and reference.

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • name exceeds maximum length (255 characters)
  • purchasePrice must be positive (> 0)
  • sellingPrice must be positive (> 0)
  • sellingPrice must be >= purchasePrice
  • quantity cannot be negative ( >= 0)
  • expiresAt must be valid ISO 8601 date
  • expiresAt must be future date
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 MEDICINE_NOT_FOUND`: Medicine doesn't exist
- `404 CATEGORY_NOT_FOUND`: Category ID doesn't exist (if categoryId provided)

---

### 3.5 Delete Medicine

**Endpoint:** `DELETE /api/medicines/{id}`  
**Access:** ADMIN only  
**Description:** Permanently delete medicine from catalog

**Response:** `204 No Content`

**Business Rules:**
- Medicine is permanently deleted from database
- Cannot delete medicine if it has been used in any prescription (referential integrity)
- Consider using Update Medicine to set quantity=0 instead of deleting for audit purposes

**Error Codes:**
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 MEDICINE_NOT_FOUND`: Medicine doesn't exist
- `409 MEDICINE_IN_USE`: Cannot delete medicine referenced in prescriptions

---

### 3.6 Update Medicine Stock

**Endpoint:** `PATCH /api/medicines/{id}/stock`  
**Access:** ADMIN (manual adjustment), Internal (prescription deduction)  
**Description:** Increment or decrement medicine quantity

**Request Body:**
```json
{
  "quantity": -20
}
```

**Note:** This endpoint uses delta values (positive to add, negative to deduct). For prescription deductions, this is called internally by the Medical Exam Service when creating prescriptions.

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "med001",
    "name": "Amoxicillin 500mg",
    "quantity": 980,
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • quantity is required
  • quantity must be non-zero integer
- `400 INSUFFICIENT_STOCK`: Requested deduction exceeds available stock (resulting quantity would be negative)
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 MEDICINE_NOT_FOUND`: Medicine doesn't exist

---

### 3.7 Medicine Categories

#### 3.7.1 Create Category
**Endpoint:** `POST /api/medicines/categories`  
**Access:** ADMIN  
**Description:** Create new medicine category

**Request Body:**
```json
{
  "name": "Antibiotics",
  "description": "Medications that fight bacterial infections"
}
```

**Validation:**
- `name`: Required, max 255 chars, unique

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "cat001",
    "name": "Antibiotics",
    "description": "Medications that fight bacterial infections",
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • name is required
  • name exceeds maximum length (255 characters)
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `409 CATEGORY_ALREADY_EXISTS`: Category name already exists

---

#### 3.7.2 Get Category by ID
**Endpoint:** `GET /api/medicines/categories/{id}`  
**Access:** Authenticated  
**Description:** Retrieve category details

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "cat001",
    "name": "Antibiotics",
    "description": "Medications that fight bacterial infections",
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token
- `404 CATEGORY_NOT_FOUND`: Category doesn't exist

---

#### 3.7.3 List Categories
**Endpoint:** `GET /api/medicines/categories`  
**Access:** Authenticated  
**Description:** List all medicine categories

**Query Parameters:**
- `page` (int, default=0): Page number
- `size` (int, default=20, max=100): Page size
- `sort` (string, default=name,asc): Sort field and direction
- `search` (string): RSQL search query

**RSQL Examples:**
- `name=like='*bio*'` (search by name)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "cat001",
        "name": "Antibiotics",
        "description": "Medications that fight bacterial infections",
        "createdAt": "2025-12-02T10:30:00Z"
      },
      {
        "id": "cat002",
        "name": "Painkillers",
        "description": "Pain relief medications",
        "createdAt": "2025-12-02T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 10,
    "totalPages": 1
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token

---

#### 3.7.4 Update Category
**Endpoint:** `PATCH /api/medicines/categories/{id}`  
**Access:** ADMIN  
**Description:** Partial update category information

**Request Body:** (All fields optional)
```json
{
  "name": "Antibiotics - Updated",
  "description": "Updated description for antibiotics category"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "cat001",
    "name": "Antibiotics - Updated",
    "description": "Updated description for antibiotics category",
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • name exceeds maximum length (255 characters)
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 CATEGORY_NOT_FOUND`: Category doesn't exist
- `409 CATEGORY_ALREADY_EXISTS`: Category name already exists (if name changed)

---

#### 3.7.5 Delete Category
**Endpoint:** `DELETE /api/medicines/categories/{id}`  
**Access:** ADMIN only  
**Description:** Delete medicine category

**Response:** `204 No Content`

**Business Rules:**
- Cannot delete category if medicines are assigned to it
- Alternatively, medicines will have `categoryId` set to NULL (ON DELETE SET NULL behavior)

**Error Codes:**
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 CATEGORY_NOT_FOUND`: Category doesn't exist
- `409 CATEGORY_HAS_MEDICINES`: Cannot delete category with assigned medicines (if enforcing strict deletion)

---

## 🏥 4. HR Service API (`/api/hr`)

**Base Path:** `/api/hr`  
**Service Port:** 8084  
**Purpose:** Hospital staff and department management

### 4.1 Department APIs

#### Create Department
**Endpoint:** `POST /api/hr/departments`  
**Access:** ADMIN  
**Request Body:**
```json
{
  "name": "Cardiology",
  "description": "Heart and cardiovascular diseases",
  "location": "Building A - Floor 3",
  "phoneExtension": "301",
  "status": "ACTIVE"
}
```

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "dept001",
    "name": "Cardiology",
    "description": "Heart and cardiovascular diseases",
    "location": "Building A - Floor 3",
    "phoneExtension": "301",
    "status": "ACTIVE",
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • name is required
  • name exceeds maximum length (255 characters)
  • status must be one of [ACTIVE, INACTIVE]
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `409 DEPARTMENT_NAME_EXISTS`: Department name already exists


---

#### Get Department by ID
**Endpoint:** `GET /api/hr/departments/{id}`  
**Access:** Authenticated  
**Description:** Retrieve department details by ID

**Path Parameters:**
- `id` (string): Department UUID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "dept001",
    "name": "Cardiology",
    "description": "Heart and cardiovascular diseases",
    "location": "Building A - Floor 3",
    "phoneExtension": "301",
    "status": "ACTIVE",
    "headDoctorId": "emp001",
    "headDoctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung"
    },
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token
- `404 DEPARTMENT_NOT_FOUND`: Department doesn't exist

---

#### List Departments
**Endpoint:** `GET /api/hr/departments`  
**Access:** Authenticated  
**Description:** List all departments with optional filters

**Query Parameters:**
- `page` (int, default=0): Page number
- `size` (int, default=20, max=100): Page size
- `sort` (string, default=name,asc): Sort field and direction
- `status` (string): Filter by ACTIVE/INACTIVE
- `search` (string): RSQL search query

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "dept001",
        "name": "Cardiology",
        "description": "Heart and cardiovascular diseases",
        "location": "Building A - Floor 3",
        "status": "ACTIVE",
        "headDoctor": {
          "id": "emp001",
          "fullName": "Dr. Nguyen Van Hung"
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 10,
    "totalPages": 1
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token

---

#### Update Department
**Endpoint:** `PATCH /api/hr/departments/{id}`  
**Access:** ADMIN  
**Description:** Partial update department information

**Request Body:** (All fields optional)
```json
{
  "name": "Cardiology - Updated",
  "description": "Heart and cardiovascular diseases - Updated",
  "location": "Building B - Floor 2",
  "phoneExtension": "302",
  "status": "ACTIVE",
  "headDoctorId": "emp002"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "dept001",
    "name": "Cardiology - Updated",
    "description": "Heart and cardiovascular diseases - Updated",
    "location": "Building B - Floor 2",
    "phoneExtension": "302",
    "status": "ACTIVE",
    "headDoctorId": "emp002",
    "headDoctor": {
      "id": "emp002",
      "fullName": "Dr. Tran Van B"
    },
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • name exceeds maximum length (255 characters)
  • status must be one of [ACTIVE, INACTIVE]
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 DEPARTMENT_NOT_FOUND`: Department doesn't exist
- `404 EMPLOYEE_NOT_FOUND`: Head doctor ID doesn't exist (if headDoctorId provided)
- `409 DEPARTMENT_NAME_EXISTS`: Department name already exists (if name changed)

---

#### Delete Department
**Endpoint:** `DELETE /api/hr/departments/{id}`  
**Access:** ADMIN only  
**Description:** Delete department (only if no employees assigned)

**Response:** `204 No Content`

**Business Rules:**
- Cannot delete department if employees are assigned to it
- Consider setting status to INACTIVE instead of deleting

**Error Codes:**
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 DEPARTMENT_NOT_FOUND`: Department doesn't exist
- `409 DEPARTMENT_HAS_EMPLOYEES`: Cannot delete department with assigned employees

---

### 4.2 Employee APIs

#### Create Employee
**Endpoint:** `POST /api/hr/employees`  
**Access:** ADMIN  
**Request Body:**
```json
{
  "accountId": "550e8400-e29b-41d4-a716-446655440003",
  "fullName": "Dr. Nguyen Van Hung",
  "role": "DOCTOR",
  "departmentId": "dept001",
  "specialization": "Interventional Cardiology",
  "licenseNumber": "MD-12345",
  "email": "dr.hung@hms.com",
  "phoneNumber": "0901111111",
  "address": "456 Hospital St",
  "status": "ACTIVE",
  "hiredAt": "2020-01-15"
}
```

**Validation:**
- `role`: One of [DOCTOR, NURSE, RECEPTIONIST, ADMIN]
- `departmentId`: Required for DOCTOR/NURSE
- `licenseNumber`: Required for DOCTOR/NURSE, unique

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "emp001",
    "accountId": "550e8400-e29b-41d4-a716-446655440003",
    "fullName": "Dr. Nguyen Van Hung",
    "role": "DOCTOR",
    "department": {
      "id": "dept001",
      "name": "Cardiology"
    },
    "specialization": "Interventional Cardiology",
    "licenseNumber": "MD-12345",
    "email": "dr.hung@hms.com",
    "phoneNumber": "0901111111",
    "address": "456 Hospital St",
    "status": "ACTIVE",
    "hiredAt": "2020-01-15T00:00:00Z",
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • fullName is required
  • fullName exceeds maximum length (255 characters)
  • role is required
  • role must be one of [DOCTOR, NURSE, RECEPTIONIST, ADMIN]
  • departmentId is required for DOCTOR and NURSE roles
  • licenseNumber is required for DOCTOR and NURSE roles
  • email must be valid format
  • phoneNumber must be valid Vietnamese format
  • hiredAt must be valid ISO 8601 date
  • status must be one of [ACTIVE, ON_LEAVE, RESIGNED]
- `404 ACCOUNT_NOT_FOUND`: Account ID doesn't exist
- `404 DEPARTMENT_NOT_FOUND`: Department ID doesn't exist
- `409 LICENSE_NUMBER_EXISTS`: License number already in use

---

#### Get Employee by ID
**Endpoint:** `GET /api/hr/employees/{id}`  
**Access:** Authenticated  
**Description:** Retrieve employee details by ID

**Path Parameters:**
- `id` (string): Employee UUID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "emp001",
    "accountId": "550e8400-e29b-41d4-a716-446655440003",
    "fullName": "Dr. Nguyen Van Hung",
    "role": "DOCTOR",
    "department": {
      "id": "dept001",
      "name": "Cardiology",
      "location": "Building A - Floor 3"
    },
    "specialization": "Interventional Cardiology",
    "licenseNumber": "MD-12345",
    "email": "dr.hung@hms.com",
    "phoneNumber": "0901111111",
    "address": "456 Hospital St",
    "status": "ACTIVE",
    "hiredAt": "2020-01-15T00:00:00Z",
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token
- `404 EMPLOYEE_NOT_FOUND`: Employee doesn't exist

---

#### List Employees
**Endpoint:** `GET /api/hr/employees`  
**Access:** Authenticated  
**Description:** List all employees with pagination and filters

**Query Parameters:**
- `page` (int, default=0): Page number
- `size` (int, default=20, max=100): Page size
- `sort` (string, default=fullName,asc): Sort field and direction
- `departmentId` (string): Filter by department
- `role` (string): Filter by role (DOCTOR, NURSE, RECEPTIONIST, ADMIN)
- `status` (string): Filter by status (ACTIVE, ON_LEAVE, RESIGNED)
- `search` (string): RSQL search query

**RSQL Examples:**
- `fullName=like='*Nguyen*'`
- `role==DOCTOR;status==ACTIVE`
- `specialization=like='*Cardio*'`

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "emp001",
        "fullName": "Dr. Nguyen Van Hung",
        "role": "DOCTOR",
        "department": {
          "id": "dept001",
          "name": "Cardiology"
        },
        "specialization": "Interventional Cardiology",
        "email": "dr.hung@hms.com",
        "phoneNumber": "0901111111",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

**Error Codes:**
- `401 UNAUTHORIZED`: Missing or invalid access token

---

#### Update Employee
**Endpoint:** `PATCH /api/hr/employees/{id}`  
**Access:** ADMIN  
**Description:** Partial update employee information

**Request Body:** (All fields optional)
```json
{
  "fullName": "Dr. Nguyen Van Hung - Updated",
  "departmentId": "dept002",
  "specialization": "Pediatric Cardiology",
  "email": "dr.hung.updated@hms.com",
  "phoneNumber": "0901111112",
  "address": "789 New Hospital St",
  "status": "ON_LEAVE"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "emp001",
    "accountId": "550e8400-e29b-41d4-a716-446655440003",
    "fullName": "Dr. Nguyen Van Hung - Updated",
    "role": "DOCTOR",
    "department": {
      "id": "dept002",
      "name": "Pediatrics"
    },
    "specialization": "Pediatric Cardiology",
    "licenseNumber": "MD-12345",
    "email": "dr.hung.updated@hms.com",
    "phoneNumber": "0901111112",
    "address": "789 New Hospital St",
    "status": "ON_LEAVE",
    "hiredAt": "2020-01-15T00:00:00Z",
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • fullName exceeds maximum length (255 characters)
  • role must be one of [DOCTOR, NURSE, RECEPTIONIST, ADMIN]
  • departmentId is required for DOCTOR and NURSE roles
  • licenseNumber is required for DOCTOR and NURSE roles
  • email must be valid format
  • phoneNumber must be valid Vietnamese format
  • hiredAt must be valid ISO 8601 date
  • status must be one of [ACTIVE, ON_LEAVE, RESIGNED]
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 EMPLOYEE_NOT_FOUND`: Employee doesn't exist
- `404 DEPARTMENT_NOT_FOUND`: Department ID doesn't exist (if departmentId provided)
- `409 LICENSE_NUMBER_EXISTS`: License number already exists (if licenseNumber changed)

---

#### Delete Employee (Soft Delete)
**Endpoint:** `DELETE /api/hr/employees/{id}`  
**Access:** ADMIN only  
**Description:** Soft delete employee (marks as deleted, preserves audit trail)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "emp001",
    "deletedAt": "2025-12-05T14:00:00Z",
    "deletedBy": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

**Business Rules:**
- Employee record is NOT permanently deleted (soft delete only)
- Sets `deletedAt` timestamp and `deletedBy` user ID
- Validates no future appointments exist before deletion
- Deleted employees excluded from all list queries

**Error Codes:**
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 EMPLOYEE_NOT_FOUND`: Employee doesn't exist or already deleted
- `409 HAS_FUTURE_APPOINTMENTS`: Cannot delete employee with scheduled future appointments

---

#### List Doctors by Department
**Endpoint:** `GET /api/hr/doctors`  
**Access:** Authenticated  
**Description:** List doctors for appointment booking, filtered by department or specialization

**Query Parameters:**
- `departmentId` (string, optional): Filter by department UUID
- `specialization` (string, optional): Filter by specialization keyword
- `status` (string, optional): Filter by status (default: ACTIVE only)
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "emp001",
        "fullName": "Dr. Nguyen Van Hung",
        "department": {
          "id": "dept001",
          "name": "Cardiology"
        },
        "specialization": "Interventional Cardiology",
        "licenseNumber": "MD-12345",
        "phoneNumber": "0901111111",
        "email": "dr.hung@hms.com",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Invalid query parameters
  • page must be >= 0
  • size must be between 1 and 100
- `404 DEPARTMENT_NOT_FOUND`: departmentId doesn't exist (if provided)

---

### 4.3 Employee Schedule APIs

#### Create Employee Schedule
**Endpoint:** `POST /api/hr/schedules`  
**Access:** ADMIN only  
**Description:** Create work schedule for any employee (doctor, nurse, receptionist, admin). Only administrators can create schedules to ensure proper coverage and coordination.

**Request Body:**
```json
{
  "employeeId": "emp001",
  "workDate": "2025-12-05",
  "startTime": "08:00",
  "endTime": "17:00",
  "status": "AVAILABLE",
  "notes": "Regular working hours"
}
```

**Validation:**
- `workDate`: Cannot be in the past
- `startTime` < `endTime`
- Unique constraint: (employeeId, workDate) - one schedule per employee per day

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "sch001",
    "employeeId": "emp001",
    "employee": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung",
      "role": "DOCTOR",
      "department": {
        "id": "dept001",
        "name": "Cardiology"
      }
    },
    "workDate": "2025-12-05",
    "startTime": "08:00",
    "endTime": "17:00",
    "status": "AVAILABLE",
    "notes": "Regular working hours",
    "createdAt": "2025-12-02T10:30:00Z",
    "createdBy": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • employeeId is required
  • workDate is required
  • workDate must be valid ISO 8601 date (YYYY-MM-DD)
  • workDate cannot be in the past
  • startTime is required (format: HH:mm)
  • endTime is required (format: HH:mm)
  • startTime must be before endTime
  • status must be one of [AVAILABLE, BOOKED, CANCELLED]
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 EMPLOYEE_NOT_FOUND`: Employee ID doesn't exist
- `409 SCHEDULE_EXISTS`: Schedule already exists for this employee on this date

---

#### Get Own Schedules
**Endpoint:** `GET /api/hr/schedules/me`  
**Access:** Authenticated (any employee)  
**Description:** Get current user's own schedules within date range

**Query Parameters:**
- `startDate` (date, required): Start date (YYYY-MM-DD)
- `endDate` (date, required): End date (YYYY-MM-DD)
- `status` (string, optional): Filter by status (AVAILABLE, BOOKED, CANCELLED)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": [
    {
      "id": "sch001",
      "workDate": "2025-12-05",
      "startTime": "08:00",
      "endTime": "17:00",
      "status": "AVAILABLE",
      "notes": "Regular working hours",
      "createdAt": "2025-12-02T10:30:00Z"
    }
  ]
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • startDate is required
  • endDate is required
  • startDate cannot be after endDate

---

#### Get Employee Availability (Admin)
**Endpoint:** `GET /api/hr/schedules/availability`  
**Access:** ADMIN only  
**Description:** Get any employee's schedules within date range. Used for shift management and scheduling oversight.

**Query Parameters:**
- `employeeId` (string, required): Employee ID
- `startDate` (date, required): Start date (YYYY-MM-DD)
- `endDate` (date, required): End date (YYYY-MM-DD)
- `status` (string, optional): Filter by status (AVAILABLE, BOOKED, CANCELLED)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": [
    {
      "id": "sch001",
      "workDate": "2025-12-05",
      "startTime": "08:00",
      "endTime": "17:00",
      "status": "AVAILABLE",
      "notes": "Regular working hours",
      "employee": {
        "id": "emp001",
        "fullName": "Dr. Nguyen Van Hung",
        "role": "DOCTOR",
        "department": {
          "id": "dept001",
          "name": "Cardiology"
        }
      }
    }
  ]
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • employeeId is required
  • startDate is required
  • endDate is required
  • startDate cannot be after endDate
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 EMPLOYEE_NOT_FOUND`: Employee ID doesn't exist

---

#### List Doctor Schedules (for Appointment Booking)
**Endpoint:** `GET /api/hr/schedules/doctors`  
**Access:** Authenticated  
**Description:** List available doctor schedules for appointment booking. Filters only DOCTOR role employees.

**Query Parameters:**
- `departmentId` (string, optional): Filter by department
- `doctorId` (string, optional): Filter by specific doctor
- `startDate` (date, required): Start date (YYYY-MM-DD)
- `endDate` (date, required): End date (YYYY-MM-DD)
- `status` (string, optional): Filter by status (default: AVAILABLE)
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)
- `sort` (string, optional): Sort field (default: workDate,asc)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "sch001",
        "workDate": "2025-12-05",
        "startTime": "08:00",
        "endTime": "17:00",
        "status": "AVAILABLE",
        "doctor": {
          "id": "emp001",
          "fullName": "Dr. Nguyen Van Hung",
          "specialization": "Interventional Cardiology",
          "department": {
            "id": "dept001",
            "name": "Cardiology"
          }
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • startDate is required
  • endDate is required
  • startDate cannot be after endDate
  • page must be >= 0
  • size must be between 1 and 100
- `404 DEPARTMENT_NOT_FOUND`: Department ID doesn't exist (if departmentId provided)
- `404 EMPLOYEE_NOT_FOUND`: Doctor ID doesn't exist (if doctorId provided)

---

#### Update Employee Schedule
**Endpoint:** `PATCH /api/hr/schedules/{id}`  
**Access:** ADMIN only  
**Description:** Update schedule details (employee, time, status, notes). Only administrators can modify schedules.

**Request Body:** (All fields optional)
```json
{
  "employeeId": "emp002",
  "workDate": "2025-12-06",
  "startTime": "09:00",
  "endTime": "18:00",
  "status": "AVAILABLE",
  "notes": "Updated schedule - reassigned to different employee"
}
```

**Validation:**
- `employeeId`: Must exist in employees table (if provided)
- `workDate`: Cannot be in the past (if provided)
- `startTime` < `endTime` (if either provided)
- Unique constraint: (employeeId, workDate) - cannot create duplicate

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "sch001",
    "employeeId": "emp002",
    "employee": {
      "id": "emp002",
      "fullName": "Nurse Tran Thi B",
      "role": "NURSE",
      "department": {
        "id": "dept001",
        "name": "Cardiology"
      }
    },
    "workDate": "2025-12-06",
    "startTime": "09:00",
    "endTime": "18:00",
    "status": "AVAILABLE",
    "notes": "Updated schedule - reassigned to different employee",
    "createdAt": "2025-12-02T10:30:00Z",
    "createdBy": "550e8400-e29b-41d4-a716-446655440001",
    "updatedAt": "2025-12-02T11:00:00Z",
    "updatedBy": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • workDate must be valid ISO 8601 date (YYYY-MM-DD)
  • workDate cannot be in the past
  • startTime must be valid format (HH:mm)
  • endTime must be valid format (HH:mm)
  • startTime must be before endTime
  • status must be one of [AVAILABLE, BOOKED, CANCELLED]
  • notes exceeds maximum length (1000 characters)
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 SCHEDULE_NOT_FOUND`: Schedule doesn't exist
- `404 EMPLOYEE_NOT_FOUND`: Employee ID doesn't exist (if employeeId provided)
- `409 SCHEDULE_EXISTS`: Schedule already exists for employee on this date (if employeeId or workDate changed)
- `409 HAS_APPOINTMENTS`: Cannot modify schedule with booked appointments

**Note:** The Appointment Service uses this endpoint internally to update schedule status to `BOOKED` when appointments are created.

---

#### Delete Employee Schedule
**Endpoint:** `DELETE /api/hr/schedules/{id}`  
**Access:** ADMIN only  
**Description:** Delete an employee schedule. Only administrators can delete schedules.

**Response:** `204 No Content`

**Business Rules:**
- Cannot delete schedule with booked appointments (status = BOOKED)
- Consider setting status to CANCELLED instead

**Error Codes:**
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 SCHEDULE_NOT_FOUND`: Schedule doesn't exist
- `409 HAS_APPOINTMENTS`: Cannot delete schedule with booked appointments

---

## 📅 5. Appointment Service API (`/api/appointments`)

**Base Path:** `/api/appointments`  
**Service Port:** 8085  
**Purpose:** Patient appointment booking and management

### 5.1 Create Appointment

**Endpoint:** `POST /api/appointments`  
**Access:** ADMIN, DOCTOR, NURSE, PATIENT (own)  
**Description:** Book new appointment

**Request Body:**
```json
{
  "patientId": "p001",
  "doctorId": "emp001",
  "appointmentTime": "2025-12-05T09:00:00",
  "type": "CONSULTATION",
  "reason": "Chest pain"
}
```

**Validation:**
- `appointmentTime`: Must be future, during doctor's schedule
- `type`: One of [CONSULTATION, FOLLOW_UP, EMERGENCY]
- Doctor must have AVAILABLE schedule for this date
- No overlapping appointments for doctor
- **Appointment Duration:** Fixed at 30 minutes (configurable in application.yml)
  - Time slots calculated as: appointmentTime to appointmentTime + 30 minutes
  - Prevents double-booking within this window

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "apt001",
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung",
      "department": "Cardiology"
    },
    "appointmentTime": "2025-12-05T09:00:00",
    "status": "SCHEDULED",
    "type": "CONSULTATION",
    "reason": "Chest pain",
    "createdAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • patientId is required
  • doctorId is required
  • appointmentTime is required
  • appointmentTime must be valid ISO 8601 datetime
  • type is required
  • type must be one of [CONSULTATION, FOLLOW_UP, EMERGENCY]
  • reason exceeds maximum length (500 characters)
- `400 PAST_APPOINTMENT`: appointmentTime cannot be in the past
- `404 PATIENT_NOT_FOUND`: Patient ID doesn't exist
- `404 EMPLOYEE_NOT_FOUND`: Doctor ID doesn't exist
- `409 DOCTOR_NOT_AVAILABLE`: Doctor has no schedule for this date/time
- `409 TIME_SLOT_TAKEN`: Time slot already booked (30-minute window)

---

### 5.2 Get Appointment by ID

**Endpoint:** `GET /api/appointments/{id}`  
**Access:** Authenticated (own) | ADMIN, DOCTOR, NURSE (all)  
**Description:** Retrieve appointment details. Patients can only view their own appointments.

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "apt001",
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A",
      "phoneNumber": "0901234567"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung",
      "department": "Cardiology",
      "phoneNumber": "0901111111"
    },
    "appointmentTime": "2025-12-05T09:00:00",
    "status": "SCHEDULED",
    "type": "CONSULTATION",
    "reason": "Chest pain",
    "notes": null,
    "cancelledAt": null,
    "cancelReason": null,
    "createdAt": "2025-12-02T10:30:00Z",
    "updatedAt": "2025-12-02T10:30:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's appointment
- `404 APPOINTMENT_NOT_FOUND`: Appointment doesn't exist

---

### 5.3 List Appointments

**Endpoint:** `GET /api/appointments`  
**Access:** Authenticated  
**Description:** List appointments with filters. Patients can only see their own appointments. Staff can see all.

**Query Parameters:**
- `patientId` (string, optional): Filter by patient (ignored for PATIENT role - uses their own)
- `doctorId` (string, optional): Filter by doctor
- `status` (string, optional): Filter by status (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW)
- `startDate` (date, optional): Filter appointments from this date (YYYY-MM-DD)
- `endDate` (date, optional): Filter appointments until this date (YYYY-MM-DD)
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)
- `sort` (string, optional): Sort field (default: appointmentTime,desc)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "apt001",
        "patient": {
          "id": "p001",
          "fullName": "Nguyen Van A"
        },
        "doctor": {
          "id": "emp001",
          "fullName": "Dr. Nguyen Van Hung",
          "department": "Cardiology"
        },
        "appointmentTime": "2025-12-05T09:00:00",
        "status": "SCHEDULED",
        "type": "CONSULTATION",
        "reason": "Chest pain"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Invalid query parameters
  • page must be >= 0
  • size must be between 1 and 100
  • startDate must be valid ISO 8601 date
  • endDate must be valid ISO 8601 date
  • startDate cannot be after endDate

---

### 5.4 Update Appointment

**Endpoint:** `PATCH /api/appointments/{id}`  
**Access:** ADMIN, DOCTOR, NURSE  
**Description:** Update appointment details (reschedule, add notes)

**Request Body:**
```json
{
  "appointmentTime": "2025-12-05T10:00:00",
  "type": "FOLLOW_UP",
  "reason": "Follow-up consultation",
  "notes": "Rescheduled per patient request"
}
```

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "apt001",
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung",
      "department": "Cardiology"
    },
    "appointmentTime": "2025-12-05T10:00:00",
    "status": "SCHEDULED",
    "type": "FOLLOW_UP",
    "reason": "Follow-up consultation",
    "notes": "Rescheduled per patient request",
    "updatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • appointmentTime must be valid ISO 8601 datetime
  • type must be one of [CONSULTATION, FOLLOW_UP, EMERGENCY]
  • reason exceeds maximum length (500 characters)
  • notes exceeds maximum length (1000 characters)
- `400 PAST_APPOINTMENT`: appointmentTime cannot be in the past
- `400 CANNOT_MODIFY_COMPLETED`: Cannot modify completed/cancelled appointments
- `403 FORBIDDEN`: User role not authorized
- `404 APPOINTMENT_NOT_FOUND`: Appointment doesn't exist
- `409 DOCTOR_NOT_AVAILABLE`: Doctor has no schedule for new date/time
- `409 TIME_SLOT_TAKEN`: New time slot already booked (if rescheduling)

---

### 5.5 Cancel Appointment

**Endpoint:** `PATCH /api/appointments/{id}/cancel`  
**Access:** ADMIN, DOCTOR, NURSE, PATIENT (own)  
**Description:** Cancel a scheduled appointment

**Request Body:**
```json
{
  "cancelReason": "Patient recovered"
}
```

**Validation:**
- `cancelReason`: Required, max 500 characters

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "apt001",
    "status": "CANCELLED",
    "cancelledAt": "2025-12-02T11:00:00Z",
    "cancelledBy": "550e8400-e29b-41d4-a716-446655440001",
    "cancelReason": "Patient recovered"
  }
}
```

**Side Effects:**
- Frees up doctor's time slot (makes available for other bookings)
- Triggers cancellation notification to patient/doctor

**Error Codes:**
- `400 VALIDATION_ERROR`: cancelReason is required
- `400 CANNOT_CANCEL_COMPLETED`: Cannot cancel completed appointments
- `400 ALREADY_CANCELLED`: Appointment is already cancelled
- `403 FORBIDDEN`: Patient trying to cancel another patient's appointment
- `404 APPOINTMENT_NOT_FOUND`: Appointment doesn't exist

---

### 5.6 Complete Appointment

**Endpoint:** `PATCH /api/appointments/{id}/complete`  
**Access:** DOCTOR only  
**Description:** Mark appointment as completed after consultation

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "apt001",
    "status": "COMPLETED",
    "completedAt": "2025-12-05T10:30:00Z",
    "completedBy": "emp001",
    "updatedAt": "2025-12-05T10:30:00Z"
  }
}
```

**Side Effects:**
- Updates doctor schedule status
- Allows creation of medical exam record
- Triggers completion notification to patient

**Error Codes:**
- `400 APPOINTMENT_ALREADY_COMPLETED`: Appointment already marked as completed
- `400 APPOINTMENT_CANCELLED`: Cannot complete a cancelled appointment
- `403 FORBIDDEN`: Only the assigned doctor can complete the appointment
- `404 APPOINTMENT_NOT_FOUND`: Appointment doesn't exist

---

## 🏥 6. Medical Exam Service API (`/api/exams`)

**Base Path:** `/api/exams`  
**Service Port:** 8086  
**Purpose:** Medical examination records and prescriptions

### 6.1 Create Medical Exam

**Endpoint:** `POST /api/exams`  
**Access:** DOCTOR only  
**Request Body:**
```json
{
  "appointmentId": "apt001",
  "diagnosis": "Hypertension Stage 1",
  "symptoms": "Headache, dizziness",
  "treatment": "Lifestyle changes, medication",
  "temperature": 36.8,
  "bloodPressureSystolic": 145,
  "bloodPressureDiastolic": 95,
  "heartRate": 78,
  "weight": 75.5,
  "height": 175.0,
  "notes": "Follow-up in 2 weeks"
}
```

**Validation:**
- `appointmentId`: Must exist and be COMPLETED
- Unique constraint: One exam per appointment

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "exam001",
    "appointment": {
      "id": "apt001",
      "appointmentTime": "2025-12-05T09:00:00"
    },
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung"
    },
    "diagnosis": "Hypertension Stage 1",
    "symptoms": "Headache, dizziness",
    "vitals": {
      "temperature": 36.8,
      "bloodPressureSystolic": 145,
      "bloodPressureDiastolic": 95,
      "heartRate": 78,
      "weight": 75.5,
      "height": 175.0
    },
    "examDate": "2025-12-05T10:30:00Z",
    "createdAt": "2025-12-05T10:30:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • appointmentId is required
  • diagnosis is required
  • temperature must be between 30.0 and 45.0 (Celsius)
  • bloodPressureSystolic must be between 50 and 250 (mmHg)
  • bloodPressureDiastolic must be between 30 and 150 (mmHg)
  • heartRate must be between 30 and 200 (bpm)
  • weight must be positive (> 0, in kg)
  • height must be positive (> 0, in cm)
- `400 APPOINTMENT_NOT_COMPLETED`: Appointment must be completed first
- `404 APPOINTMENT_NOT_FOUND`: Appointment ID doesn't exist
- `409 EXAM_EXISTS`: Medical exam already exists for this appointment

---

### 6.2 Get Medical Exam by ID

**Endpoint:** `GET /api/exams/{id}`  
**Access:** DOCTOR, NURSE, PATIENT (own)  
**Description:** Retrieve medical exam details by exam ID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "exam001",
    "appointment": {
      "id": "apt001",
      "appointmentTime": "2025-12-05T09:00:00"
    },
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung"
    },
    "diagnosis": "Hypertension Stage 1",
    "symptoms": "Headache, dizziness",
    "treatment": "Lifestyle changes, medication",
    "vitals": {
      "temperature": 36.8,
      "bloodPressureSystolic": 145,
      "bloodPressureDiastolic": 95,
      "heartRate": 78,
      "weight": 75.5,
      "height": 175.0
    },
    "notes": "Follow-up in 2 weeks",
    "examDate": "2025-12-05T10:30:00Z",
    "createdAt": "2025-12-05T10:30:00Z",
    "updatedAt": "2025-12-05T10:30:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's exam
- `404 EXAM_NOT_FOUND`: Medical exam doesn't exist

---

### 6.3 Get Medical Exam by Appointment

**Endpoint:** `GET /api/exams/by-appointment/{appointmentId}`  
**Access:** DOCTOR, NURSE, PATIENT (own)  
**Description:** Retrieve medical exam by appointment ID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "exam001",
    "appointment": {
      "id": "apt001",
      "appointmentTime": "2025-12-05T09:00:00"
    },
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung"
    },
    "diagnosis": "Hypertension Stage 1",
    "symptoms": "Headache, dizziness",
    "treatment": "Lifestyle changes, medication",
    "vitals": {
      "temperature": 36.8,
      "bloodPressureSystolic": 145,
      "bloodPressureDiastolic": 95,
      "heartRate": 78,
      "weight": 75.5,
      "height": 175.0
    },
    "notes": "Follow-up in 2 weeks",
    "examDate": "2025-12-05T10:30:00Z",
    "createdAt": "2025-12-05T10:30:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's exam
- `404 APPOINTMENT_NOT_FOUND`: Appointment doesn't exist
- `404 EXAM_NOT_FOUND`: No exam found for this appointment

---

### 6.4 List Medical Exams

**Endpoint:** `GET /api/exams`  
**Access:** DOCTOR, NURSE (all), PATIENT (own)  
**Description:** List medical exams with filters. Patients automatically see only their own exams.

**Query Parameters:**
- `patientId` (string, optional): Filter by patient (ignored for PATIENT role)
- `doctorId` (string, optional): Filter by doctor
- `startDate` (date, optional): Filter exams from this date (YYYY-MM-DD)
- `endDate` (date, optional): Filter exams until this date (YYYY-MM-DD)
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)
- `sort` (string, optional): Sort field (default: examDate,desc)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "exam001",
        "appointment": {
          "id": "apt001",
          "appointmentTime": "2025-12-05T09:00:00"
        },
        "patient": {
          "id": "p001",
          "fullName": "Nguyen Van A"
        },
        "doctor": {
          "id": "emp001",
          "fullName": "Dr. Nguyen Van Hung"
        },
        "diagnosis": "Hypertension Stage 1",
        "examDate": "2025-12-05T10:30:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Invalid query parameters
  • page must be >= 0
  • size must be between 1 and 100
  • startDate must be valid ISO 8601 date
  • endDate must be valid ISO 8601 date

---

### 6.5 Create Prescription

**Endpoint:** `POST /api/exams/{examId}/prescriptions`  
**Access:** DOCTOR only  
**Request Body:**
```json
{
  "notes": "Take with food, avoid alcohol",
  "items": [
    {
      "medicineId": "med001",
      "quantity": 20,
      "dosage": "1 capsule twice daily",
      "durationDays": 10,
      "instructions": "Take with food"
    },
    {
      "medicineId": "med002",
      "quantity": 10,
      "dosage": "1 tablet as needed",
      "durationDays": 5,
      "instructions": "Maximum 4 tablets per day"
    }
  ]
}
```

**Validation:**
- `medicineId`: Must exist and have sufficient stock
- `quantity`: Must be > 0 and <= available stock

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "rx001",
    "medicalExam": {
      "id": "exam001"
    },
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung"
    },
    "prescribedAt": "2025-12-05T10:30:00Z",
    "notes": "Take with food, avoid alcohol",
    "items": [
      {
        "id": "rxi001",
        "medicine": {
          "id": "med001",
          "name": "Amoxicillin 500mg"
        },
        "quantity": 20,
        "unitPrice": 8000,
        "dosage": "1 capsule twice daily",
        "durationDays": 10,
        "instructions": "Take with food",
        "totalCost": 160000
      }
    ],
    "totalCost": 175000
  }
}
```

**Side Effects:**
- Decrements medicine stock in medicine-service
- Triggers billing service to auto-generate invoice (adds consultation fee + medicine items)

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • items array is required and cannot be empty
  • items[].medicineId is required
  • items[].quantity is required
  • items[].quantity must be positive (> 0)
  • items[].dosage is required
  • items[].durationDays must be positive (> 0)
- `400 INSUFFICIENT_STOCK`: Not enough medicine in stock for one or more items
- `404 EXAM_NOT_FOUND`: Medical exam ID doesn't exist
- `404 MEDICINE_NOT_FOUND`: One or more medicine IDs don't exist

---

### 6.6 Get Prescription by ID

**Endpoint:** `GET /api/exams/prescriptions/{id}`  
**Access:** DOCTOR, NURSE, PATIENT (own)  
**Description:** Retrieve prescription details by prescription ID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "rx001",
    "medicalExam": {
      "id": "exam001"
    },
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "doctor": {
      "id": "emp001",
      "fullName": "Dr. Nguyen Van Hung"
    },
    "prescribedAt": "2025-12-05T10:30:00Z",
    "notes": "Take with food, avoid alcohol",
    "items": [
      {
        "id": "rxi001",
        "medicine": {
          "id": "med001",
          "name": "Amoxicillin 500mg"
        },
        "quantity": 20,
        "unitPrice": 8000,
        "dosage": "1 capsule twice daily",
        "durationDays": 10,
        "instructions": "Take with food",
        "totalCost": 160000
      }
    ],
    "totalCost": 175000,
    "createdAt": "2025-12-05T10:30:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's prescription
- `404 PRESCRIPTION_NOT_FOUND`: Prescription doesn't exist

---

### 6.7 Get Prescriptions by Patient

**Endpoint:** `GET /api/exams/prescriptions/by-patient/{patientId}`  
**Access:** DOCTOR, NURSE, PATIENT (own)  
**Description:** Retrieve all prescriptions for a specific patient

**Query Parameters:**
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "rx001",
        "medicalExam": {
          "id": "exam001"
        },
        "doctor": {
          "id": "emp001",
          "fullName": "Dr. Nguyen Van Hung"
        },
        "prescribedAt": "2025-12-05T10:30:00Z",
        "totalCost": 175000,
        "itemCount": 2
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's prescriptions
- `404 PATIENT_NOT_FOUND`: Patient doesn't exist

---

## 💰 7. Billing Service API (`/api/billing`)

**Base Path:** `/api/billing`  
**Service Port:** 8087  
**Purpose:** Invoice and payment management

### 7.1 Auto-Generate Invoice

**Endpoint:** `POST /api/billing/invoices/generate`  
**Access:** Internal (triggered by prescription creation)  
**Request Body:**
```json
{
  "appointmentId": "apt001"
}
```

**Business Logic:**
1. Fetch appointment details
2. Add consultation fee (based on doctor's department)
3. Fetch prescription items (required - invoice generated after prescription)
4. Calculate subtotal, tax (10%), total
5. Generate invoice number (INV-YYYYMMDD-XXXX)

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "inv001",
    "invoiceNumber": "INV-20251205-0001",
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "appointment": {
      "id": "apt001",
      "appointmentTime": "2025-12-05T09:00:00"
    },
    "invoiceDate": "2025-12-05T10:30:00Z",
    "dueDate": "2025-12-12T10:30:00Z",
    "items": [
      {
        "type": "CONSULTATION",
        "description": "Cardiology Consultation",
        "quantity": 1,
        "unitPrice": 200000,
        "amount": 200000
      },
      {
        "type": "MEDICINE",
        "description": "Amoxicillin 500mg",
        "quantity": 20,
        "unitPrice": 8000,
        "amount": 160000
      }
    ],
    "subtotal": 360000,
    "discount": 0,
    "tax": 36000,
    "totalAmount": 396000,
    "status": "UNPAID"
  }
}
```

---

### 7.2 Get Invoice by ID

**Endpoint:** `GET /api/billing/invoices/{id}`  
**Access:** ADMIN, PATIENT (own)  
**Description:** Retrieve invoice details by invoice ID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "inv001",
    "invoiceNumber": "INV-20251205-0001",
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "appointment": {
      "id": "apt001",
      "appointmentTime": "2025-12-05T09:00:00"
    },
    "invoiceDate": "2025-12-05T10:30:00Z",
    "dueDate": "2025-12-12T10:30:00Z",
    "items": [
      {
        "type": "CONSULTATION",
        "description": "Cardiology Consultation",
        "quantity": 1,
        "unitPrice": 200000,
        "amount": 200000
      },
      {
        "type": "MEDICINE",
        "description": "Amoxicillin 500mg",
        "quantity": 20,
        "unitPrice": 8000,
        "amount": 160000
      }
    ],
    "subtotal": 360000,
    "discount": 0,
    "tax": 36000,
    "totalAmount": 396000,
    "paidAmount": 0,
    "balanceDue": 396000,
    "status": "UNPAID",
    "createdAt": "2025-12-05T10:30:00Z",
    "updatedAt": "2025-12-05T10:30:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's invoice
- `404 INVOICE_NOT_FOUND`: Invoice doesn't exist

---

### 7.3 Get Invoice by Appointment

**Endpoint:** `GET /api/billing/invoices/by-appointment/{appointmentId}`  
**Access:** ADMIN, PATIENT (own)  
**Description:** Retrieve invoice by appointment ID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "inv001",
    "invoiceNumber": "INV-20251205-0001",
    "patient": {
      "id": "p001",
      "fullName": "Nguyen Van A"
    },
    "appointment": {
      "id": "apt001",
      "appointmentTime": "2025-12-05T09:00:00"
    },
    "invoiceDate": "2025-12-05T10:30:00Z",
    "dueDate": "2025-12-12T10:30:00Z",
    "subtotal": 360000,
    "discount": 0,
    "tax": 36000,
    "totalAmount": 396000,
    "paidAmount": 0,
    "status": "UNPAID"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's invoice
- `404 APPOINTMENT_NOT_FOUND`: Appointment doesn't exist
- `404 INVOICE_NOT_FOUND`: No invoice found for this appointment

---

### 7.4 List Invoices

**Endpoint:** `GET /api/billing/invoices`  
**Access:** ADMIN  
**Description:** List all invoices with filters (admin only)

**Query Parameters:**
- `patientId` (string, optional): Filter by patient
- `status` (string, optional): Filter by status (UNPAID, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED)
- `startDate` (date, optional): Filter invoices from this date (YYYY-MM-DD)
- `endDate` (date, optional): Filter invoices until this date (YYYY-MM-DD)
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)
- `sort` (string, optional): Sort field (default: invoiceDate,desc)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "inv001",
        "invoiceNumber": "INV-20251205-0001",
        "patient": {
          "id": "p001",
          "fullName": "Nguyen Van A"
        },
        "invoiceDate": "2025-12-05T10:30:00Z",
        "dueDate": "2025-12-12T10:30:00Z",
        "totalAmount": 396000,
        "paidAmount": 0,
        "status": "UNPAID"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Invalid query parameters
  • page must be >= 0
  • size must be between 1 and 100
  • status must be one of [UNPAID, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED]
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)

---

### 7.5 Get Patient Invoices

**Endpoint:** `GET /api/billing/invoices/by-patient/{patientId}`  
**Access:** ADMIN, PATIENT (own)  
**Description:** Retrieve all invoices for a specific patient

**Query Parameters:**
- `status` (string, optional): Filter by status
- `page` (integer, optional): Page number (default: 0)
- `size` (integer, optional): Page size (default: 20, max: 100)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": "inv001",
        "invoiceNumber": "INV-20251205-0001",
        "invoiceDate": "2025-12-05T10:30:00Z",
        "dueDate": "2025-12-12T10:30:00Z",
        "totalAmount": 396000,
        "paidAmount": 0,
        "status": "UNPAID"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's invoices
- `404 PATIENT_NOT_FOUND`: Patient doesn't exist

---

### 7.6 Create Payment

**Endpoint:** `POST /api/billing/payments`  
**Access:** ADMIN, PATIENT (own invoices)  
**Request Body:**
```json
{
  "invoiceId": "inv001",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 396000,
  "method": "CASH",
  "notes": "Paid in full"
}
```

**Validation:**
- `idempotencyKey`: Required, valid UUID format (client-generated to prevent duplicate payments)
- `method`: One of [CASH, CREDIT_CARD, BANK_TRANSFER, INSURANCE]
- `amount`: Must be > 0

**Idempotency Pattern:**
- Client generates new UUID per payment attempt (e.g., `crypto.randomUUID()`)
- On network failure/timeout, retry with SAME UUID
- Server returns existing payment if idempotency key already exists (prevents duplicate charge)

**Response:** `201 Created`
```json
{
  "status": "success",
  "data": {
    "id": "pay001",
    "invoice": {
      "id": "inv001",
      "invoiceNumber": "INV-20251205-0001",
      "totalAmount": 396000
    },
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 396000,
    "method": "CASH",
    "status": "COMPLETED",
    "paymentDate": "2025-12-05T11:00:00Z",
    "createdAt": "2025-12-05T11:00:00Z"
  }
}
```

**Side Effects:**
- Updates invoice status to PAID (if fully paid) or PARTIALLY_PAID
- Calculates remaining balance

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • invoiceId is required
  • idempotencyKey is required
  • idempotencyKey must be valid UUID format
  • amount is required
  • amount must be positive (> 0)
  • amount cannot exceed invoice remaining balance
  • method is required
  • method must be one of [CASH, CREDIT_CARD, BANK_TRANSFER, INSURANCE]
- `404 INVOICE_NOT_FOUND`: Invoice ID doesn't exist
- `409 DUPLICATE_PAYMENT`: Idempotency key already exists (returns existing payment record)
- `409 INVOICE_ALREADY_PAID`: Invoice is already fully paid

---

### 7.7 Get Payment by ID

**Endpoint:** `GET /api/billing/payments/{id}`  
**Access:** ADMIN, PATIENT (own)  
**Description:** Retrieve payment details by payment ID

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "id": "pay001",
    "invoice": {
      "id": "inv001",
      "invoiceNumber": "INV-20251205-0001",
      "totalAmount": 396000
    },
    "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 396000,
    "method": "CASH",
    "status": "COMPLETED",
    "notes": "Paid in full",
    "paymentDate": "2025-12-05T11:00:00Z",
    "createdAt": "2025-12-05T11:00:00Z"
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's payment
- `404 PAYMENT_NOT_FOUND`: Payment doesn't exist

---

### 7.8 List Payments by Invoice

**Endpoint:** `GET /api/billing/payments/by-invoice/{invoiceId}`  
**Access:** ADMIN, PATIENT (own)  
**Description:** Retrieve all payments made for a specific invoice

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "payments": [
      {
        "id": "pay001",
        "amount": 200000,
        "method": "CASH",
        "status": "COMPLETED",
        "paymentDate": "2025-12-05T11:00:00Z"
      },
      {
        "id": "pay002",
        "amount": 196000,
        "method": "CREDIT_CARD",
        "status": "COMPLETED",
        "paymentDate": "2025-12-06T09:00:00Z"
      }
    ],
    "totalPaid": 396000,
    "invoiceTotal": 396000,
    "remainingBalance": 0
  }
}
```

**Error Codes:**
- `403 FORBIDDEN`: Patient trying to access another patient's invoice payments
- `404 INVOICE_NOT_FOUND`: Invoice doesn't exist

---

## 📊 8. Reports Service API (`/api/reports`)

**Base Path:** `/api/reports`  
**Service Port:** 8088  
**Purpose:** Analytics and reporting

### 8.1 Revenue Report

**Endpoint:** `GET /api/reports/revenue`  
**Access:** ADMIN only  
**Description:** Generate revenue report for specified period

**Query Parameters:**
- `startDate` (date, required): Report start date (YYYY-MM-DD)
- `endDate` (date, required): Report end date (YYYY-MM-DD)
- `departmentId` (string, optional): Filter by department

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "period": {
      "startDate": "2025-12-01",
      "endDate": "2025-12-31"
    },
    "totalRevenue": 50000000,
    "paidRevenue": 45000000,
    "unpaidRevenue": 5000000,
    "invoiceCount": {
      "total": 150,
      "paid": 135,
      "unpaid": 10,
      "overdue": 5
    },
    "revenueByDepartment": [
      {
        "departmentId": "dept001",
        "departmentName": "Cardiology",
        "revenue": 20000000,
        "percentage": 44.4
      }
    ],
    "revenueByPaymentMethod": [
      {
        "method": "CASH",
        "amount": 25000000,
        "percentage": 55.6
      },
      {
        "method": "CREDIT_CARD",
        "amount": 15000000,
        "percentage": 33.3
      }
    ],
    "generatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • startDate is required
  • endDate is required
  • startDate must be valid ISO 8601 date
  • endDate must be valid ISO 8601 date
  • startDate cannot be after endDate
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 DEPARTMENT_NOT_FOUND`: Department ID doesn't exist (if filter applied)

---

### 8.2 Appointment Statistics

**Endpoint:** `GET /api/reports/appointments`  
**Access:** ADMIN, DOCTOR (own statistics)  
**Description:** Generate appointment statistics report

**Query Parameters:**
- `startDate` (date, required): Report start date (YYYY-MM-DD)
- `endDate` (date, required): Report end date (YYYY-MM-DD)
- `departmentId` (string, optional): Filter by department
- `doctorId` (string, optional): Filter by doctor (DOCTOR can only see own)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "period": {
      "startDate": "2025-12-01",
      "endDate": "2025-12-31"
    },
    "totalAppointments": 500,
    "appointmentsByStatus": {
      "SCHEDULED": 150,
      "COMPLETED": 300,
      "CANCELLED": 40,
      "NO_SHOW": 10
    },
    "appointmentsByType": {
      "CONSULTATION": 250,
      "FOLLOW_UP": 200,
      "EMERGENCY": 50
    },
    "appointmentsByDepartment": [
      {
        "departmentId": "dept001",
        "departmentName": "Cardiology",
        "count": 150,
        "percentage": 30.0
      }
    ],
    "dailyTrend": [
      {
        "date": "2025-12-01",
        "count": 25
      }
    ],
    "averagePerDay": 16.7,
    "generatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • startDate is required
  • endDate is required
  • startDate cannot be after endDate
- `403 FORBIDDEN`: Doctor trying to view other doctor's statistics
- `404 DEPARTMENT_NOT_FOUND`: Department ID doesn't exist (if filter applied)
- `404 EMPLOYEE_NOT_FOUND`: Doctor ID doesn't exist (if filter applied)

---

### 8.3 Doctor Performance Report

**Endpoint:** `GET /api/reports/doctors/performance`  
**Access:** ADMIN only  
**Description:** Generate doctor performance metrics report

**Query Parameters:**
- `startDate` (date, required): Report start date (YYYY-MM-DD)
- `endDate` (date, required): Report end date (YYYY-MM-DD)
- `departmentId` (string, optional): Filter by department

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "period": {
      "startDate": "2025-12-01",
      "endDate": "2025-12-31"
    },
    "doctors": [
      {
        "doctorId": "emp001",
        "doctorName": "Dr. Nguyen Van Hung",
        "department": "Cardiology",
        "statistics": {
          "totalAppointments": 80,
          "completedAppointments": 75,
          "cancelledAppointments": 3,
          "noShows": 2,
          "completionRate": 93.8,
          "totalRevenue": 8000000,
          "averageRevenuePerAppointment": 100000,
          "patientsSeen": 60,
          "prescriptionsWritten": 65
        }
      }
    ],
    "generatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • startDate is required
  • endDate is required
  • startDate cannot be after endDate
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)
- `404 DEPARTMENT_NOT_FOUND`: Department ID doesn't exist (if filter applied)

---

### 8.4 Patient Activity Report

**Endpoint:** `GET /api/reports/patients/activity`  
**Access:** ADMIN only  
**Description:** Generate patient activity and demographics report

**Query Parameters:**
- `startDate` (date, required): Report start date (YYYY-MM-DD)
- `endDate` (date, required): Report end date (YYYY-MM-DD)

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "period": {
      "startDate": "2025-12-01",
      "endDate": "2025-12-31"
    },
    "totalPatients": 1500,
    "newPatients": 50,
    "activePatients": 300,
    "patientsByGender": {
      "MALE": 700,
      "FEMALE": 750,
      "OTHER": 50
    },
    "patientsByBloodType": {
      "O+": 450,
      "A+": 400,
      "B+": 300
    },
    "averageAge": 42.5,
    "topDiagnoses": [
      {
        "diagnosis": "Hypertension",
        "count": 80
      }
    ],
    "generatedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: Input validation failed
  • startDate is required
  • endDate is required
  • startDate cannot be after endDate
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)

---

### 8.5 Clear Report Cache

**Endpoint:** `DELETE /api/reports/cache`  
**Access:** ADMIN only  
**Description:** Clear cached report data to force fresh calculation

**Query Parameters:**
- `reportType` (string, optional): Specific report type to clear (revenue, appointments, doctors, patients). If not provided, clears all caches.

**Response:** `200 OK`
```json
{
  "status": "success",
  "data": {
    "message": "Cache cleared successfully",
    "clearedTypes": ["revenue", "appointments", "doctors", "patients"],
    "clearedAt": "2025-12-02T11:00:00Z"
  }
}
```

**Error Codes:**
- `400 VALIDATION_ERROR`: reportType must be one of [revenue, appointments, doctors, patients]
- `403 FORBIDDEN`: User role not authorized (requires ADMIN)

---

## 🔧 Error Codes Reference

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| `200` | OK | Successful GET, PUT, PATCH |
| `201` | Created | Successful POST (resource created) |
| `204` | No Content | Successful DELETE |
| `400` | Bad Request | Validation errors, invalid input |
| `401` | Unauthorized | Missing/invalid JWT token |
| `403` | Forbidden | Valid token but insufficient permissions |
| `404` | Not Found | Resource doesn't exist |
| `409` | Conflict | Resource conflict (duplicate, constraint violation) |
| `500` | Internal Server Error | Server-side error |

### Application Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `INVALID_CREDENTIALS` | 401 | Wrong email/password |
| `INVALID_TOKEN` | 401 | JWT token expired/invalid |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `EMAIL_ALREADY_EXISTS` | 409 | Duplicate email |
| `SCHEDULE_EXISTS` | 409 | Schedule already exists |
| `INSUFFICIENT_STOCK` | 400 | Not enough medicine |
| `APPOINTMENT_NOT_COMPLETED` | 400 | Prerequisite not met |
| `DOCTOR_NOT_AVAILABLE` | 409 | Doctor unavailable |
| `TIME_SLOT_TAKEN` | 409 | Appointment slot taken |

---

## 📝 API Summary

### Endpoint Count by Service

| Service | Endpoints | Entity Resources |
|---------|-----------|------------------|
| **Auth Service** | 5 | Account |
| **Patient Service** | 5 | Patient |
| **Medicine Service** | 10 | Medicine, Category |
| **HR Service** | 15 | Department, Employee, DoctorSchedule |
| **Appointment Service** | 8 | Appointment |
| **Medical Exam Service** | 12 | MedicalExam, Prescription, PrescriptionItem |
| **Billing Service** | 12 | Invoice, InvoiceItem, Payment |
| **Reports Service** | 5 | ReportCache |
| **TOTAL** | **~95** | **15 entities** |

---

## 🚀 API Development Guidelines

### 1. Consistent Response Format
All APIs return JSON with standard structure:
```json
{
  "status": "success|error",
  "data": {...} | "error": {...},
  "timestamp": "ISO 8601 datetime"
}
```

### 2. Pagination Standards
- Default page size: 20
- Max page size: 100
- Page numbers start at 0
- Sort format: `field,direction` (e.g., `createdAt,desc`)

### 3. RSQL Search Syntax
- Equality: `field==value`
- Like: `field=like='*pattern*'`
- Comparison: `field>value`, `field<value`
- AND: `;` (e.g., `field1==value1;field2==value2`)
- OR: `,` (e.g., `field==value1,field==value2`)

### 4. Date/Time Formats
- Date: `YYYY-MM-DD` (ISO 8601)
- DateTime: `YYYY-MM-DDTHH:mm:ssZ` (ISO 8601 with timezone)
- Time: `HH:mm` (24-hour format)

### 5. Security Best Practices
- All endpoints (except auth) require JWT
- API Gateway validates JWT before routing
- Services receive user context via headers
- Implement rate limiting (10 req/sec per user)
- Log all API calls for audit

### 6. Error Handling
- Return appropriate HTTP status codes
- Include detailed error messages
- Use application error codes for client handling
- Never expose stack traces in production

---

## 📖 Postman Collection

**Collection Name:** HMS Backend - 3 Week MVP  
**Variables:**
- `baseUrl`: `http://localhost:8080`
- `accessToken`: (auto-updated from login)
- `patientId`, `doctorId`, `appointmentId`: Test data IDs

**Folders:**
1. Auth Service (5 requests)
2. Patient Service (5 requests)
3. Medicine Service (10 requests)
4. HR Service (15 requests)
5. Appointment Service (8 requests)
6. Medical Exam Service (12 requests)
7. Billing Service (12 requests)
8. Reports Service (5 requests)

**Pre-request Scripts:**
- Auto-inject Authorization header
- Environment variable management

**Tests:**
- Validate response status
- Extract and save tokens/IDs
- Verify response schema

---

## ✅ Ready for Implementation

**All API contracts defined and ready for:**
- ✅ Backend implementation (Spring Boot controllers)
- ✅ Frontend integration (React/Next.js API client)
- ✅ API testing (Postman, Jest, Supertest)
- ✅ OpenAPI/Swagger documentation generation
- ✅ API Gateway route configuration

**Total APIs: ~95 endpoints across 8 services** 🚀
