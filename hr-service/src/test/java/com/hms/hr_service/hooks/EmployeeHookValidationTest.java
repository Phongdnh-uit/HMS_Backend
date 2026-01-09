package com.hms.hr_service.hooks;

import com.hms.common.clients.AccountClient;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.enums.RoleEnum;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
import com.hms.hr_service.repositories.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Circuit Breaker behavior in EmployeeHook.
 * 
 * Key Pattern: VALIDATION WITH EXTERNAL SERVICE
 * When auth-service CB is open, validation fails with SERVICE_UNAVAILABLE.
 * 
 * Verifies:
 * 1. Account validation with CB pass-through (service healthy)
 * 2. CB fallback behavior (SERVICE_UNAVAILABLE)
 * 3. Local department validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeHook Circuit Breaker Tests")
class EmployeeHookValidationTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private EmployeeHook employeeHook;

    private static final String VALID_ACCOUNT_ID = "acc-123";
    private static final String VALID_DEPARTMENT_ID = "dept-456";

    @BeforeEach
    void setUp() {
        employeeHook = new EmployeeHook(departmentRepository, accountClient, circuitBreakerFactory);
        
        // Default: CB factory returns mock CB
        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    /**
     * Helper: CB passes through (healthy state)
     */
    @SuppressWarnings("unchecked")
    private void setupCircuitBreakerToPassThrough() {
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(invocation -> {
                Supplier<?> supplier = invocation.getArgument(0);
                return supplier.get();
            });
    }

    /**
     * Helper: CB triggers fallback (open state)
     */
    @SuppressWarnings("unchecked")
    private void setupCircuitBreakerToFallback(RuntimeException exception) {
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(invocation -> {
                Function<Throwable, ?> fallback = invocation.getArgument(1);
                return fallback.apply(exception);
            });
    }

    // ========================================================================
    // TEST DATA BUILDERS
    // ========================================================================

    private EmployeeRequest createValidRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setAccountId(VALID_ACCOUNT_ID);
        request.setDepartmentId(VALID_DEPARTMENT_ID);
        request.setFullName("John Doe");
        request.setRole(EmployeeRole.DOCTOR);
        request.setStatus(EmployeeStatus.ACTIVE);
        return request;
    }

    private AccountResponse createAccountResponse() {
        AccountResponse response = new AccountResponse();
        response.setId(VALID_ACCOUNT_ID);
        response.setEmail("john@example.com");
        response.setRole(RoleEnum.DOCTOR);
        response.setEmailVerified(true);
        return response;
    }

    // ========================================================================
    // 1. ACCOUNT VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("1. Account Validation with Auth Service")
    class AccountValidationTests {

        @Test
        @DisplayName("Should pass validation when account exists and CB is closed")
        void shouldPassValidation_whenAccountExists() {
            // Given
            setupCircuitBreakerToPassThrough();
            EmployeeRequest request = createValidRequest();
            
            ApiResponse<AccountResponse> accountResponse = ApiResponse.ok(createAccountResponse());
            when(accountClient.findById(VALID_ACCOUNT_ID)).thenReturn(accountResponse);
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

            // When/Then - should not throw
            assertDoesNotThrow(() -> 
                employeeHook.validateCreate(request, new HashMap<>()));
        }

        @Test
        @DisplayName("Should fail validation when account does not exist")
        void shouldFailValidation_whenAccountNotFound() {
            // Given
            setupCircuitBreakerToPassThrough();
            EmployeeRequest request = createValidRequest();
            
            ApiResponse<AccountResponse> notFoundResponse = new ApiResponse<>();
            notFoundResponse.setCode(4004); // Not found code
            notFoundResponse.setMessage("Account not found");
            
            when(accountClient.findById(VALID_ACCOUNT_ID)).thenReturn(notFoundResponse);
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

            // When/Then
            ApiException exception = assertThrows(ApiException.class, 
                () -> employeeHook.validateCreate(request, new HashMap<>()));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(exception.getFieldErrors()).containsKey("accountId");
            assertThat(exception.getFieldErrors().get("accountId")).contains("does not exist");
        }

        @Test
        @DisplayName("Should fail with SERVICE_UNAVAILABLE when auth CB is open")
        void shouldFailValidation_whenAuthCircuitBreakerIsOpen() {
            // Given
            EmployeeRequest request = createValidRequest();
            // Note: department validation happens AFTER account validation
            // If CB throws, we never reach department check
            
            // CB is open - fallback throws SERVICE_UNAVAILABLE
            setupCircuitBreakerToFallback(new RuntimeException("Auth service unavailable"));

            // When/Then - CB fallback throws SERVICE_UNAVAILABLE
            ApiException exception = assertThrows(ApiException.class, 
                () -> employeeHook.validateCreate(request, new HashMap<>()));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("Auth service unavailable");
        }

        @Test
        @DisplayName("Should skip account validation when accountId is null (no CB call)")
        void shouldSkipAccountValidation_whenAccountIdNull() {
            // Given
            EmployeeRequest request = createValidRequest();
            request.setAccountId(null);
            
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

            // When/Then - should not throw, accountId is optional
            assertDoesNotThrow(() -> 
                employeeHook.validateCreate(request, new HashMap<>()));
            
            // Verify CB was not created for auth
            verify(circuitBreakerFactory, never()).create("hrAuth");
            verify(accountClient, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should skip account validation when accountId is blank (no CB call)")
        void shouldSkipAccountValidation_whenAccountIdBlank() {
            // Given
            EmployeeRequest request = createValidRequest();
            request.setAccountId("   ");
            
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

            // When/Then
            assertDoesNotThrow(() -> 
                employeeHook.validateCreate(request, new HashMap<>()));
            
            verify(circuitBreakerFactory, never()).create("hrAuth");
            verify(accountClient, never()).findById(anyString());
        }
    }

    // ========================================================================
    // 2. DEPARTMENT VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("2. Department Validation (Local)")
    class DepartmentValidationTests {

        @Test
        @DisplayName("Should pass validation when department exists")
        void shouldPassValidation_whenDepartmentExists() {
            // Given
            EmployeeRequest request = createValidRequest();
            request.setAccountId(null); // Skip account validation
            
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

            // When/Then
            assertDoesNotThrow(() -> 
                employeeHook.validateCreate(request, new HashMap<>()));
        }

        @Test
        @DisplayName("Should fail validation when department does not exist")
        void shouldFailValidation_whenDepartmentNotFound() {
            // Given
            EmployeeRequest request = createValidRequest();
            request.setAccountId(null); // Skip account validation
            
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(false);

            // When/Then
            ApiException exception = assertThrows(ApiException.class, 
                () -> employeeHook.validateCreate(request, new HashMap<>()));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(exception.getFieldErrors()).containsKey("departmentId");
        }
    }

    // ========================================================================
    // 3. COMBINED VALIDATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("3. Combined Validation Scenarios")
    class CombinedValidationTests {

        @Test
        @DisplayName("Should collect all validation errors when both account and department invalid")
        void shouldCollectAllErrors_whenBothInvalid() {
            // Given
            setupCircuitBreakerToPassThrough();
            EmployeeRequest request = createValidRequest();
            
            ApiResponse<AccountResponse> notFoundResponse = new ApiResponse<>();
            notFoundResponse.setCode(4004);
            notFoundResponse.setMessage("Account not found");
            
            when(accountClient.findById(VALID_ACCOUNT_ID)).thenReturn(notFoundResponse);
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(false);

            // When/Then
            ApiException exception = assertThrows(ApiException.class, 
                () -> employeeHook.validateCreate(request, new HashMap<>()));

            // Both errors should be collected
            assertThat(exception.getFieldErrors()).hasSize(2);
            assertThat(exception.getFieldErrors()).containsKey("accountId");
            assertThat(exception.getFieldErrors()).containsKey("departmentId");
        }

        @Test
        @DisplayName("Should validate on update with same rules")
        void shouldValidateOnUpdate_withSameRules() {
            // Given
            setupCircuitBreakerToPassThrough();
            EmployeeRequest request = createValidRequest();
            
            ApiResponse<AccountResponse> accountResponse = ApiResponse.ok(createAccountResponse());
            when(accountClient.findById(VALID_ACCOUNT_ID)).thenReturn(accountResponse);
            when(departmentRepository.existsById(VALID_DEPARTMENT_ID)).thenReturn(true);

            // When/Then - validateUpdate uses same validate() method
            assertDoesNotThrow(() -> 
                employeeHook.validateUpdate("emp-123", request, null, new HashMap<>()));
        }
    }
}
