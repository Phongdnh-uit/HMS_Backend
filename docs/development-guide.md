# Development & Deployment Guide

**Project:** Hospital Management System (HMS)  
**Last Updated:** 2026-01-03

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Backend Development Setup](#backend-development-setup)
3. [Frontend Development Setup](#frontend-development-setup)
4. [Running the Application](#running-the-application)
5. [Testing](#testing)
6. [Build & Package](#build--package)
7. [Deployment](#deployment)
8. [Environment Configuration](#environment-configuration)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Backend Requirements

- **Java:** JDK 23 or higher
- **Gradle:** 8.14.3+ (wrapper included)
- **MySQL:** 8.0 or higher
- **Docker:** (Optional) For containerized databases
- **Git:** For version control

### Frontend Requirements

- **Node.js:** 18+ (recommended 20+)
- **pnpm:** Package manager (install via `npm install -g pnpm`)
- **Git:** For version control

### Recommended Tools

- **IDE:** IntelliJ IDEA (backend), VS Code (frontend)
- **API Client:** Postman (collection provided in `HMS_Backend/postman/`)
- **Database Client:** MySQL Workbench, DBeaver, or similar

---

## Backend Development Setup

### 1. Clone Repository

```bash
git clone <repository-url>
cd HMS-total/HMS_Backend
```

### 2. Database Setup

**Option A: Docker Compose (Recommended)**

Each service has its own Docker Compose file for development:

```bash
# Example for patient-service
cd patient-service/infrastructure/dev
docker-compose up -d

# Repeat for each service or use root-level docker-compose (if exists)
```

**Option B: Manual MySQL Setup**

Create databases for each service:

```sql
CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE patient_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE appointment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE exam_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE billing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE hr_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE medicine_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional)
CREATE USER 'hms_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON *.* TO 'hms_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Environment

Create `.env` file in `HMS_Backend/` root:

```bash
cp .env.example .env
```

Edit `.env` with your configuration:

```properties
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# JWT Configuration
JWT_SECRET=your-secret-key-min-256-bits
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# Service Ports
CONFIG_SERVER_PORT=8888
DISCOVERY_SERVER_PORT=8761
API_GATEWAY_PORT=8080
AUTH_SERVICE_PORT=8082
PATIENT_SERVICE_PORT=8083
MEDICINE_SERVICE_PORT=8081
# ... (configure other service ports)

# VNPay Integration (Billing)
VNPAY_TMN_CODE=your_vnpay_code
VNPAY_HASH_SECRET=your_vnpay_secret
VNPAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:3000/payment/callback

# Email Configuration (Notification Service)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 4. Build All Services

```bash
# From HMS_Backend root
./gradlew clean build

# Or build specific service
./gradlew :patient-service:build
```

### 5. Run Services in Order

**Start Order is Critical:**

```bash
# 1. Config Server (port 8888)
./gradlew :config-server:bootRun

# 2. Discovery Service (port 8761)
./gradlew :discovery-service:bootRun

# Wait ~30 seconds for Eureka to start, then start business services:

# 3. Auth Service (port 8082)
./gradlew :auth-service:bootRun

# 4. Patient Service (port 8083)
./gradlew :patient-service:bootRun

# 5. Medicine Service (port 8081)
./gradlew :medicine-service:bootRun

# 6. HR Service
./gradlew :hr-service:bootRun

# 7. Appointment Service
./gradlew :appointment-service:bootRun

# 8. Medical Exam Service
./gradlew :medical-exam-service:bootRun

# 9. Billing Service
./gradlew :billing-service:bootRun

# 10. Report Service
./gradlew :report-service:bootRun

# 11. Notification Service
./gradlew :notification-service:bootRun

# 12. API Gateway (port 8080) - Start LAST
./gradlew :api-gateway:bootRun
```

**Verify Services:**
- Eureka Dashboard: http://localhost:8761
- API Gateway Health: http://localhost:8080/actuator/health

### 6. Seed Test Data (Optional)

```powershell
# PowerShell script for Windows
.\seed-data.ps1

# Or manually via Postman collection
```

---

## Frontend Development Setup

### 1. Navigate to Frontend

```bash
cd QuanLyBenhVien
```

### 2. Install Dependencies

```bash
pnpm install
```

### 3. Configure Environment

Create `.env.local`:

```bash
# Backend API Base URL
NEXT_PUBLIC_BE_BASE_URL=http://localhost:8080/api

# Feature Flags
NEXT_PUBLIC_USE_MOCK=0  # Set to 1 for mock data mode (no backend needed)

# Optional: Analytics, etc.
# NEXT_PUBLIC_ANALYTICS_ID=your-analytics-id
```

### 4. Run Development Server

```bash
pnpm dev
```

Frontend runs on: http://localhost:3000

---

## Running the Application

### Full Stack Development

**Terminal 1: Backend Services**
```bash
cd HMS_Backend

# Option A: Run all services individually (see section 5 above)

# Option B: Use tmux/screen for multiple terminals
# Or use IDE run configurations to start multiple services
```

**Terminal 2: Frontend**
```bash
cd QuanLyBenhVien
pnpm dev
```

**Access Points:**
- **Frontend:** http://localhost:3000
- **API Gateway:** http://localhost:8080/api
- **Eureka Dashboard:** http://localhost:8761
- **Individual Service Actuators:** http://localhost:{port}/actuator

### Mock Mode (Frontend Only)

```bash
cd QuanLyBenhVien

# Enable mock mode
echo "NEXT_PUBLIC_USE_MOCK=1" > .env.local

pnpm dev
```

Frontend runs with MSW (Mock Service Worker) intercepting API calls.

---

## Testing

### Backend Tests

```bash
cd HMS_Backend

# Run all tests
./gradlew test

# Run specific service tests
./gradlew :patient-service:test

# Generate test reports
./gradlew test --tests '*PatientServiceTest'

# Test reports location:
# build/reports/tests/test/index.html
```

### Frontend Tests

```bash
cd QuanLyBenhVien

# Run Playwright E2E tests
pnpm test:e2e

# Run in UI mode
pnpm test:e2e:ui

# Run specific test file
pnpm playwright test tests/login.spec.ts

# Test reports:
# playwright-report/index.html
```

### API Testing

Use Postman collection:

```bash
# Import collection
HMS_Backend/HMS_Backend_API_Collection.postman_collection.json

# Or use test script
cd HMS_Backend
./test-all-endpoints.sh  # Linux/Mac
```

---

## Build & Package

### Backend Production Build

```bash
cd HMS_Backend

# Build all services (skip tests for speed)
./gradlew clean build -x test

# Build JARs location:
# <service-name>/build/libs/<service-name>-0.0.1-SNAPSHOT.jar

# Build Docker images (if Dockerfile exists)
cd <service-name>
docker build -f infrastructure/pro/Dockerfile -t hms-<service-name>:latest .
```

### Frontend Production Build

```bash
cd QuanLyBenhVien

# Production build
pnpm build

# Build output: .next/ folder

# Start production server locally
pnpm start

# Runs on http://localhost:3000 (production mode)
```

---

## Deployment

### Development Deployment (Local)

See "Running the Application" section above.

### Production Deployment (Planned)

**Current Status:** Infrastructure in progress

**Planned Architecture:**

#### Backend Deployment

```
Kubernetes Cluster
├── Config Server (Deployment, Service)
├── Discovery Service (Deployment, Service)
├── API Gateway (Deployment, Service, Ingress)
├── Auth Service (Deployment, Service)
├── Patient Service (Deployment, Service)
├── ... (other microservices)
└── MySQL Databases (StatefulSet or Cloud Managed)
```

**Docker Images:**
- Base images using multi-stage builds
- Dockerfiles in `infrastructure/pro/` per service
- Push to container registry (Docker Hub, AWS ECR, etc.)

**Kubernetes Resources:**
- Deployments for each service
- Services for internal communication
- Ingress for API Gateway
- ConfigMaps for non-sensitive config
- Secrets for credentials (JWT, DB passwords)
- HorizontalPodAutoscaler for scaling

#### Frontend Deployment

**Option A: Vercel (Recommended for Next.js)**
```bash
cd QuanLyBenhVien
vercel --prod
```

**Option B: Docker + Nginx**
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN npm install -g pnpm && pnpm install
COPY . .
RUN pnpm build

FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/package.json ./
RUN npm install -g pnpm && pnpm install --prod
EXPOSE 3000
CMD ["pnpm", "start"]
```

**Option C: Static Export (if applicable)**
```bash
pnpm build
# Deploy .next/static to CDN
```

### Environment Variables (Production)

**Backend:**
- Use Kubernetes Secrets for sensitive data
- ConfigMaps for non-sensitive config
- External secret management (AWS Secrets Manager, HashiCorp Vault)

**Frontend:**
- Build-time environment variables (NEXT_PUBLIC_*)
- Runtime configuration via API

### Database Migration

**Strategy:** Schema migrations via Flyway/Liquibase

**Execution:**
1. Backup databases before migration
2. Run migration scripts during deployment
3. Verify schema version
4. Rollback plan if needed

### CI/CD Pipeline (Suggested)

```yaml
# .github/workflows/deploy.yml example

name: Deploy HMS

on:
  push:
    branches: [main]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Java 23
        uses: actions/setup-java@v3
        with:
          java-version: '23'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Build Docker images
        run: |
          # Build and push images
      - name: Deploy to Kubernetes
        run: kubectl apply -f k8s/

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup Node
        uses: actions/setup-node@v3
        with:
          node-version: '20'
      - name: Install pnpm
        run: npm install -g pnpm
      - name: Install dependencies
        run: cd QuanLyBenhVien && pnpm install
      - name: Build
        run: cd QuanLyBenhVien && pnpm build
      - name: Deploy to Vercel
        run: vercel --prod --token=${{ secrets.VERCEL_TOKEN }}
```

---

## Environment Configuration

### Backend Configuration Files

**Location:** `<service>/src/main/resources/`

**application.properties / application.yml:**
```properties
spring.application.name=patient-service
server.port=${PORT:8083}

# Database
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/patient_db
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Eureka Client
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
eureka.instance.prefer-ip-address=true

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
```

**Config Server:** Centralized configuration in `config-server/`

### Frontend Configuration

**Environment Variables:**
- `.env.local` - Local development (gitignored)
- `.env.production` - Production build
- `.env` - Default values

**Access in Code:**
```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_BE_BASE_URL || 'http://localhost:8080/api';
```

---

## Troubleshooting

### Backend Issues

#### Service Not Registering with Eureka

**Symptom:** Service doesn't appear in Eureka dashboard

**Solutions:**
1. Verify Eureka server is running on port 8761
2. Check `eureka.client.service-url.defaultZone` in application.properties
3. Wait 30-60 seconds for registration
4. Check service logs for connection errors

#### Database Connection Failed

**Symptom:** `CommunicationsException: Communications link failure`

**Solutions:**
1. Verify MySQL is running: `mysql -u root -p`
2. Check database exists: `SHOW DATABASES;`
3. Verify credentials in `.env`
4. Check DB_HOST and DB_PORT match MySQL configuration
5. Ensure MySQL allows connections from localhost

#### Port Already in Use

**Symptom:** `Port 8080 is already in use`

**Solutions:**
1. Find process: `netstat -ano | findstr :8080` (Windows) or `lsof -i :8080` (Mac/Linux)
2. Kill process or change port in `.env`

#### JWT Token Invalid

**Symptom:** 401 Unauthorized errors

**Solutions:**
1. Verify JWT_SECRET is same across all services
2. Check token expiration (login again)
3. Ensure Authorization header format: `Bearer <token>`

### Frontend Issues

#### API Calls Failing (CORS)

**Symptom:** CORS error in browser console

**Solutions:**
1. Verify API Gateway allows CORS (should be configured)
2. Check `NEXT_PUBLIC_BE_BASE_URL` is correct
3. Ensure backend is running

#### Page Not Found (404)

**Symptom:** Next.js 404 error

**Solutions:**
1. Check route exists in `app/` directory
2. Verify file-based routing structure
3. Clear `.next` cache: `rm -rf .next && pnpm dev`

#### Module Not Found

**Symptom:** `Cannot find module '@/components/...'`

**Solutions:**
1. Reinstall dependencies: `pnpm install`
2. Check `tsconfig.json` paths configuration
3. Restart dev server

### Database Issues

#### Slow Queries

**Solutions:**
1. Add indexes on foreign keys
2. Use EXPLAIN to analyze queries
3. Enable query logging: `spring.jpa.show-sql=true`
4. Optimize JPA fetch strategies (LAZY vs EAGER)

#### Schema Out of Sync

**Solutions:**
1. Set `spring.jpa.hibernate.ddl-auto=update` for dev
2. Use Flyway/Liquibase for production migrations
3. Manual schema validation

---

## Useful Commands

### Backend

```bash
# Clean build
./gradlew clean build

# Run specific service
./gradlew :patient-service:bootRun

# View dependencies
./gradlew :patient-service:dependencies

# Generate Javadoc
./gradlew javadoc

# Check for updates
./gradlew dependencyUpdates
```

### Frontend

```bash
# Install dependencies
pnpm install

# Development server
pnpm dev

# Production build
pnpm build

# Production server
pnpm start

# Lint code
pnpm lint

# Type check
pnpm type-check

# Run tests
pnpm test:e2e

# Update dependencies
pnpm update
```

### Docker

```bash
# Build image
docker build -t hms-patient-service .

# Run container
docker run -p 8083:8083 --env-file .env hms-patient-service

# View logs
docker logs <container-id>

# Stop all containers
docker stop $(docker ps -aq)
```

---

## Performance Optimization

### Backend

- **Connection Pooling:** HikariCP (default, configured via Spring Boot)
- **Caching:** Add Redis for frequently accessed data
- **Database Indexes:** On foreign keys and query fields
- **Async Processing:** Use `@Async` for non-blocking operations
- **Query Optimization:** Use DTO projections instead of full entities

### Frontend

- **Code Splitting:** Automatic with Next.js App Router
- **Image Optimization:** Use `next/image` component
- **Caching:** TanStack Query for server state
- **Bundle Analysis:** `pnpm analyze` (if configured)
- **CDN:** Deploy static assets to CDN in production

---

## Security Checklist

### Development

- ✅ Never commit `.env` files
- ✅ Use strong JWT secrets (min 256 bits)
- ✅ Rotate refresh tokens
- ✅ Enable HTTPS in production
- ✅ Validate all inputs
- ✅ Use prepared statements (JPA handles this)
- ✅ Implement rate limiting (API Gateway)
- ✅ Enable CORS selectively

### Production

- ✅ Use secrets management (Kubernetes Secrets, AWS Secrets Manager)
- ✅ Enable SSL/TLS certificates
- ✅ Implement API authentication/authorization
- ✅ Regular security audits
- ✅ Keep dependencies updated
- ✅ Enable audit logging
- ✅ Database backups
- ✅ Disaster recovery plan

---

## Monitoring & Logging

### Backend

**Spring Boot Actuator:** Enabled on all services
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

**Logging:**
- Console logging (development)
- File logging (production)
- Log aggregation: ELK Stack or CloudWatch (planned)

**APM (Planned):**
- Distributed tracing: Spring Cloud Sleuth + Zipkin
- Metrics: Prometheus + Grafana
- Alerts: Based on health checks

### Frontend

**Error Tracking:**
- Browser console (development)
- Sentry or similar (production - planned)

**Analytics:**
- Google Analytics (optional)
- Custom event tracking

---

## Resources

### Documentation

- [Backend README](../HMS_Backend/README.md)
- [Frontend README](../QuanLyBenhVien/README.md)
- [API Contracts](./api-contracts-backend.md)
- [Data Models](./data-models.md)
- [Architecture](./architecture.md)

### External Links

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Next.js Documentation](https://nextjs.org/docs)
- [Gradle Documentation](https://docs.gradle.org/)
- [pnpm Documentation](https://pnpm.io/)

---

**Last Updated:** 2026-01-03  
**Maintained By:** HMS Development Team
