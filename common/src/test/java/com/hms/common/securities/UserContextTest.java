package com.hms.common.securities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UserContext.
 * Tests ThreadLocal user context management.
 */
@DisplayName("UC-CMN-010: UserContext Unit Tests")
class UserContextTest {

    @AfterEach
    void tearDown() {
        // Clean up ThreadLocal after each test
        UserContext.clear();
    }

    @Nested
    @DisplayName("Method: setUser() and getUser()")
    class SetAndGetUserTests {

        @Test
        @DisplayName("UC-CMN-010: Should store and retrieve user in ThreadLocal")
        void setUser_shouldStoreUserInThreadLocal() {
            // Given
            UserContext.User user = new UserContext.User();
            user.setId("user-123");
            user.setRole("DOCTOR");
            user.setEmail("doctor@hospital.com");

            // When
            UserContext.setUser(user);
            UserContext.User retrievedUser = UserContext.getUser();

            // Then
            assertThat(retrievedUser).isNotNull();
            assertThat(retrievedUser.getId()).isEqualTo("user-123");
            assertThat(retrievedUser.getRole()).isEqualTo("DOCTOR");
            assertThat(retrievedUser.getEmail()).isEqualTo("doctor@hospital.com");
        }

        @Test
        @DisplayName("Should return null when no user is set")
        void getUser_whenNoUserSet_shouldReturnNull() {
            // When
            UserContext.User user = UserContext.getUser();

            // Then
            assertThat(user).isNull();
        }

        @Test
        @DisplayName("Should update user when set multiple times")
        void setUser_whenCalledMultipleTimes_shouldUpdateUser() {
            // Given
            UserContext.User firstUser = new UserContext.User();
            firstUser.setId("user-1");
            firstUser.setRole("NURSE");

            UserContext.User secondUser = new UserContext.User();
            secondUser.setId("user-2");
            secondUser.setRole("ADMIN");

            // When
            UserContext.setUser(firstUser);
            UserContext.setUser(secondUser);
            UserContext.User retrievedUser = UserContext.getUser();

            // Then
            assertThat(retrievedUser).isNotNull();
            assertThat(retrievedUser.getId()).isEqualTo("user-2");
            assertThat(retrievedUser.getRole()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Method: clear()")
    class ClearTests {

        @Test
        @DisplayName("UC-CMN-010: Should clear user from ThreadLocal")
        void clear_shouldRemoveUserFromThreadLocal() {
            // Given
            UserContext.User user = new UserContext.User();
            user.setId("user-456");
            UserContext.setUser(user);

            // When
            UserContext.clear();
            UserContext.User retrievedUser = UserContext.getUser();

            // Then
            assertThat(retrievedUser).isNull();
        }

        @Test
        @DisplayName("Should be safe to call clear when no user is set")
        void clear_whenNoUserSet_shouldNotThrowException() {
            // When & Then
            assertThatCode(() -> UserContext.clear()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should be safe to call clear multiple times")
        void clear_whenCalledMultipleTimes_shouldNotThrowException() {
            // Given
            UserContext.User user = new UserContext.User();
            user.setId("user-789");
            UserContext.setUser(user);

            // When & Then
            assertThatCode(() -> {
                UserContext.clear();
                UserContext.clear();
                UserContext.clear();
            }).doesNotThrowAnyException();

            assertThat(UserContext.getUser()).isNull();
        }
    }

    @Nested
    @DisplayName("User Inner Class")
    class UserInnerClassTests {

        @Test
        @DisplayName("UC-CMN-010: Should create User with all properties")
        void user_shouldHaveAllProperties() {
            // When
            UserContext.User user = new UserContext.User();
            user.setId("usr-001");
            user.setRole("RECEPTIONIST");
            user.setEmail("receptionist@hospital.com");

            // Then
            assertThat(user.getId()).isEqualTo("usr-001");
            assertThat(user.getRole()).isEqualTo("RECEPTIONIST");
            assertThat(user.getEmail()).isEqualTo("receptionist@hospital.com");
        }

        @Test
        @DisplayName("Should allow null values")
        void user_shouldAllowNullValues() {
            // When
            UserContext.User user = new UserContext.User();
            user.setId(null);
            user.setRole(null);
            user.setEmail(null);

            // Then
            assertThat(user.getId()).isNull();
            assertThat(user.getRole()).isNull();
            assertThat(user.getEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("Thread Isolation")
    class ThreadIsolationTests {

        @Test
        @DisplayName("Should isolate user context between threads")
        void userContext_shouldBeThreadIsolated() throws InterruptedException {
            // Given
            UserContext.User mainThreadUser = new UserContext.User();
            mainThreadUser.setId("main-thread-user");
            mainThreadUser.setRole("ADMIN");
            UserContext.setUser(mainThreadUser);

            // When
            Thread otherThread = new Thread(() -> {
                // Other thread should not see main thread's user
                UserContext.User otherThreadUser = UserContext.getUser();
                assertThat(otherThreadUser).isNull();

                // Set user in other thread
                UserContext.User newUser = new UserContext.User();
                newUser.setId("other-thread-user");
                newUser.setRole("DOCTOR");
                UserContext.setUser(newUser);

                assertThat(UserContext.getUser().getId()).isEqualTo("other-thread-user");
                UserContext.clear();
            });

            otherThread.start();
            otherThread.join();

            // Then - main thread user should be unchanged
            UserContext.User mainUser = UserContext.getUser();
            assertThat(mainUser).isNotNull();
            assertThat(mainUser.getId()).isEqualTo("main-thread-user");
            assertThat(mainUser.getRole()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Common Use Cases")
    class CommonUseCasesTests {

        @Test
        @DisplayName("Should support request lifecycle pattern")
        void userContext_shouldSupportRequestLifecycle() {
            // Simulate request start
            UserContext.User user = new UserContext.User();
            user.setId("req-user-123");
            user.setRole("DOCTOR");
            user.setEmail("doctor@example.com");
            UserContext.setUser(user);

            // Simulate using context in business logic
            UserContext.User currentUser = UserContext.getUser();
            assertThat(currentUser).isNotNull();
            assertThat(currentUser.getId()).isEqualTo("req-user-123");

            // Simulate request end
            UserContext.clear();
            assertThat(UserContext.getUser()).isNull();
        }

        @Test
        @DisplayName("Should support user role checking")
        void userContext_shouldSupportRoleChecking() {
            // Given
            UserContext.User user = new UserContext.User();
            user.setId("user-999");
            user.setRole("ADMIN");
            user.setEmail("admin@hospital.com");
            UserContext.setUser(user);

            // When
            UserContext.User currentUser = UserContext.getUser();

            // Then
            assertThat(currentUser).isNotNull();
            assertThat(currentUser.getRole()).isEqualTo("ADMIN");
        }
    }
}
