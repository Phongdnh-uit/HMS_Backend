plugins {
    id("org.springframework.boot") version "3.5.8" apply false
    id("java-library")
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
}