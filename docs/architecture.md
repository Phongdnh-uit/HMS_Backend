# Project Architecture - HMS System

## System Overview

HMS (Hospital Management System) is a **multi-part microservices architecture** combining:
- **Backend:** 13 Spring Boot microservices (Java 23)
- **Frontend:** Next.js 16 web application (React 19 + TypeScript)
- **Communication:** REST APIs via API Gateway
- **Authentication:** JWT tokens (OAuth2)

---

## High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                              │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Next.js Frontend (Port 3000)                             │  │
│  │  - React 19 + TypeScript + Tailwind CSS                   │  │
│  │  - Role-based UI (Admin/Doctor/Nurse/Patient)             │  │
│  │  - TanStack Query + Axios                                 │  │
│  └───────────────────┬───────────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────────┘
                         │ REST API (JSON)
                         │ JWT Bearer Auth
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY LAYER                           │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  API Gateway (Port 8080)                                  │  │
│  │  - Spring Cloud Gateway                                   │  │
│  │  - Routing + Load Balancing                               │  │
│  │  - Authentication Filter                                  │  │
│  └───────────────────┬───────────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                 SERVICE DISCOVERY LAYER                          │
│                                                                   │
│  ┌──────────────────┐        ┌──────────────────────────────┐  │
│  │ Eureka Discovery │◄──────►│ Config Server (Port 8888)    │  │
│  │   (Port 8761)    │        │ - Centralized Configuration  │  │
│  └────────┬─────────┘        └──────────────────────────────┘  │
└───────────┼──────────────────────────────────────────────────────┘
            │
            ▼ Service Registry
┌─────────────────────────────────────────────────────────────────┐
│                    MICROSERVICES LAYER                           │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Auth Service │  │Patient Service│  │Appointment Service  │  │
│  │  (Port 8082) │  │  (Port 8083)  │  │  (Port TBD)         │  │
│  └──────┬───────┘  └──────┬────────┘  └─────────┬───────────┘  │
│         │                  │                     │               │
│  ┌──────┴───────┐  ┌──────┴────────┐  ┌─────────┴───────────┐  │
│  │Medical-Exam  │  │Medicine       │  │  HR Service          │  │
│  │  Service     │  │Service(8081)  │  │  (Port TBD)          │  │
│  └──────┬───────┘  └──────┬────────┘  └─────────┬───────────┘  │
│         │                  │                     │               │
│  ┌──────┴───────┐  ┌──────┴────────┐  ┌─────────┴───────────┐  │
│  │Billing       │  │Report Service │  │Notification Service  │  │
│  │  Service     │  │  (Port TBD)   │  │  (Port TBD)          │  │
│  └──────┬───────┘  └───────────────┘  └──────────────────────┘  │
│         │                                                         │
│  ┌──────┴─────────────────────────────────────────────────────┐ │
│  │              Common Library Module                          │ │
│  │  - Shared DTOs, Utils, OpenFeign Clients                   │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────┬───────────────────────────────────────────────────┘
              │
              ▼ Database per Service
┌─────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                  │
│                                                                   │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐   │
│  │Auth DB │  │Patient │  │Appt DB │  │Exam DB │  │Bill DB │   │
│  │(MySQL) │  │  DB    │  │(MySQL) │  │(MySQL) │  │(MySQL) │   │
│  └────────┘  └────────┘  └────────┘  └────────┘  └────────┘   │
│  ┌────────┐  ┌────────┐  ┌────────┐                            │
│  │Med DB  │  │HR DB   │  │Report  │                            │
│  │(MySQL) │  │(MySQL) │  │  DB    │                            │
│  └────────┘  └────────┘  └────────┘                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Architecture Patterns

### Backend: Microservices Architecture

**Pattern:** Domain-Driven Design (DDD) with microservices  
**Communication:** Synchronous REST via OpenFeign + Asynchronous (planned)  
**Data:** Database per Service pattern

**Key Characteristics:**
- ✅ **Service Independence** - Each service owns its domain
- ✅ **Decentralized Data** - No shared databases
- ✅ **Technology Agnostic** - Services can evolve independently
- ✅ **Fault Isolation** - Service failures don't cascade
- ✅ **Scalability** - Scale services independently

**Microservices Responsibilities:**

| Service | Domain | Key Responsibilities |
|---------|--------|---------------------|
| **config-server** | Infrastructure | Centralized configuration management |
| **discovery-service** | Infrastructure | Service registry & discovery |
| **api-gateway** | Infrastructure | Routing, load balancing, security |
| **auth-service** | Security | Authentication, authorization, JWT |
| **patient-service** | Healthcare | Patient records, profiles, medical history |
| **appointment-service** | Healthcare | Scheduling, queue management, walk-ins |
| **medical-exam-service** | Healthcare | Exams, prescriptions, lab orders, results |
| **billing-service** | Finance | Invoices, payments, VNPay integration |
| **hr-service** | Operations | Employees, departments, schedules |
| **medicine-service** | Inventory | Medicine catalog, stock management |
| **report-service** | Analytics | Revenue, appointment, patient reports |
| **notification-service** | Communication | Email notifications, follow-ups |
| **common** | Shared | DTOs, utilities, Feign clients |

### Frontend: Component-Based Architecture

**Pattern:** Next.js App Router + React Server Components  
**State Management:** TanStack Query (server state) + React Context (app state)  
**Routing:** File-based routing with role-based access control

**Key Characteristics:**
- ✅ **Component Reusability** - 100+ reusable components
- ✅ **Role-Based UI** - Different views per user role
- ✅ **Server-Side Rendering** - SEO, performance
- ✅ **Type Safety** - Full TypeScript coverage
- ✅ **Responsive Design** - Mobile-first approach

**Frontend Structure:**
```
app/
├── (auth)/          # Public authentication pages
├── admin/           # Admin portal (full access)
├── doctor/          # Doctor portal (exams, prescriptions)
├── nurse/           # Nurse portal (vitals, queue)
├── patient/         # Patient portal (appointments, billing)
└── payment/         # Payment processing pages
```

---

## Data Flow Patterns

### 1. User Authentication Flow
```
User → Frontend → API Gateway → Auth Service → JWT Token
     ← Frontend ← API Gateway ← Auth Service ← Database
```

### 2. Appointment Creation Flow
```
User → Frontend → API Gateway → Appointment Service
                                        ↓
                               Check Doctor Schedule
                                        ↓ (Feign Client)
                                  HR Service
                                        ↓
                               Create Appointment
                                        ↓
                                  MySQL Database
```

### 3. Medical Exam with Billing Flow
```
Doctor → Medical Exam Service → Create Exam
                ↓
         Create Prescription
                ↓
         Lab Orders (optional)
                ↓ (Event/Async call)
         Billing Service → Generate Invoice
                ↓
         Patient → Payment (VNPay/Cash)
```

---

## Inter-Service Communication

### Synchronous Communication (OpenFeign)

**Pattern:** REST API calls between services

**Common Feign Client Examples:**

```java
// In medical-exam-service
@FeignClient(name = "billing-service")
public interface BillingClient {
    @PostMapping("/invoices/upsert")
    InvoiceResponse upsertInvoice(InvoiceRequest request);
}

// In appointment-service
@FeignClient(name = "hr-service")
public interface HrClient {
    @GetMapping("/hr/schedules/by-doctor-date")
    ScheduleResponse getSchedule(Long doctorId, LocalDate date);
}
```

**Service Dependencies:**
- Medical Exam → Patient, Billing
- Appointment → Patient, HR
- Billing → Patient, Medical Exam, HR, Appointment
- Notification → Patient, Appointment, Medical Exam
- Report → All data services

### Asynchronous Communication (Planned)

**Pattern:** Event-driven architecture (not yet implemented)

**Future Integration:**
- Message Queue (RabbitMQ / Kafka)
- Event Bus for notifications
- Async billing updates
- Real-time queue updates

---

## Security Architecture

### Authentication & Authorization

**Strategy:** JWT-based OAuth2 Resource Server

```
1. User Login → Auth Service generates JWT (access + refresh tokens)
2. Frontend stores tokens (HTTP-only cookies or secure storage)
3. Every API request includes: Authorization: Bearer <access_token>
4. API Gateway validates token
5. Services trust gateway (no token revalidation)
```

**Role-Based Access Control (RBAC):**

| Role | Permissions |
|------|-------------|
| **ADMIN** | Full system access, user management, reports |
| **DOCTOR** | View patients, create exams, prescriptions, lab orders |
| **NURSE** | View appointments, enter vitals, assist exams |
| **RECEPTIONIST** | Manage appointments, walk-ins, billing |
| **PATIENT** | View own records, book appointments, pay bills |

**Security Layers:**
1. **API Gateway** - First line auth check
2. **Service Level** - Spring Security with @PreAuthorize
3. **Method Level** - Fine-grained access control
4. **Data Level** - Row-level security (patients see only their data)

---

## Scalability Considerations

### Horizontal Scaling
- **Stateless Services** - All services are stateless (can scale horizontally)
- **Load Balancing** - API Gateway distributes load
- **Service Discovery** - Eureka manages dynamic service instances

### Caching Strategy
- **Report Service** - Caches aggregated reports
- **Frontend** - TanStack Query caches API responses
- **Database** - MySQL query caching (future: Redis)

### Database Optimization
- **Indexing** - Primary keys, foreign keys, frequently queried fields
- **Connection Pooling** - HikariCP connection pool
- **Read Replicas** - Future: Separate read/write databases

---

## Deployment Architecture

### Local Development
```
Docker Compose (per service MySQL instances)
     ↓
Gradle bootRun (backend services)
     ↓
pnpm dev (frontend)
```

### Production (Planned)
```
Kubernetes Cluster
     ↓
Docker Containers (per service)
     ↓
Load Balancer → API Gateway
     ↓
Microservices (auto-scaling)
     ↓
Cloud MySQL (managed database)
```

**Infrastructure:**
- **Backend:** Dockerfile provided (`infrastructure/pro/Dockerfile`)
- **Frontend:** Next.js build + Node.js server
- **Database:** MySQL 8+ with automated backups
- **Monitoring:** Spring Boot Actuator endpoints

---

## Technology Stack Summary

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Frontend** | Next.js 16 + React 19 | Web UI with SSR |
| **API Gateway** | Spring Cloud Gateway | Routing & security |
| **Service Discovery** | Netflix Eureka | Service registry |
| **Configuration** | Spring Cloud Config | Centralized config |
| **Microservices** | Spring Boot 3.5.8 | Business logic |
| **ORM** | Spring Data JPA | Database access |
| **Database** | MySQL 8 | Persistent storage |
| **Authentication** | Spring Security + JWT | Security |
| **API Clients** | OpenFeign | Inter-service calls |
| **Monitoring** | Spring Actuator | Health checks |
| **Build** | Gradle 8.14.3 | Backend builds |
| **Package Manager** | pnpm | Frontend dependencies |

---

## Design Principles

1. **Separation of Concerns** - Clear boundaries between services and layers
2. **Single Responsibility** - Each service/component has one job
3. **Don't Repeat Yourself (DRY)** - Common module for shared code
4. **API-First Design** - Well-defined contracts between services
5. **Fail Fast** - Validate early, throw meaningful errors
6. **Eventual Consistency** - Accept temporary data inconsistency for availability
7. **Observability** - Comprehensive logging and monitoring

---

## Future Architectural Enhancements

1. **Event-Driven Architecture** - Add message queue for async operations
2. **API Gateway Caching** - Cache frequently accessed data
3. **Service Mesh** - Istio for advanced traffic management
4. **Distributed Tracing** - Spring Cloud Sleuth + Zipkin
5. **Circuit Breaker** - Resilience4j for fault tolerance
6. **Centralized Logging** - ELK Stack (Elasticsearch, Logstash, Kibana)
7. **Redis Caching** - Distributed caching layer
8. **GraphQL Gateway** - Alternative to REST for frontend
9. **Websockets** - Real-time notifications and chat
10. **Mobile Apps** - React Native apps using same backend

---

## Summary

**Architecture Type:** Microservices (Backend) + Component-Based (Frontend)  
**Communication:** REST APIs (Synchronous)  
**Data Strategy:** Database per Service  
**Scalability:** Horizontal scaling via service instances  
**Security:** JWT-based OAuth2 with RBAC  
**Deployment:** Containerized with Docker, orchestrated with Kubernetes (planned)
