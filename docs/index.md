# HMS Project Documentation Index

**Last Generated:** 2026-01-03  
**Project:** Hospital Management System (HMS)  
**Version:** 1.0.0

---

## 📋 Quick Reference

| Aspect | Details |
|--------|---------|
| **Repository Type** | Multi-part (Separate Backend/Frontend) |
| **Backend** | Spring Boot 3.5.8 Microservices (Java 23) |
| **Frontend** | Next.js 16 + React 19 (TypeScript 5) |
| **Architecture** | Microservices + Component-Based |
| **Database** | MySQL 8 (Database per Service) |
| **API Gateway** | http://localhost:8080/api |
| **Frontend URL** | http://localhost:3000 |

---

## 🏗️ Project Structure

### Backend: HMS_Backend/

**Type:** Backend Microservices  
**Language:** Java 23 + Kotlin (build scripts)  
**Framework:** Spring Boot 3.5.8 + Spring Cloud 2025.0.0  
**Build Tool:** Gradle 8.14.3+  
**Root:** [HMS_Backend/](../HMS_Backend/)

**Services (13):**
- config-server (Port 8888)
- discovery-service (Port 8761)  
- api-gateway (Port 8080)
- auth-service (Port 8082)
- patient-service (Port 8083)
- medicine-service (Port 8081)
- appointment-service
- medical-exam-service
- billing-service
- hr-service
- report-service
- notification-service
- common (shared library)

### Frontend: QuanLyBenhVien/

**Type:** Web Application  
**Language:** TypeScript 5  
**Framework:** Next.js 16 + React 19  
**Package Manager:** pnpm  
**Root:** [QuanLyBenhVien/](../QuanLyBenhVien/)

**Portals:**
- Admin Portal (`/admin/*`)
- Doctor Portal (`/doctor/*`)
- Nurse Portal (`/nurse/*`)
- Patient Portal (`/patient/*`)
- Public Pages (`/`, `/login`)

---

## 📚 Generated Documentation

### Core Documentation

- **[Technology Stack](./technology-stack.md)** - Complete tech stack analysis for backend & frontend
- **[Architecture](./architecture.md)** - System architecture, patterns, data flow
- **[API Contracts - Backend](./api-contracts-backend.md)** - 162+ REST API endpoints across all microservices
- **[UI Component Inventory](./ui-component-inventory-frontend.md)** - 100+ React components organized by domain

### Development Resources

- **[Backend README](../HMS_Backend/README.md)** - Installation, setup, running instructions
- **[Frontend README](../QuanLyBenhVien/README.md)** - Comprehensive user guide (1500+ lines)
- **[Project Guide](../QuanLyBenhVien/PROJECT_GUIDE.md)** - Project development guide

### Project Data

- **[Project Scan Report](./project-scan-report.json)** - Automated project analysis metadata
- **[Final Report Structure](../final_report.md)** - Academic report template with chapter breakdown

---

## 🎯 Getting Started

### Backend Setup

```bash
cd HMS_Backend

# 1. Configure environment
cp .env.example .env
# Edit .env with database credentials and service ports

# 2. Build all services
./gradlew clean build

# 3. Start services in order:
./gradlew :config-server:bootRun
./gradlew :discovery-service:bootRun
./gradlew :auth-service:bootRun
./gradlew :patient-service:bootRun
# ... (start other services)
./gradlew :api-gateway:bootRun
```

**Requirements:**
- Java 23
- MySQL 8+
- Docker & Docker Compose (optional)

### Frontend Setup

```bash
cd QuanLyBenhVien

# 1. Install dependencies
pnpm install

# 2. Configure environment
# Create .env.local:
# NEXT_PUBLIC_BE_BASE_URL=http://localhost:8080/api
# NEXT_PUBLIC_USE_MOCK=1  # Use mock data if backend not running

# 3. Start development server
pnpm dev

# Access at http://localhost:3000
```

**Requirements:**
- Node.js 18+ (recommended 20+)
- pnpm

---

## 🔑 Key Features Implemented

### Patient Management
- Patient registration and profiles
- Medical history tracking
- Blood type, allergies, emergency contacts
- Profile image upload

### Appointment System
- Online appointment booking
- Walk-in registration
- Doctor schedule management
- Queue system for waiting patients
- Time slot availability checking

### Medical Examinations
- Vital signs recording (BP, HR, temperature, weight, height)
- Diagnosis and symptoms documentation
- Electronic prescriptions
- Lab test ordering
- Lab result management with image uploads
- Follow-up appointment scheduling

### Billing & Payments
- Automated invoice generation
- VNPay integration for online payments
- Cash payment processing
- Payment history tracking
- Invoice management (generate, cancel, view)

### Human Resources
- Employee management (doctors, nurses, staff)
- Department organization
- Work schedule management
- Profile management with photos

### Medicine Management
- Medicine catalog
- Stock level tracking
- Category organization
- Prescription integration

### Reports & Analytics
- Revenue reports (daily, weekly, monthly, yearly)
- Appointment statistics
- Patient demographics
- Cached reporting for performance

### Notifications
- Email notifications
- Follow-up reminders
- Scheduled notification jobs

---

## 🏛️ Architecture Highlights

### Microservices Benefits
✅ Independent deployment and scaling  
✅ Technology diversity  
✅ Fault isolation  
✅ Team autonomy  
✅ Database per service pattern

### Frontend Advantages
✅ Server-side rendering (SEO, performance)  
✅ Type-safe with TypeScript  
✅ 100+ reusable components  
✅ Role-based access control  
✅ Responsive design

### Integration Pattern
- REST APIs through API Gateway
- JWT authentication
- Service discovery via Eureka
- Centralized configuration
- OpenFeign for inter-service communication

---

## 🔐 Authentication & Authorization

**Roles:**
- **ADMIN** - Full system access
- **DOCTOR** - Medical exams, prescriptions, appointments
- **NURSE** - Vitals, queue management, assist exams
- **RECEPTIONIST** - Appointments, walk-ins, billing
- **PATIENT** - Own records, appointments, payments

**Security:**
- JWT tokens (access + refresh)
- OAuth2 Resource Server
- Role-based endpoints
- API Gateway authentication filter

---

## 📊 API Overview

**Total Endpoints:** 162+  
**Base URL:** `http://localhost:8080/api`  
**Format:** JSON  
**Auth:** Bearer tokens

**Major API Groups:**
- `/auth` - Authentication (7 endpoints)
- `/patients` - Patient management (12 endpoints)
- `/appointments` - Scheduling & queue (18 endpoints)
- `/exams` - Medical exams, prescriptions, lab (20+ endpoints)
- `/invoices`, `/payments` - Billing (17 endpoints)
- `/hr` - HR management (14 endpoints)
- `/medicines` - Medicine inventory (4 endpoints)
- `/reports` - Analytics (6 endpoints)
- `/notifications` - Notifications (4 endpoints)

See [API Contracts](./api-contracts-backend.md) for complete documentation.

---

## 🎨 UI Component Library

**Total Components:** 110+

**Component Categories:**
- **Base UI (shadcn/ui):** 40+ Radix primitives
- **Appointment:** Scheduling, calendar, queue components
- **Patient:** Profile, search, avatars, badges
- **Medical Exam:** Vitals forms, exam panels
- **Billing:** Invoices, payments, VNPay integration
- **Lab:** Test orders, results, image viewing
- **HR:** Employee, department, schedule management
- **Reports:** Charts, analytics, export
- **Shared:** Filters, tables, actions, forms

See [UI Component Inventory](./ui-component-inventory-frontend.md) for details.

---

## 📦 Database Schema

**Pattern:** Database per Service  
**DBMS:** MySQL 8

**Databases:**
- auth_db - Users, accounts, roles
- patient_db - Patient profiles, medical history
- appointment_db - Appointments, schedules, queue
- exam_db - Medical exams, prescriptions, lab orders/results
- billing_db - Invoices, payments
- medicine_db - Medicines, categories, stock
- hr_db - Employees, departments, schedules
- report_db - Cached reports

---

## 🚀 Deployment

### Development
- Local MySQL instances (Docker Compose per service)
- Gradle bootRun for backend
- pnpm dev for frontend

### Production (Planned)
- Docker containers
- Kubernetes orchestration
- Cloud MySQL
- Load balancing
- Auto-scaling

**Infrastructure Files:**
- Backend Dockerfile: `HMS_Backend/infrastructure/pro/Dockerfile`
- Docker Compose: `HMS_Backend/infrastructure/dev/docker-compose.yml`

---

## 🛠️ Development Tools

### Backend
- **IDE:** IntelliJ IDEA / VS Code
- **Build:** Gradle Kotlin DSL
- **Testing:** JUnit Platform
- **API Testing:** Postman (collection provided)

### Frontend
- **IDE:** VS Code
- **Dev Server:** Next.js dev mode with hot reload
- **Testing:** Playwright E2E tests
- **Mock API:** MSW (Mock Service Worker)
- **Linting:** ESLint + Prettier
- **Type Checking:** TypeScript compiler

---

## 📖 Additional Resources

### Existing Documentation (Ignored per user request)
- HMS_Backend/docs/ - API contracts, data models, backlog
- QuanLyBenhVien/DOCS/ - FE specs, testing, validation reports

### External Links
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Next.js Docs](https://nextjs.org/docs)
- [shadcn/ui](https://ui.shadcn.com/)
- [TanStack Query](https://tanstack.com/query)

---

## 🎓 For Academic Report Writing

This documentation can be used for:

- **Chapter 5 (Tools & Technologies):** Use [technology-stack.md](./technology-stack.md)
- **Chapter 7 (System Architecture):** Use [architecture.md](./architecture.md)  
- **Chapter 8 (Feature Implementation):** Extract from API contracts and UI components
- **Screenshots:** Run the application and capture admin/doctor/patient portals

**Chapters requiring human input:**
- Chapter 1: Project overview, objectives
- Chapter 2: Requirements analysis
- Chapter 3: Project planning
- Chapter 4: Team organization
- Chapter 6: Client meetings ⚠️ CRITICAL
- Chapter 9: Resource allocation
- Chapter 11: Lessons learned

See [final_report.md](../final_report.md) for complete structure with annotations.

---

## 📝 Next Steps for AI-Assisted Development

When planning new features or modifications:

1. **Reference Architecture:** See how services communicate
2. **Check API Contracts:** Identify which endpoints to call
3. **Review Components:** Reuse existing UI components
4. **Follow Patterns:** Maintain consistency with existing code
5. **Update Documentation:** Keep this index current

---

## 🔍 Finding Information

| Need | Look Here |
|------|-----------|
| **API endpoint details** | [api-contracts-backend.md](./api-contracts-backend.md) |
| **UI component usage** | [ui-component-inventory-frontend.md](./ui-component-inventory-frontend.md) |
| **Tech stack details** | [technology-stack.md](./technology-stack.md) |
| **System design** | [architecture.md](./architecture.md) |
| **Setup instructions** | Backend/Frontend README files |
| **Project metadata** | [project-scan-report.json](./project-scan-report.json) |

---

## 📞 Project Information

**Project Name:** Hospital Management System (HMS-total)  
**Type:** Academic Software Engineering Project  
**Course:** SE214 (Software Engineering)  
**Architecture:** Multi-part Microservices + Modern Web Frontend  
**Status:** Active Development

---

**✨ Documentation generated by BMad Analyst Agent**  
**Date:** 2026-01-03  
**Scan Level:** Exhaustive  
**Files Analyzed:** 200+ source files

---

*For questions or clarifications about this project, refer to the individual documentation files listed above or contact the development team.*
