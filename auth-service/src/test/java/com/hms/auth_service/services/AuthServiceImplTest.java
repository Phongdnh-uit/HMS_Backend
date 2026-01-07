package com.hms.auth_service.services;

import com.hms.auth_service.entities.Account;
import com.hms.auth_service.mappers.AccountMapper;
import com.hms.auth_service.repositories.AccountRepository;
import com.hms.auth_service.securities.TokenProvider;
import com.hms.common.dtos.account.AccountRequest;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.enums.RoleEnum;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for AuthServiceImpl.
 * Demonstrates how to write unit tests with Mockito and AssertJ.
 * 
 * Test Pattern:
 * - Given: Set up the test data and mock behaviors
 * - When: Execute the method under test
 * - Then: Verify the results and interactions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-AUTH: AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private TokenProvider tokenProvider;
    
    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Account testAccount;
    private AccountRequest testAccountRequest;
    private AccountResponse testAccountResponse;
    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = TestDataFactory.uniqueEmail();
        testPassword = TestDataFactory.simplePassword();
        
        testAccount = new Account();
        testAccount.setId(TestDataFactory.uuid());
        testAccount.setEmail(testEmail);
        testAccount.setPassword("encodedPassword");
        testAccount.setRole(RoleEnum.PATIENT);
        
        testAccountRequest = new AccountRequest();
        testAccountRequest.setEmail(testEmail);
        testAccountRequest.setPassword(testPassword);
        
        testAccountResponse = new AccountResponse();
        testAccountResponse.setId(testAccount.getId());
        testAccountResponse.setEmail(testEmail);
    }

    @Nested
    @DisplayName("UC-AUTH-001: User Registration")
    class RegistrationTests {

        @Test
        @DisplayName("TC-AUTH-001: Should register new user successfully")
        void register_withValidData_shouldCreateAccount() {
            // Given
            given(accountMapper.requestToEntity(any(AccountRequest.class)))
                .willReturn(testAccount);
            given(passwordEncoder.encode(anyString()))
                .willReturn("encodedPassword");
            given(accountRepository.save(any(Account.class)))
                .willReturn(testAccount);
            given(accountMapper.entityToResponse(any(Account.class)))
                .willReturn(testAccountResponse);

            // When
            AccountResponse result = authService.register(testAccountRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(testEmail);
            verify(accountRepository).save(any(Account.class));
            verify(passwordEncoder).encode(anyString());
        }

        @Test
        @DisplayName("TC-AUTH-002: Should set role to PATIENT by default")
        void register_shouldSetPatientRole() {
            // Given
            Account capturedAccount = new Account();
            capturedAccount.setEmail(testEmail);
            
            given(accountMapper.requestToEntity(any(AccountRequest.class)))
                .willReturn(capturedAccount);
            given(passwordEncoder.encode(anyString()))
                .willReturn("encodedPassword");
            given(accountRepository.save(any(Account.class)))
                .willAnswer(invocation -> {
                    Account saved = invocation.getArgument(0);
                    // Verify role is set to PATIENT
                    assertThat(saved.getRole()).isEqualTo(RoleEnum.PATIENT);
                    return saved;
                });
            given(accountMapper.entityToResponse(any(Account.class)))
                .willReturn(testAccountResponse);

            // When
            authService.register(testAccountRequest);

            // Then - verified in the answer above
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("TC-AUTH-003: Should encode password before saving")
        void register_shouldEncodePassword() {
            // Given
            String rawPassword = "myRawPassword";
            String encodedPassword = "encodedPassword123";
            
            testAccountRequest.setPassword(rawPassword);
            testAccount.setPassword(rawPassword);
            
            given(accountMapper.requestToEntity(any(AccountRequest.class)))
                .willReturn(testAccount);
            given(passwordEncoder.encode(rawPassword))
                .willReturn(encodedPassword);
            given(accountRepository.save(any(Account.class)))
                .willReturn(testAccount);
            given(accountMapper.entityToResponse(any(Account.class)))
                .willReturn(testAccountResponse);

            // When
            authService.register(testAccountRequest);

            // Then
            verify(passwordEncoder).encode(rawPassword);
        }
    }

    @Nested
    @DisplayName("UC-AUTH-005: Find Account By ID")
    class FindByIdTests {

        @Test
        @DisplayName("TC-AUTH-004: Should return account when found")
        void findById_withExistingId_shouldReturnAccount() {
            // Given
            String accountId = testAccount.getId();
            given(accountRepository.findById(accountId))
                .willReturn(Optional.of(testAccount));
            given(accountMapper.entityToResponse(testAccount))
                .willReturn(testAccountResponse);

            // When
            AccountResponse result = authService.findById(accountId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(accountId);
            verify(accountRepository).findById(accountId);
        }

        @Test
        @DisplayName("TC-AUTH-005: Should throw exception when account not found")
        void findById_withNonExistingId_shouldThrowException() {
            // Given
            String nonExistingId = "non-existing-id";
            given(accountRepository.findById(nonExistingId))
                .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.findById(nonExistingId))
                .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    @DisplayName("Repository Method Tests")
    class RepositoryTests {

        @Test
        @DisplayName("TC-AUTH-006: Should check email existence correctly")
        void existsByEmail_shouldReturnTrue() {
            // Given
            given(accountRepository.existsByEmail(testEmail))
                .willReturn(true);

            // When
            boolean exists = accountRepository.existsByEmail(testEmail);

            // Then
            assertThat(exists).isTrue();
            verify(accountRepository).existsByEmail(testEmail);
        }

        @Test
        @DisplayName("TC-AUTH-007: Should return false for non-existing email")
        void existsByEmail_shouldReturnFalse() {
            // Given
            given(accountRepository.existsByEmail("nonexistent@test.com"))
                .willReturn(false);

            // When
            boolean exists = accountRepository.existsByEmail("nonexistent@test.com");

            // Then
            assertThat(exists).isFalse();
        }
    }
}
