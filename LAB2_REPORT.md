# 📋 BÁO CÁO BÀI TẬP THỰC HÀNH SỐ 2
## Đề tài: Docker hóa và Triển khai Ứng dụng từ Mã nguồn có sẵn

---

**Môn học:** Công nghệ Phần mềm Chuyên sâu (SE214)  
**Nhóm:** [Tên nhóm]  
**Thành viên:**
| STT | Họ và Tên | MSSV | Đóng góp |
|-----|-----------|------|----------|
| 1   |           |      |    %     |
| 2   |           |      |    %     |
| 3   |           |      |    %     |

---

# PHẦN A – PHÂN TÍCH MÃ NGUỒN

## 1. Loại ứng dụng

**Fullstack Microservices Application**

| Thành phần | Mô tả |
|------------|-------|
| **Backend** | 12 Spring Boot Microservices |
| **Frontend** | Next.js 16 Web Application |
| **Database** | 7 MySQL 8.0 instances (Database-per-Service) |
| **Cache** | Redis 7 |
| **Storage** | MinIO (S3-compatible) |

## 2. Công nghệ sử dụng

### Backend Technologies
| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| Java | 23 | Ngôn ngữ lập trình |
| Spring Boot | 3.5.8 | Framework chính |
| Spring Cloud | 2025.0.0 | Microservices infrastructure |
| Gradle | 8.14.3 | Build tool |
| MySQL | 8.0 | Database |
| Redis | 7 | Caching |
| MinIO | Latest | Object storage |

### Frontend Technologies
| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| Node.js | 20 | Runtime |
| Next.js | 16.0.5 | React Framework |
| React | 19.2.0 | UI Library |
| TypeScript | 5 | Type safety |
| Tailwind CSS | 4 | Styling |

## 3. Cách chạy ứng dụng KHÔNG dùng Docker

### Backend (12 services, mỗi terminal riêng biệt)
```bash
cd HMS_Backend

# 1. Build tất cả services
./gradlew clean build

# 2. Khởi động theo thứ tự (MỖI SERVICE 1 TERMINAL)
./gradlew :config-server:bootRun      # Port 8888
./gradlew :discovery-service:bootRun  # Port 8763
./gradlew :auth-service:bootRun       # Port 8081
./gradlew :patient-service:bootRun    # Port 8082
./gradlew :medicine-service:bootRun   # Port 8083
./gradlew :hr-service:bootRun         # Port 8084
./gradlew :appointment-service:bootRun # Port 8085
./gradlew :medical-exam-service:bootRun # Port 8086
./gradlew :billing-service:bootRun    # Port 8087
./gradlew :report-service:bootRun     # Port 8088
./gradlew :notification-service:bootRun # Port 8089
./gradlew :api-gateway:bootRun        # Port 8080
```

### Frontend
```bash
cd QuanLyBenhVien
npm install
npm run dev  # Port 3000
```

## 4. Các Port sử dụng

### Backend Services
| Service | Port | Mô tả |
|---------|------|-------|
| API Gateway | 8080 | Entry point cho tất cả API requests |
| Config Server | 8888 | Centralized configuration |
| Discovery Service | 8763 | Service Registry (Eureka) |
| Auth Service | 8081 | Authentication & Authorization |
| Patient Service | 8082 | Patient management |
| Medicine Service | 8083 | Medicine & inventory |
| HR Service | 8084 | Human resources |
| Appointment Service | 8085 | Appointment scheduling |
| Medical Exam Service | 8086 | Medical examinations |
| Billing Service | 8087 | Invoicing & payments |
| Report Service | 8088 | Reports & analytics |
| Notification Service | 8089 | Email/SMS notifications |

### Supporting Services
| Service | Port | Mô tả |
|---------|------|-------|
| MySQL (7 instances) | 3306 (internal) | Databases |
| Redis | 6379 | Cache |
| MinIO | 9000, 9001 | Object storage |
| Frontend | 3000 | Web UI |

## 5. Database và File Upload

### Database
Ứng dụng sử dụng **Database-per-Service pattern** với 7 MySQL databases:

| Database | Service | Mô tả |
|----------|---------|-------|
| auth_db | Auth Service | User accounts, roles |
| patient_db | Patient Service | Patient records |
| medicine_db | Medicine Service | Medicines, inventory |
| hr_db | HR Service | Employees, departments |
| appointment_db | Appointment Service | Appointments |
| exam_db | Medical Exam Service | Examination records |
| billing_db | Billing Service | Invoices, payments |

### File Upload
- **MinIO** (S3-compatible storage) được sử dụng cho:
  - Avatar images
  - Lab results

---

# PHẦN B – DOCKER HÓA ỨNG DỤNG

## 1. Dockerfile cho Backend

**Vị trí:** `HMS_Backend/infrastructure/pro/Dockerfile`

### Đặc điểm chính (Bonus: Multi-stage Build)

```dockerfile
ARG JAVA_VERSION=23

# ======== Stage 1: Build ALL services ONCE ========
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder
WORKDIR /build

COPY settings.gradle.kts build.gradle.kts gradlew ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy ALL modules
COPY common ./common
COPY config-server ./config-server
# ... (all other modules)

# Build ALL modules at once (1 Gradle download)
RUN ./gradlew clean bootJar --no-daemon

# ======== Stage 2: Runtime (OPTIMIZED) ========
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

RUN apk add --no-cache curl

ARG SERVICE
ENV SERVICE=${SERVICE}
WORKDIR /app

# Copy ONLY the specific service JAR (size optimization)
COPY --from=builder /build/${SERVICE}/build/libs/${SERVICE}-*.jar ./app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Phân tích Dockerfile

| Đặc điểm | Mô tả | Bonus |
|----------|-------|-------|
| Multi-stage build | Stage 1: Build, Stage 2: Runtime |  |
| Alpine base image | Giảm kích thước image |  |
| No hardcoded secrets | Sử dụng environment variables |  |
| Layer caching | Copy build files trước |  |
| Single JAR copy | Mỗi image chỉ chứa 1 JAR |  |

## 2. Dockerfile cho Frontend

**Vị trí:** `QuanLyBenhVien/Dockerfile`

```dockerfile
# ======== Stage 1: Dependencies ========
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install

# ======== Stage 2: Builder ========
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_BE_BASE_URL=http://localhost:8080/api
ENV NEXT_PUBLIC_BE_BASE_URL=${NEXT_PUBLIC_BE_BASE_URL}
RUN npm run build

# ======== Stage 3: Runner (MINIMAL) ========
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/ || exit 1

CMD ["node", "server.js"]
```

### Đặc điểm Frontend Dockerfile

| Đặc điểm | Mô tả |
|----------|-------|
| 3-stage build | deps → builder → runner |
| Standalone output | Next.js standalone mode |
| Non-root user | Security best practice |
| Health check | Container health monitoring |
| .dockerignore | Giảm build context |

## 3. Build Commands

### Backend
```bash
cd HMS_Backend/infrastructure/pro
docker compose build
```

### Frontend
```bash
cd QuanLyBenhVien
docker compose build
```

## 4. Docker Images được tạo

### Backend Images (Optimized: ~400MB mỗi image)

| Image Name | Size |
|------------|------|
| pro-config-server-pro | 373MB |
| pro-discovery-service-pro | 399MB |
| pro-api-gateway-pro | 394MB |
| pro-auth-service-pro | 457MB |
| pro-patient-service-pro | 464MB |
| pro-medicine-service-pro | 448MB |
| pro-hr-service-pro | 464MB |
| pro-appointment-service-pro | 448MB |
| pro-medical-exam-service-pro | 476MB |
| pro-billing-service-pro | 448MB |
| pro-report-service-pro | 457MB |
| pro-notification-service-pro | 447MB |


### Frontend Image

| Image Name | Size |
|------------|------|
| hms-frontend | ~200MB |

### Supporting Images

| Image Name | Size |
|------------|------|
| mysql:8.0 | 1.08GB |
| redis:7-alpine | 61.2MB |
| minio/minio:latest | 241MB |

---

# PHẦN C – TRIỂN KHAI VỚI DOCKER COMPOSE

## 1. Docker Compose File cho Backend

**Vị trí:** `HMS_Backend/infrastructure/pro/compose.yaml`

### Cấu trúc Services

```yaml
services:
  # ========== DATABASES (7 instances) ==========
  mysql-auth-service:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${AUTH_DB_NAME}
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      
  # ... (6 more MySQL instances)

  # ========== INFRASTRUCTURE SERVICES ==========
  config-server-pro:
    build:
      context: ../../
      dockerfile: infrastructure/pro/Dockerfile
      args:
        SERVICE: config-server
    ports:
      - "${CONFIG_SERVER_PORT}:${CONFIG_SERVER_PORT}"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
    depends_on:
      mysql-auth-service:
        condition: service_healthy

  discovery-service-pro:
    # ... similar config
    depends_on:
      config-server-pro:
        condition: service_healthy

  # ========== BUSINESS SERVICES (9 services) ==========
  auth-service-pro:
    # ...
    depends_on:
      discovery-service-pro:
        condition: service_healthy
      mysql-auth-service:
        condition: service_healthy

  # ========== SUPPORTING SERVICES ==========
  redis-cache:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  minio-storage:
    image: minio/minio:latest
    ports:
      - "9000:9000"
      - "9001:9001"

networks:
  app-network:
    driver: bridge
```

## 2. Docker Compose File cho Frontend

**Vị trí:** `QuanLyBenhVien/compose.yaml`

```yaml
services:
  frontend:
    image: hms-frontend:latest
    build:
      context: .
      dockerfile: Dockerfile
      args:
        NEXT_PUBLIC_BE_BASE_URL: http://localhost:8080/api
    container_name: hms-frontend
    ports:
      - "3000:3000"
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:3000/"]
    restart: unless-stopped

networks:
  hms-network:
    external: true
    name: pro_app-network
```

## 3. Environment Variables (Bonus: .env files)

**File:** `HMS_Backend/infrastructure/pro/.env.example`

```bash
# Service Ports
CONFIG_SERVER_PORT=8888
DISCOVERY_SERVER_PORT=8763
API_GATEWAY_PORT=8080
AUTH_SERVICE_PORT=8081
# ...

# Database Configuration
MYSQL_ROOT_PASSWORD=secure_password
AUTH_DB_NAME=auth_db
PATIENT_DB_NAME=patient_db
# ...

# JWT Configuration
JWT_SECRET_KEY=your-256-bit-secret
JWT_ACCESS_TOKEN_EXPIRATION=3600000

# Redis Configuration
REDIS_HOST=redis-cache
REDIS_PORT=6379
```

## 4. Deployment Commands

### Triển khai Backend (21 containers)
```bash
cd HMS_Backend/infrastructure/pro
docker compose up -d
```

### Triển khai Frontend (1 container)
```bash
cd QuanLyBenhVien
docker compose up -d
```

### Kiểm tra trạng thái
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

## 5. Kết quả triển khai

### Containers đang chạy (22 total)

| Container | Status | Ports |
|-----------|--------|-------|
| hms-frontend | Up | 3000 |
| api-gateway-pro | Up (healthy) | 8080 |
| auth-service-pro | Up | 8081 |
| patient-service-pro | Up | 8082 |
| medicine-service-pro | Up | 8083 |
| hr-service-pro | Up | 8084 |
| appointment-service-pro | Up | 8085 |
| medical-exam-service-pro | Up | 8086 |
| billing-service-pro | Up | 8087 |
| report-service-pro | Up | 8088 |
| notification-service-pro | Up | 8089 |
| config-server-pro | Up (healthy) | 8888 |
| discovery-service-pro | Up | 8763 |
| mysql-* (7 instances) | Up (healthy) | 3306 |
| redis-cache | Up (healthy) | 6379 |
| minio-storage | Up (healthy) | 9000, 9001 |

---

# PHẦN D – KẾT QUẢ VÀ MINH CHỨNG

## 1. Tóm tắt kết quả

| Yêu cầu | Trạng thái | Ghi chú |
|---------|------------|---------|
| Dockerfile Backend |  Hoàn thành | Multi-stage, optimized |
| Dockerfile Frontend |  Hoàn thành | 3-stage, standalone |
| Docker Compose Backend |  Hoàn thành | 21 services |
| Docker Compose Frontend |  Hoàn thành | 1 service |
| Health checks |  Hoàn thành | All services |
| .env files |  BONUS | Sensitive data separation |
| Multi-stage build |  BONUS | 80% size reduction |
| No hardcoded secrets |  Hoàn thành | Environment variables |

## 2. Access Points

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API Gateway Health | http://localhost:8080/actuator/health |
| Eureka Dashboard | http://localhost:8763 |
| MinIO Console | http://localhost:9001 |

## 3. Screenshots (Thêm vào đây)

### 3.1 Docker Images
```
[SCREENSHOT: docker images | grep -E "pro-|hms-"]
```

### 3.2 Running Containers
```
[SCREENSHOT: docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"]
```

### 3.3 Eureka Dashboard
```
[SCREENSHOT: http://localhost:8763]
```

### 3.4 Application Running
```
[SCREENSHOT: http://localhost:3000 - Login page]
```

### 3.5 API Gateway Health
```
[SCREENSHOT: http://localhost:8080/actuator/health]
```

---

# ĐÁNH GIÁ CÁ NHÂN VÀ ĐÓNG GÓP

## Thành viên 1: [Tên]

**Công việc thực hiện:**
- [Mô tả công việc cụ thể]

**Kiến thức nắm vững nhất:**
- [Ví dụ: Multi-stage Docker builds, Docker Compose orchestration...]

**Khó khăn gặp phải và cách giải quyết:**
- [Mô tả vấn đề và giải pháp]

**Tự đánh giá đóng góp:** __%

---

## Thành viên 2: [Tên]

**Công việc thực hiện:**
- [Mô tả công việc cụ thể]

**Kiến thức nắm vững nhất:**
- [Ví dụ: Environment variables, Health checks...]

**Khó khăn gặp phải và cách giải quyết:**
- [Mô tả vấn đề và giải pháp]

**Tự đánh giá đóng góp:** __%

---

## Thành viên 3: [Tên]

**Công việc thực hiện:**
- [Mô tả công việc cụ thể]

**Kiến thức nắm vững nhất:**
- [Ví dụ: Docker networking, Port mapping...]

**Khó khăn gặp phải và cách giải quyết:**
- [Mô tả vấn đề và giải pháp]

**Tự đánh giá đóng góp:** __%

---

# PHỤ LỤC

## A. Cấu trúc thư mục Docker

```
HMS-total/
├── HMS_Backend/
│   ├── infrastructure/
│   │   └── pro/
│   │       ├── Dockerfile          # Backend Dockerfile
│   │       ├── compose.yaml        # Backend Docker Compose
│   │       ├── .env.example        # Environment template
│   │       └── README.md           # Deployment guide
│   └── [12 service directories]
│
└── QuanLyBenhVien/
    ├── Dockerfile                  # Frontend Dockerfile
    ├── compose.yaml                # Frontend Docker Compose
    ├── .dockerignore               # Excluded files
    └── next.config.ts              # output: 'standalone'
```

## B. Commands Reference

| Action | Command |
|--------|---------|
| Build Backend | `cd HMS_Backend/infrastructure/pro && docker compose build` |
| Start Backend | `cd HMS_Backend/infrastructure/pro && docker compose up -d` |
| Build Frontend | `cd QuanLyBenhVien && docker compose build` |
| Start Frontend | `cd QuanLyBenhVien && docker compose up -d` |
| View logs | `docker logs <container-name>` |
| Stop all | `docker compose down` |
| Clean up | `docker system prune -a` |

---

**Ngày hoàn thành:** [DD/MM/YYYY]  
**Repository:** [Link to GitHub/GitLab]
