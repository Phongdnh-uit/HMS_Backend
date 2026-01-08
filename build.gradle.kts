plugins {
    java
    id("org.springframework.boot") version "3.5.8" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("jacoco")
}

allprojects {
    group = "com.hms"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "jacoco")

    val springCloudVersion = "2025.0.0"
    val testcontainersVersion = "1.20.4"
    val wiremockVersion = "3.10.0"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
    }

    dependencies {
        implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.3.0")

        // ==================== TEST DEPENDENCIES ====================
        // Core Testing
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        }
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        // Mockito for mocking
        testImplementation("org.mockito:mockito-core:5.14.2")
        testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")

        // AssertJ for fluent assertions
        testImplementation("org.assertj:assertj-core:3.27.2")

        // H2 in-memory database for tests
        testImplementation("com.h2database:h2:2.3.232")

        // Spring Security Test
        testImplementation("org.springframework.security:spring-security-test")

        // WireMock for HTTP mocking (Feign clients)
        testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")

        // Testcontainers for integration tests
        testImplementation(platform("org.testcontainers:testcontainers-bom:$testcontainersVersion"))
        testImplementation("org.testcontainers:testcontainers")
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.testcontainers:mysql")

        // Lombok for test classes
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")

        // REST Assured for API testing (optional but powerful)
        testImplementation("io.rest-assured:rest-assured:5.5.0")
        testImplementation("io.rest-assured:spring-mock-mvc:5.5.0")

        // Awaitility for async testing
        testImplementation("org.awaitility:awaitility:4.2.2")

        // Faker for generating test data
        testImplementation("net.datafaker:datafaker:2.4.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Enable parallel test execution
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        // Test logging
        testLogging {
            events("passed", "skipped", "failed")
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        // JaCoCo integration
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // Fail build if coverage is below threshold (optional, enable when ready)
    // tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    //     violationRules {
    //         rule {
    //             limit {
    //                 minimum = "0.60".toBigDecimal()
    //             }
    //         }
    //     }
    // }
}
