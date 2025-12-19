# Fast Docker Build Guide

## Problem: Slow Docker Builds (~15 minutes)

The original `Dockerfile` builds all services inside Docker every time you run `docker-compose build`, which is slow because:
- Gradle downloads dependencies every time
- All 8 services are compiled in Docker
- No layer caching for the build process

## Solution: Build on Host, Copy to Docker (2-3 minutes)

### Approach 1: Quick Build (Recommended)

Build JARs on your machine first, then Docker just copies them:

```bash
# Step 1: Build all JARs on host (1-2 min with Gradle daemon)
cd c:\Users\ASUS\Desktop\HMS_DOCS\HMS_Backend
./gradlew clean bootJar -x test --parallel

# Step 2: Build Docker images (30 seconds - just copies JARs)
cd infrastructure/pro
docker-compose build --parallel

# Step 3: Start containers
docker-compose up -d

# Optional: View logs
docker-compose logs -f
```

### Approach 2: One-Command Build (Linux/Git Bash)

Use the provided script:

```bash
cd c:\Users\ASUS\Desktop\HMS_DOCS\HMS_Backend\infrastructure\pro
bash build-and-run.sh
```

### Approach 3: Windows PowerShell

```powershell
# Step 1: Build JARs
cd c:\Users\ASUS\Desktop\HMS_DOCS\HMS_Backend
.\gradlew.bat clean bootJar -x test --parallel

# Step 2: Build Docker images
cd infrastructure\pro
docker-compose build --parallel

# Step 3: Start containers
docker-compose up -d
```

## Time Comparison

| Approach | Build Time | When to Use |
|----------|-----------|-------------|
| Old Dockerfile | ~15 minutes | Never (deprecated) |
| **Dockerfile.fast** | **2-3 minutes** | Always (recommended) |
| Just `docker-compose up` (cached) | 30 seconds | When JARs unchanged |

## What Changed?

### Old Dockerfile (slow)
- Copied all source code into Docker
- Ran `./gradlew clean bootJar` inside Docker
- No caching between builds
- Downloaded dependencies every time

### New Dockerfile.fast (fast)
- Only copies pre-built JARs from `build/libs/`
- No Gradle build in Docker
- Uses host's Gradle cache
- 10x faster

## Troubleshooting

### "No such file or directory: app.jar"
You forgot to build JARs first. Run:
```bash
./gradlew clean bootJar -x test
```

### Containers show "Error" status
This is normal during startup. MySQL needs ~60 seconds to initialize. Check logs:
```bash
docker-compose logs mysql-hr-service
```

If you see "ready for connections", it's working - just wait for healthcheck to pass.

### Config-server timeout
Config-server takes ~70 seconds to start. The healthcheck now waits 90 seconds before failing.

## Health Check Status

All services now have extended healthcheck timeouts:
- **MySQL**: 60s start_period, 10 retries
- **Config-server**: 90s start_period, 10 retries
- Other services: Wait for config-server to be healthy

## Verification

Check all services are running:
```bash
docker-compose ps
```

Expected output after 2-3 minutes:
```
NAME                    STATUS
config-server-pro       Up (healthy)
discovery-service-pro   Up
mysql-auth-service      Up (healthy)
mysql-hr-service        Up (healthy)
...
```

Visit Eureka dashboard:
```
http://localhost:8763
```

You should see all services registered within 5 minutes.
