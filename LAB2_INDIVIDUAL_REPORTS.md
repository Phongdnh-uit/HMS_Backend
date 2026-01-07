# BÁO CÁO CÁ NHÂN - BÀI TẬP THỰC HÀNH SỐ 2
## Docker hóa và Triển khai Ứng dụng

---

# THÀNH VIÊN 1: Nguyễn Ngọc Trường Giang (ngoctruonggiang)

## 1. Phần công việc cá nhân đã trực tiếp thực hiện trong nhóm

### Backend Dockerfile - Tối ưu hóa Multi-stage Build

**Commit:** `b262ddf` - 🚀 perf(server): reduce image size

**File:** `infrastructure/pro/Dockerfile`

**TRƯỚC (Single service build - mỗi service build riêng):**
```dockerfile
ARG SERVICE
COPY ${SERVICE} ./${SERVICE}
RUN ./gradlew :${SERVICE}:clean :${SERVICE}:bootJar --no-daemon
COPY --from=builder /build/${SERVICE}/build/libs/${SERVICE}-*.jar ./${SERVICE}.jar
ENTRYPOINT ["sh","-c","java -jar /app/${SERVICE}.jar"]
```

**SAU (Optimized multi-stage build - build tất cả cùng lúc):**
```dockerfile
# Stage 1: Build ALL services at once
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder
COPY common ./common
COPY config-server ./config-server
# ... copy all modules
RUN ./gradlew clean bootJar --no-daemon

# Stage 2: Minimal runtime image
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
RUN apk add --no-cache curl
COPY --from=builder /build/${SERVICE}/build/libs/${SERVICE}-*.jar ./app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Frontend Dockerization

**Files tạo mới:**
- `QuanLyBenhVien/Dockerfile` - 3-stage build với standalone output
- `QuanLyBenhVien/compose.yaml` - Docker Compose cho frontend
- `QuanLyBenhVien/.dockerignore` - Loại bỏ files không cần thiết

### Docker Hub Push

Đã push 13 Docker images lên Docker Hub (`ngoctruonggiang/*`):
- hms-frontend, hms-api-gateway, hms-config-server
- hms-discovery-service, hms-auth-service, hms-patient-service
- hms-medicine-service, hms-hr-service, hms-appointment-service
- hms-medical-exam-service, hms-billing-service, hms-report-service
- hms-notification-service

---

## 2. Phần kiến thức cá nhân nắm rõ nhất trong bài thực hành

**Multi-stage Docker Builds & Image Optimization**

- Tách build stage và runtime stage để giảm kích thước image
- Sử dụng Alpine base image (`-alpine`) thay vì full image
- Sử dụng JRE thay vì JDK trong runtime stage
- Copy chỉ artifacts cần thiết (JAR files)
- Tối ưu layer caching: copy build files trước, source code sau

**Key learnings:**
```dockerfile
# Build stage: dùng JDK đầy đủ để compile
FROM eclipse-temurin:23-jdk AS builder

# Runtime stage: chỉ dùng JRE nhẹ để chạy
FROM eclipse-temurin:23-jre-alpine AS runner
# → Giảm từ 2.14GB xuống ~400MB (-80%)
```

---

## 3. Một khó khăn kỹ thuật đã gặp trong quá trình thực hiện và cách giải quyết

### Khó khăn: Docker image quá lớn (2.14GB mỗi service)

**Nguyên nhân phân tích:**
1. Base image sử dụng JDK đầy đủ thay vì JRE
2. Copy toàn bộ source code vào runtime image
3. Không sử dụng Alpine variant (image nhẹ hơn)

**Cách giải quyết:**

| Bước | Thay đổi | Kết quả |
|------|----------|---------|
| 1 | Chuyển từ `eclipse-temurin:23-jdk` sang `eclipse-temurin:23-jre-alpine` | Giảm ~300MB |
| 2 | Multi-stage build: stage 1 build, stage 2 chỉ copy JAR | Giảm ~1.5GB |
| 3 | Thêm `.dockerignore` để loại bỏ files không cần | Tăng tốc build |

**Kết quả:** Giảm từ **2.14GB** xuống **~400MB** (-80%)

---

## 4. Tự đánh giá mức độ đóng góp của bản thân trong nhóm

**Đóng góp: 50%**

| Công việc | Chi tiết |
|-----------|----------|
| Dockerfile Backend | Tối ưu hóa multi-stage build, giảm 80% image size |
| Dockerfile Frontend | Tạo 3-stage build với Next.js standalone |
| Docker Compose Frontend | Cấu hình frontend service kết nối backend |
| Docker Hub | Push 13 images lên registry |
| README | Viết hướng dẫn deployment |

---
---

# THÀNH VIÊN 2: Bùi Minh Tuấn Chinh (chinhbmt122)

## 1. Phần công việc cá nhân đã trực tiếp thực hiện trong nhóm

### Docker Compose Configuration

**Commits:**
- `44fe0c8` - Add .env.example for easier teammate setup
- `b580ff6` - Merge compose.yaml với các services mới

**File:** `infrastructure/pro/compose.yaml`

**Thêm MinIO Object Storage:**
```yaml
minio:
  image: minio/minio:latest
  container_name: minio-storage
  ports:
    - "9000:9000"   # API
    - "9001:9001"   # Console
  environment:
    MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
    MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin123}
  command: server /data --console-address ":9001"
  volumes:
    - minio-data:/data
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
```

### Environment Variables Setup

**File:** `infrastructure/pro/.env.example`

Tạo template cho environment variables để teammates dễ setup:
```bash
# MinIO Configuration
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123

# Service Ports
CONFIG_SERVER_PORT=8888
DISCOVERY_SERVER_PORT=8763
# ... etc
```

### README Documentation

**File:** `infrastructure/pro/README.md`

Tạo documentation với Quick Start guide, Service list, Common commands, Troubleshooting, Architecture diagram.

---

## 2. Phần kiến thức cá nhân nắm rõ nhất trong bài thực hành

**Docker Compose & Environment Variables**

- Cấu hình multi-container applications với Docker Compose
- Sử dụng `.env` files để quản lý sensitive data (passwords, tokens)
- Health checks trong Docker Compose để đảm bảo service dependency
- Volume management cho persistent storage
- Network configuration để services giao tiếp với nhau

**Key learnings:**
```yaml
# Health check pattern
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
  interval: 10s
  timeout: 5s
  retries: 5

# Environment variable với default value
environment:
  MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
  #                               ↑ default value nếu không set
```

---

## 3. Một khó khăn kỹ thuật đã gặp trong quá trình thực hiện và cách giải quyết

### Khó khăn: Merge conflict trong compose.yaml

**Nguyên nhân:**
- Nhiều branches (feature/medical-exam, feature/billing, etc.) thêm services vào compose.yaml cùng lúc
- Mỗi branch thêm MySQL instance và service mới

**Cách giải quyết:**

1. Checkout từ develop branch
2. Merge từng phần một, resolve conflicts thủ công
3. Kiểm tra structure của compose.yaml sau merge
4. Test lại toàn bộ services: `docker compose up -d`
5. Verify tất cả containers healthy: `docker ps`

**Commit:** `b580ff6 - Merge origin/develop into merge-with-develop`

**Kết quả:** Merge thành công 21 services vào compose.yaml

---

## 4. Tự đánh giá mức độ đóng góp của bản thân trong nhóm

**Đóng góp: 50%**

| Công việc | Chi tiết |
|-----------|----------|
| Docker Compose | Thêm MinIO service, merge các services |
| Environment Variables | Tạo .env.example template |
| Merge & Resolve | Xử lý conflicts từ nhiều branches |
| README | Viết documentation cho deployment |

---

# TỔNG KẾT

| Thành viên | Công việc chính | Đóng góp |
|------------|-----------------|----------|
| ngoctruonggiang | Dockerfile optimization, Frontend Docker, Docker Hub push | 50% |
| chinhbmt122 | Docker Compose, MinIO setup, .env, Merge conflicts | 50% |

**Tổng số containers:** 22 (21 backend + 1 frontend)

**Docker Hub:** https://hub.docker.com/u/ngoctruonggiang

---

*Bản tự đánh giá này được trình bày trung thực, rõ ràng, có nội dung kỹ thuật cụ thể.*
