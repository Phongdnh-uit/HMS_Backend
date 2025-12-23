#!/bin/bash

# Get fresh token
echo "=== Getting fresh token ==="
RESPONSE=$(curl -s --max-time 10 -X POST http://localhost:8080/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "patient@test.com", "password": "Patient@123"}')
TOKEN=$(echo $RESPONSE | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Token obtained: ${TOKEN:0:50}..."

# Test 1: Duplicate appointmentId (should fail)
echo ""
echo "=== TEST 1: Create exam with duplicate appointmentId (apt-004) ==="
echo "Expected: Should FAIL - exam already exists for apt-004"
curl -s --max-time 10 -X POST http://localhost:8080/medical-exam-service/api/exams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"appointmentId": "apt-004", "diagnosis": "Duplicate test", "symptoms": "test", "treatment": "test"}' | head -c 300
echo ""

# Test 2: Delete exam (should fail)
echo ""
echo "=== TEST 2: Delete exam (should FAIL - not allowed) ==="
curl -s --max-time 10 -X DELETE http://localhost:8080/medical-exam-service/api/exams/167f4ef4-ac4a-4f1c-a9ac-e9474661e6c0 \
  -H "Authorization: Bearer $TOKEN" | head -c 300
echo ""

# Test 3: Create duplicate prescription for same exam
echo ""
echo "=== TEST 3: Create duplicate prescription for exam 167f4ef4 (should FAIL) ==="
curl -s --max-time 10 -X POST "http://localhost:8080/medical-exam-service/api/exams/167f4ef4-ac4a-4f1c-a9ac-e9474661e6c0/prescriptions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"notes": "Duplicate", "items": [{"medicineId": "cfb133bd-be74-45af-9c9c-055371348dae", "quantity": 1, "dosage": "1/day", "durationDays": 1, "instructions": "test"}]}' | head -c 300
echo ""

# Test 4: Create prescription with invalid medicineId
echo ""
echo "=== TEST 4: Create prescription with invalid medicineId (should FAIL) ==="
# First create a new exam
NEW_EXAM=$(curl -s --max-time 10 -X POST http://localhost:8080/medical-exam-service/api/exams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"appointmentId": "apt-test-invalid", "diagnosis": "Test", "symptoms": "test", "treatment": "test"}')
EXAM_ID=$(echo $NEW_EXAM | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created exam: $EXAM_ID"
curl -s --max-time 10 -X POST "http://localhost:8080/medical-exam-service/api/exams/$EXAM_ID/prescriptions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"notes": "Invalid med", "items": [{"medicineId": "invalid-medicine-id", "quantity": 1, "dosage": "1/day", "durationDays": 1, "instructions": "test"}]}' | head -c 300
echo ""

# Test 5: Create prescription with insufficient stock
echo ""
echo "=== TEST 5: Create prescription with insufficient stock (should FAIL) ==="
NEW_EXAM2=$(curl -s --max-time 10 -X POST http://localhost:8080/medical-exam-service/api/exams \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"appointmentId": "apt-test-stock", "diagnosis": "Test", "symptoms": "test", "treatment": "test"}')
EXAM_ID2=$(echo $NEW_EXAM2 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created exam: $EXAM_ID2"
curl -s --max-time 10 -X POST "http://localhost:8080/medical-exam-service/api/exams/$EXAM_ID2/prescriptions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"notes": "Insufficient", "items": [{"medicineId": "cfb133bd-be74-45af-9c9c-055371348dae", "quantity": 99999, "dosage": "1/day", "durationDays": 1, "instructions": "test"}]}' | head -c 300
echo ""

# Test 6: Cancel prescription and verify
echo ""
echo "=== TEST 6: Cancel prescription (should SUCCEED) ==="
echo "Cancelling prescription b9d27df9-fcaa-4e84-bc9b-0dd2ab99a601..."
curl -s --max-time 10 -X POST "http://localhost:8080/medical-exam-service/api/exams/prescriptions/b9d27df9-fcaa-4e84-bc9b-0dd2ab99a601/cancel" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"reason": "Test cancellation"}' | head -c 400
echo ""

# Test 7: Delete prescription (should fail)
echo ""
echo "=== TEST 7: Delete prescription (should FAIL - not allowed) ==="
curl -s --max-time 10 -X DELETE "http://localhost:8080/medical-exam-service/api/exams/prescriptions/b9d27df9-fcaa-4e84-bc9b-0dd2ab99a601" \
  -H "Authorization: Bearer $TOKEN" | head -c 300
echo ""

echo ""
echo "=== ALL TESTS COMPLETED ==="
