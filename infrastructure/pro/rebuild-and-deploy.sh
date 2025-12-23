#!/bin/bash
# Automated rebuild and deploy script for HMS microservices
# This ensures all steps are executed in correct order

set -e  # Exit on any error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/../.."

echo "========================================="
echo "HMS Microservices Rebuild & Deploy"
echo "========================================="
echo ""

# Step 1: Build all JARs with Gradle
echo "Step 1: Building JARs with Gradle..."
cd "$PROJECT_ROOT"
./gradlew bootJar -x test --parallel

if [ $? -ne 0 ]; then
    echo "❌ Gradle build failed! Stopping deployment."
    exit 1
fi

echo "✅ Gradle build completed"
echo ""

# Step 2: Build Docker images
echo "Step 2: Building Docker images..."
cd "$SCRIPT_DIR"
docker compose build

if [ $? -ne 0 ]; then
    echo "❌ Docker build failed! Stopping deployment."
    exit 1
fi

echo "✅ Docker images built"
echo ""

# Step 3: Deploy containers
echo "Step 3: Deploying containers..."
docker compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Container deployment failed!"
    exit 1
fi

echo "✅ Containers deployed"
echo ""
echo "========================================="
echo "Deployment Complete!"
echo "========================================="
echo ""
echo "Services starting up... Check status with:"
echo "  docker compose ps"
echo ""
echo "Check logs with:"
echo "  docker logs <service-name>"
