package com.hms.common.test;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Composite annotation for controller/API integration tests.
 * Configures MockMvc for HTTP testing with H2 database.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @ControllerTest
 * class PatientControllerTest {
 *     @Autowired
 *     private MockMvc mockMvc;
 *     
 *     @Test
 *     void getPatient_shouldReturn200() throws Exception {
 *         mockMvc.perform(get("/patients/1"))
 *             .andExpect(status().isOk());
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
public @interface ControllerTest {
}
