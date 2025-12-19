#!/bin/bash
# Fast build script - builds JARs on host, then builds Docker images

set -e

echo "Step 1: Building all services with Gradle..."
cd ../..
./gradlew clean bootJar -x test --parallel --no-daemon

echo "Step 2: Building Docker images (fast - no rebuild in Docker)..."
cd infrastructure/pro
docker-compose build --parallel

echo "Step 3: Starting containers..."
docker-compose up -d

echo "Done! Use 'docker-compose logs -f' to see logs"
