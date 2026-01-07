package com.hms.auth_service.hooks;

import com.hms.auth_service.entities.Account;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.common.dtos.account.AccountRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AccountHook.
 * Tests lifecycle hooks for Account entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-AUTH-012: AccountHook Unit Tests")
class AccountHookTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountHook accountHook;

    private AccountRequest testRequest;
    private Account testEntity;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        context = new HashMap<>();

        testRequest = AccountRequest.builder()
                .email(TestDataFactory.uniqueEmail())
                .password(TestDataFactory.simplePassword())
                .build();

        testEntity = new Account();
        testEntity.setEmail(testRequest.getEmail());
        testEntity.setPassword(testRequest.getPassword());
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("UC-AUTH-012: Should encode password before creating account")
        void enrichCreate_shouldEncodePassword() {
            // Given
            String plainPassword = "Test@123456";
            testRequest.setPassword(plainPassword);
            testEntity.setPassword(plainPassword);

            String encodedPassword = "$2a$10$encoded.password.hash";
            given(passwordEncoder.encode(plainPassword)).willReturn(encodedPassword);

            // When
            accountHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPassword()).isEqualTo(encodedPassword);
            assertThat(testEntity.getPassword()).isNotEqualTo(plainPassword);

            then(passwordEncoder).should().encode(plainPassword);
        }

        @Test
        @DisplayName("Should handle null password gracefully")
        void enrichCreate_withNullPassword_shouldNotEncodePassword() {
            // Given
            testRequest = AccountRequest.builder()
                    .email(testRequest.getEmail())
                    .password(null)
                    .build();
            testEntity.setPassword(null);

            // When
            accountHook.enrichCreate(testRequest, testEntity, context);

            // Then
            then(passwordEncoder).should(never()).encode(any());
        }

        @Test
        @DisplayName("Should preserve other entity fields")
        void enrichCreate_shouldPreserveOtherFields() {
            // Given
            testEntity.setEmail("test@example.com");
            testEntity.setRole(RoleEnum.DOCTOR);
            testEntity.setEmailVerified(true);

            String encodedPassword = "$2a$10$encoded.password.hash";
            given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);

            // When
            accountHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getEmail()).isEqualTo("test@example.com");
            assertThat(testEntity.getRole()).isEqualTo(RoleEnum.DOCTOR);
            assertThat(testEntity.isEmailVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("Method: enrichUpdate()")
    class EnrichUpdateTests {

        @Test
        @DisplayName("Should encode password when updating with new password")
        void enrichUpdate_withNewPassword_shouldEncodePassword() {
            // Given
            String newPassword = "NewPassword@123";
            testRequest = AccountRequest.builder()
                    .email(testRequest.getEmail())
                    .password(newPassword)
                    .build();
            testEntity.setPassword("oldEncodedPassword");

            String encodedNewPassword = "$2a$10$new.encoded.password";
            given(passwordEncoder.encode(newPassword)).willReturn(encodedNewPassword);

            // When
            accountHook.enrichUpdate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPassword()).isEqualTo(encodedNewPassword);
            assertThat(testEntity.getPassword()).isNotEqualTo("oldEncodedPassword");

            then(passwordEncoder).should().encode(newPassword);
        }

        @Test
        @DisplayName("Should not encode password when password is null")
        void enrichUpdate_withNullPassword_shouldNotEncodePassword() {
            // Given
            testRequest = AccountRequest.builder()
                    .email(testRequest.getEmail())
                    .password(null)
                    .build();
            String existingPassword = "$2a$10$existing.password";
            testEntity.setPassword(existingPassword);

            // When
            accountHook.enrichUpdate(testRequest, testEntity, context);

            // Then
            then(passwordEncoder).should(never()).encode(any());
        }
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("Should pass validation for valid account request")
        void validateCreate_withValidRequest_shouldPass() {
            // Given
            testRequest = AccountRequest.builder()
                    .email("valid@example.com")
                    .password("ValidPassword@123")
                    .build();

            // When/Then - Should not throw exception
            assertThatCode(() -> accountHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("Should pass validation for valid update request")
        void validateUpdate_withValidRequest_shouldPass() {
            // Given
            String accountId = "test-account-id";
            testRequest = AccountRequest.builder()
                    .email("updated@example.com")
                    .password("UpdatedPassword@123")
                    .build();

            // When/Then - Should not throw exception
            assertThatCode(() -> accountHook.validateUpdate(accountId, testRequest, testEntity, context))
                    .doesNotThrowAnyException();
        }
    }
}
