dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.cloud:spring-cloud-config-client")
    
    // OpenAPI aggregation for API Gateway (WebFlux version)
    // Version 2.8.0+ required for Spring Boot 3.5.x compatibility
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.4")

}

tasks.test {
    // Exclude E2E tests from regular test task - they require full infrastructure
    exclude("**/e2e/**")
}

// Separate task for E2E tests (run manually with full infrastructure)
tasks.register<Test>("e2eTest") {
    description = "Runs E2E integration tests (requires full infrastructure)"
    group = "verification"
    include("**/e2e/**")
    shouldRunAfter(tasks.test)
}