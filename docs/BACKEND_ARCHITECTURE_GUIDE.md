# HMS Backend Architecture Guide

## Overview

HMS Backend là một hệ thống quản lý bệnh viện (Hospital Management System) được xây dựng theo kiến trúc **Microservices** sử dụng **Spring Boot** và **Spring Cloud**. Hệ thống được thiết kế để đảm bảo tính module hóa, khả năng mở rộng và dễ bảo trì.

---

## 1. Backend Folder Structure

### 1.1 Root Level Structure

```
HMS_Backend/
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Module definitions
├── .env                          # Environment variables (local)
│
├── config-server/                # Centralized configuration service
├── discovery-service/            # Service registry (Eureka)
├── api-gateway/                  # API Gateway & Authentication
├── common/                       # Shared library module
│
├── auth-service/                 # Authentication & Authorization
├── patient-service/              # Patient management
├── medicine-service/             # Medicine inventory
├── hr-service/                   # Human resources (doctors, staff)
├── appointment-service/          # Appointment scheduling
├── medical-exam-service/         # Medical examinations
├── billing-service/              # Billing & payments
├── notification-service/         # Email/SMS notifications
├── report-service/               # Reporting & analytics
│
└── infrastructure/               # Docker & deployment configs
    ├── dev/                      # Development Docker Compose files
    └── pro/                      # Production Docker Compose files
```

### 1.2 Service Module Structure (Standard Pattern)

Mỗi service tuân theo cấu trúc chuẩn sau:

```
{service-name}/
├── build.gradle.kts              # Service-specific dependencies
├── src/
│   ├── main/
│   │   ├── java/com/hms/{service_name}/
│   │   │   ├── {ServiceName}Application.java    # Main Spring Boot entry
│   │   │   ├── controllers/      # REST API endpoints
│   │   │   ├── services/         # Business logic
│   │   │   ├── repositories/     # Database access (JPA)
│   │   │   ├── entities/         # JPA entities
│   │   │   ├── dtos/             # Data Transfer Objects
│   │   │   ├── mappers/          # Entity ↔ DTO mapping (MapStruct)
│   │   │   ├── hooks/            # Business logic hooks
│   │   │   ├── clients/          # Feign clients for inter-service calls (optional)
│   │   │   ├── configs/          # Service-specific configurations
│   │   │   ├── helpers/          # Utility classes
│   │   │   └── constants/        # Constants & enums
│   │   └── resources/
│   │       └── application.yaml  # Service bootstrap config
│   └── test/                     # Unit & integration tests
```

### 1.2.1 Common Module Structure

The `common` module has a specialized structure with shared utilities:

```
common/src/main/java/com/hms/common/
├── CommonApplication.java        # Module entry point
├── clients/                      # Feign client interfaces (e.g., AccountClient)
├── configs/                      # Shared configurations (FeignConfig, etc.)
├── controllers/                  # GenericController base class
├── dtos/                         # ApiResponse, PageResponse, Action groups
├── enums/                        # Shared enums
├── exceptions/                   # GlobalExceptionHandler, error handling
│   ├── errors/                   # ErrorCode enum, ApiException class
│   ├── FeignCustomErrorDecoder.java
│   └── GlobalExceptionHandler.java
├── helpers/                      # Utility classes (FeignHelper, etc.)
├── hooks/                        # GenericHook interface
├── mappers/                      # GenericMapper interface
├── repositories/                 # SimpleRepository interface
├── securities/                   # UserContext, UserContextFilter
└── services/                     # CrudService interface with default methods
```

### 1.3 Module Descriptions

#### Infrastructure Services

| Module                | Port | Purpose                                                                                             |
| --------------------- | ---- | --------------------------------------------------------------------------------------------------- |
| **config-server**     | 8888 | Centralized configuration management. Stores all service configurations in `/configuration/` folder |
| **discovery-service** | 8763 | Service registry using Netflix Eureka. All services register here for load balancing                |
| **api-gateway**       | 8080 | Single entry point. JWT validation, routing, CORS, rate limiting                                    |

> **Note:** Ports shown above are from `.env.example` and are the **authoritative values** used in production. The `.env` file overrides any defaults in config YAML files.

#### Business Services

| Module                   | Port | Purpose                                                 |
| ------------------------ | ---- | ------------------------------------------------------- |
| **auth-service**         | 8081 | User authentication, JWT token management, account CRUD |
| **patient-service**      | 8082 | Patient records, medical history, profile management    |
| **medicine-service**     | 8083 | Medicine inventory, prescriptions catalog               |
| **hr-service**           | 8084 | Staff management, doctor schedules, departments         |
| **appointment-service**  | 8085 | Appointment booking, scheduling, queue management       |
| **medical-exam-service** | 8086 | Medical examinations, lab tests, prescriptions          |
| **billing-service**      | 8087 | Invoice generation, payment processing (VNPay)          |
| **notification-service** | 8089 | Email notifications, appointment reminders              |
| **report-service**       | 8088 | Analytics & reporting                                   |

> **Note:** Ports shown above are from `.env.example` which is the **authoritative source**. Environment variables always override the fallback defaults in config YAML files. For local development without `.env`, some config defaults may differ (see Section 5.3).

#### Shared Module

| Module     | Purpose                                                                                                                                                                            |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **common** | Shared utilities: `GenericController`, `GenericService`, `GenericMapper`, `GenericHook`, exception handling, DTOs, security utilities. All business services depend on this module |

---

## 2. Configuration Files

### 2.1 Configuration Architecture

HMS Backend sử dụng **Spring Cloud Config** để quản lý cấu hình tập trung:

```
┌─────────────────────────────────────────────────────────────┐
│                     Config Server                           │
│  config-server/src/main/resources/configuration/            │
│  ├── api-gateway.yml                                        │
│  ├── auth-service.yml                                       │
│  ├── patient-service.yml                                    │
│  ├── medicine-service.yml                                   │
│  ├── hr-service.yml                                         │
│  ├── appointment-service.yml                                │
│  ├── medical-exam-service.yml                               │
│  ├── billing-service.yml                                    │
│  ├── notification-service.yml                               │
│  └── discovery-service.yaml                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Business Services                         │
│  Each service has minimal application.yaml that points to   │
│  Config Server for full configuration                       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Service Application.yaml (Bootstrap Config)

Mỗi service có file `application.yaml` tối thiểu chỉ định:

```yaml
# Example: api-gateway/src/main/resources/application.yaml (CORRECT)
spring:
  application:
    name: api-gateway
  config:
    import: optional:configserver:http://${CONFIG_SERVER_HOST:localhost}:${CONFIG_SERVER_PORT:8888}
```

> ⚠️ **Warning:** Many services have incorrect `CONFIG_SERVER_PORT` defaults (e.g., `8081` instead of `8888`). This works in production because `.env` overrides these defaults. See Section 5.2 for details.

**Giải thích:**

- `spring.application.name`: Tên service, dùng để:
  - Eureka registration
  - Config Server lấy file config tương ứng (e.g., `auth-service.yml`)
- `spring.config.import`: Kết nối đến Config Server với fallback

### 2.3 Config Server Configuration Pattern

#### Common Pattern cho Business Services với Database:

```yaml
# Pattern: {service-name}.yml in config-server
spring:
  # ======== Docker Compose Integration ========
  docker:
    compose:
      enabled: ${DOCKER_COMPOSE_ENABLED:false} # Auto-start compose for dev
      file: ${DOCKER_COMPOSE_FILE_{SERVICE}:file:infrastructure/dev/{service}/compose.yaml}

  # ======== Database Configuration ========
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${SERVICE_DB_HOST:localhost}:${SERVICE_DB_PORT:3306}/${SERVICE_DB_NAME:mydatabase}
    username: ${SERVICE_DB_USERNAME:myuser}
    password: ${SERVICE_DB_PASSWORD:secret}

  # ======== JPA/Hibernate ========
  jpa:
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update # ⚠️ Development only! Use 'validate' + Flyway/Liquibase for production
    show-sql: true # Log SQL for debugging

# ======== Server Configuration ========
server:
  port: ${SERVICE_PORT:80XX}

# ======== Eureka Client (Service Discovery) ========
eureka:
  instance:
    prefer-ip-address: true # Register with IP instead of hostname
  client:
    serviceUrl:
      defaultZone: http://${DISCOVERY_SERVICE_HOST:localhost}:${DISCOVERY_SERVICE_PORT:8761}/eureka/
# NOTE: The fallback default 8761 is the standard Eureka port.
# In production, .env sets DISCOVERY_SERVICE_PORT=8763, which takes precedence.
```

### 2.4 Detailed Configuration File Analysis

#### 2.4.1 Config Server (config-server/application.yaml)

```yaml
spring:
  application:
    name: config-server
  profiles:
    active: native # Use local filesystem (not Git)
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/configuration # Where config files are stored
      override-system-properties: false

server:
  port: ${CONFIG_SERVER_PORT:8888}
```

**Purpose:** Config Server lưu trữ tất cả configurations và phân phối cho các services.

#### 2.4.2 Discovery Service (discovery-service.yaml)

```yaml
server:
  port: ${DISCOVERY_SERVICE_PORT:8761} # ⚠️ Default is 8761, .env uses 8763

eureka:
  instance:
    hostname: ${DISCOVERY_SERVICE_HOST:localhost}
  client:
    register-with-eureka: false # Server doesn't register itself
    fetch-registry: false # Server doesn't need registry
```

> **Note:** The fallback port `8761` differs from `.env.example` which uses `8763`. In production, the `.env` value takes precedence.

**Purpose:** Netflix Eureka Server cho service discovery và load balancing.

#### 2.4.3 API Gateway (api-gateway.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Route pattern: /api/{resource}/** → lb://{service}/{resource}/**
        - id: auth-service-api
          uri: lb://auth-service # Load-balanced URI
          predicates:
            - Path=/api/auth/** # Match incoming path
          filters:
            - StripPrefix=1 # Remove /api prefix

        # Direct service access (service-to-service)
        - id: auth-service-direct
          uri: lb://auth-service
          predicates:
            - Path=/auth-service/**
          filters:
            - StripPrefix=1

# JWT validation (public key only - no private key at gateway)
jwt:
  public-key: ${JWT_PUBLIC_KEY:...}

server:
  port: ${API_GATEWAY_PORT:8080}

eureka:
  instance:
    prefer-ip-address: true
  client:
    serviceUrl:
      defaultZone: http://${DISCOVERY_SERVICE_HOST:localhost}:${DISCOVERY_SERVICE_PORT:8763}/eureka/
```

**Purpose:**

- Route requests to appropriate services
- JWT token validation
- Extract user info and add to headers (`X-User-ID`, `X-User-Role`, `X-User-Email`)

#### 2.4.4 Auth Service (auth-service.yml) - Advanced Example

```yaml
spring:
  # ... standard database config ...

  # Email configuration for password reset
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:...}
    password: ${MAIL_PASSWORD:...}
    properties:
      mail.smtp:
        auth: true
        starttls:
          enable: true
          required: true

# JWT configuration (private + public key for token generation)
jwt:
  private-key: ${JWT_PRIVATE_KEY:...} # For signing tokens
  public-key: ${JWT_PUBLIC_KEY:...} # For verifying tokens
  refresh-token:
    expiration: ${JWT_REFRESH_TOKEN_EXPIRATION:604800} # 7 days
  access-token:
    expiration: ${JWT_ACCESS_TOKEN_EXPIRATION:3600} # 1 hour

# Frontend URLs for email links
app:
  frontend:
    reset-password-url: ${FRONTEND_RESET_PASSWORD_URL:http://localhost:3000/password-reset/new-password}
    activate-account-url: ${FRONTEND_ACTIVATE_ACCOUNT_URL:http://localhost:3000/verify-email}
```

> ⚠️ **Note:** Frontend URL paths must match actual frontend routes. Check `.env.example` for current values.

#### 2.4.5 Medical Exam Service (medical-exam-service.yml) - External Integrations

```yaml
spring:
  # ... standard database config ...

# External service integrations
medicine-service:
  base-url: http://${MEDICINE_SERVICE_HOST:localhost}:${MEDICINE_SERVICE_PORT:8082} # ⚠️ Note: Default 8082, but .env uses 8083

appointment-service:
  base-url: http://${APPOINTMENT_SERVICE_HOST:appointment-service-pro}:${APPOINTMENT_SERVICE_PORT:8085}

# MinIO object storage for lab test images
minio:
  endpoint: http://${MINIO_HOST:minio-storage}:${MINIO_PORT:9000}
  public-endpoint: http://${MINIO_PUBLIC_HOST:localhost}:${MINIO_PUBLIC_PORT:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin123}
  bucket-name: ${MINIO_BUCKET_NAME:lab-images}
```

#### 2.4.6 Billing Service (billing-service.yml) - Payment Gateway

```yaml
spring:
  # ... standard database config ...

# Feign client timeouts
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000

# VNPay payment gateway integration
vnpay:
  tmn-code: ${VNPAY_TMN_CODE:DEMO}
  hash-secret: ${VNPAY_HASH_SECRET:DEMO_SECRET}
  pay-url: ${VNPAY_PAY_URL:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}
  api-url: ${VNPAY_API_URL:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}
  return-url: ${VNPAY_RETURN_URL:http://localhost:3000/payment/result}
  ipn-url: ${VNPAY_IPN_URL:http://localhost:8080/api/payments/vnpay-ipn}
  expire-minutes: ${VNPAY_EXPIRE_MINUTES:15}
```

#### 2.4.7 Notification Service (notification-service.yml) - Scheduled Tasks

```yaml
spring:
  # ... mail configuration ...

# Feign client for cross-service communication
feign:
  client:
    config:
      appointment-service:
        url: ${APPOINTMENT_SERVICE_URL:http://appointment-service-pro:8083}
      patient-service:
        url: ${PATIENT_SERVICE_URL:http://patient-service-pro:8082}
      medical-exam-service:
        url: ${MEDICAL_EXAM_SERVICE_URL:http://medical-exam-service-pro:8084}

# Scheduled notification settings
notification:
  followup:
    cron: "${FOLLOWUP_CRON:0 0 8 * * ?}" # 8 AM daily
    days-offset: ${FOLLOWUP_DAYS_OFFSET:1} # Tomorrow's appointments
  reminder:
    cron: "${REMINDER_CRON:0 0 8 * * ?}" # 8 AM daily
    days-offset: ${REMINDER_DAYS_OFFSET:1} # Tomorrow's appointments

app:
  name: ${APP_NAME:Hospital Management System}

logging:
  level:
    com.hms.notification_service: INFO
```

### 2.5 Environment Variables Pattern

Tất cả configurations sử dụng pattern: `${VAR_NAME:default_value}`

```
${VARIABLE_NAME:default_value}
     │              │
     │              └── Giá trị mặc định khi biến không tồn tại
     └── Tên biến môi trường
```

**Common Environment Variables:**

| Category      | Variables                                                                                                       |
| ------------- | --------------------------------------------------------------------------------------------------------------- |
| Config Server | `CONFIG_SERVER_HOST`, `CONFIG_SERVER_PORT`                                                                      |
| Discovery     | `DISCOVERY_SERVICE_HOST`, `DISCOVERY_SERVICE_PORT`                                                              |
| Database      | `{SERVICE}_DB_HOST`, `{SERVICE}_DB_PORT`, `{SERVICE}_DB_NAME`, `{SERVICE}_DB_USERNAME`, `{SERVICE}_DB_PASSWORD` |
| JWT           | `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `JWT_ACCESS_TOKEN_EXPIRATION`, `JWT_REFRESH_TOKEN_EXPIRATION`              |
| Mail          | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`                                                      |
| Development   | `DOCKER_COMPOSE_ENABLED`                                                                                        |

---

## 3. Design Patterns Applied

### 3.1 Generic CRUD Pattern

HMS Backend áp dụng **Generic CRUD Pattern** để giảm boilerplate code và đảm bảo consistency.

#### Component Hierarchy:

```
┌──────────────────────┐
│   GenericController  │  Abstract base controller with CRUD endpoints
├──────────────────────┤
│   • GET /all         │
│   • GET /{id}        │
│   • POST /           │
│   • PUT /{id}        │
│   • DELETE /{id}     │
│   • DELETE /bulk     │
└──────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  CrudService<E,ID,I,O>       │  Interface with default method implementations
│  (Interface)                 │
├──────────────────────────────┤
│   • findAll()                │  Abstract methods
│   • findById()               │
│   • create()                 │
│   • update()                 │
│   • delete()                 │
│   • deleteAll()              │
│   • defaultXxx() methods     │  Default implementations
└──────────────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  GenericService<E,ID,I,O>    │  Concrete class implementing CrudService
│  (Class - @Scope prototype)  │
├──────────────────────────────┤
│   Injects via constructor:   │
│   • SimpleRepository         │
│   • GenericMapper            │
│   • GenericHook              │
└──────────────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  ServiceRegistration         │  @Configuration class that creates
│  (@Configuration)            │  GenericService beans with dependencies
└──────────────────────────────┘
```

> **Note:** HMS uses a **three-tier architecture**:
>
> 1. `CrudService` - Interface with Java default methods (`defaultFindAll`, `defaultCreate`, etc.)
> 2. `GenericService` - Prototype-scoped concrete class that implements `CrudService`
> 3. `ServiceRegistration` - Configuration class that creates singleton beans of `GenericService` with injected dependencies
>
> This pattern allows each entity to have its own configured service instance while maintaining type safety and reducing boilerplate. The `@Scope("prototype")` annotation ensures each `new GenericService(...)` call creates a fresh instance.
>
> **Reference:** [Spring @Bean Method](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html) (Official Spring Documentation)

#### GenericController Implementation:

```java
// common/controllers/GenericController.java
@RequiredArgsConstructor
public abstract class GenericController<E, ID, I, O> {
    protected final CrudService<E, ID, I, O> service;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<O>>> findAll(
            Pageable pageable,
            @RequestParam(value = "filter", required = false) @Nullable String filter,    // RSQL filter support
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        Specification<E> specification = RSQLJPASupport.toSpecification(filter);
        if (all) {
            pageable = Pageable.unpaged(pageable.getSort());  // Return all records with sort
        }
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<O>> findById(@PathVariable("id") ID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<O>> create(
            @Validated({Default.class, Action.Create.class}) @RequestBody I input) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(input)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<O>> update(
            @PathVariable("id") ID id,
            @Validated({Default.class, Action.Update.class}) @RequestBody I input) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, input)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") ID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

**Type Parameters:**

- `E` - Entity type (JPA Entity)
- `ID` - Entity ID type (String, Long, UUID)
- `I` - Input DTO (Request)
- `O` - Output DTO (Response)

### 3.2 Hook Pattern (Business Logic Injection)

**GenericHook** cho phép inject business logic vào CRUD flow mà không cần override toàn bộ service:

```java
// common/hooks/GenericHook.java
public interface GenericHook<E, ID, I, O> {
    // ==== VIEW ====
    default void enrichFindAll(PageResponse<O> response) {}
    default void enrichFindById(O response) {}

    // ==== CREATE ====
    default void validateCreate(I input, Map<String, Object> context) {}
    default void enrichCreate(I input, E entity, Map<String, Object> context) {}
    default void afterCreate(E entity, O response, Map<String, Object> context) {}

    // ==== UPDATE ====
    default void validateUpdate(ID id, I input, E existingEntity, Map<String, Object> context) {}
    default void enrichUpdate(I input, E entity, Map<String, Object> context) {}
    default void afterUpdate(E entity, O response, Map<String, Object> context) {}

    // ==== DELETE ====
    default void validateDelete(ID id) {}
    default void afterDelete(ID id) {}

    // ==== BULK DELETE ====
    default void validateBulkDelete(Iterable<ID> ids) {}
    default void afterBulkDelete(Iterable<ID> ids) {}
}
```

**CRUD Flow with Hooks:**

```
CREATE Request
      │
      ▼
┌─────────────────┐
│ validateCreate  │  Validate business rules (e.g., duplicate check)
└─────────────────┘
      │
      ▼
┌─────────────────┐
│ Map DTO→Entity  │  Convert Request DTO to Entity
└─────────────────┘
      │
      ▼
┌─────────────────┐
│  enrichCreate   │  Set default values, auto-fill fields
└─────────────────┘
      │
      ▼
┌─────────────────┐
│  Save to DB     │  JPA Repository save
└─────────────────┘
      │
      ▼
┌─────────────────┐
│  afterCreate    │  Post-processing (e.g., send notification)
└─────────────────┘
      │
      ▼
Response DTO
```

**Example - PatientHook:**

```java
@Component
public class PatientHook implements GenericHook<Patient, String, PatientRequest, PatientResponse> {

    @Override
    public void validateCreate(PatientRequest input, Map<String, Object> context) {
        // Check if patient already exists
        if (PatientHelper.isAccountExists(input, patientRepository))
            throw new RuntimeException("Patient already exists");
    }

    @Override
    public void enrichCreate(PatientRequest input, Patient entity, Map<String, Object> context) {
        // Auto-fill email from Account if available
        if (entity.getAccountId() != null && entity.getEmail() == null) {
            var account = authClient.findById(entity.getAccountId());
            entity.setEmail(account.getData().getEmail());
        }
        PatientHelper.enrichDefaultData(entity);  // Set default values
    }
}
```

### 3.3 Mapper Pattern (MapStruct)

Sử dụng **MapStruct** để tự động generate mapping code:

```java
// common/mappers/GenericMapper.java
public interface GenericMapper<E, I, O> {
    E requestToEntity(I request);           // Request DTO → Entity
    O entityToResponse(E entity);           // Entity → Response DTO
    void partialUpdate(I request, @MappingTarget E entity);  // Partial update
}
```

**Implementation Example:**

```java
@Mapper(componentModel = "spring")
public interface PatientMapper extends GenericMapper<Patient, PatientRequest, PatientResponse> {

    @Override
    Patient requestToEntity(PatientRequest request);

    @Override
    PatientResponse entityToResponse(Patient entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(PatientRequest request, @MappingTarget Patient entity);
}
```

### 3.4 API Response Pattern

Standardized API response format:

```java
// common/dtos/ApiResponse.java
@Getter
@Setter
public class ApiResponse<T> {
    private Integer code;           // Status code (1000 = success)
    private String message;         // Human-readable message
    private T data;                 // Response payload
    private Map<String, String> errors;  // Field-level errors
    private Instant timestamp;      // Response timestamp

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(1000);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
}
```

**Response Examples:**

```json
// Success Response
{
    "code": 1000,
    "message": "success",
    "data": { ... },
    "timestamp": "2026-01-08T10:30:00Z"
}

// Error Response (Resource Not Found)
{
    "code": 2002,
    "message": "Resource Not Found",
    "errors": null,
    "timestamp": "2026-01-08T10:30:00Z"
}

// Validation Error Response
{
    "code": 2000,
    "message": "Validation Error",
    "errors": {
        "email": "must be a valid email",
        "phone": "must not be blank"
    },
    "timestamp": "2026-01-08T10:30:00Z"
}
```

### 3.5 Global Exception Handling Pattern

Centralized exception handling:

```java
// common/exceptions/GlobalExceptionHandler.java
@RestControllerAdvice
@Order(1)
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ex.getErrorCode().getCode());
        response.setMessage(ex.getMessage());
        response.setErrors(ex.getFieldErrors());
        return ResponseEntity.status(ex.getErrorCode().getHttpCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(...) {
        // Handle @Valid annotation errors
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(...) {
        // Handle database constraint violations
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUncatchException(Exception ex) {
        // Catch-all for unexpected errors
    }
}
```

### 3.6 Security Pattern (Gateway Authentication)

HMS sử dụng **Centralized Authentication** tại API Gateway:

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Request                             │
│                      Authorization: Bearer <JWT>                     │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          API GATEWAY                                │
│  1. Validate JWT token                                              │
│  2. Extract claims: subject (userId), role, email                   │
│  3. Add headers: X-User-ID, X-User-Role, X-User-Email               │
│  4. Apply RBAC rules (SecurityConfig)                               │
│  5. Forward to backend service                                      │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Backend Service                               │
│  1. UserContextFilter reads X-User-* headers                        │
│  2. Sets ThreadLocal UserContext                                    │
│  3. Business logic accesses UserContext.getUser()                   │
└─────────────────────────────────────────────────────────────────────┘
```

\*AuthFilter (Gateway):\*\*

```java
// api-gateway/configs/AuthFilter.java
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> {
                    Jwt jwt = authentication.getToken();

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-ID", jwt.getSubject())
                            .header("X-User-Role", jwt.getClaim("role"))
                            .header("X-User-Email", jwt.getClaim("email"))
                            .build();

                    return exchange.mutate().request(mutatedRequest).build();
                })
                .switchIfEmpty(Mono.just(exchange))
                .flatMap(chain::filter);
    }
}
```

**UserContextFilter (Backend Services):**

```java
// common/securities/UserContextFilter.java
@Component
public class UserContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String userId = request.getHeader("X-User-ID");
        String userRole = request.getHeader("X-User-Role");
        String userEmail = request.getHeader("X-User-Email");

        if (userId != null) {
            UserContext.User user = new UserContext.User();
            user.setId(userId);
            user.setRole(userRole);
            user.setEmail(userEmail);
            UserContext.setUser(user);    // ThreadLocal storage
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();           // Clean up to prevent memory leaks
        }
    }
}*
```

**Usage in Controllers:**

```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile() {
    UserContext.User currentUser = UserContext.getUser();
    if (currentUser == null) {
        throw new ApiException(ErrorCode.AUTHENTICATION_REQUIRED, "User not authenticated");
    }

    Patient patient = patientRepository.findByAccountId(currentUser.getId())
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

    return ResponseEntity.ok(ApiResponse.ok(patientMapper.entityToResponse(patient)));
}
```

### 3.6.1 Role-Based Access Control (RBAC)

The API Gateway implements fine-grained RBAC through `SecurityConfig.java`:

#### System Roles

| Role           | Description          | Typical Permissions                              |
| -------------- | -------------------- | ------------------------------------------------ |
| `ADMIN`        | System administrator | Full access to all resources                     |
| `DOCTOR`       | Medical doctor       | Exams, prescriptions, patient records (read)     |
| `NURSE`        | Nursing staff        | Vital signs, lab results, patient records (read) |
| `RECEPTIONIST` | Front desk staff     | Appointments, patient registration, billing      |
| `PATIENT`      | Registered patient   | Self-service: own appointments, invoices, exams  |

#### Permission Matrix (Key Endpoints)

| Endpoint                  | ADMIN | DOCTOR | NURSE | RECEPTIONIST | PATIENT |
| ------------------------- | ----- | ------ | ----- | ------------ | ------- |
| `POST /api/auth/accounts` | ✅    | ❌     | ❌    | ❌           | ❌      |
| `GET /api/auth/accounts`  | ✅    | ❌     | ❌    | ✅           | ❌      |
| `POST /api/patients`      | ✅    | ❌     | ❌    | ✅           | ❌      |
| `GET /api/patients`       | ✅    | ✅     | ✅    | ✅           | ❌      |
| `GET /api/patients/me`    | ✅    | ✅     | ✅    | ✅           | ✅      |
| `POST /api/appointments`  | ✅    | ✅     | ✅    | ✅           | ✅      |
| `POST /api/exams`         | ✅    | ✅     | ✅    | ❌           | ❌      |
| `PUT /api/exams`          | ✅    | ✅     | ❌    | ❌           | ❌      |
| `POST /api/medicines`     | ✅    | ❌     | ❌    | ❌           | ❌      |
| `GET /api/medicines`      | ✅    | ✅     | ✅    | ✅           | ✅      |
| `POST /api/invoices`      | ✅    | ❌     | ❌    | ✅           | ❌      |
| `GET /api/invoices/my`    | ❌    | ❌     | ❌    | ❌           | ✅      |
| `GET /api/reports`        | ✅    | ❌     | ❌    | ❌           | ❌      |

#### Public Endpoints (No Authentication Required)

```java
// SecurityConstant.java - PUBLIC_URLS
String[] PUBLIC_URLS = {
    "/api/auth/login",
    "/api/auth/register",
    "/api/auth/refresh",
    "/api/auth/logout",
    "/api/auth/send-password-reset-token",
    "/api/auth/reset-password",
    "/api/auth/send-verification-email",
    "/api/auth/verify-email",
    "/actuator/health",
    "/api/exams/lab-results/images/*/download",  // Public image downloads
};
```

> **Reference:** [Spring Security Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html) (Official Spring Documentation)

### 3.6.2 Redis Caching Pattern (Report Service)

The `report-service` uses Redis caching for expensive analytics queries:

```java
// report-service/services/AppointmentReportService.java
@Service
@EnableCaching  // Enable on application class
public class AppointmentReportService {

    @Cacheable(value = "appointment-reports", key = "#startDate + '-' + #endDate")
    public AppointmentReportResponse generateAppointmentReport(LocalDate startDate, LocalDate endDate) {
        // Expensive aggregation - cached for 15 minutes (configured in application.yml)
        log.info("Cache MISS - fetching from appointment-service");
        return appointmentClient.getAppointmentStats(startDate, endDate);
    }

    @CacheEvict(value = "appointment-reports", allEntries = true)
    public void clearCache() {
        log.info("Cleared appointment-reports cache");
    }
}
```

**Cache Configuration (`report-service/application.yml`):**

```yaml
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
  cache:
    type: redis
    redis:
      time-to-live: 900000 # 15 minutes in milliseconds
      cache-null-values: false
```

**Available Cache Names:**

| Cache Name            | Service        | TTL   | Purpose                |
| --------------------- | -------------- | ----- | ---------------------- |
| `appointment-reports` | report-service | 15min | Appointment statistics |
| `patient-reports`     | report-service | 15min | Patient demographics   |
| `revenue-reports`     | report-service | 15min | Revenue analytics      |

> **Reference:** [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html) (Official Spring Documentation)

### 3.7 Feign Client Pattern (Service-to-Service Communication)

Services communicate via **OpenFeign** clients:

**Configuration:**

```java
// common/configs/FeignConfig.java
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignCustomErrorDecoder();   // Handle feign errors
    }

    @Bean
    public RequestInterceptor userContextRequestInterceptor() {
        return requestTemplate -> {
            UserContext.User user = UserContext.getUser();
            if (user != null) {
                // Forward user context to downstream services
                requestTemplate.header("X-User-ID", user.getId());
                requestTemplate.header("X-User-Role", user.getRole());
                requestTemplate.header("X-User-Email", user.getEmail());
            }
        };
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,    // Connect timeout
                10, TimeUnit.SECONDS,   // Read timeout
                true                    // Follow redirects
        );
    }

    /**
     * Retry configuration for Feign clients.
     * Retries on IO exceptions (connection failures), NOT on HTTP errors.
     * - Max 3 attempts (initial + 2 retries)
     * - Starting interval: 100ms
     * - Max interval: 1 second
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}
```

**Client Interface Example:**

```java
@FeignClient(name = "auth-service", configuration = FeignConfig.class)
public interface AccountClient {
    @GetMapping("/auth/accounts/{id}")
    ApiResponse<AccountResponse> findById(@PathVariable("id") String id);

    @PostMapping("/auth/accounts")
    ResponseEntity<ApiResponse<AccountResponse>> create(@Valid @RequestBody AccountRequest accountRequest);
}
```

### 3.7.1 Feign Client Communication Patterns

The system uses **two different patterns** for inter-service communication:

| Pattern              | Description                                                     | Services Using                                        |
| -------------------- | --------------------------------------------------------------- | ----------------------------------------------------- |
| **Eureka Discovery** | Uses `lb://service-name` - Eureka resolves to healthy instances | API Gateway routes                                    |
| **Direct URL**       | Uses hardcoded URLs with environment variable fallback          | notification-service, hr-service, appointment-service |

**Pattern 1: Eureka-based (via API Gateway)**

```yaml
# api-gateway.yml
- id: auth-service-api
  uri: lb://auth-service # Eureka load-balanced
```

**Pattern 2: Direct URL (bypasses Eureka for some services)**

```yaml
# notification-service.yml
feign:
  client:
    config:
      appointment-service:
        url: ${APPOINTMENT_SERVICE_URL:http://appointment-service-pro:8083}
```

> ⚠️ **Note:** Direct URL pattern means these services won't benefit from Eureka's load balancing or health checking when communicating directly. This is intentional for Docker Compose environments where service names are DNS-resolvable.

**Reference:** [Spring Cloud OpenFeign Documentation](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)

### 3.8 RSQL/FIQL Query Pattern

HMS sử dụng **RSQL** (RESTful Service Query Language) cho filtering:

```java
// GenericController
@GetMapping("/all")
public ResponseEntity<ApiResponse<PageResponse<O>>> findAll(
        Pageable pageable,
        @RequestParam("filter") String filter) {
    Specification<E> specification = RSQLJPASupport.toSpecification(filter);
    return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
}
```

**Query Examples:**

```
# Equality
GET /patients?filter=gender==MALE

# Like/Contains
GET /patients?filter=fullName=like=%John%

# Comparison
GET /patients?filter=dateOfBirth>2000-01-01

# Logical AND
GET /patients?filter=gender==MALE;bloodType==A_POSITIVE

# Logical OR
GET /patients?filter=status==ACTIVE,status==PENDING

# Complex Query
GET /appointments?filter=status==SCHEDULED;doctorId==abc123;appointmentDate>2026-01-01
```

> **Reference:** [RSQL Parser - GitHub](https://github.com/jirutka/rsql-parser), [RSQL JPA Spring Boot Starter](https://github.com/perplexhub/rsql-jpa-specification)

### 3.9 ServiceRegistration Pattern

HMS uses a **ServiceRegistration** pattern to configure generic services with their specific repository, mapper, and hook:

```java
// patient-service/controllers/ServiceRegistration.java
@RequiredArgsConstructor
@Configuration
public class ServiceRegistration {

    private final ApplicationContext context;

    @Bean
    CrudService<Patient, String, PatientRequest, PatientResponse> patientService() {
        return new GenericService<Patient, String, PatientRequest, PatientResponse>(
                context.getBean(PatientRepository.class),
                context.getBean(PatientMapper.class),
                context.getBean(PatientHook.class)
        );
    }
}
```

**Why this pattern?**

- Creates service instances as Spring beans with proper dependency injection
- Each entity gets its own configured `GenericService` instance
- Controllers receive the service via constructor injection
- Avoids boilerplate while maintaining type safety

**Usage in Controller:**

```java
@RestController
@RequestMapping("/patients")
public class PatientController extends GenericController<Patient, String, PatientRequest, PatientResponse> {

    public PatientController(CrudService<Patient, String, PatientRequest, PatientResponse> service) {
        super(service);  // Inject the configured service from ServiceRegistration
    }
}
```

> **Reference:** [Spring Bean Factory](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html) (Official Spring Framework Documentation)

### 3.10 Error Code Catalog

The system uses standardized error codes defined in `common/exceptions/errors/ErrorCode.java`:

#### General Errors (2000-2099)

| Code | HTTP Status        | Error Name                | Description                  |
| ---- | ------------------ | ------------------------- | ---------------------------- |
| 2000 | 400 Bad Request    | VALIDATION_ERROR          | Request validation failed    |
| 2001 | 409 Conflict       | RESOURCE_EXISTS           | Resource already exists      |
| 2002 | 404 Not Found      | RESOURCE_NOT_FOUND        | Requested resource not found |
| 2003 | 401 Unauthorized   | AUTHENTICATION_REQUIRED   | Authentication is required   |
| 2004 | 403 Forbidden      | FORBIDDEN                 | Access denied                |
| 2005 | 401 Unauthorized   | TOKEN_EXPIRED             | JWT token has expired        |
| 2006 | 401 Unauthorized   | TOKEN_INVALID             | JWT token is invalid         |
| 2007 | 401 Unauthorized   | INVALID_CREDENTIALS       | Wrong username or password   |
| 2008 | 400 Bad Request    | VERIFICATION_CODE_INVALID | Invalid verification code    |
| 2009 | 400 Bad Request    | VERIFICATION_CODE_EXPIRED | Verification code expired    |
| 2010 | 400 Bad Request    | UPLOAD_FAILED             | File upload failed           |
| 2011 | 400 Bad Request    | SIGNATURE_INVALID         | Invalid request signature    |
| 2012 | 401 Unauthorized   | OAUTH2_ERROR              | OAuth2 authentication error  |
| 2099 | 500 Internal Error | INTERNAL_SERVER_ERROR     | Unexpected server error      |

#### OTP Errors (2100-2199)

| Code | HTTP Status     | Error Name  | Description         |
| ---- | --------------- | ----------- | ------------------- |
| 2100 | 400 Bad Request | OTP_EXPIRED | OTP has expired     |
| 2101 | 400 Bad Request | OTP_INVALID | OTP code is invalid |

#### Account Errors (2200-2299)

| Code | HTTP Status               | Error Name         | Description               |
| ---- | ------------------------- | ------------------ | ------------------------- |
| 2200 | 423 Locked                | ACCOUNT_LOCKED     | Account has been locked   |
| 2201 | 403 Forbidden             | ACCOUNT_DISABLED   | Account is disabled       |
| 2202 | 428 Precondition Required | EMAIL_NOT_VERIFIED | Email verification needed |

#### Operation Errors (2300-2399)

| Code | HTTP Status     | Error Name            | Description             |
| ---- | --------------- | --------------------- | ----------------------- |
| 2300 | 400 Bad Request | OPERATION_NOT_ALLOWED | Operation not permitted |

#### Service-Specific Errors (3000+)

| Range     | Service              | Examples                                    |
| --------- | -------------------- | ------------------------------------------- |
| 3000-3099 | Patient Service      | PATIENT_NOT_FOUND                           |
| 3100-3199 | Medicine Service     | MEDICINE_NOT_FOUND, INSUFFICIENT_STOCK      |
| 3200-3299 | Appointment Service  | APPOINTMENT_NOT_FOUND, APPOINTMENT_CONFLICT |
| 3300-3399 | Medical Exam Service | EXAM_NOT_FOUND, PRESCRIPTION_EXISTS         |
| 3400-3499 | HR Service           | EMPLOYEE_NOT_FOUND, DOCTOR_NOT_FOUND        |
| 3500-3599 | Billing Service      | INVOICE_NOT_FOUND, DUPLICATE_PAYMENT        |

> **API Contract:** All API responses follow the standardized format. Clients should check `code` field: `1000` = success, `>=2000` = error.

### 3.11 Transaction Management Pattern

HMS uses `@Transactional` annotation for operations requiring ACID guarantees:

```java
@Service
public class LabOrderService {

    @Transactional  // Ensures atomicity - rollback on exception
    public LabOrderResponse updateLabOrderStatus(String id, LabOrderStatus newStatus) {
        LabOrder labOrder = labOrderRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        validateStatusTransition(labOrder.getStatus(), newStatus);
        labOrder.setStatus(newStatus);
        return labOrderMapper.entityToResponse(labOrderRepository.save(labOrder));
    }

    @Transactional(readOnly = true)  // Optimization for read-only operations
    public List<LabOrderResponse> findByExamId(String examId) {
        return labOrderRepository.findByMedicalExamId(examId).stream()
            .map(labOrderMapper::entityToResponse)
            .collect(Collectors.toList());
    }
}
```

**Important Notes:**

- `GenericService` does **NOT** have `@Transactional` by default
- Services requiring transaction management must add it explicitly
- Use `@Transactional(readOnly = true)` for read operations (performance optimization)
- Controllers like `PrescriptionController` use `@Transactional` for complex create/update operations

> **Reference:** [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html) (Official Spring Documentation)

### 3.12 Async Processing Pattern

The system uses `@Async` for non-blocking operations like email sending:

```java
// auth-service/services/MailService.java
@Service
public class MailService {

    @Async  // Runs in separate thread - doesn't block caller
    public void sendResetPasswordEmail(String email, String resetLink) {
        // Email sending logic - may take several seconds
        // Caller continues immediately without waiting
    }

    @Async
    public void sendVerificationEmail(String email, String activationLink) {
        // Same pattern for verification emails
    }
}
```

**Requires `@EnableAsync`** on the application class:

```java
@SpringBootApplication
@EnableAsync
public class AuthServiceApplication { ... }
```

> **Reference:** [Spring Async Processing](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async) (Official Spring Documentation)

### 3.13 Scheduled Tasks Pattern

The `notification-service` uses `@Scheduled` for periodic tasks:

```java
// notification-service/services/FollowUpNotificationScheduler.java
@Component
public class FollowUpNotificationScheduler {

    @Scheduled(cron = "${notification.followup.cron:0 0 8 * * ?}")  // Default: 8 AM daily
    public void sendFollowUpReminders() {
        // Query patients needing follow-up reminders
        // Send notification emails
    }
}

// notification-service/services/AppointmentReminderScheduler.java
@Component
public class AppointmentReminderScheduler {

    @Scheduled(cron = "${notification.reminder.cron:0 0 8 * * ?}")  // Default: 8 AM daily
    public void sendAppointmentReminders() {
        // Query tomorrow's appointments
        // Send reminder emails to patients
    }
}
```

**Requires `@EnableScheduling`** on the application class:

```java
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication { ... }
```

**Configuration via `.env`:**

```bash
FOLLOWUP_CRON=0 0 8 * * ?         # Run at 8 AM daily
FOLLOWUP_DAYS_OFFSET=1            # Remind 1 day before
REMINDER_CRON=0 0 8 * * ?         # Run at 8 AM daily
REMINDER_DAYS_OFFSET=1            # Remind 1 day before appointment
```

> **Reference:** [Spring Task Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html) (Official Spring Documentation)

### 3.14 JPA Auditing Pattern

HMS uses **Spring Data JPA Auditing** to automatically manage timestamp fields on entities:

**Enabling Auditing (Application Class):**

```java
@SpringBootApplication
@EnableJpaAuditing  // Required on services with database entities
public class PatientServiceApplication { ... }
```

**Entity Configuration:**

```java
@Entity
@EntityListeners(AuditingEntityListener.class)  // Enable auditing for this entity
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Business fields...

    @CreatedDate  // Auto-set on insert
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate  // Auto-updated on every save
    private Instant updatedAt;
}
```

**Services Using JPA Auditing:**

| Service              | Has `@EnableJpaAuditing`      | Has Database    |
| -------------------- | ----------------------------- | --------------- |
| patient-service      | ✅                            | ✅              |
| medicine-service     | ✅                            | ✅              |
| hr-service           | ✅                            | ✅              |
| appointment-service  | ✅                            | ✅              |
| medical-exam-service | ✅                            | ✅              |
| billing-service      | ✅                            | ✅              |
| auth-service         | ❌ (uses own timestamp logic) | ✅              |
| report-service       | ❌                            | ❌ (Redis only) |
| notification-service | ❌                            | ❌ (stateless)  |

> **Reference:** [Spring Data JPA Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html) (Official Spring Data Documentation)

### 3.15 Saga Pattern (Compensating Transactions)

The `medical-exam-service` implements a **Saga pattern** for prescription creation to maintain data consistency across services when decrementing medicine stock:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Prescription Creation Saga                           │
├─────────────────────────────────────────────────────────────────────────┤
│ 1. Validate prescription data (exam exists, medicines valid)            │
│ 2. Save prescription to database                                        │
│ 3. For each prescription item:                                          │
│    └─→ Call medicine-service to decrement stock                        │
│        ├─→ Success: Record in completedDecrements list                 │
│        └─→ Failure: ROLLBACK all completedDecrements, delete prescription│
│ 4. Return prescription response                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Implementation in PrescriptionHook:**

```java
// medical-exam-service/hooks/PrescriptionHook.java
private void decrementStockWithSagaCompensation(List<PrescriptionItem> items) {
    List<Pair<String, Integer>> completedDecrements = new ArrayList<>();

    for (PrescriptionItem item : items) {
        try {
            medicineClient.decrementStock(item.getMedicineId(), item.getQuantity());
            completedDecrements.add(Pair.of(item.getMedicineId(), item.getQuantity()));
        } catch (Exception e) {
            // ROLLBACK: Restore all previously decremented stock
            rollbackStockDecrements(completedDecrements);
            throw new ApiException(ErrorCode.STOCK_DECREMENT_FAILED,
                "Saga rollback executed for " + completedDecrements.size() + " items");
        }
    }
}
```

**Why Saga Pattern here?**

| Challenge                       | Solution                                                                            |
| ------------------------------- | ----------------------------------------------------------------------------------- |
| **No distributed transactions** | Each microservice has its own database                                              |
| **Partial failures**            | If medicine service fails mid-operation, we need to undo completed stock decrements |
| **Data consistency**            | Compensating transactions (rollback) restore previous state                         |

**Error Code:** `STOCK_DECREMENT_FAILED (3305)` - Indicates saga rollback was executed

> **Reference:** [Microservices Saga Pattern](https://microservices.io/patterns/data/saga.html) (Microservices.io)

---

## 4. Quick Reference

### 4.1 Service Startup Order

```
1. config-server     (Must start first - provides configurations)
2. discovery-service (Must start second - service registry)
3. auth-service      (Business services can start in parallel)
4. patient-service
5. medicine-service
6. hr-service
7. appointment-service
8. medical-exam-service
9. billing-service
10. notification-service
11. report-service
12. api-gateway      (Start last - routes to all services)
```

### 4.2 Key Dependencies (build.gradle.kts)

| Dependency                                   | Purpose                         |
| -------------------------------------------- | ------------------------------- |
| `spring-cloud-config-client`                 | Fetch config from Config Server |
| `spring-cloud-starter-netflix-eureka-client` | Register with Eureka            |
| `spring-boot-starter-data-jpa`               | Database access                 |
| `mapstruct`                                  | DTO mapping                     |
| `rsql-jpa-spring-boot-starter`               | RSQL query support              |
| `spring-cloud-starter-openfeign`             | Service-to-service calls        |

### 4.3 Adding a New Service

1. Create new module in `settings.gradle.kts`:

   ```kotlin
   include("new-service")
   ```

2. Create `build.gradle.kts`:

   ```kotlin
   dependencies {
       implementation(project(":common"))
       implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
       implementation("org.springframework.cloud:spring-cloud-config-client")
       runtimeOnly("com.mysql:mysql-connector-j")
   }
   ```

3. Create `application.yaml`:

   ```yaml
   spring:
     application:
       name: new-service
     config:
       import: optional:configserver:http://${CONFIG_SERVER_HOST:localhost}:${CONFIG_SERVER_PORT:8888}
   ```

4. Add config in `config-server/src/main/resources/configuration/new-service.yml`

5. Add routes in `api-gateway.yml`

6. Update `.env` with new service variables

7. Add to Docker Compose if needed

---

## 5. Important Notes & Best Practices

### 5.0 Environment Variable Precedence (CRITICAL)

> 🔑 **Key Concept:** The `.env` file (or `.env.example` as template) is the **single source of truth** for all configuration values.

**How Spring Boot resolves configuration:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Configuration Priority (High → Low)                │
├──────────────────────────────────────────────────────────────────────┤
│ 1. OS Environment Variables (set in Docker Compose or shell)         │
│ 2. .env file (loaded by Docker Compose or IDE)                       │
│ 3. Config Server (config-server/configuration/*.yml)                 │
│ 4. Service's application.yaml (fallback defaults only)               │
└──────────────────────────────────────────────────────────────────────┘
```

**Example Resolution:**

```yaml
# In config-server/configuration/auth-service.yml
server:
  port: ${AUTH_SERVICE_PORT:8082} # Fallback default = 8082
```

```bash
# In .env.example (or .env)
AUTH_SERVICE_PORT=8081  # Actual value = 8081
```

**Result:** Auth service runs on port **8081** (from .env), NOT 8082 (fallback).

> ⚠️ **Important:** Throughout this document, when you see discrepancies between config YAML defaults and `.env.example` values, the `.env.example` values are **authoritative**. The YAML defaults are only used when environment variables are not set.

**Reference:** [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) (Official Documentation)

### 5.1 Production Considerations

#### Database Schema Management

⚠️ **Critical Warning:** The `ddl-auto: update` setting is **NOT RECOMMENDED** for production environments.

| Environment | Recommended Setting       | Migration Tool              |
| ----------- | ------------------------- | --------------------------- |
| Development | `update` or `create-drop` | None (optional)             |
| Testing     | `create-drop`             | None                        |
| Production  | `validate` or `none`      | **Flyway** or **Liquibase** |

**Why avoid `ddl-auto: update` in production?**

- Cannot handle complex migrations (data transformation)
- May cause data loss on column type changes
- No rollback capability
- Not transactional - partial failures leave schema inconsistent

**Reference:** [Spring Boot SQL Database Documentation](https://docs.spring.io/spring-boot/reference/data/sql.html) (Official Spring Boot Reference)

### 5.2 Config Server Port Issue

⚠️ **Known Issue:** Some service `application.yaml`/`application.yml` files have incorrect `CONFIG_SERVER_PORT` default values:

| Service              | Current Default | Correct Default | File Extension | Status                                                             |
| -------------------- | --------------- | --------------- | -------------- | ------------------------------------------------------------------ |
| auth-service         | 8081            | **8888**        | `.yaml`        | ⚠️ Wrong                                                           |
| patient-service      | 8081            | **8888**        | `.yaml`        | ⚠️ Wrong                                                           |
| medicine-service     | 8081            | **8888**        | `.yaml`        | ⚠️ Wrong                                                           |
| medical-exam-service | 8081            | **8888**        | `.yml`         | ⚠️ Wrong                                                           |
| hr-service           | 8081            | **8888**        | `.yaml`        | ⚠️ Wrong                                                           |
| appointment-service  | 8081            | **8888**        | `.yaml`        | ⚠️ Wrong                                                           |
| notification-service | 8088            | **8888**        | `.yml`         | ⚠️ Wrong                                                           |
| report-service       | Hardcoded 9001  | **8888**        | `.yml`         | ⚠️ Wrong (completely invalid - doesn't use Config Server)          |
| discovery-service    | _(no default)_  | **8888**        | `.yaml`        | ⚠️ Missing default (uses `${CONFIG_SERVER_PORT}` without fallback) |
| billing-service      | 8888 ✅         | **8888**        | `.yaml`        | ✅ Correct                                                         |
| api-gateway          | 8888 ✅         | **8888**        | `.yaml`        | ✅ Correct                                                         |

> **Note:** Some services use `.yaml` extension while others use `.yml`. Both are valid YAML formats, but consistency is recommended.

These work in production because the `CONFIG_SERVER_PORT` environment variable is set correctly in Docker Compose, but **local development without `.env` may fail** to connect to Config Server.

### 5.2.1 Services Without Database (Stateless Services)

Two services in HMS are **stateless** and do not use a database:

| Service                  | Purpose                    | Data Storage     | Spring Boot Config                              |
| ------------------------ | -------------------------- | ---------------- | ----------------------------------------------- |
| **report-service**       | Analytics & cached reports | Redis (caching)  | `exclude = {DataSourceAutoConfiguration.class}` |
| **notification-service** | Email notifications        | None (stateless) | `exclude = {DataSourceAutoConfiguration.class}` |

**Why exclude DataSourceAutoConfiguration?**

```java
@SpringBootApplication(
    scanBasePackages = {"com.hms.report_service", "com.hms.common"},
    exclude = {DataSourceAutoConfiguration.class}  // No database needed
)
@EnableFeignClients
@EnableCaching  // Uses Redis instead
public class ReportServiceApplication { ... }
```

Without this exclusion, Spring Boot would fail to start because:

- `spring-boot-starter-data-jpa` is on the classpath (from `common` module)
- JPA requires a DataSource, which requires database configuration
- These services don't have database configuration

> **Reference:** [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html) (Official Spring Boot Documentation)

### 5.2.2 Report-Service Special Case

⚠️ **Architecture Difference:** The `report-service` does **NOT** use Config Server. Its configuration is fully embedded in its own `application.yml`:

```yaml
# report-service/src/main/resources/application.yml (actual configuration)
spring:
  application:
    name: report-service
  config:
    import: optional:configserver:http://localhost:9001 # ⚠️ Invalid port - Config Server not used

  # Redis Cache Configuration (standalone)
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      timeout: 60000

  cache:
    type: redis
    redis:
      time-to-live: 900000 # 15 minutes

server:
  port: ${REPORT_SERVICE_PORT:8088}

eureka:
  instance:
    hostname: ${HOSTNAME:localhost}
    prefer-ip-address: true
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8763/eureka/}
```

**Why is report-service standalone?**

- Designed for analytics/caching with Redis - different configuration pattern
- Uses different environment variable naming convention (`SPRING_DATA_REDIS_*` vs other services)
- The config server import line is vestigial - port 9001 is invalid (Config Server runs on 8888)
- Uses `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` instead of `DISCOVERY_SERVICE_HOST/PORT`

**Implication:** Configuration changes for report-service must be made in its local `application.yml`, not in config-server.

> ⚠️ **Environment Variable Naming:** Report-service uses `SPRING_DATA_REDIS_HOST` and `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`. The `.env.example` file includes both `REDIS_*` and `SPRING_DATA_REDIS_*` variables to ensure compatibility.

### 5.3 Config Default Ports (Local Development Note)

> 🔑 **Critical:** The `.env` file (or `.env.example` as template) is the **authoritative source** for all ports. The fallback defaults in YAML files are **only used when environment variables are NOT set**.

When running **locally without `.env` file**, services fall back to default ports in config YAML files. Some defaults don't match the intended `.env.example` ports:

| Config File              | Fallback Default | .env.example Port | Match? | Notes                                                          |
| ------------------------ | ---------------- | ----------------- | ------ | -------------------------------------------------------------- |
| discovery-service.yaml   | 8761             | 8763              | ❌     | Standard Eureka default (8761) vs custom .env (8763)           |
| auth-service.yml         | 8082             | 8081              | ❌     | Fallback 8082 conflicts with patient-service .env port         |
| patient-service.yml      | 8083             | 8082              | ❌     | Fallback 8083 conflicts with medicine-service .env port        |
| medicine-service.yml     | 8082             | 8083              | ❌     | Fallback 8082 conflicts with patient-service .env port         |
| hr-service.yml           | 8084             | 8084              | ✅     | Matches correctly                                              |
| appointment-service.yml  | 8083             | 8085              | ❌     | Fallback 8083 conflicts with medicine-service .env port        |
| medical-exam-service.yml | 8086             | 8086              | ✅     | Matches correctly                                              |
| billing-service.yml      | 8087             | 8087              | ✅     | Matches correctly                                              |
| report-service.yml       | 8088             | 8088              | ✅     | Standalone (does NOT use Config Server - embedded config only) |
| notification-service.yml | 8089             | 8089              | ✅     | Matches correctly (also has embedded eureka config)            |

> ⚠️ **Note on Config Server Port Defaults:** Many service `application.yaml` files incorrectly default to `CONFIG_SERVER_PORT:8081` instead of `8888`. This works in production because `.env` overrides this, but local development without `.env` will fail. See Section 5.2.

> ⚠️ **Source of Truth:** These fallback values were verified against actual config files in `config-server/src/main/resources/configuration/` (January 2026). The `.env.example` file remains the **authoritative source** for production.
>
> **Reference:** [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) (Official Documentation)

> **Verified:** All fallback port defaults were verified against actual config files in `config-server/src/main/resources/configuration/`. The `.env.example` file remains the **authoritative source** for production.
>
> **Reference:** [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties)

> ⚠️ **Source of Truth:** The `.env.example` file (or `.env` in production) is the **authoritative source** for all port configurations. The fallback defaults in config YAML files are only used when environment variables are not set. **Always use `.env` file for development and production.**

**This is NOT an issue in production** because `.env` or Docker Compose environment variables always override these defaults.

**For local development**, either:

1. Copy `.env.example` to `.env` (recommended)
2. Or manually set environment variables before starting services

### 5.3.1 Inter-Service URL Configuration Issues

⚠️ **Warning:** Some services have hardcoded inter-service URLs with default ports that don't match `.env.example`:

| Service Config       | Target Service       | Config Default Host      | Config Default Port | .env.example Port | Status                      |
| -------------------- | -------------------- | ------------------------ | ------------------- | ----------------- | --------------------------- |
| medical-exam-service | medicine-service     | localhost                | 8082                | **8083**          | ⚠️ Port mismatch (off by 1) |
| medical-exam-service | appointment-service  | appointment-service-pro  | 8085                | 8085              | ✅ Correct                  |
| notification-service | appointment-service  | appointment-service-pro  | 8083                | **8085**          | ⚠️ Port mismatch (off by 2) |
| notification-service | patient-service      | patient-service-pro      | 8082                | 8082              | ✅ Correct                  |
| notification-service | medical-exam-service | medical-exam-service-pro | 8084                | **8086**          | ⚠️ Port mismatch (off by 2) |
| hr-service           | appointment-service  | appointment-service-pro  | 8085                | 8085              | ✅ Correct                  |
| appointment-service  | hr-service           | hr-service-pro           | 8084                | 8084              | ✅ Correct                  |

> **Why These Mismatches Exist:** In Docker Compose environments, service names (e.g., `appointment-service-pro`) are DNS-resolvable, so the host part works correctly. However, the hardcoded fallback ports may not match `.env.example` because they were set before the port assignments were finalized. This is **not an issue in production** because `.env` environment variables override all defaults.

> **Recommendation:** Always use `.env` file or ensure environment variables are set correctly before starting services.

**Example from `medical-exam-service.yml`:**

```yaml
medicine-service:
  base-url: http://${MEDICINE_SERVICE_HOST:localhost}:${MEDICINE_SERVICE_PORT:8082} # .env uses 8083

appointment-service:
  base-url: http://${APPOINTMENT_SERVICE_HOST:appointment-service-pro}:${APPOINTMENT_SERVICE_PORT:8085} # Correct
```

**Example from `notification-service.yml`:**

```yaml
feign:
  client:
    config:
      appointment-service:
        url: ${APPOINTMENT_SERVICE_URL:http://appointment-service-pro:8083} # Should be 8085
      patient-service:
        url: ${PATIENT_SERVICE_URL:http://patient-service-pro:8082} # Correct
      medical-exam-service:
        url: ${MEDICAL_EXAM_SERVICE_URL:http://medical-exam-service-pro:8084} # Should be 8086
```

> **Important:** These mismatches are **not issues in production** because `.env` environment variables override all defaults. However, they can cause confusion during local development without `.env` file.

### 5.4 Missing Microservice Resilience Patterns

The current architecture **does not implement** circuit breaker patterns. For production-grade resilience, consider adding:

```kotlin
// build.gradle.kts
implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
```

**Recommended patterns:**

- **Circuit Breaker:** Prevent cascade failures when a service is down
- **Rate Limiting:** Protect services from being overwhelmed
- **Bulkhead:** Isolate failures to prevent affecting the whole system
- **Retry with Backoff:** Already implemented via `Retryer.Default` in FeignConfig

**Reference:** [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker)

### 5.4.1 Missing: Distributed Tracing

⚠️ **Not Implemented:** The system does **not** have distributed tracing for tracking requests across services.

**Why it matters for microservices:**

- A single user request may traverse multiple services (Gateway → Auth → Patient → Appointment)
- Without tracing, debugging distributed failures is extremely difficult
- No visibility into request latency breakdown across services

**Recommended Setup (Spring Boot 3.x with Micrometer Tracing):**

```kotlin
// build.gradle.kts (add to common or each service)
implementation("io.micrometer:micrometer-tracing-bridge-brave")
implementation("io.zipkin.reporter2:zipkin-reporter-brave")
```

```yaml
# Add to each service's config
management:
  tracing:
    sampling:
      probability: 1.0 # Sample 100% of requests (reduce in production)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans # Zipkin server URL
```

**What you get:**

- Unique trace ID for each request, propagated across all services
- Visual request flow in Zipkin/Jaeger UI
- Latency breakdown per service
- Automatic correlation of logs with trace IDs

**Reference:** [Spring Boot Observability](https://docs.spring.io/spring-boot/reference/actuator/tracing.html) | [Micrometer Tracing](https://micrometer.io/docs/tracing)

### 5.5 Technology Stack Summary

| Category           | Technology              | Version    | Purpose                     |
| ------------------ | ----------------------- | ---------- | --------------------------- |
| Language           | Java                    | 23         | Runtime                     |
| Framework          | Spring Boot             | 3.5.8      | Application framework       |
| Cloud              | Spring Cloud            | 2025.0.0   | Microservice infrastructure |
| Build              | Gradle (Kotlin DSL)     | Latest     | Build automation            |
| ORM                | Hibernate/JPA           | Latest     | Database access             |
| Mapping            | MapStruct               | 1.6.3      | DTO↔Entity conversion       |
| Query              | RSQL-JPA                | 6.0.32     | REST query language         |
| Service Discovery  | Netflix Eureka          | Via Spring Cloud | Service registry      |
| Config             | Spring Cloud Config     | Via Spring Cloud | Centralized configuration |
| Gateway            | Spring Cloud Gateway    | Via Spring Cloud | API Gateway           |
| Monitoring         | Spring Boot Actuator    | Via Spring Boot | Health checks & metrics |
| **API Documentation** | **springdoc-openapi** | **2.3.0** | **OpenAPI/Swagger UI** |
| Caching            | Redis                   | Latest     | Report-service caching      |
| Object Storage     | MinIO                   | Latest     | File uploads (lab images)   |
| Database           | MySQL                   | 8.x        | Persistent data storage     |

> **Reference:** Verified from [build.gradle.kts](../build.gradle.kts) - Spring Boot 3.5.8, Spring Cloud 2025.0.0, Java 23.

### 5.5.1 Hibernate Dialect Inconsistency

⚠️ **Note:** The codebase uses two different Hibernate dialects inconsistently:

| Service                                                                     | Dialect Used                          | Notes                         |
| --------------------------------------------------------------------------- | ------------------------------------- | ----------------------------- |
| auth-service                                                                | `org.hibernate.dialect.MySQL8Dialect` | ⚠️ Deprecated in Hibernate 6+ |
| patient-service, appointment-service, billing-service, medical-exam-service | `org.hibernate.dialect.MySQLDialect`  | ✅ Recommended                |
| medicine-service, hr-service                                                | _(Not specified - auto-detected)_     | ✅ Hibernate auto-selects     |

**Recommendation:** For consistency and explicit control, use `org.hibernate.dialect.MySQLDialect` (modern Hibernate 6+ auto-selects the correct version). Version-specific dialects like `MySQL8Dialect` are deprecated in Hibernate 6.x - Hibernate now auto-detects the database version and adapts accordingly.

**Reference:** [Hibernate 6 Dialect Migration](https://docs.hibernate.org/orm/6.0/migration-guide/migration-guide.html#version-specific-and-spatial-dialects) (Official Hibernate ORM Documentation)

### 5.5.2 API Documentation (OpenAPI/Swagger)

✅ **Implemented:** The system uses **springdoc-openapi** for automatic API documentation generation.

**Setup Architecture:**

1. **Dependency added to `common` module** - All business services automatically inherit it
2. **Configuration class per service** - Each service has `OpenApiConfig.java` with service-specific metadata
3. **Automatic endpoint detection** - Scans all `@RestController` classes without manual annotations
4. **Disabled for infrastructure services** - API Gateway, Config Server, Discovery Service have OpenAPI disabled

**Accessing Documentation:**

| Service              | Swagger UI URL                                 | OpenAPI JSON URL                      |
| -------------------- | ---------------------------------------------- | ------------------------------------- |
| auth-service         | http://localhost:8081/swagger-ui.html          | http://localhost:8081/v3/api-docs     |
| patient-service      | http://localhost:8082/swagger-ui.html          | http://localhost:8082/v3/api-docs     |
| medicine-service     | http://localhost:8083/swagger-ui.html          | http://localhost:8083/v3/api-docs     |
| hr-service           | http://localhost:8084/swagger-ui.html          | http://localhost:8084/v3/api-docs     |
| appointment-service  | http://localhost:8085/swagger-ui.html          | http://localhost:8085/v3/api-docs     |
| medical-exam-service | http://localhost:8086/swagger-ui.html          | http://localhost:8086/v3/api-docs     |
| billing-service      | http://localhost:8087/swagger-ui.html          | http://localhost:8087/v3/api-docs     |
| report-service       | http://localhost:8088/swagger-ui.html          | http://localhost:8088/v3/api-docs     |
| notification-service | http://localhost:8089/swagger-ui.html          | http://localhost:8089/v3/api-docs     |

**What's Automatically Documented:**

- ✅ All REST endpoints (GET, POST, PUT, DELETE)
- ✅ Request/Response DTOs with field types
- ✅ Validation constraints (`@NotNull`, `@Size`, `@Email`, etc.)
- ✅ HTTP status codes
- ✅ Pagination parameters (`page`, `size`, `sort`)
- ✅ RSQL filter syntax support

**Optional Enhancement (Not Required):**

You can add annotations for richer documentation:

```java
@RestController
@RequestMapping("/patients")
@Tag(name = "Patients", description = "Patient management endpoints")
public class PatientController extends GenericController<...> {

    @Operation(summary = "Get patient by ID", description = "Retrieves a patient record by their unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patient found"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientResponse>> findById(@PathVariable String id) {
        return super.findById(id);
    }
}
```

**Configuration Example:**

```java
// patient-service/configs/OpenApiConfig.java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI patientServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Patient Service API")
                        .version("1.0.0")
                        .description("REST API for patient management"))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Direct"),
                        new Server().url("http://localhost:8080/api/patients").description("Via Gateway")
                ));
    }
}
```

**Infrastructure Services (OpenAPI Disabled):**

```yaml
# api-gateway/application.yaml, config-server/application.yaml, discovery-service/application.yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

> **Reference:** [springdoc-openapi Documentation](https://springdoc.org/) | [OpenAPI Specification](https://swagger.io/specification/)

### 5.6 Security Architecture Notes

The JWT authentication pattern uses a **centralized gateway** approach:

1. **Only the API Gateway** validates JWT tokens (using public key)
2. **Only the Auth Service** can generate/sign tokens (using private key)
3. Backend services trust the `X-User-*` headers set by the gateway
4. **Important:** Backend services should **never** be exposed directly to clients

This is a **custom implementation** using Spring Cloud Gateway's `GlobalFilter`, not a built-in Spring Cloud feature.

**Reference:** [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)

### 5.6.1 CORS Configuration

CORS (Cross-Origin Resource Sharing) is configured at the API Gateway to allow frontend applications to access the backend APIs:

```java
// api-gateway/configs/CorsConfig.java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed frontend origins (update for production)
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));

        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-User-ID", "X-User-Role"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}
```

> ⚠️ **Production Warning:** Update `allowedOriginPatterns` to include only your production frontend URL. Using `*` (wildcard) with `allowCredentials=true` is not allowed by browsers.

**Reference:** [Spring Web CORS Documentation](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html)

### 5.7 RBAC (Role-Based Access Control)

The system implements fine-grained access control at the API Gateway level:

| Role             | Permissions                                                        |
| ---------------- | ------------------------------------------------------------------ |
| **ADMIN**        | Full access to all resources                                       |
| **DOCTOR**       | Read/Write exams, prescriptions, read patients/appointments        |
| **NURSE**        | Create exams (vital signs), read patients, update lab results      |
| **RECEPTIONIST** | Manage appointments, read/create patient profiles, search accounts |
| **PATIENT**      | Read own profile, appointments, exams; manage own appointments     |

**SecurityConfig Pattern (api-gateway):**

```java
.pathMatchers(HttpMethod.GET, "/api/hr/**")
    .hasAnyAuthority("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST", "PATIENT")
.pathMatchers(HttpMethod.POST, "/api/hr/employees/**")
    .hasAuthority("ADMIN")
```

### 5.8 Error Code Conventions

The system uses categorized error codes for consistent error handling:

| Range         | Category             | Examples                                                        |
| ------------- | -------------------- | --------------------------------------------------------------- |
| **1000**      | Success              | `1000` = Success                                                |
| **2000-2099** | General Errors       | `2000` = Validation, `2002` = Not Found, `2003` = Auth Required |
| **2100-2199** | OTP Errors           | `2100` = OTP Expired, `2101` = OTP Invalid                      |
| **2200-2299** | Account Errors       | `2200` = Account Locked, `2202` = Email Not Verified            |
| **2300-2399** | Operation Errors     | `2300` = Operation Not Allowed                                  |
| **3000-3099** | Patient Service      | Domain-specific errors                                          |
| **3100-3199** | Medicine Service     | `3102` = Insufficient Stock                                     |
| **3200-3299** | Appointment Service  | `3202` = Time Conflict                                          |
| **3300-3399** | Medical Exam Service | `3301` = Exam Exists                                            |
| **3400-3499** | HR Service           | `3400` = Employee Not Found                                     |
| **3500-3599** | Billing Service      | `3503` = Already Paid                                           |

**Reference:** See `common/exceptions/errors/ErrorCode.java` for complete list.

### 5.9 PageResponse Pattern

All paginated endpoints return a standardized `PageResponse` structure:

```java
@Getter @Setter
public class PageResponse<T> {
    private Integer page;           // Current page (0-indexed)
    private Integer size;           // Page size
    private Long totalElements;     // Total records in database
    private Integer totalPages;     // Total pages available
    private Integer numberOfElements; // Records in current page
    private List<T> content;        // Actual data
}
```

**Query Parameters:**

- `page` - Page number (0-indexed, default: 0)
- `size` - Page size (default: 20)
- `sort` - Sort field and direction (e.g., `sort=createdAt,desc`)
- `filter` - RSQL filter expression
- `all=true` - Return all records (ignores pagination)

### 5.10 File Storage (MinIO)

Services that handle file uploads use **MinIO** object storage:

| Service              | Use Case                | Bucket            |
| -------------------- | ----------------------- | ----------------- |
| medical-exam-service | Lab test result images  | `lab-images`      |
| patient-service      | Patient profile photos  | `patient-photos`  |
| hr-service           | Employee profile photos | `employee-photos` |

**Configuration Pattern:**

```yaml
minio:
  endpoint: http://${MINIO_HOST:minio-storage}:${MINIO_PORT:9000}
  public-endpoint: http://${MINIO_PUBLIC_HOST:localhost}:${MINIO_PUBLIC_PORT:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin123}
  bucket-name: ${MINIO_BUCKET_NAME:lab-images}
```

### 5.11 Caching (Redis)

The **report-service** uses Redis for caching analytics data:

```yaml
# report-service/src/main/resources/application.yml (actual configuration)
spring:
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      timeout: 60000
  cache:
    type: redis
    redis:
      time-to-live: 900000 # 15 minutes in milliseconds
      cache-null-values: false
```

> **Note:** The report-service uses `SPRING_DATA_REDIS_HOST` and `SPRING_DATA_REDIS_PORT` environment variables, while `.env.example` defines `REDIS_HOST` and `REDIS_PORT`. Ensure your deployment configuration maps these correctly.

Enable with `@EnableCaching` annotation on the main application class. Use `@Cacheable` for methods that benefit from caching.

**Reference:** [Spring Boot Cache Documentation](https://docs.spring.io/spring-boot/reference/io/caching.html)

### 5.12 Scheduled Tasks

The **notification-service** uses Spring's `@Scheduled` for automated tasks:

```java
// notification-service/NotificationServiceApplication.java
@SpringBootApplication
@EnableScheduling  // Required to enable @Scheduled methods
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

| Task                    | Cron Expression                | Purpose                             |
| ----------------------- | ------------------------------ | ----------------------------------- |
| Follow-up Notifications | `${FOLLOWUP_CRON:0 0 8 * * ?}` | Send post-exam follow-up reminders  |
| Appointment Reminders   | `${REMINDER_CRON:0 0 8 * * ?}` | Send upcoming appointment reminders |

**Cron Format:** `seconds minutes hours day-of-month month day-of-week`

**Example values:**

- `0 0 8 * * ?` - Every day at 8:00 AM
- `*/5 * * * * ?` - Every 5 seconds (for testing)

**Implementation Pattern:**

```java
// notification-service/services/AppointmentReminderScheduler.java
@Scheduled(cron = "${notification.reminder.cron:0 0 8 * * ?}")
public void sendAppointmentReminders() {
    // Fetch tomorrow's appointments
    // Send reminder emails to patients
}
```

**Reference:** [Spring Scheduling Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

### 5.13 API Documentation (OpenAPI/Swagger)

⚠️ **Current Status:** The system **does not implement** OpenAPI/Swagger documentation out-of-the-box.

**Recommended Setup:** Add SpringDoc OpenAPI to generate API documentation:

```kotlin
// In build.gradle.kts (per service or in common)
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")
```

**Configuration (add to each service's config):**

```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
  show-actuator: true
```

**Access URLs (after implementation):**

- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/api-docs`

**Reference:** [SpringDoc OpenAPI Documentation](https://springdoc.org/)

### 5.14 Health Checks & Actuator Endpoints

All services include **Spring Boot Actuator** for health monitoring:

```yaml
# Already included in each service config
management:
  endpoints:
    web:
      exposure:
        include: health,info # Exposed endpoints
  endpoint:
    health:
      show-details: when_authorized # Show details for authenticated users
```

**Available Endpoints:**

| Endpoint           | Purpose                 | URL Example                             |
| ------------------ | ----------------------- | --------------------------------------- |
| `/actuator/health` | Service health status   | `http://localhost:8081/actuator/health` |
| `/actuator/info`   | Application information | `http://localhost:8081/actuator/info`   |

**Through API Gateway:**

```
GET http://localhost:8080/api/auth/actuator/health
```

**Reference:** [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/reference/actuator/index.html)

### 5.15 Transaction Management

The system uses **Spring's `@Transactional`** for database operations:

```java
// Example from LabOrderService.java
@Transactional
public LabOrderResponse updateLabOrderStatus(String id, LabOrderStatus newStatus) {
    LabOrder labOrder = labOrderRepository.findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    labOrder.setStatus(newStatus);
    return labOrderMapper.entityToResponse(labOrderRepository.save(labOrder));
}
```

**Key Considerations:**

- GenericService does NOT automatically wrap operations in transactions
- Override service methods with `@Transactional` when needed for complex operations
- Use `@Transactional(readOnly = true)` for read-only operations to improve performance

**Reference:** [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)

### 5.16 Logging Configuration

Each service configures logging in its config file:

```yaml
logging:
  level:
    root: INFO
    com.hms.{service_name}: DEBUG # Service-specific logging
    org.hibernate.SQL: DEBUG # Show SQL queries
    org.hibernate.orm.jdbc.bind: TRACE # Show SQL parameters
```

**Best Practices:**

- Use SLF4J with Lombok's `@Slf4j` annotation
- Log at appropriate levels: ERROR > WARN > INFO > DEBUG > TRACE
- Include correlation IDs for distributed tracing (consider Micrometer Tracing - see Section 5.4.1)

> ⚠️ **Note:** Spring Cloud Sleuth is **deprecated** as of Spring Boot 3.x. Use **Micrometer Tracing** instead for distributed tracing. See [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide#spring-cloud-sleuth).

**Reference:** [Spring Boot Logging Documentation](https://docs.spring.io/spring-boot/reference/features/logging.html)

### 5.17 Asynchronous Processing (@Async)

The **auth-service** uses Spring's `@Async` for non-blocking email operations:

```java
// auth-service/AuthServiceApplication.java
@SpringBootApplication
@EnableAsync  // Enable async method execution
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

```java
// auth-service/services/MailService.java
@Service
public class MailService {

    @Async  // Method runs in separate thread
    public void sendPasswordResetEmail(String to, String resetLink) {
        // Email sending doesn't block the main request
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Click here to reset: " + resetLink);
        mailSender.send(message);
    }

    @Async
    public void sendActivationEmail(String to, String activationLink) {
        // Similar async email sending
    }
}
```

**Why use `@Async`?**

- User registration/password reset returns immediately without waiting for email delivery
- Email server delays don't affect API response time
- Better user experience with faster response times

**Key Considerations:**

- `@Async` methods must be `public` (Spring creates a proxy)
- Calling `@Async` methods from the same class won't work (use separate service)
- Exceptions in async methods are not propagated to caller
- Consider using `@Async("taskExecutor")` with custom thread pool for production

**Reference:** [Spring Async Documentation](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async)

### 5.18 Validation Groups Pattern

HMS uses **Jakarta Validation** with validation groups to apply different validation rules for create vs update operations:

```java
// common/dtos/Action.java
public interface Action {
    interface Create {}  // Validation rules for POST (create)
    interface Update {}  // Validation rules for PUT (update)
}
```

**Usage in DTO:**

```java
public class PatientRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(groups = Action.Create.class, message = "Email is required on creation")
    @Email(message = "Invalid email format")
    private String email;  // Required only on create, optional on update
}
```

**Usage in Controller (GenericController pattern):**

```java
@PostMapping()
public ResponseEntity<ApiResponse<O>> create(
        @Validated({Default.class, Action.Create.class}) @RequestBody I input) {
    return ResponseEntity.ok(ApiResponse.ok(service.create(input)));
}

@PutMapping("/{id}")
public ResponseEntity<ApiResponse<O>> update(
        @PathVariable("id") ID id,
        @Validated({Default.class, Action.Update.class}) @RequestBody I input) {
    return ResponseEntity.ok(ApiResponse.ok(service.update(id, input)));
}
```

**Reference:** [Jakarta Bean Validation](https://beanvalidation.org/)

### 5.19 Spring Cloud & Spring Boot Version Compatibility

The system uses **Spring Boot 3.5.8** with **Spring Cloud 2025.0.0 (Northfields)**:

| Spring Boot Version | Spring Cloud Version | Release Train Name |
| ------------------- | -------------------- | ------------------ |
| 4.0.x               | 2025.1.x             | Oakwood            |
| **3.5.x**           | **2025.0.x**         | **Northfields**    |
| 3.4.x               | 2024.0.x             | Moorgate           |
| 3.3.x, 3.2.x        | 2023.0.x             | Leyton             |

> ⚠️ **Important:** Always check the [Spring Cloud Release Train compatibility](https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions#supported-releases) before upgrading either Spring Boot or Spring Cloud.

**Reference:** [Spring Cloud Project Page](https://spring.io/projects/spring-cloud)

### 5.20 Database Connection Pooling (HikariCP)

⚠️ **Current Status:** The system does **not explicitly configure** connection pooling. Spring Boot uses **HikariCP** by default, but relies on default settings.

**Default HikariCP Settings (Spring Boot 3.x):**

| Setting                    | Default Value | Production Recommendation |
| -------------------------- | ------------- | ------------------------- |
| `maximum-pool-size`        | 10            | 20-50 (based on workload) |
| `minimum-idle`             | Same as max   | 5-10                      |
| `idle-timeout`             | 600000 (10m)  | 300000 (5m)               |
| `connection-timeout`       | 30000 (30s)   | 10000 (10s)               |
| `max-lifetime`             | 1800000 (30m) | 1200000 (20m)             |
| `leak-detection-threshold` | 0 (disabled)  | 60000 (1m) for debugging  |

**Recommended Configuration (add to each service's config):**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 10000
      max-lifetime: 1200000
      pool-name: HMS-{ServiceName}-Pool
      # Enable for debugging connection leaks
      # leak-detection-threshold: 60000
```

**Why this matters for microservices:**

- Each service has its own database and connection pool
- Default pool size (10) may be insufficient for high traffic
- Connection leaks can exhaust the pool, causing service failures
- Proper configuration prevents database connection exhaustion

**Reference:** [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby) | [Spring Boot DataSource Configuration](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.datasource.configuration)

---

## 6. Troubleshooting Guide

### 6.1 Common Issues

#### Config Server Connection Failure

**Symptom:** Service fails to start with "Could not locate PropertySource"

**Causes:**

1. Config Server not running
2. Incorrect `CONFIG_SERVER_PORT` default value in service's `application.yaml`
3. Network issues between services

**Solution:**

```bash
# Ensure Config Server is running first
# Check service's application.yaml has correct default:
spring:
  config:
    import: optional:configserver:http://${CONFIG_SERVER_HOST:localhost}:${CONFIG_SERVER_PORT:8888}
```

#### Eureka Registration Issues

**Symptom:** Services not appearing in Eureka dashboard

**Causes:**

1. Discovery Service not running
2. Service using wrong Eureka URL
3. `prefer-ip-address` setting conflicts in Docker

**Solution:**

```yaml
eureka:
  instance:
    prefer-ip-address: true # Use IP instead of hostname
  client:
    service-url:
      defaultZone: http://${DISCOVERY_SERVICE_HOST:localhost}:${DISCOVERY_SERVICE_PORT:8761}/eureka/
```

> **Note:** The default port for discovery-service in configs is `8761`, not `8763`. Make sure your `.env` file is consistent.

#### Feign Client Timeout

**Symptom:** `feign.RetryableException: Read timed out`

**Causes:**

1. Target service slow to respond
2. Default timeout too short

**Solution:** Adjust timeout in service's config or FeignConfig:

```java
@Bean
public Request.Options requestOptions() {
    return new Request.Options(
        5, TimeUnit.SECONDS,   // connect timeout
        30, TimeUnit.SECONDS,  // read timeout (increase for slow operations)
        true
    );
}
```

### 6.2 Development Tips

1. **Start services in correct order:** Config Server → Discovery Service → Other services → API Gateway
2. **Use `optional:configserver:` prefix** to allow services to start without Config Server during development
3. **Check Eureka dashboard** at `http://localhost:8763` (if using `.env.example` port) or `http://localhost:8761` (fallback default) to verify service registration
4. **Use Postman collection** in `/postman/` folder for API testing
5. **Enable SQL logging** with `spring.jpa.show-sql=true` for debugging queries

---

## 7. Appendix

### 7.1 ServiceRegistration Pattern (Actual Implementation)

Services in HMS don't directly implement `CrudService` or extend `GenericService`. Instead, they use a **factory pattern** via `ServiceRegistration` configuration classes:

```java
// Example: auth-service/controllers/ServiceRegistration.java
@Configuration
public class ServiceRegistration {

    @Bean
    public CrudService<Account, String, AccountRequest, AccountResponse> accountService(
            AccountRepository repository,
            AccountMapper mapper,
            AccountHook hook) {
        // Create GenericService instance with specific dependencies
        return new GenericService<Account, String, AccountRequest, AccountResponse>(
                repository, mapper, hook);
    }
}
```

**Why this pattern?**

- **Prototype scope:** `GenericService` is `@Scope("prototype")` - new instance per injection
- **Type safety:** Each service bean is fully typed with entity, DTO types
- **Customization:** Hooks allow business logic injection without subclassing
- **Testability:** Dependencies can be easily mocked

**Usage in Controllers:**

```java
@RestController
@RequestMapping("/accounts")
public class AccountController extends GenericController<Account, String, AccountRequest, AccountResponse> {

    public AccountController(
            CrudService<Account, String, AccountRequest, AccountResponse> accountService) {
        super(accountService);  // Inject the typed CrudService bean
    }
}
```

**Reference:** [Spring @Bean Methods](https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html)

### 7.2 Inter-Service Communication Summary

| Communication Type    | Technology             | Use Case                   | Example                                    |
| --------------------- | ---------------------- | -------------------------- | ------------------------------------------ |
| **Client → Gateway**  | REST/HTTP              | External API calls         | Frontend → API Gateway                     |
| **Gateway → Service** | Load-balanced (lb://)  | Route to service instances | Gateway → lb://auth-service                |
| **Service → Service** | OpenFeign + Eureka     | Internal calls             | patient-service → auth-service             |
| **Service → Service** | OpenFeign + Direct URL | Docker Compose DNS         | notification-service → appointment-service |
| **Scheduled Jobs**    | Spring @Scheduled      | Background tasks           | Email reminders                            |

> **Note:** There is **no message queue** (RabbitMQ/Kafka) implementation. All communication is synchronous HTTP.

### 7.3 Environment Variable Naming Conventions

| Service/Component | DB Variables             | Service URL Variables    |
| ----------------- | ------------------------ | ------------------------ |
| auth-service      | `AUTH_DB_*`              | `AUTH_SERVICE_PORT`      |
| patient-service   | `PATIENT_DB_*`           | `PATIENT_SERVICE_PORT`   |
| report-service    | `SPRING_DATA_REDIS_*` ⚠️ | `REPORT_SERVICE_PORT`    |
| All services      | -                        | `{SERVICE}_SERVICE_PORT` |

> ⚠️ **Inconsistency:** `report-service` uses `SPRING_DATA_REDIS_HOST` while `.env.example` defines `REDIS_HOST`. This is a known issue.

### 7.4 @EnableFeignClients Configuration Pattern

Services that need to call other services must enable Feign clients. The project uses **two patterns**:

**Pattern 1: Only common clients (most services)**

```java
// For services using only AccountClient from common module
@EnableFeignClients(basePackages = "com.hms.common.clients")
@SpringBootApplication(scanBasePackages = "com.hms")
public class PatientServiceApplication { ... }
```

**Pattern 2: Common + service-specific clients**

```java
// For services with their own Feign clients
@EnableFeignClients(basePackages = {
    "com.hms.common.clients",           // AccountClient
    "com.hms.medical_exam_service.clients"  // Service-specific clients
})
@SpringBootApplication
public class MedicalExamServiceApplication { ... }
```

| Service              | @EnableFeignClients Configuration                                                   |
| -------------------- | ----------------------------------------------------------------------------------- |
| patient-service      | `basePackages = "com.hms.common.clients"`                                           |
| hr-service           | `basePackages = {"com.hms.common.clients", "com.hms.hr_service.clients"}`           |
| medical-exam-service | `basePackages = {"com.hms.common.clients", "com.hms.medical_exam_service.clients"}` |
| appointment-service  | `basePackages = {"com.hms.common.clients", "com.hms.appointment_service.clients"}`  |
| billing-service      | `@EnableFeignClients` (no basePackages - scans application package)                 |
| notification-service | `@EnableFeignClients` (no basePackages - scans application package)                 |
| report-service       | `@EnableFeignClients` (no basePackages - scans application package)                 |

> **Reference:** [Spring Cloud OpenFeign - @EnableFeignClients](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#spring-cloud-feign)

### 7.4.1 @SpringBootApplication scanBasePackages Pattern

Services need to scan the `com.hms.common` package to pick up shared components like `GlobalExceptionHandler`, `UserContextFilter`, and `FeignConfig`:

```java
// Most services use wildcard pattern
@SpringBootApplication(scanBasePackages = "com.hms")
public class PatientServiceApplication { ... }

// Some services use explicit package list
@SpringBootApplication(scanBasePackages = {"com.hms.billing_service", "com.hms.common"})
public class BillingServiceApplication { ... }
```

| Service              | scanBasePackages Configuration                                                    |
| -------------------- | --------------------------------------------------------------------------------- |
| patient-service      | `"com.hms"` (wildcard - scans all)                                                |
| medicine-service     | `"com.hms"` (wildcard)                                                            |
| auth-service         | `"com.hms"` (wildcard)                                                            |
| hr-service           | `"com.hms"` (wildcard)                                                            |
| appointment-service  | _(default)_ + `@ComponentScan` not used                                           |
| medical-exam-service | Uses `@ComponentScan({"com.hms.medical_exam_service", "com.hms.common"})` instead |
| billing-service      | `{"com.hms.billing_service", "com.hms.common"}` (explicit)                        |
| notification-service | `{"com.hms.notification_service", "com.hms.common"}` + excludes DataSource        |
| report-service       | `{"com.hms.report_service", "com.hms.common"}` + excludes DataSource              |

**Why this matters:**

- `GlobalExceptionHandler` (in common) handles exceptions uniformly
- `UserContextFilter` (in common) extracts user info from headers
- `FeignConfig` (in common) configures Feign clients with error handling
- Using `"com.hms"` wildcard is simpler but may scan unnecessary packages

> **Reference:** [Spring Boot @SpringBootApplication](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/autoconfigure/SpringBootApplication.html)

### 7.4.2 @EnableDiscoveryClient Usage

Some services explicitly use `@EnableDiscoveryClient` to register with Eureka:

| Service              | Has `@EnableDiscoveryClient` | Notes                              |
| -------------------- | ---------------------------- | ---------------------------------- |
| api-gateway          | ✅                           | Required for load-balanced routing |
| patient-service      | ✅                           | Explicit declaration               |
| medicine-service     | ✅                           | Explicit declaration               |
| appointment-service  | ✅                           | Explicit declaration               |
| medical-exam-service | ✅                           | Explicit declaration               |
| hr-service           | ❌                           | Relies on auto-configuration       |
| auth-service         | ❌                           | Relies on auto-configuration       |
| billing-service      | ❌                           | Relies on auto-configuration       |
| notification-service | ❌                           | Relies on auto-configuration       |
| report-service       | ❌                           | Relies on auto-configuration       |

> **Note:** In Spring Cloud 3.x+, `@EnableDiscoveryClient` is **optional**. Services with `spring-cloud-starter-netflix-eureka-client` on the classpath are auto-registered with Eureka. The annotation is only needed if you want to be explicit or if auto-configuration is disabled.
>
> **Reference:** [Spring Cloud Netflix Eureka Client](https://cloud.spring.io/spring-cloud-netflix/reference/html/#service-discovery-eureka-clients) (Official Documentation)

### 7.5 @Transactional Usage Patterns

The GenericService **does NOT use `@Transactional`** by default. Services that need transaction management must handle it explicitly:

**Where @Transactional is used:**

| Service              | Component                        | Use Case                                                 |
| -------------------- | -------------------------------- | -------------------------------------------------------- |
| appointment-service  | `AppointmentService`             | Status transitions, walk-in, cancel, complete operations |
| hr-service           | `ScheduleService`                | Bulk schedule updates                                    |
| medical-exam-service | `LabOrderService`                | Status transitions with validation                       |
| medical-exam-service | `LabTestResultService`           | Result updates with image upload                         |
| medical-exam-service | `PrescriptionController`         | Create prescription with items                           |
| medicine-service     | `MedicineController.updateStock` | Inventory adjustments                                    |

**Example Pattern:**

```java
@Service
public class LabOrderService {

    @Transactional  // Ensures rollback on exception
    public LabOrderResponse updateLabOrderStatus(String id, LabOrderStatus newStatus) {
        LabOrder labOrder = labOrderRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

        // Validation that might throw exception
        validateStatusTransition(labOrder.getStatus(), newStatus);

        labOrder.setStatus(newStatus);
        return labOrderMapper.entityToResponse(labOrderRepository.save(labOrder));
    }
}
```

> **Best Practice:** Use `@Transactional(readOnly = true)` for read operations to optimize performance.
>
> **Reference:** [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html)

### 7.6 SimpleRepository Interface

All repositories extend `SimpleRepository` which combines JPA and Specification support:

```java
// common/repositories/SimpleRepository.java
@NoRepositoryBean
public interface SimpleRepository<E, ID>
    extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {
}
```

**Why this pattern?**

- `JpaRepository` - Standard CRUD operations
- `JpaSpecificationExecutor` - Dynamic queries via RSQL filter support
- `@NoRepositoryBean` - Prevents Spring from instantiating this interface directly

**Usage:**

```java
// patient-service/repositories/PatientRepository.java
public interface PatientRepository extends SimpleRepository<Patient, String> {
    // Add custom queries here
    Optional<Patient> findByAccountId(String accountId);
}
```

> **Reference:** [Spring Data JPA Repositories](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)

### 7.7 Entity ID Generation Strategy

All entities in HMS use **UUID-based primary keys** with JPA's `GenerationType.UUID`:

```java
@Entity
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;  // Auto-generated UUID string like "550e8400-e29b-41d4-a716-446655440000"
}
```

**Why UUIDs?**

| Benefit                    | Description                                                             |
| -------------------------- | ----------------------------------------------------------------------- |
| **Distributed-friendly**   | IDs can be generated client-side or in any service without coordination |
| **No database bottleneck** | No need to query database for sequence/auto-increment values            |
| **URL-safe**               | Safe to use in REST URLs without encoding                               |
| **Collision-resistant**    | Statistically impossible to generate duplicates                         |

**Entity ID Types in HMS:**

| Entity Type  | ID Type         | Generation            |
| ------------ | --------------- | --------------------- |
| All entities | `String` (UUID) | `GenerationType.UUID` |

> **Note:** Hibernate 6+ with `GenerationType.UUID` generates RFC 4122 compliant UUIDs. The `String` type is used instead of `java.util.UUID` for simpler JSON serialization.
>
> **Reference:** [Hibernate UUID Generation](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#identifiers-generators-uuid)

### 7.8 Database Indexing Strategy

HMS implements database indexing for frequently queried columns to optimize performance:

**Explicit Index Definitions:**

```java
// billing-service - Payment.java
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_txn_ref", columnList = "txnRef", unique = true),
    @Index(name = "idx_payment_invoice_id", columnList = "invoice_id")
})
public class Payment { ... }
```

| Service         | Entity  | Index Name               | Column(s)    | Unique | Purpose                        |
| --------------- | ------- | ------------------------ | ------------ | ------ | ------------------------------ |
| billing-service | Payment | `idx_payment_txn_ref`    | `txnRef`     | ✅     | Fast lookup by transaction ref |
| billing-service | Payment | `idx_payment_invoice_id` | `invoice_id` | ❌     | Fast lookup by invoice         |

> ⚠️ **Note:** Most entities rely on Hibernate's auto-generated indexes for `@Id` and foreign key columns. Additional indexes should be added based on query patterns observed in production.

**Recommended Indexing for High-Traffic Queries:**

| Service              | Entity      | Suggested Index            | Reason                        |
| -------------------- | ----------- | -------------------------- | ----------------------------- |
| appointment-service  | Appointment | `(doctorId, date, status)` | Schedule lookups              |
| patient-service      | Patient     | `(accountId)`              | Auth-to-patient mapping       |
| medical-exam-service | MedicalExam | `(appointmentId)`          | Exam-appointment relationship |

> **Reference:** [Hibernate Performance Tuning](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#performance) | [JPA @Index Annotation](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html#a5113)

### 7.9 JPA Entity Relationships

HMS uses JPA annotations to define entity relationships within each microservice:

**Relationship Types Used:**

| Annotation    | Fetch Strategy | Usage in HMS                                     |
| ------------- | -------------- | ------------------------------------------------ |
| `@ManyToOne`  | LAZY (default) | Child → Parent (PrescriptionItem → Prescription) |
| `@OneToMany`  | LAZY (default) | Parent → Children (Invoice → InvoiceItems)       |
| `@ManyToMany` | Not used       | Avoided for microservice simplicity              |

**Example - Prescription with Items:**

```java
// medical-exam-service - Prescription.java
@Entity
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();
}

// medical-exam-service - PrescriptionItem.java
@Entity
public class PrescriptionItem {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
}
```

**Cross-Service References:**

Since each microservice has its own database, cross-service entity references use **ID fields** (not JPA relationships):

```java
// medical-exam-service - MedicalExam.java
@Entity
public class MedicalExam {
    // References appointment from appointment-service (different DB)
    @Column(nullable = false)
    private String appointmentId;  // NOT @ManyToOne - different database!

    // References doctor from hr-service (different DB)
    @Column(nullable = false)
    private String doctorId;       // NOT @ManyToOne - different database!
}
```

> **Key Principle:** JPA relationships (`@ManyToOne`, `@OneToMany`) are only used within a single microservice's database. Cross-service references are stored as simple ID strings and resolved via Feign client calls.

> **Reference:** [JPA Entity Relationships](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#associations) | [Microservices Data Management](https://microservices.io/patterns/data/database-per-service.html)

### 7.10 Official Documentation References

| Topic                   | Reference                                                                                                                                           |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Spring Boot             | [docs.spring.io/spring-boot](https://docs.spring.io/spring-boot/reference/)                                                                         |
| Spring Cloud            | [spring.io/projects/spring-cloud](https://spring.io/projects/spring-cloud)                                                                          |
| Spring Cloud Gateway    | [docs.spring.io/spring-cloud-gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)                                     |
| Spring Cloud Config     | [docs.spring.io/spring-cloud-config](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)                                       |
| Netflix Eureka          | [cloud.spring.io/spring-cloud-netflix](https://cloud.spring.io/spring-cloud-netflix/reference/html/)                                                |
| OpenFeign               | [docs.spring.io/spring-cloud-openfeign](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)                                 |
| Spring Data JPA         | [docs.spring.io/spring-data/jpa](https://docs.spring.io/spring-data/jpa/reference/jpa.html)                                                         |
| Spring Transaction      | [docs.spring.io/spring-framework/reference/data-access/transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction.html) |
| Spring Security         | [docs.spring.io/spring-security](https://docs.spring.io/spring-security/reference/)                                                                 |
| Hibernate ORM 6.x       | [docs.jboss.org/hibernate/orm/6.0](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html)                        |
| Jakarta Bean Validation | [beanvalidation.org](https://beanvalidation.org/2.0/spec/)                                                                                          |
| MapStruct               | [mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/)                                                                         |
| RSQL Parser             | [github.com/jirutka/rsql-parser](https://github.com/jirutka/rsql-parser)                                                                            |
| HikariCP                | [github.com/brettwooldridge/HikariCP](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)                                         |
| Micrometer Tracing      | [micrometer.io/docs/tracing](https://micrometer.io/docs/tracing)                                                                                    |
| Microservices Patterns  | [microservices.io/patterns](https://microservices.io/patterns/)                                                                                     |
| 12-Factor App           | [12factor.net](https://12factor.net/)                                                                                                               |

---

## 8. Known Issues & Technical Debt

### 8.1 Port Configuration Inconsistencies (Verified January 2026)

Several config files have fallback port defaults that don't match `.env.example`. This is **not an issue in production** (`.env` overrides all), but causes confusion during local development without `.env`:

| Config File              | Fallback Default | .env.example | Status                        |
| ------------------------ | ---------------- | ------------ | ----------------------------- |
| discovery-service.yaml   | 8761             | 8763         | ⚠️ Mismatch (Eureka standard) |
| auth-service.yml         | 8082             | 8081         | ⚠️ Mismatch                   |
| patient-service.yml      | 8083             | 8082         | ⚠️ Mismatch                   |
| medicine-service.yml     | 8082             | 8083         | ⚠️ Mismatch                   |
| appointment-service.yml  | 8083             | 8085         | ⚠️ Mismatch                   |
| hr-service.yml           | 8084             | 8084         | ✅ Correct                    |
| medical-exam-service.yml | 8086             | 8086         | ✅ Correct                    |
| billing-service.yml      | 8087             | 8087         | ✅ Correct                    |
| notification-service.yml | 8089             | 8089         | ✅ Correct                    |

> **Root Cause:** Config files use arbitrary fallback defaults that were never aligned with `.env.example`.
>
> **Solution:** Always use `.env` file for development. Copy `.env.example` to `.env` before starting services.
>
> **Why not fix?** Fixing fallbacks risks breaking existing deployments that might rely on current defaults.

### 8.2 Missing Production-Ready Features

| Feature                 | Status          | Recommendation                              | Priority |
| ----------------------- | --------------- | ------------------------------------------- | -------- |
| Circuit Breaker         | Not implemented | Add Resilience4j for service resilience     | High     |
| Distributed Tracing     | Not implemented | Add Micrometer Tracing + Zipkin/Jaeger      | High     |
| API Documentation       | Not implemented | Add SpringDoc OpenAPI (Swagger)             | Medium   |
| Database Migrations     | Not implemented | Add Flyway for production schema management | Critical |
| Centralized Logging     | Not implemented | Add ELK Stack or Grafana Loki               | Medium   |
| Rate Limiting           | Partial         | Expand Gateway rate limiting configuration  | Low      |
| Health Checks Dashboard | Not implemented | Add Spring Boot Admin                       | Low      |

> **Reference:** [Spring Cloud Circuit Breaker](https://spring.io/projects/spring-cloud-circuitbreaker), [Micrometer Tracing](https://micrometer.io/docs/tracing), [SpringDoc OpenAPI](https://springdoc.org/)

### 8.3 Environment Variable Naming Inconsistency

The `report-service` uses different environment variable names than other services:

```yaml
# report-service uses:
SPRING_DATA_REDIS_HOST  # Standard Spring Boot convention
SPRING_DATA_REDIS_PORT

# .env.example defines:
REDIS_HOST              # Custom convention
REDIS_PORT
```

> **Current Workaround:** `.env.example` now includes both naming conventions:
>
> ```bash
> REDIS_HOST=redis
> SPRING_DATA_REDIS_HOST=redis
> ```
>
> **Recommendation:** Standardize on Spring Boot conventions (`SPRING_DATA_*`) across all services.

### 8.4 Docker Compose Deployment

The system supports two deployment modes:

**Development Mode** (`infrastructure/dev/`):

- Individual `compose.yaml` files per service (for local database instances)
- Used when `DOCKER_COMPOSE_ENABLED=true` - Spring Boot auto-starts dependent containers

**Production Mode** (`infrastructure/pro/`):

```bash
# From project root
cd infrastructure/pro
cp .env.example .env
# Edit .env with your configuration
docker compose up -d
```

> **Important:** Always copy `.env.example` to `.env` and configure before deploying. Never commit `.env` to version control.

### 8.5 Transaction Management Gap

⚠️ **Important:** The `GenericService` class does **NOT** have `@Transactional` annotation by default. This means:

- Simple CRUD operations work fine (single repository call)
- Complex operations with multiple database calls may have partial failures
- Services requiring atomicity must add `@Transactional` explicitly

**Services with explicit `@Transactional`:**

- `AppointmentService` (appointment-service) - Status transitions, walk-in, cancel, complete
- `ScheduleService` (hr-service) - Bulk schedule updates
- `LabOrderService` (medical-exam-service) - Status transitions
- `LabTestResultService` (medical-exam-service) - Result updates
- `PrescriptionController` (medical-exam-service) - Create with items
- `MedicineController` (medicine-service) - Stock updates

> **Recommendation:** Consider adding `@Transactional` to `GenericService` or document clearly that services requiring transactions must implement their own service layer.

---

_Document last verified: January 8, 2026_
_Codebase version: Spring Boot 3.5.8, Spring Cloud 2025.0.0 (Northfields), Java 23_
