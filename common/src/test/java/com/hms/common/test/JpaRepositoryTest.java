package com.hms.common.test;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Composite annotation for JPA repository tests.
 * Configures H2 in-memory database with create-drop schema management.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @JpaRepositoryTest
 * class PatientRepositoryTest {
 *     @Autowired
 *     private PatientRepository repository;
 *     
 *     @Test
 *     void findByEmail_shouldReturnPatient() {
 *         // Test repository methods
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DataJpaTest
@ActiveProfiles("test")
public @interface JpaRepositoryTest {
}
