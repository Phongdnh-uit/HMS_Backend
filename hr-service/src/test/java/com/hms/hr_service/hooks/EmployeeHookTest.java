package com.hms.hr_service.hooks;

import com.hms.common.clients.AccountClient;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.account.AccountResponse;
import com.hms.common.enums.RoleEnum;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.enums.DepartmentStatus;
import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
import com.hms.hr_service.repositories.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for EmployeeHook.
 * Tests account validation, department enrichment, and file storage logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-HR-008/010: EmployeeHook Unit Tests")
class EmployeeHookTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private EmployeeHook employeeHook;

    private Employee testEmployee;
    private EmployeeRequest testRequest;
    private EmployeeResponse testResponse;
    private Department testDepartment;
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {
        String departmentId = TestDataFactory.uuid();

        testDepartment = new Department();
        testDepartment.setId(departmentId);
        testDepartment.setName("Cardiology");
        testDepartment.setStatus(DepartmentStatus.ACTIVE);

        testEmployee = new Employee();
        testEmployee.setId(TestDataFactory.uuid());
        testEmployee.setAccountId(TestDataFactory.uuid());
        testEmployee.setFullName(TestDataFactory.fullName());
        testEmployee.setRole(EmployeeRole.DOCTOR);
        testEmployee.setDepartmentId(departmentId);
        testEmployee.setStatus(EmployeeStatus.ACTIVE);

        testRequest = new EmployeeRequest();
        testRequest.setAccountId(TestDataFactory.uuid());
        testRequest.setFullName(TestDataFactory.fullName());
        testRequest.setRole(EmployeeRole.NURSE);
        testRequest.setDepartmentId(departmentId);
        testRequest.setStatus(EmployeeStatus.ACTIVE);

        testResponse = new EmployeeResponse();
        testResponse.setId(testEmployee.getId());
        testResponse.setFullName(testEmployee.getFullName());
        testResponse.setDepartmentId(departmentId);

        accountResponse = new AccountResponse();
        accountResponse.setId(TestDataFactory.uuid());
        accountResponse.setEmail(TestDataFactory.uniqueEmail());
        accountResponse.setRole(RoleEnum.DOCTOR);
    }

    @Nested
    @DisplayName("Method: enrichFindById()")
    class EnrichFindByIdTests {

        @Test
        @DisplayName("UC-HR-008: Should enrich employee with department name")
        void enrichFindById_withDepartment_shouldEnrichName() {
            // Given
            given(departmentRepository.findById(testEmployee.getDepartmentId()))
                    .willReturn(java.util.Optional.of(testDepartment));

            // When
            employeeHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getDepartmentName()).isEqualTo("Cardiology");
            then(departmentRepository).should().findById(testEmployee.getDepartmentId());
        }

        @Test
        @DisplayName("Should handle employee without department")
        void enrichFindById_withoutDepartment_shouldNotEnrich() {
            // Given
            testResponse.setDepartmentId(null);

            // When
            employeeHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getDepartmentName()).isNull();
            then(departmentRepository).should(never()).findById(anyString());
        }

        @Test
        @DisplayName("Should handle department not found")
        void enrichFindById_withInvalidDepartment_shouldNotEnrich() {
            // Given
            given(departmentRepository.findById(testEmployee.getDepartmentId()))
                    .willReturn(java.util.Optional.empty());

            // When
            employeeHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getDepartmentName()).isNull();
            then(departmentRepository).should().findById(testEmployee.getDepartmentId());
        }
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("UC-HR-008: Should validate account exists when accountId provided")
        void validateCreate_withValidAccountId_shouldPass() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(accountClient.findById(testRequest.getAccountId()))
                    .willReturn(ApiResponse.ok(accountResponse));
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(true);

            // When & Then
            assertThatCode(() -> employeeHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(accountClient).should().findById(testRequest.getAccountId());
            then(departmentRepository).should().existsById(testRequest.getDepartmentId());
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void validateCreate_withInvalidAccountId_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            ApiResponse<AccountResponse> errorResponse = new ApiResponse<>();
            errorResponse.setCode(ErrorCode.RESOURCE_NOT_FOUND.getCode());
            
            given(accountClient.findById(testRequest.getAccountId()))
                    .willReturn(errorResponse);
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(true);

            // When & Then
            assertThatThrownBy(() -> employeeHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

            then(accountClient).should().findById(testRequest.getAccountId());
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void validateCreate_withInvalidDepartmentId_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setAccountId(null); // Skip account validation
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> employeeHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("departmentId");
                        assertThat(errors.get("departmentId")).contains("does not exist");
                    });

            then(departmentRepository).should().existsById(testRequest.getDepartmentId());
        }

        @Test
        @DisplayName("Should allow creation without accountId")
        void validateCreate_withoutAccountId_shouldValidateOnlyDepartment() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setAccountId(null);
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(true);

            // When & Then
            assertThatCode(() -> employeeHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(accountClient).should(never()).findById(anyString());
            then(departmentRepository).should().existsById(testRequest.getDepartmentId());
        }

        @Test
        @DisplayName("Should allow creation with blank accountId")
        void validateCreate_withBlankAccountId_shouldValidateOnlyDepartment() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setAccountId("   ");
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(true);

            // When & Then
            assertThatCode(() -> employeeHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(accountClient).should(never()).findById(anyString());
            then(departmentRepository).should().existsById(testRequest.getDepartmentId());
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("UC-HR-008: Should validate account and department on update")
        void validateUpdate_withValidData_shouldPass() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(accountClient.findById(testRequest.getAccountId()))
                    .willReturn(ApiResponse.ok(accountResponse));
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(true);

            // When & Then
            assertThatCode(() -> employeeHook.validateUpdate(
                    testEmployee.getId(), testRequest, testEmployee, context))
                    .doesNotThrowAnyException();

            then(accountClient).should().findById(testRequest.getAccountId());
            then(departmentRepository).should().existsById(testRequest.getDepartmentId());
        }

        @Test
        @DisplayName("Should throw exception when department not found")
        void validateUpdate_withInvalidDepartmentId_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setAccountId(null);
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> employeeHook.validateUpdate(
                    testEmployee.getId(), testRequest, testEmployee, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);

            then(departmentRepository).should().existsById(testRequest.getDepartmentId());
        }

        @Test
        @DisplayName("Should collect multiple validation errors")
        void validateUpdate_withMultipleErrors_shouldCollectAllErrors() {
            // Given
            Map<String, Object> context = new HashMap<>();
            ApiResponse<AccountResponse> errorResponse = new ApiResponse<>();
            errorResponse.setCode(ErrorCode.RESOURCE_NOT_FOUND.getCode());
            
            given(accountClient.findById(testRequest.getAccountId()))
                    .willReturn(errorResponse);
            given(departmentRepository.existsById(testRequest.getDepartmentId()))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> employeeHook.validateUpdate(
                    testEmployee.getId(), testRequest, testEmployee, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).hasSize(2);
                        assertThat(errors).containsKeys("accountId", "departmentId");
                    });
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("UC-HR-010: Should not modify entity during enrichCreate")
        void enrichCreate_shouldNotModifyEntity() {
            // Given
            Map<String, Object> context = new HashMap<>();
            Employee entity = new Employee();
            entity.setFullName("Test Name");

            // When
            employeeHook.enrichCreate(testRequest, entity, context);

            // Then
            assertThat(entity.getFullName()).isEqualTo("Test Name");
        }
    }

    @Nested
    @DisplayName("Method: afterCreate()")
    class AfterCreateTests {

        @Test
        @DisplayName("UC-HR-010: Should not modify response during afterCreate")
        void afterCreate_shouldNotModifyResponse() {
            // Given
            Map<String, Object> context = new HashMap<>();
            EmployeeResponse response = new EmployeeResponse();
            response.setFullName("Test Name");

            // When
            employeeHook.afterCreate(testEmployee, response, context);

            // Then
            assertThat(response.getFullName()).isEqualTo("Test Name");
        }
    }
}
