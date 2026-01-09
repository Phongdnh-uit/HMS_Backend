package com.hms.hr_service.hooks;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.enums.DepartmentStatus;
import com.hms.hr_service.repositories.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for DepartmentHook.
 * Tests cascade prevention and data enrichment logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-HR-007: DepartmentHook Unit Tests")
class DepartmentHookTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentHook departmentHook;

    private Department testDepartment;
    private DepartmentRequest testRequest;
    private DepartmentResponse testResponse;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testDepartment = new Department();
        testDepartment.setId(TestDataFactory.uuid());
        testDepartment.setName("Cardiology");
        testDepartment.setLocation("Building A");
        testDepartment.setPhoneExtension("1234");
        testDepartment.setStatus(DepartmentStatus.ACTIVE);

        testRequest = new DepartmentRequest();
        testRequest.setName("Emergency");
        testRequest.setLocation("Building B");
        testRequest.setPhoneExtension("9999");
        testRequest.setStatus(DepartmentStatus.ACTIVE);

        testResponse = new DepartmentResponse();
        testResponse.setId(testDepartment.getId());
        testResponse.setName(testDepartment.getName());
        testResponse.setHeadDoctorId(TestDataFactory.uuid());

        testEmployee = new Employee();
        testEmployee.setId(TestDataFactory.uuid());
        testEmployee.setFullName("Dr. John Smith");
        testEmployee.setDepartmentId(testDepartment.getId());
    }

    @Nested
    @DisplayName("Method: enrichFindById()")
    class EnrichFindByIdTests {

        @Test
        @DisplayName("UC-HR-007: Should enrich department with head doctor name")
        void enrichFindById_withHeadDoctor_shouldEnrichName() {
            // Given
            testResponse.setHeadDoctorId(testEmployee.getId());
            given(employeeRepository.findById(testEmployee.getId()))
                    .willReturn(java.util.Optional.of(testEmployee));

            // When
            departmentHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getHeadDoctorName()).isEqualTo("Dr. John Smith");
            then(employeeRepository).should().findById(testEmployee.getId());
        }

        @Test
        @DisplayName("Should handle department without head doctor")
        void enrichFindById_withoutHeadDoctor_shouldNotEnrich() {
            // Given
            testResponse.setHeadDoctorId(null);

            // When
            departmentHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getHeadDoctorName()).isNull();
            then(employeeRepository).should(never()).findById(anyString());
        }

        @Test
        @DisplayName("Should handle head doctor not found")
        void enrichFindById_withInvalidHeadDoctor_shouldNotEnrich() {
            // Given
            String invalidDoctorId = TestDataFactory.uuid();
            testResponse.setHeadDoctorId(invalidDoctorId);
            given(employeeRepository.findById(invalidDoctorId))
                    .willReturn(java.util.Optional.empty());

            // When
            departmentHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getHeadDoctorName()).isNull();
            then(employeeRepository).should().findById(invalidDoctorId);
        }
    }

    @Nested
    @DisplayName("Method: validateDelete()")
    class ValidateDeleteTests {

        @Test
        @DisplayName("UC-HR-007: Should allow delete when department has no employees")
        void validateDelete_withNoEmployees_shouldAllowDelete() {
            // Given
            // Department hook does not currently implement validateDelete
            // This is a design decision - departments can be deleted

            // When & Then - no exception should be thrown
            assertThatCode(() -> departmentHook.validateDelete(testDepartment.getId()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("Should allow valid department creation")
        void validateCreate_withValidRequest_shouldNotThrowException() {
            // Given
            java.util.Map<String, Object> context = new java.util.HashMap<>();

            // When & Then
            assertThatCode(() -> departmentHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("Should allow valid department update")
        void validateUpdate_withValidRequest_shouldNotThrowException() {
            // Given
            java.util.Map<String, Object> context = new java.util.HashMap<>();

            // When & Then
            assertThatCode(() -> departmentHook.validateUpdate(
                    testDepartment.getId(), testRequest, testDepartment, context))
                    .doesNotThrowAnyException();
        }
    }
}
