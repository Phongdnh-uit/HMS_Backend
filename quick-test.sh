#!/bin/bash

# Quick API Contract Validation Test
# Tests the updated GET endpoints directly

BASE_URL="http://localhost:8080"

echo "========================================="
echo "Quick API Contract Test"
echo "========================================="

echo ""
echo "1. Testing GET /api/medicines (should work - public endpoint)"
curl -s "$BASE_URL/api/medicines" | head -3

echo ""
echo "2. Testing GET /api/medicines/categories (should work - public endpoint)"
curl -s "$BASE_URL/api/medicines/categories" | head -3

echo ""
echo "3. Testing GET /api/patients (should require auth - 401)"
curl -s "$BASE_URL/api/patients" | head -3

echo ""
echo "4. Testing direct medicine service (port 8083)"
curl -s "http://localhost:8083/api/medicines" | head -3

echo ""
echo "5. Testing direct patient service (port 8082)"
curl -s "http://localhost:8082/api/patients" | head -3

echo ""
echo "========================================="
echo "If you see 404, there's a routing issue"
echo "If you see 401 for patients, auth is working"
echo "If you see empty responses from direct services, they're not ready"
echo "========================================="
