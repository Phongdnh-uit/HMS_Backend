# JMeter Load Testing for HMS Backend

## 📋 Overview

This directory contains JMeter test plans for load testing the Hospital Management System (HMS) backend services.

## 🛠️ Prerequisites

### 1. Install JMeter

**Option A: Download from Apache**
```powershell
# Download JMeter 5.6.3 (latest stable)
# https://jmeter.apache.org/download_jmeter.cgi
# Extract to C:\apache-jmeter-5.6.3
```

**Option B: Using Chocolatey**
```powershell
choco install jmeter
```

**Option C: Using Scoop**
```powershell
scoop install jmeter
```

### 2. Set Environment Variable (Optional)
```powershell
$env:JMETER_HOME = "C:\apache-jmeter-5.6.3"
$env:PATH += ";$env:JMETER_HOME\bin"
```

## 📁 Directory Structure

```
jmeter/
├── README.md                          # This file
├── test-plans/
│   ├── HMS_LoadTest_50VU.jmx          # Main 50 VU load test
│   ├── HMS_ConcurrentLogins.jmx       # Login stress test
│   ├── HMS_AppointmentBooking.jmx     # Appointment booking test
│   └── HMS_ReadQueries.jmx            # Read-heavy test
├── data/
│   ├── patient_credentials.csv        # Patient login data
│   ├── doctor_credentials.csv         # Doctor login data
│   ├── nurse_credentials.csv          # Nurse login data
│   └── receptionist_credentials.csv   # Receptionist login data
├── results/                           # Test results output
└── run-tests.ps1                      # Test execution script
```

## 🚀 Quick Start

### 1. Verify Services are Running
```powershell
# Check all containers
docker ps

# Ensure these services are healthy:
# - api-gateway-pro (8080)
# - auth-service-pro
# - patient-service-pro
# - appointment-service-pro
# - medical-exam-service-pro
```

### 2. Run Load Test (GUI Mode - for debugging)
```powershell
jmeter -t test-plans\HMS_LoadTest_50VU.jmx
```

### 3. Run Load Test (Non-GUI Mode - for actual testing)
```powershell
.\run-tests.ps1
# Or manually:
jmeter -n -t test-plans\HMS_LoadTest_50VU.jmx -l results\results.jtl -e -o results\report
```

### 4. View Results
```powershell
# Open HTML report
start results\report\index.html
```

## 📊 Test Scenarios (Scaled for 8GB System)

| Test Plan | VUs | Duration | Focus Area |
|-----------|-----|----------|------------|
| **HMS_LoadTest_50VU** | 50 | 5 min | Combined business flow |
| **HMS_ConcurrentLogins** | 30 | 3 min | Auth service stress |
| **HMS_AppointmentBooking** | 20 | 3 min | Booking concurrency |
| **HMS_ReadQueries** | 50 | 5 min | Read performance |

## ⚙️ Configuration

### API Gateway URL
Default: `http://localhost:8080`

To change, edit the User Defined Variables in each `.jmx` file or use:
```powershell
jmeter -Jhost=your-host -Jport=8080 -t test-plans\HMS_LoadTest_50VU.jmx
```

## 📈 Acceptance Criteria (50 VU Scale)

| Metric | Target |
|--------|--------|
| Response Time (P95) | < 2000ms |
| Error Rate | < 5% |
| Throughput | > 20 req/s |
| Login Success | 100% |

## 🔧 Troubleshooting

### JMeter runs out of memory
```powershell
# Edit jmeter.bat or set:
set HEAP=-Xms512m -Xmx1024m
```

### Connection refused errors
- Ensure Docker containers are running
- Check API Gateway port (8080)
- Verify service discovery is healthy

### Slow response times
- Reduce VU count to 25-30
- Increase think time
- Check Docker memory usage

## 📝 Notes

- Test data uses seeded accounts from `seed-loadtest-data.sql`
- Patient accounts: `patient1@email.com` to `patient1000@email.com` (Password: `Patient123!@`)
- Doctor accounts: `doctor1@hms.com` to `doctor60@hms.com` (Password: `Doctor123!@`)
- Nurse accounts: `nurse1@hms.com` to `nurse50@hms.com` (Password: `Nurse123!@`)
- Receptionist accounts: `receptionist1@hms.com` to `receptionist40@hms.com` (Password: `Receptionist123!@`)
- Admin accounts: `admin1@hms.com` to `admin5@hms.com` (Password: `Admin123!@`)

