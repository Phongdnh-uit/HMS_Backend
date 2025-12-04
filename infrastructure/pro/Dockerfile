# -------- Stage 1: Build --------
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder

ARG SERVICE
WORKDIR /build

# Copy root build files
COPY settings.gradle.kts build.gradle.kts gradlew ./
COPY gradle gradle

# Copy only the service folder + common libs
COPY common ./common
COPY ${SERVICE} ./${SERVICE}

RUN chmod +x gradlew
RUN ./gradlew :${SERVICE}:bootJar --no-daemon

# -------- Stage 2: Run --------
FROM eclipse-temurin:${JAVA_VERSION}-jre

ARG SERVICE
WORKDIR /app

COPY --from=builder /build/${SERVICE}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
