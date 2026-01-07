package com.hms.common.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base test class for integration tests that require a real MySQL database.
 * Uses Testcontainers to spin up a MySQL container for realistic testing.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * class MyIntegrationTest extends MySQLContainerTest {
 *     @Test
 *     void testSomething() {
 *         // Test with real MySQL
 *     }
 * }
 * }
 * </pre>
 */
@Testcontainers
public abstract class MySQLContainerTest {

    @Container
    protected static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("hms_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Reuse container across tests for faster execution

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
    }
}
