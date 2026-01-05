# API Contracts - HMS Backend

## Overview

HMS Backend exposes **162+ REST API endpoints** across 13 microservices, accessed through API Gateway on port 8080.

**Base URL:** `http://localhost:8080/api`  
**Authentication:** JWT Bearer tokens (OAuth2)  
**Data Format:** JSON

---

## Service Endpoints Summary

| Service | Base Path | Endpoints | Description |
|---------|-----------|-----------|-------------|
| **Auth Service** | `/auth` | 7 | Authentication, registration, token management |
| **Patient Service** | `/patients` | 12 | Patient records, profile management |
| **Appointment Service** | `/appointments` | 18 | Appointment scheduling, queue management |
| **Medical Exam Service** | `/exams` | 20+ | Medical examinations, prescriptions, lab orders |
| **Billing Service** | `/invoices`, `/payments` | 17 | Invoice generation, payment processing (VNPay) |
| **HR Service** | `/hr` | 14 | Employees, departments, schedules |
| **Medicine Service** | `/medicines` | 4 | Medicine inventory, categories |
| **Report Service** | `/reports` | 6 | Revenue, appointment, patient reports |
| **Notification Service** | `/notifications` | 4 | Email notifications, job triggers |

---

## 1. Authentication Service (`/auth`)

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login and get tokens | No |
| POST | `/auth/refresh` | Refresh access token | Yes |
| POST | `/auth/logout` | Logout user | Yes |
| GET | `/auth/me` | Get current user info | Yes |
| GET | `/auth/accounts/{id}` | Get account by ID | Yes |
| POST | `/auth/accounts` | Create account | Yes (Admin) |

### Key Request/Response Schemas

**POST /auth/register**
```json
Request: { "email": "string", "password": "string", "fullName": "string" }
Response: { "id": "long", "email": "string", "roles": ["PATIENT"] }
```

**POST /auth/login**
```json
Request: { "email": "string", "password": "string" }
Response: { "accessToken": "string", "refreshToken": "string", "expiresIn": 3600 }
```

---

## 2. Patient Service (`/patients`)

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/patients` | List all patients (paginated) | Yes (Admin) |
| GET | `/patients/me` | Get my patient profile | Yes (Patient) |
| POST | `/patients/me` | Create my patient profile | Yes (Patient) |
| PATCH | `/patients/me` | Update my patient profile | Yes (Patient) |
| GET | `/patients/by-account` | Get patient by account ID | Yes |
| GET | `/patients/stats` | Get patient statistics | Yes (Admin) |
| POST | `/patients/{id}/profile-image` | Upload profile image | Yes |
| DELETE | `/patients/{id}/profile-image` | Delete profile image | Yes |
| POST | `/patients/me/profile-image` | Upload my profile image | Yes (Patient) |
| DELETE | `/patients/me/profile-image` | Delete my profile image | Yes (Patient) |

### Key Schemas

**Patient Profile**
```json
{
  "id": "long",
  "fullName": "string",
  "dateOfBirth": "date",
  "gender": "MALE|FEMALE|OTHER",
  "phoneNumber": "string",
  "address": "string",
  "emergencyContact": "string",
  "bloodType": "A+|A-|B+|B-|O+|O-|AB+|AB-",
  "allergies": "string",
  "medicalHistory": "string",
  "profileImageUrl": "string"
}
```

---

## 3. Appointment Service (`/appointments`)

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/appointments/all` | List all appointments (paginated) | Yes |
| POST | `/appointments` | Create appointment | Yes |
| GET | `/appointments/{id}` | Get appointment by ID | Yes |
| PUT | `/appointments/{id}` | Update appointment | Yes |
| DELETE | `/appointments/{id}` | Delete appointment | Yes |
| GET | `/appointments/slots` | Get available time slots | Yes |
| GET | `/appointments/by-patient/{patientId}` | Get patient appointments | Yes |
| PATCH | `/appointments/{id}/cancel` | Cancel appointment | Yes |
| PATCH | `/appointments/{id}/complete` | Mark appointment completed | Yes (Doctor) |
| POST | `/appointments/bulk-cancel` | Bulk cancel appointments | Yes (Admin) |
| POST | `/appointments/bulk-restore` | Bulk restore appointments | Yes (Admin) |
| GET | `/appointments/count` | Count appointments | Yes |
| GET | `/appointments/stats` | Appointment statistics | Yes (Admin) |
| POST | `/appointments/walk-in` | Walk-in registration | Yes (Receptionist) |
| GET | `/appointments/queue/all` | Get all queue items | Yes |
| GET | `/appointments/queue/doctor/{doctorId}` | Get doctor's queue | Yes |
| GET | `/appointments/queue/next/{doctorId}` | Get next in queue | Yes (Doctor) |
| PATCH | `/appointments/queue/call-next/{doctorId}` | Call next patient | Yes (Doctor) |

### Key Schemas

**Appointment**
```json
{
  "id": "long",
  "patientId": "long",
  "doctorId": "long",
  "appointmentDate": "datetime",
  "appointmentType": "CHECKUP|FOLLOWUP|EMERGENCY",
  "status": "SCHEDULED|COMPLETED|CANCELLED",
  "reason": "string",
  "notes": "string"
}
```

---

## 4. Medical Exam Service (`/exams`)

### Main Exam Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/exams/all` | List all exams (paginated) | Yes |
| POST | `/exams` | Create medical exam | Yes (Doctor) |
| GET | `/exams/{id}` | Get exam by ID | Yes |
| PUT | `/exams/update/{id}` | Update exam | Yes (Doctor) |
| DELETE | `/exams/{id}` | Delete exam | Yes (Admin) |
| GET | `/exams/by-appointment/{appointmentId}` | Get exam by appointment | Yes |
| GET | `/exams/my` | Get my exams (as doctor) | Yes (Doctor) |
| GET | `/exams/stats` | Exam statistics | Yes (Admin) |

### Prescription Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/exams/{examId}/prescriptions` | Create prescription | Yes (Doctor) |
| PUT | `/exams/{examId}/prescription` | Update prescription | Yes (Doctor) |
| GET | `/exams/prescriptions/{id}` | Get prescription by ID | Yes |
| GET | `/exams/{examId}/prescription` | Get exam prescription | Yes |
| GET | `/exams/prescriptions/by-patient/{patientId}` | Get patient prescriptions | Yes |
| POST | `/exams/prescriptions/{id}/cancel` | Cancel prescription | Yes (Doctor) |
| POST | `/exams/prescriptions/{id}/dispense` | Dispense prescription | Yes (Pharmacist) |

### Lab Test Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/exams/lab-tests/all` | List all lab tests | Yes |
| GET | `/exams/lab-tests/{id}` | Get lab test by ID | Yes |
| POST | `/exams/lab-tests` | Create lab test | Yes (Admin) |
| PUT | `/exams/lab-tests/{id}` | Update lab test | Yes (Admin) |
| DELETE | `/exams/lab-tests/{id}` | Delete lab test | Yes (Admin) |
| GET | `/exams/lab-tests/active` | Get active lab tests | Yes |
| GET | `/exams/lab-tests/category/{category}` | Get by category | Yes |
| GET | `/exams/lab-tests/code/{code}` | Get by code | Yes |

### Lab Order Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/exams/lab-orders/all` | List all lab orders | Yes |
| GET | `/exams/lab-orders/{id}` | Get lab order by ID | Yes |
| POST | `/exams/lab-orders` | Create lab order | Yes (Doctor) |
| PUT | `/exams/lab-orders/{id}` | Update lab order | Yes |
| DELETE | `/exams/lab-orders/{id}` | Delete lab order | Yes |
| GET | `/exams/lab-orders/exam/{examId}` | Get orders by exam | Yes |
| GET | `/exams/lab-orders/patient/{patientId}` | Get orders by patient | Yes |
| POST | `/exams/lab-orders/auto-group` | Auto-group lab orders | Yes |

### Lab Result Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/exams/lab-results/all` | List all results | Yes |
| GET | `/exams/lab-results/{id}` | Get result by ID | Yes |
| POST | `/exams/lab-results` | Create result | Yes (Lab Tech) |
| PUT | `/exams/lab-results/{id}` | Update result | Yes (Lab Tech) |
| GET | `/exams/lab-results/exam/{examId}` | Get results by exam | Yes |
| GET | `/exams/lab-results/patient/{patientId}` | Get results by patient | Yes |
| POST | `/exams/lab-results/{id}/images` | Upload result images | Yes |
| GET | `/exams/lab-results/{id}/images` | Get result images | Yes |
| DELETE | `/exams/lab-results/images/{imageId}` | Delete image | Yes |
| GET | `/exams/lab-results/images/{imageId}/download` | Download image | Yes |

### Key Schemas

**Medical Exam**
```json
{
  "id": "long",
  "appointmentId": "long",
  "patientId": "long",
  "doctorId": "long",
  "diagnosis": "string",
  "symptoms": "string",
  "vitalSigns": {
    "bloodPressure": "string",
    "heartRate": "number",
    "temperature": "number",
    "weight": "number",
    "height": "number"
  },
  "notes": "string",
  "examDate": "datetime",
  "followUpRequired": "boolean",
  "followUpDate": "datetime"
}
```

---

## 5. Billing Service

### Invoice Endpoints (`/invoices`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/invoices` | List all invoices (paginated) | Yes |
| GET | `/invoices/{id}` | Get invoice by ID | Yes |
| POST | `/invoices/generate` | Generate invoice | Yes |
| POST | `/invoices/upsert` | Upsert invoice | Yes (System) |
| GET | `/invoices/my` | Get my invoices | Yes (Patient) |
| GET | `/invoices/by-appointment/{appointmentId}` | Get by appointment | Yes |
| GET | `/invoices/by-exam/{examId}` | Get by exam | Yes |
| GET | `/invoices/by-patient/{patientId}` | Get by patient | Yes |
| GET | `/invoices/stats` | Invoice statistics | Yes (Admin) |
| POST | `/invoices/{id}/cancel` | Cancel invoice | Yes (Admin) |
| DELETE | `/invoices/{id}` | Delete invoice | Yes (Admin) |

### Payment Endpoints (`/payments`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/payments/init` | Initialize VNPay payment | Yes (Patient) |
| GET | `/payments/vnpay-return` | VNPay return callback | No |
| POST | `/payments/vnpay-ipn` | VNPay IPN callback | No |
| POST | `/payments/{invoiceId}/cash` | Process cash payment | Yes (Receptionist) |
| GET | `/payments/{id}` | Get payment by ID | Yes |
| GET | `/payments/by-invoice/{invoiceId}` | Get payments by invoice | Yes |
| GET | `/payments/summary-cards` | Payment summary cards | Yes (Admin) |

### Key Schemas

**Invoice**
```json
{
  "id": "long",
  "invoiceNumber": "string",
  "patientId": "long",
  "appointmentId": "long",
  "examId": "long",
  "totalAmount": "decimal",
  "paidAmount": "decimal",
  "status": "PENDING|PAID|PARTIALLY_PAID|CANCELLED",
  "items": [
    {
      "description": "string",
      "quantity": "number",
      "unitPrice": "decimal",
      "amount": "decimal"
    }
  ],
  "issuedDate": "datetime",
  "dueDate": "datetime"
}
```

---

## 6. HR Service (`/hr`)

### Employee Endpoints (`/hr/employees`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/hr/employees` | List all employees (Generic) | Yes |
| GET | `/hr/employees/{id}` | Get employee by ID (Generic) | Yes |
| POST | `/hr/employees` | Create employee (Generic) | Yes (Admin) |
| PUT | `/hr/employees/{id}` | Update employee (Generic) | Yes (Admin) |
| DELETE | `/hr/employees/{id}` | Delete employee (Generic) | Yes (Admin) |
| POST | `/hr/employees/{id}/profile-image` | Upload profile image | Yes (Admin) |
| DELETE | `/hr/employees/{id}/profile-image` | Delete profile image | Yes (Admin) |
| GET | `/hr/employees/me` | Get my employee profile | Yes |
| PUT | `/hr/employees/me` | Update my profile | Yes |
| POST | `/hr/employees/me/profile-image` | Upload my profile image | Yes |
| DELETE | `/hr/employees/me/profile-image` | Delete my profile image | Yes |

### Department Endpoints (`/hr/departments`)

Uses Generic Controller - supports full CRUD operations

### Schedule Endpoints (`/hr/schedules`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/hr/schedules` | List all schedules (Generic) | Yes |
| POST | `/hr/schedules` | Create schedule (Generic) | Yes (Admin) |
| GET | `/hr/schedules/me` | Get my schedules | Yes (Doctor) |
| GET | `/hr/schedules/doctors` | Get all doctor schedules | Yes |
| GET | `/hr/schedules/by-doctor-date` | Get schedule by doctor & date | Yes |
| PATCH | `/hr/schedules/{id}/status` | Update schedule status | Yes (Admin) |
| POST | `/hr/schedules/{id}/cancel` | Cancel schedule | Yes (Admin) |

---

## 7. Medicine Service (`/medicines`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/medicines` | List all medicines (paginated) | Yes |
| POST | `/medicines` | Create medicine (Generic) | Yes (Admin) |
| PUT | `/medicines/{id}` | Update medicine (Generic) | Yes (Admin) |
| PATCH | `/medicines/{id}/stock` | Update stock quantity | Yes (Admin) |
| GET | `/medicines/categories` | List categories | Yes |

---

## 8. Report Service (`/reports`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| GET | `/reports/revenue` | Revenue report | Yes (Admin) |
| GET | `/reports/appointments` | Appointment report | Yes (Admin) |
| GET | `/reports/patients` | Patient report | Yes (Admin) |
| DELETE | `/reports/cache` | Clear report cache | Yes (Admin) |
| GET | `/reports/health` | Service health check | No |

**Query Parameters (all reports):**
- `startDate`: Start date (ISO format)
- `endDate`: End date (ISO format)
- `groupBy`: DAY | WEEK | MONTH | YEAR

---

## 9. Notification Service (`/notifications`)

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/notifications/trigger-followup-job` | Trigger follow-up job | Yes (Admin) |
| POST | `/notifications/test-email` | Send test email | Yes (Admin) |
| GET | `/notifications/health` | Service health check | No |

---

## Generic Controller Pattern

Many services use a `GenericController` base class providing standard CRUD operations:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/{resource}/all` | List all (paginated) |
| GET | `/{resource}/{id}` | Get by ID |
| POST | `/{resource}` | Create |
| PUT | `/{resource}/{id}` | Update |
| DELETE | `/{resource}/{id}` | Delete single |
| DELETE | `/{resource}/bulk` | Bulk delete |

**Services using Generic Controller:**
- HR Service (Employees, Departments, Schedules)
- Medicine Service (Medicines, Categories)
- Patient Service (base operations)

---

## Common Patterns

### Authentication Headers
```
Authorization: Bearer <JWT_TOKEN>
```

### Pagination Parameters
```
?page=0&size=20&sort=id,desc
```

### Common Response Structures

**Success Response:**
```json
{
  "data": {...},
  "message": "Success",
  "timestamp": "2026-01-03T12:00:00Z"
}
```

**Error Response:**
```json
{
  "error": "Error message",
  "status": 400,
  "timestamp": "2026-01-03T12:00:00Z",
  "path": "/api/endpoint"
}
```

**Paginated Response:**
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

---

## Inter-Service Communication

Services communicate via **OpenFeign clients**:

- **Patient Service** → Auth Service (get account info)
- **Appointment Service** → Patient Service, HR Service
- **Medical Exam Service** → Patient Service, Billing Service
- **Billing Service** → Patient Service, Medical Exam Service, HR Service, Appointment Service
- **Notification Service** → Patient Service, Appointment Service, Medical Exam Service
- **Report Service** → Patient Service, Billing Service, Medical Exam Service, Appointment Service

---

## Summary

- **Total Endpoints:** 162+
- **Services:** 13 microservices
- **Authentication:** JWT (OAuth2)
- **API Gateway:** Port 8080
- **Pattern:** RESTful with consistent response formats
- **Pagination:** Spring Data JPA pagination
- **File Upload:** Multipart form data for images
- **Payment Integration:** VNPay for online payments
