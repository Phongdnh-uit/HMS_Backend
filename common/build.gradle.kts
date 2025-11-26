plugins {
	id("org.springframework.boot") version "3.5.8" apply false
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
    implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation ("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor ("org.mapstruct:mapstruct-processor:1.6.3")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}