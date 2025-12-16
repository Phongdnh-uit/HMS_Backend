#!/bin/bash

echo "=== Gateway Routing Verification ==="
echo ""
echo "1. Checking config-server health..."
curl -s http://localhost:8888/actuator/health
echo ""
echo ""

echo "2. Checking if config has correct path structure..."
CONFIG_CHECK=$(curl -s http://localhost:8888/api-gateway/default | grep -o 'spring.cloud.gateway.routes\[0\]')
if [ -n "$CONFIG_CHECK" ]; then
    echo "✓ Config uses correct path: spring.cloud.gateway.routes"
else
    echo "✗ Config still uses old path: spring.cloud.gateway.server.webflux.routes"
    curl -s http://localhost:8888/api-gateway/default | grep -o 'spring.cloud.gateway[^"]*' | head -n 1
fi
echo ""
echo ""

echo "3. Checking API Gateway health..."
curl -s http://localhost:8080/actuator/health
echo ""
echo ""

echo "4. Testing direct auth-service access..."
curl -s -w "\nHTTP: %{http_code}\n" http://localhost:8081/auth/register \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"direct@test.com","password":"Test123!@"}'
echo ""
echo ""

echo "5. Testing via Gateway (/api/auth/register)..."
curl -s -w "\nHTTP: %{http_code}\n" http://localhost:8080/api/auth/register \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"gateway@test.com","password":"Test123!@"}'
echo ""
echo ""

echo "6. Testing direct service route (/auth-service/auth/register)..."
curl -s -w "\nHTTP: %{http_code}\n" http://localhost:8080/auth-service/auth/register \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"email":"direct-route@test.com","password":"Test123!@"}'
echo ""
