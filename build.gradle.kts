plugins {
    java
    id("org.springframework.boot") version "3.5.8" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
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

    val springCloudVersion = "2025.0.0"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
    }

    dependencies {
        implementation("io.github.cdimascio:dotenv-java:3.2.0")
        implementation(platform("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion"))
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
