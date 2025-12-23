# -------- Stage 1: Build --------
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /build

# Copy root build files first (tối ưu cache)
COPY settings.gradle.kts build.gradle.kts gradlew ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew

# Copy source code tất cả module
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

# Build tất cả modules
RUN ./gradlew clean bootJar --no-daemon

# -------- Stage 2: Run --------
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app

# curl cho healthcheck
RUN apk add --no-cache curl

# Copy chỉ jar của từng service
COPY --from=builder /build/api-gateway/build/libs/api-gateway-*.jar ./api-gateway.jar
COPY --from=builder /build/auth-service/build/libs/auth-service-*.jar ./auth-service.jar
COPY --from=builder /build/config-server/build/libs/config-server-*.jar ./config-server.jar
COPY --from=builder /build/discovery-service/build/libs/discovery-service-*.jar ./discovery-service.jar
COPY --from=builder /build/medicine-service/build/libs/medicine-service-*.jar ./medicine-service.jar
COPY --from=builder /build/patient-service/build/libs/patient-service-*.jar ./patient-service.jar
COPY --from=builder /build/hr-service/build/libs/hr-service-*.jar ./hr-service.jar
COPY --from=builder /build/appointment-service/build/libs/appointment-service-*.jar ./appointment-service.jar
COPY --from=builder /build/medical-exam-service/build/libs/medical-exam-service-*.jar ./medical-exam-service.jar
COPY --from=builder /build/billing-service/build/libs/billing-service-*.jar ./billing-service.jar
COPY --from=builder /build/report-service/build/libs/report-service-*.jar ./report-service.jar
COPY --from=builder /build/notification-service/build/libs/notification-service-*.jar ./notification-service.jar

ENTRYPOINT ["sh","-c","java -jar /app/${SERVICE}.jar"]
