# HMS - Docker Deployment Guide

## Overview

Hospital Management System (HMS) - A fullstack microservices healthcare application.

- **Backend:** 12 Spring Boot Microservices
- **Frontend:** Next.js 16 Web Application
- **Database:** 7 MySQL instances + Redis + MinIO

## Prerequisites

- Docker & Docker Compose
- 8GB+ RAM recommended
- Ports: 3000, 8080-8089, 8763, 8888, 6379, 9000-9001

## Quick Start

### 1. Deploy Backend (21 containers)

```bash
cd HMS_Backend/infrastructure/pro
cp .env.example .env
docker compose up -d
```

### 2. Deploy Frontend (1 container)

```bash
cd QuanLyBenhVien
docker compose up -d
```

### 3. Access Application

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API Gateway Health | http://localhost:8080/actuator/health |
| Eureka Dashboard | http://localhost:8763 |
| MinIO Console | http://localhost:9001 |

## Backend Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Entry point for all API requests |
| auth-service | 8081 | Authentication & authorization |
| patient-service | 8082 | Patient management |
| medicine-service | 8083 | Medicine catalog & inventory |
| hr-service | 8084 | Employee & department management |
| appointment-service | 8085 | Appointment scheduling |
| medical-exam-service | 8086 | Medical examinations |
| billing-service | 8087 | Invoicing & payments |
| report-service | 8088 | Reports & analytics |
| notification-service | 8089 | Email/SMS notifications |
| discovery-service | 8763 | Service registry (Eureka) |
| config-server | 8888 | Centralized configuration |

## Common Commands

```bash
# Start all backend services
cd HMS_Backend/infrastructure/pro
docker compose up -d

# Start frontend
cd QuanLyBenhVien
docker compose up -d

# View running containers
docker ps --format "table {{.Names}}\t{{.Status}}"

# View logs
docker logs [container-name] -f

# Stop all services
docker compose down

# Rebuild a specific service
docker compose build [service-name]
docker compose up -d [service-name]
```

## Troubleshooting

### Services not starting
```bash
# Check config-server first (others depend on it)
docker logs config-server-pro

# Check service logs
docker logs [container-name]
```

### Database connection issues
```bash
# Verify MySQL containers are healthy
docker ps | findstr mysql
```

### Frontend can't connect to backend
- Ensure backend is running: `docker ps`
- Check API Gateway: http://localhost:8080/actuator/health

## Architecture

```
┌─────────────────────────────────────────────┐
│     Frontend (Next.js - 3000)               │
├─────────────────────────────────────────────┤
│     API Gateway (8080)                      │
├─────────────────────────────────────────────┤
│     Discovery Service (8763)                │
├─────────────────────────────────────────────┤
│  Business Services (8081-8089)              │
│  auth | patient | medicine | hr |           │
│  appointment | exam | billing | report |    │
│  notification                               │
├─────────────────────────────────────────────┤
│  MySQL (7) | Redis | MinIO                  │
└─────────────────────────────────────────────┘
```

## Lab 2 Checklist

- [x] Dockerfile with multi-stage build (Backend + Frontend)
- [x] docker-compose.yml for single-command deployment
- [x] Environment variables via .env file
- [x] 22 containers running (21 backend + 1 frontend)
- [x] Health checks configured
- [x] Optimized image sizes (~400MB backend, ~200MB frontend)
