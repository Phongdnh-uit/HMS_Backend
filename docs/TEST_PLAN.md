
## 📦 Phase 6: Performance Tests (11 tests)

### 🖥️ System Resources: 8GB RAM | 4 Logical Cores

**Optimized Container Resource Allocation (Constrained Environment):**

| Container                | CPU Limit | CPU Reserved | RAM Limit  | RAM Reserved | Priority |
| ------------------------ | --------- | ------------ | ---------- | ------------ | -------- |
| **PostgreSQL**           | 1.0       | 0.5          | 1.5GB      | 1.0GB        | High     |
| **API Gateway**          | 0.5       | 0.25         | 512MB      | 384MB        | High     |
| **Discovery Service**    | 0.25      | 0.1          | 384MB      | 256MB        | High     |
| **Auth Service**         | 0.5       | 0.25         | 640MB      | 512MB        | High     |
| **Patient Service**      | 0.4       | 0.2          | 512MB      | 384MB        | Medium   |
| **Appointment Service**  | 0.5       | 0.25         | 640MB      | 512MB        | High     |
| **Medical Exam Service** | 0.4       | 0.2          | 512MB      | 384MB        | Medium   |
| **Medicine Service**     | 0.25      | 0.1          | 384MB      | 256MB        | Low      |
| **HR Service**           | 0.4       | 0.2          | 512MB      | 384MB        | Medium   |
| **Billing Service**      | 0.4       | 0.2          | 512MB      | 384MB        | Medium   |
| **Notification Service** | 0.15      | 0.05         | 256MB      | 128MB        | Low      |
| **Config Server**        | 0.2       | 0.1          | 384MB      | 256MB        | Medium   |
| **Load Test Tool**       | 1.0       | 0.5          | 1.0GB      | 768MB        | -        |
| **TOTAL**                | **5.95**  | **2.9**      | **7.75GB** | **5.5GB**    | -        |

**Notes:**

- Total reserved: 5.5GB RAM (leaves ~2.5GB for Windows OS overhead)
- CPU limits allow bursting up to 5.95 cores (time-sliced on 4 cores)
- Database connection pool reduced to 50 max connections
- JVM heap sizes: -Xmx384m to -Xmx512m per service
- Consider running non-critical services on-demand only
- Network: All containers share host network

**Recommended JVM Settings (per microservice):**

```
JAVA_OPTS=-Xms256m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

**Docker Compose Settings:**

```yaml
version: "3.8"
services:
  postgres:
    cpus: "1.0"
    cpu_shares: 1024
    mem_limit: 1536m
    mem_reservation: 1024m
    environment:
      - POSTGRES_MAX_CONNECTIONS=50
      - POSTGRES_SHARED_BUFFERS=256MB

  api-gateway:
    cpus: "0.5"
    cpu_shares: 512
    mem_limit: 512m
    mem_reservation: 384m

  auth-service:
    cpus: "0.5"
    cpu_shares: 512
    mem_limit: 640m
    mem_reservation: 512m
    environment:
      - JAVA_OPTS=-Xms256m -Xmx512m

  appointment-service:
    cpus: "0.5"
    cpu_shares: 512
    mem_limit: 640m
    mem_reservation: 512m
    environment:
      - JAVA_OPTS=-Xms256m -Xmx512m
```

**⚠️ Resource Optimization Tips for 8GB System:**

1. Run load test tool on a separate machine if possible
2. Disable non-essential services during testing (notification, config-server)
3. Use external database instead of containerized PostgreSQL
4. Reduce VU count to 200-300 for stable testing
5. Increase think times to reduce concurrent load

---

### 📊 Load Test Data Seeding Plan

**Scripts Location:** `infrastructure/pro/`

| File                     | Description                                        |
| ------------------------ | -------------------------------------------------- |
| `seed-loadtest-data.sql` | Complete SQL seed data with all entities           |
| `seed-loadtest-data.ps1` | PowerShell automation script for Docker DB seeding |

#### Data Volume Summary

| Entity                 | Count | Purpose                                                                          |
| ---------------------- | ----- | -------------------------------------------------------------------------------- |
| **Accounts**           | 1,155 | Auth data (5 admin + 60 doctors + 50 nurses + 40 receptionists + 1,000 patients) |
| **Patients**           | 1,000 | Patient profiles with full demographics                                          |
| **Employees**          | 155   | Staff records (60 doctors + 50 nurses + 40 receptionists + 5 admins)             |
| **Departments**        | 10    | Hospital departments (Cardiology, Neurology, etc.)                               |
| **Employee Schedules** | 420   | Doctor availability (60 doctors × 7 days)                                        |
| **Medicines**          | 200   | Medicine catalog across 10 categories                                            |
| **Categories**         | 10    | Medicine categories (Antibiotics, Analgesics, etc.)                              |
| **Lab Tests**          | 50    | Lab test definitions (20 LAB + 20 IMAGING + 10 PATHOLOGY)                        |
| **Appointments**       | 500   | Historical appointment data for testing                                          |

#### Seeding Commands

```powershell
# Execute all seed scripts
.\infrastructure\pro\seed-loadtest-data.ps1

# Reset and reseed (truncate tables first)
.\infrastructure\pro\seed-loadtest-data.ps1 -Reset

# Verify existing data
.\infrastructure\pro\seed-loadtest-data.ps1 -Verify
```

#### Entity Field Mapping (Verified from Source Code)

| Service              | Table                | Key Fields (snake_case)                                                              |
| -------------------- | -------------------- | ------------------------------------------------------------------------------------ |
| auth-service         | `accounts`           | `id`, `email`, `password`, `role`, `email_verified`                                  |
| patient-service      | `patient`            | `id`, `account_id`, `full_name`, `email`, `date_of_birth`, `gender`, `phone_number`  |
| hr-service           | `employees`          | `id`, `account_id`, `full_name`, `role`, `department_id`, `status`, `hired_at`       |
| hr-service           | `departments`        | `id`, `name`, `description`, `location`, `phone_extension`, `status`                 |
| hr-service           | `employee_schedules` | `id`, `employee_id`, `work_date`, `start_time`, `end_time`, `status`                 |
| medicine-service     | `medicine`           | `id`, `name`, `active_ingredient`, `unit`, `quantity`, `selling_price`, `expires_at` |
| medicine-service     | `category`           | `id`, `name`, `description`                                                          |
| medical-exam-service | `lab_tests`          | `id`, `code`, `name`, `category`, `price`, `normal_range`, `is_active`               |
| appointment-service  | `appointment`        | `id`, `patient_id`, `doctor_id`, `appointment_time`, `status`, `type`                |

#### Enum Values Reference

| Enum                | Values                                                          |
| ------------------- | --------------------------------------------------------------- |
| `RoleEnum`          | `ADMIN`, `PATIENT`, `DOCTOR`, `NURSE`, `RECEPTIONIST`           |
| `EmployeeRole`      | `DOCTOR`, `NURSE`, `RECEPTIONIST`, `ADMIN`                      |
| `EmployeeStatus`    | `ACTIVE`, `ON_LEAVE`, `RESIGNED`                                |
| `ScheduleStatus`    | `AVAILABLE`, `BOOKED`, `PENDING_CANCEL`, `CANCELLED`            |
| `Gender`            | `MALE`, `FEMALE`, `OTHER`                                       |
| `AppointmentStatus` | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
| `AppointmentType`   | `CONSULTATION`, `FOLLOW_UP`, `EMERGENCY`, `WALK_IN`             |
| `LabTestCategory`   | `LAB`, `IMAGING`, `PATHOLOGY`                                   |
| `DepartmentStatus`  | `ACTIVE`, `INACTIVE`                                            |

---

### Load Tests (5 tests)

#### PERF-LOAD-001: Combined Business Flow - 500 Concurrent Users

| ID            | PERF-LOAD-001                                                                                        |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **Test Case** | 500 VU Combined Business Flow (Full Hospital Day Simulation)                                         |
| **Status**    | [ ]                                                                                                  |
| **Scenario**  | Comprehensive load test simulating all HMS user roles executing core business workflows concurrently |

---

#### 👥 Role Distribution & Virtual User Allocation (500 VUs Total)

| Role             | VUs | % of Total | Primary Workflows                                      | Think Time | Session Duration |
| ---------------- | --- | ---------- | ------------------------------------------------------ | ---------- | ---------------- |
| **Patient**      | 275 | 55%        | Registration, Booking, View Records, Payment           | 5-10s      | 15-30 min        |
| **Doctor**       | 90  | 18%        | View Schedule, Examine Patients, Prescribe, Lab Orders | 10-20s     | Full test        |
| **Nurse**        | 70  | 14%        | Check Vitals, Update Records, Assist Doctor            | 5-15s      | Full test        |
| **Receptionist** | 60  | 12%        | Walk-in Registration, Appointment Booking, Billing     | 3-8s       | Full test        |
| **Admin**        | 5   | 1%         | Manage Staff, Departments, Reports, Medicine Stock     | 10-30s     | Full test        |

**Total**: 500 Virtual Users

---

#### 🔄 Detailed Workflow Scenarios by Role

### **1. PATIENT Workflow (275 VUs - 55%)**

**Sub-scenarios:**

- **Appointment Booking (140 VUs - 28%)**:

  ```
  1. POST /auth/login
  2. GET /schedules/doctors (browse doctors)
  3. GET /appointments/available-slots?doctorId=X&date=Y
  4. POST /appointments (book appointment)
  5. GET /appointments/by-patient/{id} (confirm)
  Duration: 5-10 minutes
  Think time: 5-10s between steps
  ```

- **View Medical History (70 VUs - 14%)**:

  ```
  1. POST /auth/login
  2. GET /patients/me
  3. GET /exams/by-patient/{id}
  4. GET /prescriptions/{id}
  5. GET /lab-results/by-patient/{id}
  Duration: 3-7 minutes
  ```

- **Payment Processing (65 VUs - 13%)**:
  ```
  1. POST /auth/login
  2. GET /invoices/by-patient/{id}
  3. POST /payments/init (VNPay)
  4. GET /payments/callback (simulate callback)
  5. GET /invoices/{id} (verify paid)
  Duration: 3-5 minutes
  ```

**Traffic Pattern**:

- Ramp-up: 0→250 in 2 minutes
- Sustain: 250 VUs for 20 minutes
- Variable arrivals (Poisson distribution)

---

### **2. DOCTOR Workflow (90 VUs - 18%)**

**Main Workflow (Continuous Loop):**

```
1. POST /auth/login
2. GET /appointments/by-doctor/{id}?status=CHECKED_IN (queue)
3. GET /appointments/{id} (patient details)
4. GET /patients/{id} (medical history)
5. GET /exams/by-patient/{id} (previous exams)

6. POST /exams (create medical exam)
   - diagnosis, symptoms, vital signs

7. POST /prescriptions (80% of cases)
   - Select medicines
   - Set dosage, frequency, duration
   - Add instructions

8. POST /lab-orders (40% of cases)
   - Order blood test, X-ray, etc.

9. PUT /appointments/{id} (mark COMPLETED)

10. Wait 10-20s (think time)
11. Loop back to step 2 (next patient)

Duration: Full test (30 minutes)
Patients per doctor: 5-8 during test
```

**Resource Intensity**:

- High read operations (patient history)
- Complex writes (exams, prescriptions)
- Multiple service interactions (Feign calls)

---

### **3. NURSE Workflow (70 VUs - 14%)**

**Main Workflow (Continuous Loop):**

```
1. POST /auth/login
2. GET /appointments/queue (waiting patients)
3. POST /appointments/{id}/check-in (patient arrival)
4. GET /patients/{id}
5. PUT /patients/{id} (update vital signs - weight, BP, temp)
6. POST /exams/{examId}/vitals (record vitals)
7. GET /lab-orders?status=PENDING (check lab queue)
8. PUT /lab-orders/{id}/status (update to IN_PROGRESS)
9. Wait 5-15s
10. Loop

Duration: Full test
Actions per nurse: 15-25 during test
```

**Resource Intensity**:

- Medium read/write balance
- Queue management
- Real-time updates

---

### **4. RECEPTIONIST Workflow (60 VUs - 12%)**

**Main Workflow (Continuous Loop):**

```
1. POST /auth/login

Scenario A - Walk-in Registration (30% of time):
  2a. POST /patients (register walk-in)
  3a. POST /appointments (create walk-in appointment)
  4a. POST /appointments/{id}/check-in

Scenario B - Appointment Booking (40% of time):
  2b. GET /patients?search=name/phone
  3b. GET /schedules/doctors
  4b. GET /appointments/available-slots
  5b. POST /appointments

Scenario C - Billing (30% of time):
  2c. GET /appointments?status=COMPLETED
  3c. POST /invoices (create invoice)
  4c. GET /invoices/{id}
  5c. POST /payments (cash payment)

6. Wait 3-8s
7. Loop

Duration: Full test
Transactions per receptionist: 20-35 during test
```

**Resource Intensity**:

- High transaction volume
- Mixed operations
- Critical path for patient flow

---

### **5. ADMIN Workflow (5 VUs - 1%)**

**Main Workflow:**

```
1. POST /auth/login

Cycle through scenarios:

Scenario A - Staff Management (every 5 min):
  2. GET /employees
  3. POST /employees (add new staff)
  4. PUT /employees/{id} (update schedule)
  5. GET /employees/doctors

Scenario B - Department Management (every 8 min):
  6. GET /departments
  7. POST /schedules (create doctor schedules)
  8. GET /schedules/doctors

Scenario C - Medicine Inventory (every 10 min):
  9. GET /medicines/low-stock
  10. PUT /medicines/{id}/stock (restock)
  11. GET /categories

Scenario D - Reports & Analytics (every 15 min):
  12. GET /appointments/stats
  13. GET /invoices/stats
  14. GET /patients?page=0&size=100 (patient list)

Duration: Full test
Think time: 10-30s between operations
```

**Resource Intensity**:

- Lower frequency, heavier queries
- Administrative operations
- Reporting/analytics

---

#### ⚙️ Test Execution Configuration

**Load Profile:**

```
Phase 1: Ramp-Up (5 minutes)
  - Minute 0-1: 100 VUs (staff login)
  - Minute 1-3: 100→300 VUs (gradual patient arrivals)
  - Minute 3-5: 300→500 VUs (peak morning rush)

Phase 2: Peak Load (20 minutes)
  - Sustained 500 VUs
  - All workflows running concurrently
  - Natural think times between actions

Phase 3: Ramp-Down (5 minutes)
  - 500→200 VUs (patients leaving)
  - 200→50 VUs (staff completing tasks)
  - 50→0 VUs (close)

Total Duration: 30 minutes
```

**Data Requirements:**

- 1,000 pre-registered patient accounts
- 60 doctor accounts with schedules
- 50 nurse accounts
- 40 receptionist accounts
- 5 admin accounts
- 500 available appointment slots
- 200 medicine items in inventory
- 50 lab test templates

---

#### 📊 Acceptance Criteria

**Response Time SLAs:**

- ✅ Login (P95): < 500ms
- ✅ Patient Registration (P95): < 1500ms
- ✅ Appointment Booking (P95): < 1000ms
- ✅ Medical Exam Creation (P95): < 2000ms
- ✅ Prescription Creation (P95): < 1500ms
- ✅ Lab Order Creation (P95): < 1000ms
- ✅ Payment Processing (P95): < 2000ms
- ✅ Query Operations (P95): < 300ms

**System Health:**

- ✅ Overall error rate < 1%
- ✅ No 500 errors (server crashes)
- ✅ 4xx errors < 0.5% (validation only)
- ✅ Database connection pool < 80% utilization
- ✅ CPU usage per container < 75%
- ✅ Memory usage stable (no growing trend)
- ✅ No circuit breaker trips

**Business Logic Validation:**

- ✅ Zero double-booking conflicts
- ✅ All appointments have unique time slots per doctor
- ✅ Prescriptions linked to valid exams
- ✅ Lab orders processed in FIFO order
- ✅ Invoice amounts calculated correctly
- ✅ Payment status updates reflected immediately
- ✅ Queue positions accurate
- ✅ Data consistency across services (eventual consistency < 5s)

**Throughput Targets:**

- ✅ Total requests/second: > 250 req/s
- ✅ Successful transactions: > 95%
- ✅ Concurrent active sessions: 500
- ✅ Database queries/second: > 500
- ✅ Feign client calls: < 200ms average

---

#### 🔍 Metrics Collection

**Application Metrics (per service):**

- Request count, rate, duration
- Error rate by endpoint
- JVM heap/non-heap memory
- GC frequency and duration
- Thread pool usage
- Circuit breaker state

**Database Metrics:**

- Active connections
- Query execution time
- Deadlocks/lock waits
- Transaction rate
- Cache hit ratio

**Infrastructure Metrics:**

- Container CPU %
- Container memory MB
- Network I/O
- Disk I/O
- API Gateway latency

**Business Metrics:**

- Appointments created/cancelled
- Patients registered
- Exams completed
- Prescriptions issued
- Lab orders processed
- Payments successful
- Revenue generated (mock)

---

#### 🛠️ Load Testing Tools Configuration

**Option 1: JMeter (Recommended)**

```xml
<ThreadGroup>
  <numThreads>500</numThreads>
  <rampUp>300</rampUp>
  <duration>1800</duration>
  <scheduler>true</scheduler>

  <!-- Patient Thread Group: 275 threads -->
  <!-- Doctor Thread Group: 90 threads -->
  <!-- Nurse Thread Group: 70 threads -->
  <!-- Receptionist Thread Group: 60 threads -->
  <!-- Admin Thread Group: 5 threads -->
</ThreadGroup>

<HTTPSamplerProxy>
  <connectTimeout>10000</connectTimeout>
  <responseTimeout>30000</responseTimeout>
</HTTPSamplerProxy>
```

**Option 2: Gatling (Alternative)**

```scala
setUp(
  patientScenario.inject(
    rampUsers(275) during (300 seconds)
  ),
  doctorScenario.inject(
    rampUsers(90) during (60 seconds)
  ),
  nurseScenario.inject(
    rampUsers(70) during (60 seconds)
  ),
  receptionistScenario.inject(
    rampUsers(60) during (60 seconds)
  ),
  adminScenario.inject(
    rampUsers(5) during (60 seconds)
  )
).protocols(httpProtocol)
  .assertions(
    global.responseTime.percentile(95).lt(2000),
    global.successfulRequests.percent.gt(95)
  )
```

---

#### 📋 Pre-Test Checklist

- [ ] All 12 containers deployed with resource limits
- [ ] Database seeded with test data (1000+ patients)
- [ ] JWT tokens pre-generated for test users
- [ ] Monitoring dashboards configured (Grafana)
- [ ] Prometheus scraping all services
- [ ] Load balancer configured (if using multiple instances)
- [ ] Test environment isolated from production
- [ ] Backup database before test
- [ ] Log aggregation ready (ELK/Loki)
- [ ] Network bandwidth verified
- [ ] Cleanup script prepared (delete test data post-run)

---

### Load Tests (Original - 4 tests)

#### PERF-LOAD-001: 100 Concurrent User Logins

| ID            | PERF-LOAD-001                                                                                          |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| **Test Case** | 100 concurrent logins                                                                                  |
| **Status**    | [x] ✅ ConcurrentLoginsSimulation.scala                                                                |
| **Scenario**  | Simulate morning peak hours when hospital staff (doctors, nurses, receptionists) log in simultaneously |

**Test Steps:**

1. Prepare 100 valid user accounts (mixed roles: 30 doctors, 30 nurses, 20 receptionists, 20 patients)
2. Execute concurrent login requests via API Gateway
3. Each thread performs: POST `/auth/login` → Validate JWT → GET `/auth/me`
4. Ramp-up: 0 to 100 users in 10 seconds
5. Sustain load for 5 minutes
6. Ramp-down: 100 to 0 in 10 seconds

**Acceptance Criteria:**

- ✅ 95th percentile response time < 500ms
- ✅ 99th percentile response time < 1000ms
- ✅ Error rate < 1%
- ✅ Successful JWT generation for all valid logins
- ✅ CPU usage < 70% across auth-service containers
- ✅ Memory usage remains stable (no leaks)
- ✅ Database connection pool < 80% utilization

**Metrics to Collect:**

- Response time (min, max, avg, p95, p99)
- Throughput (requests/second)
- Error rate
- CPU/Memory usage per service
- Database connection pool metrics
- JWT generation time

---

#### PERF-LOAD-002: 50 Concurrent Appointment Bookings

| ID            | PERF-LOAD-002                                                                                       |
| ------------- | --------------------------------------------------------------------------------------------------- |
| **Test Case** | 50 concurrent bookings                                                                              |
| **Status**    | [x] ✅ ConcurrentBookingsSimulation.scala                                                           |
| **Scenario**  | Simulate peak booking hours (8-9 AM) when patients book appointments through mobile/web application |

**Test Steps:**

1. Prepare test data:
   - 50 authenticated patient accounts
   - 10 active doctors with available schedules
   - Schedule slots: 100 available slots across different times
2. Each virtual user performs:
   - GET `/schedules/doctors` (find available doctors)
   - GET `/appointments/available-slots?doctorId=X&date=Y` (check slots)
   - POST `/appointments` (book appointment)
   - GET `/appointments/by-patient/{id}` (verify booking)
3. Concurrent execution: 50 users booking simultaneously
4. Duration: 3 minutes sustained load

**Acceptance Criteria:**

- ✅ 95th percentile response time < 800ms for booking
- ✅ 99th percentile response time < 1500ms
- ✅ Zero double-booking errors (data consistency)
- ✅ All 50 bookings successful without conflicts
- ✅ Pessimistic locking prevents race conditions
- ✅ Queue system handles overflow correctly
- ✅ Notification service triggered for each booking
- ✅ Error rate < 0.5%

**Business Logic Validation:**

- No overlapping appointments for same doctor/time slot
- Appointment status correctly transitions
- Queue position assigned when slots full
- Audit trail recorded for each booking

**Metrics to Collect:**

- Booking success rate
- Conflict detection accuracy
- Database lock wait time
- Transaction rollback rate
- Feign client call latency (HR service for doctor info)

---

#### PERF-LOAD-003: 1000 Concurrent Read Queries

| ID            | PERF-LOAD-003                                                                                    |
| ------------- | ------------------------------------------------------------------------------------------------ |
| **Test Case** | 1000 concurrent queries                                                                          |
| **Status**    | [x] ✅ ConcurrentQueriesSimulation.scala                                                         |
| **Scenario**  | Simulate multiple staff members querying patient records, appointments, and medical exam history |

**Test Steps:**

1. Populate database with realistic data:
   - 5,000 patients
   - 10,000 appointments (historical + upcoming)
   - 3,000 medical exams
   - 2,000 prescriptions
2. Execute mixed read operations:
   - 30% GET `/patients?page=X&size=20` (paginated list)
   - 25% GET `/patients/{id}` (individual patient)
   - 20% GET `/appointments/by-doctor/{id}` (doctor's schedule)
   - 15% GET `/exams/by-patient/{id}` (medical history)
   - 10% GET `/prescriptions/{id}` (prescription details)
3. Ramp-up: 0 to 1000 users in 30 seconds
4. Sustain: 1000 concurrent users for 10 minutes
5. Ramp-down: 30 seconds

**Acceptance Criteria:**

- ✅ 95th percentile response time < 200ms
- ✅ 99th percentile response time < 500ms
- ✅ Throughput > 500 requests/second
- ✅ Database query execution time < 50ms (avg)
- ✅ Proper index utilization (check query plans)
- ✅ Cache hit ratio > 70% (if caching enabled)
- ✅ Error rate < 0.1%
- ✅ No database connection timeouts

**Performance Optimizations to Verify:**

- Database connection pooling efficiency
- JPA query optimization (N+1 prevention)
- Pagination performance
- Index usage on foreign keys
- Feign client circuit breaker functionality

**Metrics to Collect:**

- Request distribution per endpoint
- Database connection pool usage
- Cache hit/miss ratio
- Query execution time breakdown
- Network latency between services

---

#### PERF-LOAD-004: Gateway Routing Under Load

| ID            | PERF-LOAD-004                                                                          |
| ------------- | -------------------------------------------------------------------------------------- |
| **Test Case** | Gateway routing under load                                                             |
| **Status**    | [x] ✅ GatewayRoutingSimulation.scala                                                  |
| **Scenario**  | Test API Gateway routing, load balancing, and circuit breaker under mixed traffic load |

**Test Steps:**

1. Configure multiple instances:
   - API Gateway: 2 instances
   - Each microservice: 2 instances
   - Discovery Service: 1 instance
2. Generate mixed traffic (500 concurrent users):
   - 20% Authentication requests → auth-service
   - 20% Patient operations → patient-service
   - 20% Appointment operations → appointment-service
   - 15% Medical exam operations → medical-exam-service
   - 15% Billing operations → billing-service
   - 10% HR operations → hr-service
3. Simulate service failure scenarios:
   - Kill 1 instance of appointment-service mid-test
   - Introduce 3-second delay in medical-exam-service
4. Duration: 15 minutes

**Acceptance Criteria:**

- ✅ Gateway routing latency < 50ms (overhead)
- ✅ Successful failover to healthy instances
- ✅ Circuit breaker opens after 50% error threshold
- ✅ Load distribution: 50/50 between instances (±5%)
- ✅ JWT validation at gateway < 20ms
- ✅ Header injection (X-User-Id, X-User-Role) working
- ✅ Rate limiting enforced (if configured)
- ✅ Overall error rate < 2% during failure scenarios
- ✅ Service discovery updates within 30 seconds

**Gateway Features to Validate:**

- Spring Cloud Gateway routing rules
- Resilience4j circuit breaker patterns
- Token validation performance
- CORS handling under load
- WebFlux non-blocking I/O efficiency

**Metrics to Collect:**

- Routing overhead (latency added by gateway)
- Circuit breaker state transitions
- Load balancer distribution
- Failed requests during instance failure
- Service discovery sync time

---

### Stress Tests (3 tests)

#### PERF-STRESS-001: Database Connection Pool Limits

| ID            | PERF-STRESS-001                                                                    |
| ------------- | ---------------------------------------------------------------------------------- |
| **Test Case** | DB connection pool limits                                                          |
| **Status**    | [x] ✅ DbConnectionPoolSimulation.scala                                            |
| **Scenario**  | Push database connection pool to its limits and test graceful degradation/recovery |

**Test Steps:**

1. Configure connection pool:
   - Max connections: 100
   - Min idle: 10
   - Connection timeout: 30 seconds
2. Gradually increase concurrent database operations:
   - Start: 50 concurrent users
   - Increment: +25 users every minute
   - Peak: 300 concurrent users (3x pool size)
3. Each user performs complex queries:
   - JOIN operations across 3-4 tables
   - Transaction with multiple INSERT/UPDATE
   - Hold connection for 2-5 seconds
4. Monitor until connection pool exhaustion
5. Reduce load and verify recovery

**Acceptance Criteria:**

- ✅ Connection pool reaches 100 active connections
- ✅ Additional requests queue (not fail immediately)
- ✅ Connection timeout after 30 seconds for waiting requests
- ✅ Proper error handling: 503 Service Unavailable
- ✅ No connection leaks after load reduction
- ✅ Pool recovers to baseline within 60 seconds
- ✅ Circuit breaker opens to protect database
- ✅ Zero data corruption or transaction failures

**Failure Scenarios:**

- Long-running queries blocking connections
- Unintended connection leaks in code
- Transaction deadlocks under contention

**Metrics to Collect:**

- Active connections over time
- Waiting queue length
- Connection acquisition time
- Query execution time
- Timeout error rate
- Connection leak detection

---

#### PERF-STRESS-002: Memory Usage Under Load

| ID            | PERF-STRESS-002                                                                       |
| ------------- | ------------------------------------------------------------------------------------- |
| **Test Case** | Memory usage under load                                                               |
| **Status**    | [x] ✅ MemoryUsageSimulation.scala                                                    |
| **Scenario**  | Test service behavior when approaching JVM heap limits and trigger garbage collection |

**Test Steps:**

1. Configure JVM settings:
   - Initial heap: 512MB
   - Max heap: 2GB
   - GC: G1GC with logging enabled
2. Execute memory-intensive operations:
   - Fetch large datasets (1000+ patient records with full history)
   - Generate PDF reports for medical exams
   - Process file uploads (images, documents)
   - Export large Excel reports
3. Sustain load: 100 concurrent users for 30 minutes
4. Monitor heap usage, GC frequency, and response degradation

**Acceptance Criteria:**

- ✅ Heap usage stays below 80% of max (< 1.6GB)
- ✅ Full GC events < 5 during test period
- ✅ GC pause time < 200ms
- ✅ No OutOfMemoryError exceptions
- ✅ Response time degradation < 20% at 70% heap usage
- ✅ Proper pagination for large datasets
- ✅ File uploads handled with streaming (not loaded into memory)
- ✅ Memory released after request completion

**Memory Optimization Checks:**

- Lazy loading of JPA relationships
- DTOs prevent over-fetching
- Stream processing for large files
- Proper closure of resources (files, streams)
- Cache size limits enforced

**Metrics to Collect:**

- Heap usage over time
- GC frequency and duration
- Object allocation rate
- Memory leaks (growing old generation)
- Response time correlation with memory usage

---

#### PERF-STRESS-003: Service Recovery After Failure

| ID            | PERF-STRESS-003                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------ |
| **Test Case** | Service recovery                                                                                 |
| **Status**    | [x] ✅ ServiceRecoverySimulation.scala                                                           |
| **Scenario**  | Test system resilience and automatic recovery after cascading failures and network interruptions |

**Test Steps:**

1. Establish baseline load: 200 concurrent users
2. Inject failure scenarios sequentially:
   - **T+0min**: Kill patient-service instance (simulate crash)
   - **T+2min**: Network partition: appointment-service cannot reach HR service
   - **T+5min**: Database connection failure (restart PostgreSQL)
   - **T+8min**: API Gateway overload (rate limit exceeded)
3. Observe circuit breakers, fallbacks, retries
4. Restore all services
5. Verify full system recovery

**Acceptance Criteria:**

- ✅ Circuit breaker opens within 10 seconds of failure
- ✅ Fallback responses returned (cached data or default)
- ✅ Services auto-reconnect to database within 30 seconds
- ✅ Service discovery detects instance failure < 30 seconds
- ✅ Load redistributed to healthy instances
- ✅ Retry logic: 3 attempts with exponential backoff
- ✅ Error rate < 10% during failure
- ✅ Full recovery within 2 minutes after restoration
- ✅ No manual intervention required
- ✅ Transaction consistency maintained (no partial updates)

**Resilience Patterns to Validate:**

- Circuit Breaker (Resilience4j)
- Retry with backoff
- Timeout handling
- Bulkhead isolation
- Graceful degradation

**Metrics to Collect:**

- Circuit breaker state changes
- Retry attempt distribution
- Time to recovery
- Error rate during failure
- Impact radius (affected services)

---

### Endurance Tests (3 tests)

#### PERF-END-001: 24-Hour Continuous Operation

| ID            | PERF-END-001                                                                     |
| ------------- | -------------------------------------------------------------------------------- |
| **Test Case** | 24-hour operation                                                                |
| **Status**    | [x] ✅ ContinuousOperationSimulation.scala                                       |
| **Scenario**  | Simulate realistic hospital workload over 24 hours with varying traffic patterns |

**Test Steps:**

1. Create realistic daily traffic pattern:
   - **00:00-06:00**: Low (10 users) - Night shift, emergency only
   - **06:00-09:00**: Ramp-up (10→100 users) - Morning arrival
   - **09:00-12:00**: Peak (100-150 users) - Morning consultations
   - **12:00-14:00**: Medium (50-80 users) - Lunch break
   - **14:00-18:00**: Peak (100-150 users) - Afternoon consultations
   - **18:00-24:00**: Ramp-down (100→10 users) - Evening closure
2. Mixed operations throughout:
   - Patient registration: 5%
   - Appointment booking: 20%
   - Medical exams: 25%
   - Prescription management: 15%
   - Lab orders/results: 15%
   - Billing/payments: 10%
   - Reporting/queries: 10%
3. Run continuously for 24 hours
4. No restarts or manual interventions

**Acceptance Criteria:**

- ✅ Zero unhandled exceptions
- ✅ All services running continuously (100% uptime)
- ✅ Response time remains consistent (< 10% variance)
- ✅ Database maintains consistent performance
- ✅ No gradual performance degradation
- ✅ Scheduled tasks execute correctly (notifications, backups)
- ✅ Log file rotation working properly
- ✅ Session management: expired sessions cleaned up
- ✅ Metrics collection continuous without gaps

**Long-Running Concerns:**

- Thread pool stability
- Session/token cleanup
- Log file growth
- Temporary file cleanup
- Cache invalidation
- Background job execution

**Metrics to Collect (hourly snapshots):**

- Response time trends
- Error rate trends
- Memory usage trends
- CPU usage trends
- Database connection pool
- Thread pool usage
- Disk space usage

---

#### PERF-END-002: Memory Leak Detection

| ID            | PERF-END-002                                                                |
| ------------- | --------------------------------------------------------------------------- |
| **Test Case** | Memory leak detection                                                       |
| **Status**    | [x] ✅ MemoryLeakDetectionSimulation.scala                                  |
| **Scenario**  | Run extended tests specifically designed to expose memory leaks in services |

**Test Steps:**

1. Enable heap dump on OOM: `-XX:+HeapDumpOnOutOfMemoryError`
2. Execute repetitive operations over 12 hours:
   - Create and delete 10,000 appointments
   - Upload and delete 5,000 patient files
   - Generate 1,000 medical exam reports (PDF)
   - Process 2,000 billing invoices
3. Take heap dumps every 2 hours
4. Force full GC between cycles
5. Analyze heap growth patterns

**Acceptance Criteria:**

- ✅ Old generation heap stable (±5% variance)
- ✅ Objects properly garbage collected after use
- ✅ No ClassLoader leaks
- ✅ ThreadLocal variables cleaned up
- ✅ File handles closed properly
- ✅ Database connections returned to pool
- ✅ Cache size remains bounded
- ✅ No abandoned HTTP connections

**Common Leak Sources to Check:**

- Static collections growing unbounded
- Event listeners not deregistered
- ThreadLocal not cleared
- Unclosed streams/readers
- Cache without eviction policy
- Circular references preventing GC

**Tools to Use:**

- JProfiler / VisualVM
- Heap dump analysis (Eclipse MAT)
- GC logs analysis

**Metrics to Collect:**

- Heap size over time (young/old generation)
- GC frequency and type
- Object retention
- Class instance counts
- Native memory usage

---

#### PERF-END-003: Connection Leak Detection

| ID            | PERF-END-003                                                                  |
| ------------- | ----------------------------------------------------------------------------- |
| **Test Case** | Connection leak detection                                                     |
| **Status**    | [ ]                                                                           |
| **Scenario**  | Detect and prevent database and HTTP connection leaks over extended operation |

**Test Steps:**

1. Enable connection leak detection:
   - HikariCP leak detection threshold: 10 seconds
   - Feign client connection pool monitoring
2. Execute operations prone to leaks (8 hours):
   - Exception scenarios (rollback without close)
   - Timeout scenarios (abandoned connections)
   - Async operations with failures
   - Long-running transactions
3. Deliberately inject leak scenarios:
   - Service method throws exception before closing
   - Transaction timeout without cleanup
   - Feign client call never completes
4. Monitor connection pool exhaustion

**Acceptance Criteria:**

- ✅ Database connection pool size stable
- ✅ All connections returned within 30 seconds
- ✅ Leak detection warnings logged
- ✅ No connection pool exhaustion
- ✅ HTTP connection pool (Feign) stable
- ✅ Connections closed in finally blocks
- ✅ Transaction timeout triggers rollback + close
- ✅ Proper exception handling releases resources

**Connection Types to Monitor:**

- Database connections (HikariCP)
- HTTP connections (Feign, RestTemplate)
- File handles
- Socket connections
- Thread pool threads

**Detection Methods:**

- HikariCP leak detection logs
- Connection pool metrics
- Thread dump analysis
- Network connection monitoring (netstat)

**Metrics to Collect:**

- Active database connections over time
- Connection acquisition wait time
- Leaked connection count
- Connection lifetime distribution
- Pool exhaustion events

---

## 📊 Performance Test Execution Checklist

Before running performance tests:

- [ ] All services deployed in containers
- [ ] Database populated with realistic data volume
- [ ] Monitoring tools configured (Prometheus, Grafana)
- [ ] Load testing tool installed (JMeter/Gatling)
- [ ] Baseline metrics captured
- [ ] Test environment isolated from production
- [ ] Network bandwidth sufficient
- [ ] Backup/recovery plan in place

---

---

## 📦 Phase 7: Contract Tests (12 tests)

### API Contracts (7 tests)

| ID           | Test Case                     | Status | Notes |
| ------------ | ----------------------------- | ------ | ----- |
| CONTRACT-001 | Auth service contract         | [ ]    |       |
| CONTRACT-002 | Patient service contract      | [ ]    |       |
| CONTRACT-003 | Appointment service contract  | [ ]    |       |
| CONTRACT-004 | Medical exam service contract | [ ]    |       |
| CONTRACT-005 | Medicine service contract     | [ ]    |       |
| CONTRACT-006 | HR service contract           | [ ]    |       |
| CONTRACT-007 | Billing service contract      | [ ]    |       |

### Feign Client Contracts (5 tests)

| ID                 | Test Case                  | Status | Notes |
| ------------------ | -------------------------- | ------ | ----- |
| CONTRACT-FEIGN-001 | PatientClient contract     | [ ]    |       |
| CONTRACT-FEIGN-002 | HrClient contract          | [ ]    |       |
| CONTRACT-FEIGN-003 | AppointmentClient contract | [ ]    |       |
| CONTRACT-FEIGN-004 | BillingClient contract     | [ ]    |       |
| CONTRACT-FEIGN-005 | MedicalExamClient contract | [ ]    |       |

---

## 📝 Test Session Log

Use this section to track your testing sessions:

| Date       | Session   | Tests Completed | Notes       |
| ---------- | --------- | --------------- | ----------- |
| YYYY-MM-DD | Session 1 | 0               | Starting... |

---

_Last Updated: 2026-01-07_