ARG JAVA_VERSION=23

# -------- Stage 1: Build ALL services ONCE --------
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder
WORKDIR /build

# Copy root build files first (optimize cache)
COPY settings.gradle.kts build.gradle.kts gradlew ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Copy source code for all modules
COPY common ./common
COPY config-server ./config-server
COPY discovery-service ./discovery-service
COPY api-gateway ./api-gateway
COPY auth-service ./auth-service
COPY medicine-service ./medicine-service
COPY patient-service ./patient-service
COPY hr-service ./hr-service
COPY appointment-service ./appointment-service
COPY medical-exam-service ./medical-exam-service
COPY billing-service ./billing-service
COPY report-service ./report-service
COPY notification-service ./notification-service

# Build ALL modules at once (only 1 Gradle download!)
RUN ./gradlew clean bootJar --no-daemon

# -------- Stage 2: Runtime (OPTIMIZED - only 1 JAR per image) --------
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

RUN apk add --no-cache curl

ARG SERVICE
ENV SERVICE=${SERVICE}
WORKDIR /app

# Copy ONLY the specific service JAR (reduces image from 2.14GB to ~300MB)
COPY --from=builder /build/${SERVICE}/build/libs/${SERVICE}-*.jar ./app.jar

# Note: HEALTHCHECK is defined in compose.yaml with correct port per service

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
