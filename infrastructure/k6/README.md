# HMS Backend - k6 Load Testing

Modern, JavaScript-based load testing for the HMS Backend APIs.

## Prerequisites

1. **Install k6**: https://k6.io/docs/get-started/installation/
   ```powershell
   winget install GrafanaLabs.k6
   ```

2. **Start HMS Backend**:
   ```powershell
   cd infrastructure/pro
   docker compose up -d
   ```

3. **Seed Test Data**:
   ```powershell
   # For up to 50 VUs
   cd infrastructure/pro
   .\seed-50vu.ps1
   
   # For 50-200 VUs
   cd infrastructure/k6
   .\seed-200vu.ps1
   ```

## Quick Start

```powershell
cd infrastructure/k6

# Run with default 50 VUs for 5 minutes
k6 run load-test.js

# Run with 100 VUs
k6 run -e VUS=100 load-test.js

# Run with 200 VUs for 10 minutes
k6 run -e VUS=200 -e DURATION=10m load-test.js

# Run with 500 VUs
k6 run -e VUS=500 load-test.js
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VUS` | 50 | Total number of virtual users |
| `DURATION` | 5m | Test duration |
| `BASE_URL` | http://localhost:8080 | API base URL |
| `PATIENTS` | 120 | Number of patient accounts available |
| `DOCTORS` | 40 | Number of doctor accounts available |
| `NURSES` | 30 | Number of nurse accounts available |
| `RECEPTIONISTS` | 25 | Number of receptionist accounts available |

## VU Distribution

The VUs are automatically distributed proportionally:

| Role         | Percentage | 50 VUs | 100 VUs | 200 VUs | 500 VUs |
|--------------|------------|--------|---------|---------|---------|
| Patient      | 55%        | 28     | 55      | 110     | 275     |
| Doctor       | 18%        | 9      | 18      | 36      | 90      |
| Nurse        | 14%        | 7      | 14      | 28      | 70      |
| Receptionist | 12%        | 6      | 12      | 24      | 60      |
| Admin        | 1%         | 1      | 1       | 2       | 5       |

## Test Scenarios

### Patient Workflow (55%)
- Login → View Profile → Browse Doctors → Book Appointment
- Login → View Profile → View Appointments → View Medicines

### Doctor Workflow (18%)
- Login → View Appointments → View Patients → Create Medical Exam → View Medicines

### Nurse Workflow (14%)
- Login → View Patients → Record Vital Signs → Update Lab Test Results

### Receptionist Workflow (12%)
- Walk-in: Search Patient → Create Walk-in Appointment
- Booking: Search Patient → View Schedules → View Doctors
- Billing: View Appointments → View/Create Invoices

### Admin Workflow (1%)
- Staff/Schedule/Medicine/Reports management cycles

## Test Accounts

| Role         | Email Pattern              | Password           | Count |
|--------------|---------------------------|--------------------| ------|
| Admin        | admin1-5@hms.com          | Admin123!@         | 5     |
| Doctor       | doctor1-40@hms.com        | Doctor123!@        | 40    |
| Nurse        | nurse1-30@hms.com         | Nurse123!@         | 30    |
| Receptionist | receptionist1-25@hms.com  | Receptionist123!@  | 25    |
| Patient      | patient1-120@email.com    | Patient123!@       | 120   |

## Acceptance Criteria

- **Response Time**: p(95) < 3000ms
- **Error Rate**: < 10%
- **Login Success**: > 95%

## Files

| File | Description |
|------|-------------|
| `load-test.js` | Main configurable test script |
| `seed-200vu.ps1` | Seed script for 200+ VUs |
| `load-test-50vu.js` | Fixed 50 VU test (legacy) |
| `load-test-200vu.js` | Fixed 200 VU test (legacy) |
