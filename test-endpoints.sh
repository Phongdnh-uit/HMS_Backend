#!/bin/bash

# Quick Endpoint Test Script
# Tests all major endpoints through the API Gateway

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "========================================="
echo "HMS Backend Endpoint Test"
echo "========================================="

# Test 1: Register
TIMESTAMP=$(date +%s)
TEST_EMAIL="test-${TIMESTAMP}@example.com"
echo -e "\n1. Register New User ($TEST_EMAIL)"
REGISTER=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"Test123!@\"}")
STATUS=$(echo "$REGISTER" | tail -1)
if [ "$STATUS" = "200" ]; then
  echo -e "${GREEN}✓ Register: SUCCESS${NC}"
else
  echo -e "${RED}✗ Register: FAILED (HTTP $STATUS)${NC}"
fi

# Test 2: Login
echo -e "\n2. Login"
LOGIN=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"Test123!@\"}")
STATUS=$(echo "$LOGIN" | tail -1)
TOKEN=$(echo "$LOGIN" | head -1 | grep -o '"accessToken":"[^"]*"' | sed 's/"accessToken":"//;s/"//')

if [ "$STATUS" = "200" ] && [ -n "$TOKEN" ]; then
  echo -e "${GREEN}✓ Login: SUCCESS${NC}"
  echo "  Token: ${TOKEN:0:50}..."
else
  echo -e "${RED}✗ Login: FAILED (HTTP $STATUS)${NC}"
  exit 1
fi

# Test 3: Get Categories (Public)
echo -e "\n3. Get Categories (Public)"
CATEGORIES=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/categories/all")
STATUS=$(echo "$CATEGORIES" | tail -1)
if [ "$STATUS" = "200" ]; then
  echo -e "${GREEN}✓ Get Categories: SUCCESS${NC}"
else
  echo -e "${RED}✗ Get Categories: FAILED (HTTP $STATUS)${NC}"
fi

# Test 4: Get Medicines (Public)
echo -e "\n4. Get Medicines (Public)"
MEDICINES=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/medicines/all")
STATUS=$(echo "$MEDICINES" | tail -1)
if [ "$STATUS" = "200" ]; then
  echo -e "${GREEN}✓ Get Medicines: SUCCESS${NC}"
else
  echo -e "${RED}✗ Get Medicines: FAILED (HTTP $STATUS)${NC}"
fi

# Test 5: Get Patients (Authenticated)
echo -e "\n5. Get Patients (Authenticated)"
PATIENTS=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/patients/all" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$PATIENTS" | tail -1)
if [ "$STATUS" = "200" ]; then
  echo -e "${GREEN}✓ Get Patients: SUCCESS${NC}"
else
  echo -e "${RED}✗ Get Patients: FAILED (HTTP $STATUS)${NC}"
fi

# Test 6: Get My Profile (Authenticated)
echo -e "\n6. Get My Profile (Authenticated)"
PROFILE=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/patients/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$PROFILE" | tail -1)
if [ "$STATUS" = "200" ] || [ "$STATUS" = "404" ]; then
  echo -e "${GREEN}✓ Get Profile: SUCCESS (404 expected if no profile)${NC}"
else
  echo -e "${RED}✗ Get Profile: FAILED (HTTP $STATUS)${NC}"
fi

echo -e "\n========================================="
echo "Test Complete!"
echo "========================================="
