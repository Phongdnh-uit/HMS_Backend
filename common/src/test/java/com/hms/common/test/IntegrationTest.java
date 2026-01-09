package com.hms.common.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Composite annotation for integration tests that run without external dependencies.
 * Disables Eureka, Config Server, and uses H2 database.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @IntegrationTest
 * class PatientServiceIntegrationTest {
 *     @Autowired
 *     private PatientService service;
 *     
 *     @Test
 *     void createPatient_shouldPersistAndReturn() {
 *         // Full integration test
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    }
)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
