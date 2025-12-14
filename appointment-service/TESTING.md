# Appointment Service Testing Guide

This guide explains how to test the appointment-service and its integration with hr-service and patient-service.

## Prerequisites

### Required Services Running
1. **auth-service** (port 8081) - For JWT tokens
2. **patient-service** (port 8083) - For patient validation
3. **hr-service** (port 8084) - For doctor/schedule validation
4. **appointment-service** (port 8085) - The service under test

### Start Services
```bash
# Start each service in separate terminals
cd HMS_Backend
./gradlew :auth-service:bootRun
./gradlew :patient-service:bootRun
./gradlew :hr-service:bootRun
./gradlew :appointment-service:bootRun
```

---

## Step 1: Get Authentication Token

```bash
# Login as admin
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

Save the token:
```bash
export TOKEN="<access_token_from_response>"
```

---

## Step 2: Create Test Data

### 2.1 Create a Department (hr-service)
```bash
curl -X POST http://localhost:8084/hr/departments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Cardiology",
    "description": "Heart specialists"
  }'
```
Save: `DEPT_ID`

### 2.2 Create a Doctor (hr-service)
```bash
curl -X POST http://localhost:8084/hr/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Dr. John Smith",
    "role": "DOCTOR",
    "departmentId": "<DEPT_ID>",
    "email": "john.smith@hospital.com",
    "phone": "1234567890",
    "specialization": "Cardiologist"
  }'
```
Save: `DOCTOR_ID`

### 2.3 Create a Schedule for Today (hr-service)
```bash
# Get today's date in YYYY-MM-DD format
TODAY=$(date +%Y-%m-%d)

curl -X POST http://localhost:8084/hr/schedules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"employeeId\": \"<DOCTOR_ID>\",
    \"workDate\": \"$TODAY\",
    \"startTime\": \"09:00\",
    \"endTime\": \"17:00\"
  }"
```
Save: `SCHEDULE_ID`

### 2.4 Create a Patient (patient-service)
```bash
curl -X POST http://localhost:8083/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Jane Doe",
    "dateOfBirth": "1990-05-15",
    "gender": "FEMALE",
    "phoneNumber": "0987654321",
    "address": "123 Main St"
  }'
```
Save: `PATIENT_ID`

---

## Step 3: Test Appointment Creation

### 3.1 Create Appointment (Happy Path)
```bash
# Use a future time within schedule hours (e.g., 10:00 today)
APPOINTMENT_TIME="${TODAY}T10:00:00Z"

curl -X POST http://localhost:8085/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"<PATIENT_ID>\",
    \"doctorId\": \"<DOCTOR_ID>\",
    \"appointmentTime\": \"$APPOINTMENT_TIME\",
    \"type\": \"CONSULTATION\",
    \"reason\": \"Annual checkup\",
    \"notes\": \"First visit\"
  }"
```

**Expected Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": "...",
    "patient": { "id": "...", "fullName": "Jane Doe" },
    "doctor": { "id": "...", "fullName": "Dr. John Smith", "department": "Cardiology" },
    "status": "SCHEDULED",
    "type": "CONSULTATION"
  }
}
```

Save: `APPOINTMENT_ID`

---

## Step 4: Test Validation Rules

### 4.1 Invalid Patient
```bash
curl -X POST http://localhost:8085/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "non-existent-id",
    "doctorId": "<DOCTOR_ID>",
    "appointmentTime": "2025-12-15T10:00:00Z",
    "type": "CONSULTATION",
    "reason": "Test"
  }'
```
**Expected:** 404 - Patient not found

### 4.2 Invalid Doctor
```bash
curl -X POST http://localhost:8085/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "<PATIENT_ID>",
    "doctorId": "non-existent-id",
    "appointmentTime": "2025-12-15T10:00:00Z",
    "type": "CONSULTATION",
    "reason": "Test"
  }'
```
**Expected:** 404 - Doctor not found

### 4.3 Outside Schedule Hours
```bash
curl -X POST http://localhost:8085/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"<PATIENT_ID>\",
    \"doctorId\": \"<DOCTOR_ID>\",
    \"appointmentTime\": \"${TODAY}T20:00:00Z\",
    \"type\": \"CONSULTATION\",
    \"reason\": \"Test\"
  }"
```
**Expected:** 400 - Outside doctor's schedule hours

### 4.4 Double Booking
```bash
# Create second appointment at same time
curl -X POST http://localhost:8085/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"<PATIENT_ID>\",
    \"doctorId\": \"<DOCTOR_ID>\",
    \"appointmentTime\": \"$APPOINTMENT_TIME\",
    \"type\": \"FOLLOW_UP\",
    \"reason\": \"Duplicate test\"
  }"
```
**Expected:** 400 - Time slot already booked

---

## Step 5: Test Appointment Operations

### 5.1 Get Appointment
```bash
curl -X GET "http://localhost:8085/appointments/<APPOINTMENT_ID>" \
  -H "Authorization: Bearer $TOKEN"
```

### 5.2 List All Appointments
```bash
curl -X GET "http://localhost:8085/appointments" \
  -H "Authorization: Bearer $TOKEN"
```

### 5.3 Complete Appointment
```bash
curl -X PATCH "http://localhost:8085/appointments/<APPOINTMENT_ID>/complete" \
  -H "Authorization: Bearer $TOKEN"
```
**Expected:** Status changes to `COMPLETED`

### 5.4 Cancel Appointment (create new one first)
```bash
# Create another appointment
NEW_TIME="${TODAY}T14:00:00Z"
curl -X POST http://localhost:8085/appointments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"patientId\": \"<PATIENT_ID>\",
    \"doctorId\": \"<DOCTOR_ID>\",
    \"appointmentTime\": \"$NEW_TIME\",
    \"type\": \"CONSULTATION\",
    \"reason\": \"To be cancelled\"
  }"
# Save the new APPOINTMENT_ID

# Cancel it
curl -X PATCH "http://localhost:8085/appointments/<NEW_APPOINTMENT_ID>/cancel" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cancelReason": "Patient requested cancellation"}'
```
**Expected:** Status changes to `CANCELLED`, cancelledAt and cancelReason populated

---

## Step 6: Test HR-Service Integration

### 6.1 Cancel Schedule (Cascades to Appointments)
```bash
curl -X POST "http://localhost:8084/hr/schedules/<SCHEDULE_ID>/cancel" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Doctor unavailable"}'
```
**Expected:**
- Schedule status → `CANCELLED`
- All SCHEDULED appointments for that doctor on that date → `CANCELLED`

### 6.2 Verify Appointments Cancelled
```bash
curl -X GET "http://localhost:8085/appointments?doctorId=<DOCTOR_ID>" \
  -H "Authorization: Bearer $TOKEN"
```
**Expected:** All appointments show `status: CANCELLED`

---

## Quick Reference

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/appointments` | POST | Create appointment |
| `/appointments` | GET | List appointments |
| `/appointments/{id}` | GET | Get by ID |
| `/appointments/{id}` | PATCH | Update appointment |
| `/appointments/{id}/cancel` | PATCH | Cancel appointment |
| `/appointments/{id}/complete` | PATCH | Complete appointment |
| `/appointments/bulk-cancel` | POST | Bulk cancel (internal) |
| `/appointments/count` | GET | Count appointments (internal) |
| `/appointments/bulk-restore` | POST | Compensation restore (internal) |

## Business Rules Summary

1. **Appointment Time**: Must be in the future
2. **Patient**: Must exist in patient-service
3. **Doctor**: Must exist and have role `DOCTOR` in hr-service
4. **Schedule**: Doctor must have a schedule for the appointment date
5. **Time Range**: Appointment must fit within schedule hours (30-minute duration)
6. **No Double Booking**: Cannot book same doctor at overlapping times
7. **Status Transitions**:
   - Only `SCHEDULED` → `COMPLETED`
   - Only `SCHEDULED` → `CANCELLED`
