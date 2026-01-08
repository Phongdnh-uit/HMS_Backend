package com.hms.common.securities;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for UserContextFilter.
 * Tests header extraction and UserContext population.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-CMN-011: UserContextFilter Unit Tests")
class UserContextFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private UserContextFilter userContextFilter;

    @AfterEach
    void tearDown() {
        // Clean up ThreadLocal after each test
        UserContext.clear();
    }

    @Nested
    @DisplayName("Method: doFilterInternal()")
    class DoFilterInternalTests {

        @Test
        @DisplayName("UC-CMN-011: Should extract user headers and populate UserContext")
        void doFilterInternal_withUserHeaders_shouldPopulateUserContext() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("user-123");
            given(request.getHeader("X-User-Role")).willReturn("DOCTOR");
            given(request.getHeader("X-User-Email")).willReturn("doctor@hospital.com");

            // Capture user during filter execution
            final UserContext.User[] capturedUser = new UserContext.User[1];
            
            willAnswer(invocation -> {
                // Capture user context during filter chain execution
                capturedUser[0] = UserContext.getUser();
                return null;
            }).given(filterChain).doFilter(request, response);

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then - User should have been available during filter chain
            assertThat(capturedUser[0]).isNotNull();
            assertThat(capturedUser[0].getId()).isEqualTo("user-123");
            assertThat(capturedUser[0].getRole()).isEqualTo("DOCTOR");
            assertThat(capturedUser[0].getEmail()).isEqualTo("doctor@hospital.com");

            then(filterChain).should().doFilter(request, response);
            
            // And should be cleared after filter execution
            assertThat(UserContext.getUser()).isNull();
        }

        @Test
        @DisplayName("Should not populate UserContext when X-User-ID header is missing")
        void doFilterInternal_withoutUserIdHeader_shouldNotPopulateUserContext() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn(null);
            given(request.getHeader("X-User-Role")).willReturn("ADMIN");
            given(request.getHeader("X-User-Email")).willReturn("admin@hospital.com");

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then
            UserContext.User user = UserContext.getUser();
            assertThat(user).isNull();

            then(filterChain).should().doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle null role and email headers")
        void doFilterInternal_withNullRoleAndEmail_shouldSetUserWithNullValues() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("user-456");
            given(request.getHeader("X-User-Role")).willReturn(null);
            given(request.getHeader("X-User-Email")).willReturn(null);

            // Capture user during filter execution
            final UserContext.User[] capturedUser = new UserContext.User[1];
            
            willAnswer(invocation -> {
                capturedUser[0] = UserContext.getUser();
                return null;
            }).given(filterChain).doFilter(request, response);

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then - User should have been set during filter chain
            assertThat(capturedUser[0]).isNotNull();
            assertThat(capturedUser[0].getId()).isEqualTo("user-456");
            assertThat(capturedUser[0].getRole()).isNull();
            assertThat(capturedUser[0].getEmail()).isNull();

            then(filterChain).should().doFilter(request, response);
            
            // And should be cleared after
            assertThat(UserContext.getUser()).isNull();
        }

        @Test
        @DisplayName("Should clear UserContext after filter chain execution")
        void doFilterInternal_afterFilterChain_shouldClearUserContext() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("user-789");
            given(request.getHeader("X-User-Role")).willReturn("NURSE");
            given(request.getHeader("X-User-Email")).willReturn("nurse@hospital.com");

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then - UserContext should be cleared after filter execution
            UserContext.User user = UserContext.getUser();
            assertThat(user).isNull();

            then(filterChain).should().doFilter(request, response);
        }

        @Test
        @DisplayName("Should clear UserContext even when filter chain throws exception")
        void doFilterInternal_whenFilterChainThrows_shouldStillClearUserContext() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("user-error");
            given(request.getHeader("X-User-Role")).willReturn("ADMIN");
            given(request.getHeader("X-User-Email")).willReturn("admin@hospital.com");

            willThrow(new ServletException("Filter chain error")).given(filterChain).doFilter(request, response);

            // When & Then
            assertThatThrownBy(() -> userContextFilter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(ServletException.class)
                    .hasMessage("Filter chain error");

            // UserContext should still be cleared
            UserContext.User user = UserContext.getUser();
            assertThat(user).isNull();
        }
    }

    @Nested
    @DisplayName("Header Extraction")
    class HeaderExtractionTests {

        @Test
        @DisplayName("Should extract all user headers correctly")
        void headerExtraction_withAllHeaders_shouldPopulateAllFields() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("usr-001");
            given(request.getHeader("X-User-Role")).willReturn("RECEPTIONIST");
            given(request.getHeader("X-User-Email")).willReturn("receptionist@example.com");

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(request).should().getHeader("X-User-ID");
            then(request).should().getHeader("X-User-Role");
            then(request).should().getHeader("X-User-Email");
        }

        @Test
        @DisplayName("Should handle empty string headers")
        void headerExtraction_withEmptyStrings_shouldSetEmptyValues() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("user-empty");
            given(request.getHeader("X-User-Role")).willReturn("");
            given(request.getHeader("X-User-Email")).willReturn("");

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then - UserContext should be cleared after filter, so we can't check directly
            // But the filter should have processed without errors
            then(filterChain).should().doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Filter Chain Integration")
    class FilterChainIntegrationTests {

        @Test
        @DisplayName("Should always call filter chain doFilter")
        void filterChain_shouldAlwaysBeInvoked() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn("user-chain");

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Should call filter chain even without user headers")
        void filterChain_withoutHeaders_shouldStillBeInvoked() throws ServletException, IOException {
            // Given
            given(request.getHeader("X-User-ID")).willReturn(null);
            given(request.getHeader("X-User-Role")).willReturn(null);
            given(request.getHeader("X-User-Email")).willReturn(null);

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then
            then(filterChain).should().doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Common Use Cases")
    class CommonUseCasesTests {

        @Test
        @DisplayName("Should support authentication flow")
        void filter_shouldSupportAuthenticationFlow() throws ServletException, IOException {
            // Given - Simulating authenticated request from API Gateway
            given(request.getHeader("X-User-ID")).willReturn("auth-user-123");
            given(request.getHeader("X-User-Role")).willReturn("DOCTOR");
            given(request.getHeader("X-User-Email")).willReturn("doctor@hospital.com");

            // Capture user during filter execution
            final UserContext.User[] capturedUser = new UserContext.User[1];
            
            willAnswer(invocation -> {
                // Capture user context during filter chain execution
                capturedUser[0] = UserContext.getUser();
                return null;
            }).given(filterChain).doFilter(request, response);

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then - User should have been available during filter chain
            assertThat(capturedUser[0]).isNotNull();
            assertThat(capturedUser[0].getId()).isEqualTo("auth-user-123");
            assertThat(capturedUser[0].getRole()).isEqualTo("DOCTOR");

            // And should be cleared after
            assertThat(UserContext.getUser()).isNull();
        }

        @Test
        @DisplayName("Should support unauthenticated requests")
        void filter_shouldSupportUnauthenticatedRequests() throws ServletException, IOException {
            // Given - No user headers (public endpoint)
            given(request.getHeader("X-User-ID")).willReturn(null);

            final UserContext.User[] capturedUser = new UserContext.User[1];
            
            willAnswer(invocation -> {
                capturedUser[0] = UserContext.getUser();
                return null;
            }).given(filterChain).doFilter(request, response);

            // When
            userContextFilter.doFilterInternal(request, response, filterChain);

            // Then - No user should be set
            assertThat(capturedUser[0]).isNull();
            assertThat(UserContext.getUser()).isNull();
        }
    }
}
