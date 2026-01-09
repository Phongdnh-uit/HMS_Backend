# HMS Backend - Performance Tests

This module contains Gatling-based performance tests for the HMS (Hospital Management System) Backend.

## Prerequisites

1. **Java 17+** - Required for Gatling
2. **Gradle** - Build tool (wrapper included)
3. **Running HMS Services** - All microservices should be running

## Test Data

Performance tests require seeded data in the database. The seed data includes:

- 1,000 patient accounts
- 60 doctor accounts with schedules
- 50 nurse accounts
- 40 receptionist accounts
- 5 admin accounts
- 200 medicine items
- 50 lab test templates

To seed data:

```powershell
# From project root
.\infrastructure\pro\seed-loadtest-data.ps1
```

## Running Tests

### Individual Tests

```bash
# PERF-LOAD-001: 100 Concurrent User Logins
./gradlew :performance-tests:loadTestConcurrentLogins

# PERF-LOAD-001 (500 VU): Combined Business Flow
./gradlew :performance-tests:combinedBusinessFlow

# PERF-LOAD-002: 50 Concurrent Appointment Bookings
./gradlew :performance-tests:loadTestConcurrentBookings

# PERF-LOAD-003: 1000 Concurrent Read Queries
./gradlew :performance-tests:loadTestConcurrentQueries

# PERF-LOAD-004: Gateway Routing Under Load
./gradlew :performance-tests:loadTestGatewayRouting

# Stress Tests
./gradlew :performance-tests:stressTestDbPool
./gradlew :performance-tests:stressTestMemory
./gradlew :performance-tests:stressTestRecovery

# Endurance Tests
./gradlew :performance-tests:enduranceTest24h
./gradlew :performance-tests:enduranceTestMemoryLeak
```

### Run All Load Tests

```bash
./gradlew :performance-tests:runAllLoadTests
```

### Using Gatling directly

```bash
./gradlew :performance-tests:gatlingRun
```

## Configuration

### Environment Variables

| Variable       | Default                 | Description      |
| -------------- | ----------------------- | ---------------- |
| `HMS_BASE_URL` | `http://localhost:8080` | API Gateway URL  |
| `HMS_AUTH_URL` | Same as BASE_URL        | Auth service URL |

### Test Accounts

All test accounts use the password: `Password@123`

| Role         | Email Pattern                | Count |
| ------------ | ---------------------------- | ----- |
| Admin        | `admin{1-5}@hms.com`         | 5     |
| Doctor       | `doctor{1-60}@hms.com`       | 60    |
| Nurse        | `nurse{1-50}@hms.com`        | 50    |
| Receptionist | `receptionist{1-40}@hms.com` | 40    |
| Patient      | `patient{1-1000}@email.com`  | 1000  |

## Test Descriptions

### Load Tests

| ID            | Test                | VUs  | Duration | Description                   |
| ------------- | ------------------- | ---- | -------- | ----------------------------- |
| PERF-LOAD-001 | Concurrent Logins   | 100  | 6 min    | Morning peak login simulation |
| PERF-LOAD-001 | Combined Flow       | 500  | 30 min   | Full hospital day simulation  |
| PERF-LOAD-002 | Concurrent Bookings | 50   | 4 min    | Peak booking hours            |
| PERF-LOAD-003 | Concurrent Queries  | 1000 | 11 min   | Read-heavy workload           |
| PERF-LOAD-004 | Gateway Routing     | 500  | 15 min   | Mixed service traffic         |

### Acceptance Criteria

| Metric      | Target      |
| ----------- | ----------- |
| Login P95   | < 500ms     |
| Query P95   | < 200ms     |
| Booking P95 | < 800ms     |
| Error Rate  | < 1%        |
| Throughput  | > 250 req/s |

## Reports

Gatling generates HTML reports in:

```
performance-tests/build/reports/gatling/
```

Open `index.html` in a browser to view detailed metrics.

## Architecture

```
performance-tests/
├── build.gradle.kts          # Gatling plugin configuration
├── src/gatling/
│   └── scala/
│       └── simulations/
│           ├── HmsSimulationBase.scala    # Base configuration
│           ├── load/                       # Load test simulations
│           │   ├── ConcurrentLoginsSimulation.scala
│           │   ├── ConcurrentBookingsSimulation.scala
│           │   ├── ConcurrentQueriesSimulation.scala
│           │   ├── GatewayRoutingSimulation.scala
│           │   └── CombinedBusinessFlowSimulation.scala
│           ├── stress/                     # Stress test simulations
│           │   ├── DbConnectionPoolSimulation.scala
│           │   ├── MemoryUsageSimulation.scala
│           │   └── ServiceRecoverySimulation.scala
│           └── endurance/                  # Endurance test simulations
│               ├── ContinuousOperationSimulation.scala
│               └── MemoryLeakDetectionSimulation.scala
```

## Notes

- Tests are designed for 8GB RAM / 4 CPU cores environment
- Reduce VU counts proportionally for lower-spec machines
- Monitor container resources during tests with `docker stats`
- Database connection pool limit: 50 connections
