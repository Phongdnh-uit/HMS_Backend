# Project Overview

**Project Name:** Hospital Management System (HMS)  
**Project Type:** Full-Stack Web Application (Microservices + SPA)  
**Academic Context:** Software Engineering Course (SE214)  
**Last Updated:** 2026-01-03

---

## Executive Summary

The **Hospital Management System (HMS)** is a comprehensive digital solution designed to modernize hospital operations through automation and integrated workflows. The system manages patient records, appointment scheduling, medical examinations, prescriptions, laboratory tests, billing, human resources, and reporting through a microservices-based backend and a responsive web frontend.

**Key Value Propositions:**
- ✅ **Digitalized Patient Records** - Electronic health records with complete medical history
- ✅ **Efficient Appointment System** - Online booking + walk-in queue management
- ✅ **Integrated Medical Workflow** - Seamless exam → prescription → lab → billing flow
- ✅ **Automated Billing** - Invoice generation with VNPay online payment integration
- ✅ **Real-time Reports** - Revenue and operational analytics
- ✅ **Role-Based Access** - Secure access control for Admin, Doctors, Nurses, Receptionists, Patients

---

## Project Structure

### Multi-Part Architecture

The HMS project consists of **two main parts**:

#### 1. Backend (HMS_Backend)
- **Type:** Microservices Architecture
- **Language:** Java 23
- **Framework:** Spring Boot 3.5.8 + Spring Cloud 2025.0.0
- **Build Tool:** Gradle 8.14.3 (Kotlin DSL)
- **Services:** 13 microservices
- **Database:** MySQL 8+ (database-per-service pattern)
- **API Gateway:** Spring Cloud Gateway (port 8080)
- **Service Discovery:** Netflix Eureka (port 8761)

#### 2. Frontend (QuanLyBenhVien)
- **Type:** Web Application
- **Language:** TypeScript 5
- **Framework:** Next.js 16 + React 19
- **UI Library:** shadcn/ui + Radix UI (40+ components)
- **Styling:** Tailwind CSS 4
- **State Management:** TanStack Query + React Context
- **Package Manager:** pnpm

---

## Domain Model

### Core Domains

The system is organized around these key domains:

#### 1. **Authentication & Authorization**
- User account management
- Role-based access control (ADMIN, DOCTOR, NURSE, RECEPTIONIST, PATIENT)
- JWT token authentication with refresh token rotation

#### 2. **Patient Management**
- Patient registration and profiles
- Medical history tracking
- Blood type, allergies, emergency contacts
- Profile image management

#### 3. **Appointment Management**
- Online appointment booking
- Walk-in patient registration
- Queue management with priority system
- Doctor schedule integration

#### 4. **Medical Examination**
- Vital signs recording (BP, temperature, heart rate, weight, height)
- Diagnosis and symptoms documentation
- Electronic prescriptions
- Follow-up appointment scheduling

#### 5. **Laboratory Management**
- Lab test ordering
- Test result recording
- Diagnostic image upload (X-ray, CT, MRI)
- Test catalog management

#### 6. **Billing & Payments**
- Automated invoice generation
- VNPay online payment integration
- Cash payment processing
- Payment history tracking
- Invoice management (generate, view, cancel)

#### 7. **Medicine Management**
- Medicine catalog
- Stock level tracking
- Category organization
- Prescription integration

#### 8. **Human Resources**
- Employee management (doctors, nurses, admin, receptionists)
- Department organization
- Work schedule management
- Profile management with photos

#### 9. **Reports & Analytics**
- Revenue reports (daily, weekly, monthly, yearly)
- Appointment statistics
- Patient demographics
- Cached reporting for performance

#### 10. **Notifications**
- Email notifications
- Follow-up appointment reminders
- Scheduled notification jobs

---

## User Roles & Permissions

### 1. Admin
**Responsibilities:**
- System configuration
- User account management
- Access to all features
- Report generation

**Key Features:**
- Employee management (create, update, delete)
- Department management
- System-wide reports
- Medicine catalog management

### 2. Doctor
**Responsibilities:**
- Patient examination
- Medical diagnosis
- Prescription writing
- Lab test ordering

**Key Features:**
- View appointments
- Conduct medical exams
- Create prescriptions
- Order lab tests
- View patient medical history
- Schedule follow-ups

### 3. Nurse
**Responsibilities:**
- Patient intake
- Vital signs recording
- Assist doctors
- Queue management

**Key Features:**
- Record vitals
- Manage waiting queue
- Update patient information
- View appointments

### 4. Receptionist
**Responsibilities:**
- Appointment scheduling
- Walk-in registration
- Billing and payments

**Key Features:**
- Create/cancel appointments
- Register walk-in patients
- Generate invoices
- Process payments
- Manage queue

### 5. Patient
**Responsibilities:**
- Manage own appointments
- View medical records
- Make payments

**Key Features:**
- Book appointments online
- View appointment history
- Access medical exam records
- View and pay invoices
- Update profile

---

## Technical Architecture

### Backend Architecture

**Pattern:** Microservices with Domain-Driven Design (DDD)

**Infrastructure Services:**
1. **Config Server (port 8888)** - Centralized configuration management
2. **Discovery Service (port 8761)** - Service registry (Netflix Eureka)
3. **API Gateway (port 8080)** - Entry point, routing, authentication

**Business Services:**
4. **Auth Service (port 8082)** - Authentication & authorization
5. **Patient Service (port 8083)** - Patient management
6. **Medicine Service (port 8081)** - Medicine catalog & inventory
7. **Appointment Service** - Appointment scheduling & queue
8. **Medical Exam Service** - Examinations, prescriptions, lab tests
9. **Billing Service** - Invoicing & payments
10. **HR Service** - Employee & department management
11. **Report Service** - Analytics & reporting
12. **Notification Service** - Email notifications

**Shared Module:**
13. **Common** - Shared DTOs, enums, utilities

**Communication:**
- REST APIs over HTTP
- OpenFeign for inter-service communication
- JSON data format
- JWT Bearer token authentication

**Data Architecture:**
- Database-per-service pattern
- MySQL 8+ for all services
- JPA/Hibernate ORM
- Denormalized snapshots for cross-service data

### Frontend Architecture

**Pattern:** Component-Based with File-Based Routing

**Structure:**
```
app/
├── (auth)/          # Public authentication pages
│   ├── login/
│   └── register/
├── admin/           # Admin portal
├── doctor/          # Doctor portal
├── nurse/           # Nurse portal
├── patient/         # Patient portal
└── layout.tsx       # Root layout

components/
├── ui/              # Base shadcn/ui components
├── appointment/     # Appointment components
├── patients/        # Patient components
├── medical-exam/    # Medical exam components
├── billing/         # Billing components
├── lab/             # Lab components
├── hr/              # HR components
├── reports/         # Report components
└── shared/          # Shared utilities
```

**State Management:**
- **Server State:** TanStack Query (API data caching, refetching)
- **App State:** React Context (AuthContext, theme, etc.)
- **Form State:** React Hook Form + Zod validation

**API Integration:**
- Axios HTTP client
- Base URL: `http://localhost:8080/api`
- JWT token interceptors
- Error handling middleware

---

## Key Features & Workflows

### 1. Patient Registration Flow

```
User Registration → Account Creation (auth-service)
                 ↓
         Create Patient Profile (patient-service)
                 ↓
         Complete Profile (demographics, emergency contact)
                 ↓
         Ready to Book Appointments
```

### 2. Appointment Booking Flow

**Online Booking:**
```
Patient selects doctor + date/time → Check availability (appointment-service)
                                  ↓
                          Create appointment (PENDING)
                                  ↓
                          Receptionist confirms (CONFIRMED)
                                  ↓
                          Patient arrives (WAITING)
                                  ↓
                          Doctor starts exam (IN_PROGRESS)
```

**Walk-In:**
```
Receptionist registers walk-in → Create appointment (type: WALK_IN)
                               ↓
                       Assign queue number + priority
                               ↓
                       Patient waits (WAITING)
```

### 3. Medical Examination Flow

```
Doctor selects appointment → Record vitals (nurse or doctor)
                          ↓
                  Enter diagnosis + symptoms
                          ↓
              ┌───────────┴───────────┐
              ↓                       ↓
      Create Prescription      Order Lab Tests
              ↓                       ↓
    Prescription Items         Lab Order Created
    (medicine stock reserved)        ↓
              ↓               Lab results entered
              ↓                       ↓
      Pharmacy dispenses      Upload diagnostic images
              ↓                       ↓
              └───────────┬───────────┘
                          ↓
                  Exam completed
                          ↓
              Generate Invoice (billing-service)
```

### 4. Billing Flow

```
Medical Exam Completed → Check hasPrescription flag
                      ↓
              ┌───────┴───────┐
              ↓               ↓
      Has Prescription    No Prescription
              ↓               ↓
    Wait for dispensed  Generate immediately
              ↓               ↓
    Prescription dispensed → Generate Invoice
                          ↓
                  Create invoice items:
                  - Exam fee
                  - Medicine costs
                  - Lab test costs
                          ↓
                  Patient pays (cash or VNPay)
                          ↓
                  Update payment status
```

### 5. Prescription Lifecycle

```
ACTIVE → Doctor creates prescription
         (medicine stock reserved via medicine-service)
         ↓
    ┌────┴────┐
    ↓         ↓
CANCELLED  DISPENSED → Pharmacy gives medicines to patient
    ↓         ↓         (stock deducted)
Stock    Generate       ↓
Restored  Invoice   Terminal State
```

---

## Technology Stack Summary

### Backend Technologies

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 23 |
| Framework | Spring Boot | 3.5.8 |
| Cloud | Spring Cloud | 2025.0.0 |
| Build | Gradle (Kotlin DSL) | 8.14.3 |
| Database | MySQL | 8+ |
| ORM | Spring Data JPA + Hibernate | - |
| Service Discovery | Netflix Eureka | - |
| API Gateway | Spring Cloud Gateway | - |
| Security | Spring Security OAuth2 | - |
| Authentication | JWT Tokens | - |
| REST Client | OpenFeign | 4.3.0 |
| Mapping | MapStruct | 1.6.3 |
| Utilities | Lombok | - |

### Frontend Technologies

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | TypeScript | 5 |
| Framework | Next.js | 16.0.5 |
| UI Library | React | 19.2.0 |
| Component Library | shadcn/ui + Radix UI | - |
| Styling | Tailwind CSS | 4 |
| State (Server) | TanStack Query | 5.90.11 |
| Forms | React Hook Form | 7.67.0 |
| Validation | Zod | 4.1.13 |
| HTTP Client | Axios | 1.13.2 |
| Charts | Recharts | 2.15.2 |
| Icons | Lucide React | - |
| Testing | Playwright | 1.57.0 |
| Mock API | MSW | 2.2.14 |
| Package Manager | pnpm | - |

---

## Project Statistics

### Backend

- **Microservices:** 13 (11 business + 2 infrastructure)
- **API Endpoints:** 162+
- **Entities:** 17 (across 7 databases)
- **Lines of Code:** ~50,000+ (estimated)
- **Dependencies:** 20+ Spring libraries, 10+ third-party

### Frontend

- **Pages/Routes:** 30+
- **Components:** 110+ (40 base UI + 70 feature)
- **Contexts:** 3 (Auth, Theme, App)
- **Services:** 10+ (API service layers)
- **Lines of Code:** ~30,000+ (estimated)
- **Dependencies:** 40+ npm packages

### Total Project

- **Total Lines:** ~80,000+
- **Configuration Files:** 50+
- **Documentation Files:** 40+
- **Test Files:** In development

---

## Development Methodology

### Version Control

- **System:** Git
- **Branching Strategy:** Feature branches (assumed)
- **Repository Structure:** Monorepo (backend + frontend in same root)

### Code Quality

**Backend:**
- Lombok for boilerplate reduction
- MapStruct for DTO mapping
- Spring Boot validation annotations
- JPA auditing for all entities

**Frontend:**
- TypeScript strict mode
- ESLint + Prettier
- Zod schema validation
- Component-based architecture

### Testing Strategy

**Backend:**
- JUnit Platform (configured)
- Integration tests (planned)
- Postman API testing (collection provided)

**Frontend:**
- Playwright E2E tests
- MSW for API mocking
- Component testing (planned)

---

## Current Status

### Completed Features

✅ Authentication & Authorization (JWT + refresh tokens)  
✅ Patient Management (CRUD, profile images)  
✅ Appointment Scheduling (online + walk-in)  
✅ Queue Management (priority system)  
✅ Medical Examinations (vitals, diagnosis, prescriptions)  
✅ Lab Test Management (orders, results, images)  
✅ Billing & Invoicing (automated generation)  
✅ VNPay Payment Integration  
✅ Medicine Inventory  
✅ HR Management (employees, departments, schedules)  
✅ Reporting (revenue, appointments, patients)  
✅ Email Notifications (follow-ups)  
✅ Role-Based Access Control (5 roles)  
✅ API Gateway with routing  
✅ Service Discovery (Eureka)  
✅ Responsive Web UI (all portals)

### In Progress

🔄 Comprehensive testing (E2E, integration)  
🔄 Production deployment configuration  
🔄 Database migration scripts (Flyway/Liquibase)  
🔄 Docker Compose orchestration  
🔄 CI/CD pipeline

### Planned Enhancements

📋 Event-driven architecture (message queues)  
📋 Service mesh (Istio)  
📋 Distributed tracing (Zipkin)  
📋 Caching layer (Redis)  
📋 API rate limiting  
📋 Advanced analytics dashboard  
📋 Mobile application (React Native)  
📋 Telehealth integration  
📋 DICOM image viewer

---

## Business Value

### For Hospitals

- **Operational Efficiency:** Reduced paperwork, faster patient processing
- **Data Accuracy:** Centralized digital records eliminate duplication
- **Revenue Tracking:** Real-time financial reports
- **Resource Management:** Optimized doctor schedules and queue management
- **Compliance:** Audit trails for all operations

### For Patients

- **Convenience:** Online appointment booking
- **Transparency:** Access to own medical records and billing
- **Payment Flexibility:** Cash or online payment options
- **Better Care:** Complete medical history available to doctors

### For Medical Staff

- **Productivity:** Streamlined workflows, less manual data entry
- **Information Access:** Quick access to patient history, lab results
- **Collaboration:** Integrated workflow between doctors, nurses, pharmacists
- **Decision Support:** Complete patient information at point of care

---

## Scalability Considerations

### Horizontal Scaling

- Microservices can scale independently
- Stateless services (JWT authentication)
- Load balancing via Kubernetes

### Database Scaling

- Database-per-service allows independent scaling
- Read replicas for reporting service
- Caching layer (Redis) for frequently accessed data

### Performance Optimization

- API Gateway caching
- Database query optimization (indexes, DTO projections)
- Frontend: Code splitting, image optimization, CDN
- TanStack Query for client-side caching

---

## Security Features

### Authentication

- JWT access tokens (1 hour expiry)
- Refresh tokens (7 day expiry, rotation on use)
- BCrypt password hashing
- Email verification (implemented)

### Authorization

- Role-based access control (RBAC)
- Role-specific UI components (RoleGuard)
- API endpoint protection (Spring Security)
- Row-level security (createdBy, updatedBy audit fields)

### Data Protection

- HTTPS (production)
- Environment variable secrets
- Database connection pooling (HikariCP)
- Input validation (Zod frontend, Jakarta Validation backend)

---

## Documentation

### Generated Documentation

- [Technology Stack](./technology-stack.md)
- [Architecture](./architecture.md)
- [API Contracts (Backend)](./api-contracts-backend.md)
- [UI Component Inventory](./ui-component-inventory-frontend.md)
- [Data Models](./data-models.md)
- [Development Guide](./development-guide.md)
- [Master Index](./index.md)

### Existing Documentation

- Backend README: `HMS_Backend/README.md`
- Frontend README: `QuanLyBenhVien/README.md`
- Frontend Project Guide: `QuanLyBenhVien/PROJECT_GUIDE.md`
- API Testing: Postman collection
- Existing docs in `HMS_Backend/docs/` and `QuanLyBenhVien/DOCS/`

---

## Getting Started

### Quick Start (Development)

**Prerequisites:** Java 23, MySQL 8, Node.js 20, pnpm

```bash
# 1. Clone repository
git clone <repo-url>
cd HMS-total

# 2. Setup backend
cd HMS_Backend
cp .env.example .env
# Edit .env with database credentials
./gradlew clean build

# Start services (in order):
./gradlew :config-server:bootRun
./gradlew :discovery-service:bootRun
./gradlew :auth-service:bootRun
# ... (start other services)
./gradlew :api-gateway:bootRun

# 3. Setup frontend
cd ../QuanLyBenhVien
pnpm install
echo "NEXT_PUBLIC_BE_BASE_URL=http://localhost:8080/api" > .env.local
pnpm dev

# Access:
# Frontend: http://localhost:3000
# API Gateway: http://localhost:8080/api
# Eureka: http://localhost:8761
```

See [Development Guide](./development-guide.md) for detailed instructions.

---

## Contact & Support

**Project Type:** Academic Software Engineering Project  
**Course:** SE214  
**Institution:** [University Name]

**Documentation:** See `docs/` folder for comprehensive guides  
**Issues:** Check existing documentation and troubleshooting guides  
**API Testing:** Use Postman collection in `HMS_Backend/postman/`

---

## License

[Specify License - MIT, Apache 2.0, Proprietary, etc.]

---

**Document Version:** 1.0  
**Generated:** 2026-01-03  
**Maintained By:** HMS Development Team
