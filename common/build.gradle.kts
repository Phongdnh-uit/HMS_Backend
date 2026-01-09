plugins {
    id("org.springframework.boot") version "3.5.8" apply false
    id("java-library")
    id("java-test-fixtures") // Expose test utilities to other modules
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("io.github.perplexhub:rsql-jpa-spring-boot-starter:6.0.32")
    api("org.mapstruct:mapstruct:1.6.3")
    api("com.h2database:h2")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Test fixtures dependencies (available to other modules)
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework.boot:spring-boot-test-autoconfigure")
    testFixturesApi("org.testcontainers:testcontainers")
    testFixturesApi("org.testcontainers:junit-jupiter")
    testFixturesApi("org.testcontainers:mysql")
    testFixturesApi("org.wiremock:wiremock-standalone:3.10.0")
    testFixturesApi("net.datafaker:datafaker:2.4.2")
    testFixturesApi("org.assertj:assertj-core:3.27.2")
    testFixturesApi("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}