# -------- Stage 0: Declaring build arguments (required for FROM) --------
ARG JAVA_VERSION=23

# -------- Stage 1: Build --------
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder

ARG SERVICE
WORKDIR /build

COPY settings.gradle.kts build.gradle.kts gradlew ./
COPY gradle gradle

COPY common ./common
COPY ${SERVICE} ./${SERVICE}

RUN chmod +x gradlew
RUN ./gradlew :${SERVICE}:bootJar --no-daemon

# -------- Stage 2: Run --------
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine

RUN apk add --no-cache curl

ARG SERVICE
WORKDIR /app

COPY --from=builder /build/${SERVICE}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
