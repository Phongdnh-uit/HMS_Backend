# Technology Stack - HMS System

## Overview

Hospital Management System is built as a **multi-part architecture** with separate backend (microservices) and frontend (web application) components.

---

## Backend Technology Stack (HMS_Backend)

### Core Framework & Language

| Category | Technology | Version | Justification |
|----------|-----------|---------|---------------|
| **Language** | Java | 23 | Latest LTS with modern features, strong type safety |
| **Framework** | Spring Boot | 3.5.8 | Industry-standard microservices framework with extensive ecosystem |
| **Cloud Platform** | Spring Cloud | 2025.0.0 | Microservices orchestration, service discovery, configuration management |

### Architecture Pattern

**Microservices Architecture** - 13 independent services communicating via REST APIs and service discovery

### Microservices Components

| Service | Purpose | Port | Technology Stack |
|---------|---------|------|------------------|
| **config-server** | Centralized configuration | 8888 | Spring Cloud Config Server |
| **discovery-service** | Service registry | 8761 | Netflix Eureka Server |
| **api-gateway** | API routing & aggregation | 8080 | Spring Cloud Gateway |
| **auth-service** | Authentication & authorization | 8082 | Spring Security + OAuth2 + JWT |
| **patient-service** | Patient management | 8083 | Spring Boot + JPA |
| **medicine-service** | Medicine inventory | 8081 | Spring Boot + JPA |
| **hr-service** | Human resources | TBD | Spring Boot + JPA |
| **appointment-service** | Appointment scheduling | TBD | Spring Boot + JPA |
| **medical-exam-service** | Medical examinations | TBD | Spring Boot + JPA |
| **billing-service** | Billing & payments | TBD | Spring Boot + JPA |
| **report-service** | Reporting & analytics | TBD | Spring Boot + JPA |
| **notification-service** | Notifications | TBD | Spring Boot + Messaging |
| **common** | Shared utilities | N/A | Common library module |

### Key Dependencies

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Build Tool** | Gradle | 8.14.3+ | Multi-module build automation |
| **Language** | Kotlin DSL | Latest | Build script language |
| **Service Discovery** | Netflix Eureka Client | Latest | Service registration & discovery |
| **API Client** | OpenFeign | 4.3.0 | Declarative REST client for inter-service communication |
| **Database Driver** | MySQL Connector/J | Latest | MySQL database connectivity |
| **Security** | Spring Security OAuth2 Resource Server | Latest | JWT-based authentication |
| **Configuration** | Spring Cloud Config Client | Latest | Distributed configuration |
| **Object Mapping** | MapStruct | 1.6.3 | DTO-Entity mapping |
| **Code Generation** | Lombok | Latest | Boilerplate code reduction |
| **Monitoring** | Spring Boot Actuator | Latest | Health checks & metrics |
| **Hot Reload** | Spring Boot DevTools | Latest | Development productivity |
| **Container Support** | Docker Compose | Latest | Local development environment |

### Database Architecture

| Service | Database | Type | Purpose |
|---------|----------|------|---------|
| **auth-service** | MySQL | Relational | User credentials, roles, permissions |
| **patient-service** | MySQL | Relational | Patient records, medical history |
| **medicine-service** | MySQL | Relational | Medicine inventory, prescriptions |
| **hr-service** | MySQL | Relational | Employee data, schedules |
| **appointment-service** | MySQL | Relational | Appointments, schedules |
| **medical-exam-service** | MySQL | Relational | Examination records |
| **billing-service** | MySQL | Relational | Invoices, payments |

**Pattern:** Database per Service (each microservice has its own database instance)

### Testing & Quality

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Testing Framework** | JUnit Platform | Unit & integration testing |
| **Test Runner** | Spring Boot Test | Spring context testing |

---

## Frontend Technology Stack (QuanLyBenhVien)

### Core Framework & Language

| Category | Technology | Version | Justification |
|----------|-----------|---------|---------------|
| **Framework** | Next.js | 16.0.5 | Modern React framework with SSR, routing, and optimization |
| **Library** | React | 19.2.0 | Latest React with concurrent features |
| **Language** | TypeScript | 5.x | Type safety, better developer experience |
| **Package Manager** | pnpm | Latest | Fast, disk-efficient package management |

### Architecture Pattern

**Component-Based Architecture** with Role-Based Access Control (RBAC)

### UI & Styling

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **CSS Framework** | Tailwind CSS | 4.x | Utility-first styling |
| **UI Components** | shadcn/ui + Radix UI | Latest | Accessible component library (40+ Radix components) |
| **Icons** | Lucide React | 0.555.0 | Icon library |
| **Theme** | next-themes | 0.4.6 | Dark/light mode support |
| **Styling Utilities** | class-variance-authority | 0.7.1 | Component variant management |
| **CSS Utilities** | clsx + tailwind-merge | Latest | Conditional className utilities |

### State Management & Data Fetching

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Data Fetching** | TanStack Query (React Query) | 5.90.11 | Server state management, caching |
| **HTTP Client** | Axios | 1.13.2 | API requests |
| **Forms** | React Hook Form | 7.67.0 | Form state & validation |
| **Validation** | Zod | 4.1.13 | Schema validation |

### UI Components & Interactions

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **Tables** | TanStack Table | 8.21.3 | Data tables with sorting, filtering, pagination |
| **Date Picker** | React Day Picker | 9.11.2 | Date selection |
| **Date Utils** | date-fns | 4.1.0 | Date formatting & manipulation |
| **Charts** | Recharts | 2.15.2 | Data visualization |
| **Carousel** | Embla Carousel | 8.6.0 | Image/content carousel |
| **Command Palette** | cmdk | 1.1.1 | Command menu |
| **Notifications** | Sonner | 2.0.7 | Toast notifications |
| **Drawers** | Vaul | 1.1.2 | Drawer component |
| **Panels** | React Resizable Panels | 2.1.7 | Resizable layout panels |
| **OTP Input** | input-otp | 1.4.2 | One-time password input |
| **Excel Export** | xlsx | 0.18.5 | Spreadsheet generation |

### Development & Testing

| Category | Technology | Version | Purpose |
|----------|-----------|---------|---------|
| **E2E Testing** | Playwright | 1.57.0 | End-to-end testing |
| **Mock API** | MSW (Mock Service Worker) | 2.2.14 | API mocking for development |
| **Code Quality** | ESLint | 9.x | Linting |
| **Formatter** | Prettier | 3.7.4 | Code formatting |
| **React Compiler** | eslint-plugin-react-compiler | 19.1.0-rc.2 | React compiler support |

### Additional Utilities

| Technology | Purpose |
|-----------|---------|
| **js-cookie** | Cookie management |
| **PostCSS** | CSS processing |

---

## Integration Architecture

### Frontend ↔ Backend Communication

- **Protocol:** REST API over HTTP
- **Base URL:** `http://localhost:8080/api` (via API Gateway)
- **Authentication:** JWT tokens (Bearer authentication)
- **Data Format:** JSON

### Service Discovery Flow

```
Client (Browser) 
  ↓
Next.js Frontend (Port 3000)
  ↓
API Gateway (Port 8080)
  ↓
Eureka Discovery Service (Port 8761)
  ↓
Individual Microservices (Ports 8081-8083+)
  ↓
MySQL Databases (Per-service databases)
```

### Configuration Management

- **Backend:** Spring Cloud Config Server (centralized)
- **Frontend:** Environment variables (.env.local)
- **Deployment:** Docker Compose for local development

---

## Development Environment

### Backend Requirements

- Java 23 (or Docker)
- Gradle 8.14.3+
- MySQL 8.x
- Docker & Docker Compose

### Frontend Requirements

- Node.js 18+ (recommended 20+)
- pnpm/npm/yarn
- Modern browser with ES2017+ support

### Build Tools

| Tool | Purpose | Configuration |
|------|---------|--------------|
| **Gradle Kotlin DSL** | Backend build automation | build.gradle.kts |
| **Next.js** | Frontend build & bundling | next.config.ts |
| **TypeScript** | Type checking | tsconfig.json |
| **Tailwind CSS** | CSS processing | postcss.config.mjs |

---

## Production Considerations

### Backend Deployment

- **Containerization:** Docker support via infrastructure/pro/Dockerfile
- **Orchestration:** Docker Compose configurations per service
- **Configuration:** Externalized via Config Server
- **Monitoring:** Spring Boot Actuator endpoints

### Frontend Deployment

- **Build:** Production build via `pnpm build`
- **SSR:** Server-side rendering via Next.js
- **Static Assets:** Public folder for static files
- **Environment:** Environment-specific .env files

---

## Summary

**Backend:** Modern Java microservices with Spring Boot/Cloud ecosystem  
**Frontend:** Next.js 16 + React 19 with comprehensive UI component library  
**Integration:** RESTful APIs with JWT authentication  
**Architecture:** Microservices (backend) + Component-based (frontend)  
**Database:** MySQL with database-per-service pattern
