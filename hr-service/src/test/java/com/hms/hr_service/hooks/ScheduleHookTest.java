package com.hms.hr_service.hooks;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.clients.AppointmentClient;
import com.hms.hr_service.dtos.schedule.ScheduleRequest;
import com.hms.hr_service.dtos.schedule.ScheduleResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.DepartmentStatus;
import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
import com.hms.hr_service.enums.ScheduleStatus;
import com.hms.hr_service.repositories.DepartmentRepository;
import com.hms.hr_service.repositories.EmployeeRepository;
import com.hms.hr_service.repositories.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for ScheduleHook.
 * Tests validation logic, conflict detection, and cascade operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-HR-009: ScheduleHook Unit Tests")
class ScheduleHookTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AppointmentClient appointmentClient;

    @InjectMocks
    private ScheduleHook scheduleHook;

    private EmployeeSchedule testSchedule;
    private ScheduleRequest testRequest;
    private ScheduleResponse testResponse;
    private Employee testEmployee;
    private Department testDepartment;
    private String testEmployeeId;
    private String testDepartmentId;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        testEmployeeId = TestDataFactory.uuid();
        testDepartmentId = TestDataFactory.uuid();
        futureDate = LocalDate.now().plusDays(1);

        testDepartment = new Department();
        testDepartment.setId(testDepartmentId);
        testDepartment.setName("Cardiology");
        testDepartment.setStatus(DepartmentStatus.ACTIVE);

        testEmployee = new Employee();
        testEmployee.setId(testEmployeeId);
        testEmployee.setFullName("Dr. John Smith");
        testEmployee.setRole(EmployeeRole.DOCTOR);
        testEmployee.setDepartmentId(testDepartmentId);
        testEmployee.setSpecialization("Cardiology");
        testEmployee.setStatus(EmployeeStatus.ACTIVE);

        testSchedule = new EmployeeSchedule();
        testSchedule.setId(TestDataFactory.uuid());
        testSchedule.setEmployeeId(testEmployeeId);
        testSchedule.setWorkDate(futureDate);
        testSchedule.setStartTime(LocalTime.of(9, 0));
        testSchedule.setEndTime(LocalTime.of(17, 0));
        testSchedule.setStatus(ScheduleStatus.AVAILABLE);

        testRequest = new ScheduleRequest();
        testRequest.setEmployeeId(testEmployeeId);
        testRequest.setWorkDate(futureDate);
        testRequest.setStartTime(LocalTime.of(9, 0));
        testRequest.setEndTime(LocalTime.of(17, 0));
        testRequest.setStatus(ScheduleStatus.AVAILABLE);

        testResponse = new ScheduleResponse();
        testResponse.setId(testSchedule.getId());
        testResponse.setEmployeeId(testEmployeeId);
        testResponse.setWorkDate(futureDate);
        testResponse.setStartTime(LocalTime.of(9, 0));
        testResponse.setEndTime(LocalTime.of(17, 0));
        testResponse.setStatus(ScheduleStatus.AVAILABLE);
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("UC-HR-009: Should validate successful schedule creation")
        void validateCreate_withValidData_shouldPass() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));
            given(scheduleRepository.existsByEmployeeIdAndWorkDate(testEmployeeId, futureDate))
                    .willReturn(false);

            // When & Then
            assertThatCode(() -> scheduleHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            assertThat(context).containsKey("employee");
            then(employeeRepository).should().findById(testEmployeeId);
            then(scheduleRepository).should().existsByEmployeeIdAndWorkDate(testEmployeeId, futureDate);
        }

        @Test
        @DisplayName("Should throw exception when employee not found")
        void validateCreate_withInvalidEmployee_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("employeeId");
                        assertThat(errors.get("employeeId")).contains("not found");
                    });

            then(employeeRepository).should().findById(testEmployeeId);
        }

        @Test
        @DisplayName("Should throw exception when work date is in the past")
        void validateCreate_withPastDate_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setWorkDate(LocalDate.now().minusDays(1));
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("workDate");
                        assertThat(errors.get("workDate")).contains("cannot be in the past");
                    });
        }

        @Test
        @DisplayName("Should throw exception when start time is after end time")
        void validateCreate_withInvalidTimeRange_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setStartTime(LocalTime.of(17, 0));
            testRequest.setEndTime(LocalTime.of(9, 0));
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("endTime");
                        assertThat(errors.get("endTime")).contains("must be after start time");
                    });
        }

        @Test
        @DisplayName("Should throw exception when start time equals end time")
        void validateCreate_withEqualTimes_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setStartTime(LocalTime.of(9, 0));
            testRequest.setEndTime(LocalTime.of(9, 0));
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("endTime");
                    });
        }

        @Test
        @DisplayName("Should throw exception when schedule already exists for employee and date")
        void validateCreate_withDuplicateSchedule_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));
            given(scheduleRepository.existsByEmployeeIdAndWorkDate(testEmployeeId, futureDate))
                    .willReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("workDate");
                        assertThat(errors.get("workDate")).contains("already exists");
                    });

            then(scheduleRepository).should().existsByEmployeeIdAndWorkDate(testEmployeeId, futureDate);
        }

        @Test
        @DisplayName("Should collect multiple validation errors")
        void validateCreate_withMultipleErrors_shouldCollectAllErrors() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setWorkDate(LocalDate.now().minusDays(1)); // Past date
            testRequest.setStartTime(LocalTime.of(17, 0)); // Invalid time range
            testRequest.setEndTime(LocalTime.of(9, 0));
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.empty()); // Invalid employee

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).hasSizeGreaterThanOrEqualTo(2);
                        assertThat(errors).containsKeys("employeeId", "workDate");
                    });
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("UC-HR-009: Should set default status to AVAILABLE")
        void enrichCreate_withNullStatus_shouldSetAvailable() {
            // Given
            Map<String, Object> context = new HashMap<>();
            EmployeeSchedule entity = new EmployeeSchedule();
            entity.setStatus(null);

            // When
            scheduleHook.enrichCreate(testRequest, entity, context);

            // Then
            assertThat(entity.getStatus()).isEqualTo(ScheduleStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Should preserve provided status")
        void enrichCreate_withProvidedStatus_shouldPreserveIt() {
            // Given
            Map<String, Object> context = new HashMap<>();
            EmployeeSchedule entity = new EmployeeSchedule();
            entity.setStatus(ScheduleStatus.BOOKED);

            // When
            scheduleHook.enrichCreate(testRequest, entity, context);

            // Then
            assertThat(entity.getStatus()).isEqualTo(ScheduleStatus.BOOKED);
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("UC-HR-009: Should validate successful schedule update")
        void validateUpdate_withValidData_shouldPass() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setStartTime(LocalTime.of(10, 0));
            testRequest.setEndTime(LocalTime.of(18, 0));
            // No employee ID change, so no repository stub needed

            // When & Then
            assertThatCode(() -> scheduleHook.validateUpdate(
                    testSchedule.getId(), testRequest, testSchedule, context))
                    .doesNotThrowAnyException();

            assertThat(context).containsKey("oldStatus");
        }

        @Test
        @DisplayName("Should throw exception when changing to CANCELLED status")
        void validateUpdate_withCancelledStatus_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setStatus(ScheduleStatus.CANCELLED);

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateUpdate(
                    testSchedule.getId(), testRequest, testSchedule, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("Use POST /hr/schedules/{id}/cancel");
        }

        @Test
        @DisplayName("Should throw exception when setting PENDING_CANCEL status")
        void validateUpdate_withPendingCancelStatus_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testRequest.setStatus(ScheduleStatus.PENDING_CANCEL);

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateUpdate(
                    testSchedule.getId(), testRequest, testSchedule, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("internal status");
        }

        @Test
        @DisplayName("Should allow updating already cancelled schedule")
        void validateUpdate_withAlreadyCancelledSchedule_shouldAllowStatusUpdate() {
            // Given
            Map<String, Object> context = new HashMap<>();
            testSchedule.setStatus(ScheduleStatus.CANCELLED);
            testRequest.setStatus(ScheduleStatus.CANCELLED); // Same status

            // When & Then
            assertThatCode(() -> scheduleHook.validateUpdate(
                    testSchedule.getId(), testRequest, testSchedule, context))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should validate employee change")
        void validateUpdate_withChangedEmployee_shouldValidate() {
            // Given
            Map<String, Object> context = new HashMap<>();
            String newEmployeeId = TestDataFactory.uuid();
            testRequest.setEmployeeId(newEmployeeId);
            given(employeeRepository.findById(newEmployeeId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateUpdate(
                    testSchedule.getId(), testRequest, testSchedule, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("employeeId");
                    });
        }

        @Test
        @DisplayName("Should validate duplicate when employee or date changes")
        void validateUpdate_withChangedDateCreatingDuplicate_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            LocalDate newDate = futureDate.plusDays(1);
            testRequest.setWorkDate(newDate);
            given(scheduleRepository.existsByEmployeeIdAndWorkDate(testEmployeeId, newDate))
                    .willReturn(true);

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateUpdate(
                    testSchedule.getId(), testRequest, testSchedule, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting(ex -> ((ApiException) ex).getFieldErrors())
                    .satisfies(errors -> {
                        assertThat(errors).containsKey("workDate");
                        assertThat(errors.get("workDate")).contains("already exists");
                    });
        }
    }

    @Nested
    @DisplayName("Method: validateDelete()")
    class ValidateDeleteTests {

        @Test
        @DisplayName("UC-HR-009: Should allow delete when no appointments exist")
        void validateDelete_withNoAppointments_shouldAllowDelete() {
            // Given
            given(scheduleRepository.findById(testSchedule.getId()))
                    .willReturn(Optional.of(testSchedule));
            given(appointmentClient.countByDoctorAndDate(testEmployeeId, futureDate))
                    .willReturn(ApiResponse.ok(0));

            // When & Then
            assertThatCode(() -> scheduleHook.validateDelete(testSchedule.getId()))
                    .doesNotThrowAnyException();

            then(appointmentClient).should().countByDoctorAndDate(testEmployeeId, futureDate);
        }

        @Test
        @DisplayName("Should throw exception when appointments exist")
        void validateDelete_withActiveAppointments_shouldThrowException() {
            // Given
            given(scheduleRepository.findById(testSchedule.getId()))
                    .willReturn(Optional.of(testSchedule));
            given(appointmentClient.countByDoctorAndDate(testEmployeeId, futureDate))
                    .willReturn(ApiResponse.ok(3));

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateDelete(testSchedule.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("Cannot delete schedule with 3 active appointment(s)");

            then(appointmentClient).should().countByDoctorAndDate(testEmployeeId, futureDate);
        }

        @Test
        @DisplayName("Should handle schedule not found gracefully")
        void validateDelete_withNonExistentSchedule_shouldNotThrowException() {
            // Given
            String nonExistentId = TestDataFactory.uuid();
            given(scheduleRepository.findById(nonExistentId))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatCode(() -> scheduleHook.validateDelete(nonExistentId))
                    .doesNotThrowAnyException();

            then(appointmentClient).should(never()).countByDoctorAndDate(anyString(), any());
        }

        @Test
        @DisplayName("Should fail safe when appointment service unavailable")
        void validateDelete_whenAppointmentServiceFails_shouldBlockDelete() {
            // Given
            given(scheduleRepository.findById(testSchedule.getId()))
                    .willReturn(Optional.of(testSchedule));
            given(appointmentClient.countByDoctorAndDate(testEmployeeId, futureDate))
                    .willThrow(new RuntimeException("Service unavailable"));

            // When & Then
            assertThatThrownBy(() -> scheduleHook.validateDelete(testSchedule.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("Unable to verify");

            then(appointmentClient).should().countByDoctorAndDate(testEmployeeId, futureDate);
        }
    }

    @Nested
    @DisplayName("Method: enrichFindById()")
    class EnrichFindByIdTests {

        @Test
        @DisplayName("Should enrich response with employee and department info")
        void enrichFindById_withValidData_shouldEnrichResponse() {
            // Given
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));
            given(departmentRepository.findById(testDepartmentId))
                    .willReturn(Optional.of(testDepartment));

            // When
            scheduleHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getEmployee()).isNotNull();
            assertThat(testResponse.getEmployee().getId()).isEqualTo(testEmployeeId);
            assertThat(testResponse.getEmployee().getFullName()).isEqualTo("Dr. John Smith");
            assertThat(testResponse.getEmployee().getRole()).isEqualTo("DOCTOR");
            assertThat(testResponse.getEmployee().getSpecialization()).isEqualTo("Cardiology");
            assertThat(testResponse.getEmployee().getDepartment()).isNotNull();
            assertThat(testResponse.getEmployee().getDepartment().getId()).isEqualTo(testDepartmentId);
            assertThat(testResponse.getEmployee().getDepartment().getName()).isEqualTo("Cardiology");

            then(employeeRepository).should().findById(testEmployeeId);
            then(departmentRepository).should().findById(testDepartmentId);
        }

        @Test
        @DisplayName("Should handle employee not found")
        void enrichFindById_withInvalidEmployee_shouldNotEnrich() {
            // Given
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.empty());

            // When
            scheduleHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getEmployee()).isNull();
            then(employeeRepository).should().findById(testEmployeeId);
            then(departmentRepository).should(never()).findById(anyString());
        }

        @Test
        @DisplayName("Should handle employee without department")
        void enrichFindById_withEmployeeWithoutDepartment_shouldEnrichWithoutDepartment() {
            // Given
            testEmployee.setDepartmentId(null);
            given(employeeRepository.findById(testEmployeeId))
                    .willReturn(Optional.of(testEmployee));

            // When
            scheduleHook.enrichFindById(testResponse);

            // Then
            assertThat(testResponse.getEmployee()).isNotNull();
            assertThat(testResponse.getEmployee().getFullName()).isEqualTo("Dr. John Smith");
            assertThat(testResponse.getEmployee().getDepartment()).isNull();

            then(employeeRepository).should().findById(testEmployeeId);
            then(departmentRepository).should(never()).findById(anyString());
        }
    }
}
