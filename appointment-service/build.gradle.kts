dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.cloud:spring-cloud-config-client")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}