package com.hms.auth_service.securities;

import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.enums.RoleEnum;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for CustomUserDetailsService.
 * Tests user loading and authentication user details.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-AUTH-013: CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account();
        testAccount.setId(UUID.randomUUID().toString());
        testAccount.setEmail(TestDataFactory.uniqueEmail());
        testAccount.setPassword("$2a$10$encoded.password");
        testAccount.setRole(RoleEnum.PATIENT);
        testAccount.setEmailVerified(true);
    }

    @Nested
    @DisplayName("Method: loadUserByUsername()")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("UC-AUTH-013: Should load user by email successfully")
        void loadUserByUsername_withValidEmail_shouldReturnUserDetails() {
            // Given
            String email = testAccount.getEmail();

            given(accountRepository.findOne(any(Specification.class)))
                    .willReturn(Optional.of(testAccount));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(CustomUserDetails.class);

            CustomUserDetails customDetails = (CustomUserDetails) result;
            assertThat(customDetails.getId()).isEqualTo(testAccount.getId());
            assertThat(customDetails.getEmail()).isEqualTo(testAccount.getEmail());
            assertThat(customDetails.getPassword()).isEqualTo(testAccount.getPassword());
            assertThat(customDetails.getRole()).isEqualTo(RoleEnum.PATIENT.name());

            then(accountRepository).should().findOne(any(Specification.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void loadUserByUsername_withNonExistentEmail_shouldThrowException() {
            // Given
            String nonExistentEmail = "nonexistent@example.com";

            given(accountRepository.findOne(any(Specification.class)))
                    .willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found with email: " + nonExistentEmail);

            then(accountRepository).should().findOne(any(Specification.class));
        }

        @Test
        @DisplayName("Should throw exception when email is not verified")
        void loadUserByUsername_withUnverifiedEmail_shouldThrowException() {
            // Given
            testAccount.setEmailVerified(false);

            given(accountRepository.findOne(any(Specification.class)))
                    .willReturn(Optional.of(testAccount));

            // When/Then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(testAccount.getEmail()))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Email not verified for user with email: " + testAccount.getEmail());

            then(accountRepository).should().findOne(any(Specification.class));
        }

        @Test
        @DisplayName("Should load user with different roles correctly")
        void loadUserByUsername_withDifferentRoles_shouldSetCorrectRole() {
            // Given - Test with DOCTOR role
            testAccount.setRole(RoleEnum.DOCTOR);

            given(accountRepository.findOne(any(Specification.class)))
                    .willReturn(Optional.of(testAccount));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(testAccount.getEmail());

            // Then
            CustomUserDetails customDetails = (CustomUserDetails) result;
            assertThat(customDetails.getRole()).isEqualTo(RoleEnum.DOCTOR.name());
        }

        @Test
        @DisplayName("Should load ADMIN user correctly")
        void loadUserByUsername_withAdminRole_shouldReturnAdminUserDetails() {
            // Given
            testAccount.setRole(RoleEnum.ADMIN);

            given(accountRepository.findOne(any(Specification.class)))
                    .willReturn(Optional.of(testAccount));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(testAccount.getEmail());

            // Then
            CustomUserDetails customDetails = (CustomUserDetails) result;
            assertThat(customDetails.getRole()).isEqualTo(RoleEnum.ADMIN.name());
            assertThat(customDetails.getId()).isEqualTo(testAccount.getId());
        }

        @Test
        @DisplayName("Should preserve password hash in user details")
        void loadUserByUsername_shouldPreservePasswordHash() {
            // Given
            String expectedPasswordHash = "$2a$10$specific.encoded.password.hash";
            testAccount.setPassword(expectedPasswordHash);

            given(accountRepository.findOne(any(Specification.class)))
                    .willReturn(Optional.of(testAccount));

            // When
            UserDetails result = userDetailsService.loadUserByUsername(testAccount.getEmail());

            // Then
            assertThat(result.getPassword()).isEqualTo(expectedPasswordHash);
        }
    }
}
