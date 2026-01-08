package com.hms.hr_service.hooks;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.hr_service.clients.AppointmentClient;
import com.hms.hr_service.entities.EmployeeSchedule;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for graceful degradation behavior in ScheduleHook.
 * 
 * Key Pattern: FAIL-SAFE
 * When appointment-service is unavailable, ScheduleHook blocks the delete operation
 * to prevent data integrity issues (orphaned appointments).
 * 
 * Verifies:
 * 1. validateDelete - blocks delete when appointment service fails (fail-safe)
 * 2. validateBulkDelete - blocks bulk delete when appointment service fails
 * 3. Normal operations when appointment service responds correctly
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleHook Fail-Safe Degradation Tests")
class ScheduleHookFailSafeTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AppointmentClient appointmentClient;

    private ScheduleHook scheduleHook;

    private static final String SCHEDULE_ID = "schedule-123";
    private static final String EMPLOYEE_ID = "emp-456";
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 1, 15);

    @BeforeEach
    void setUp() {
        scheduleHook = new ScheduleHook(
            scheduleRepository,
            employeeRepository,
            departmentRepository,
            appointmentClient
        );
    }

    // ========================================================================
    // TEST DATA BUILDERS
    // ========================================================================

    private EmployeeSchedule createSchedule() {
        EmployeeSchedule schedule = new EmployeeSchedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setEmployeeId(EMPLOYEE_ID);
        schedule.setWorkDate(WORK_DATE);
        schedule.setStartTime(LocalTime.of(8, 0));
        schedule.setEndTime(LocalTime.of(17, 0));
        schedule.setStatus(ScheduleStatus.AVAILABLE);
        return schedule;
    }

    // ========================================================================
    // 1. VALIDATE DELETE TESTS
    // ========================================================================

    @Nested
    @DisplayName("1. Validate Delete - Appointment Count Check")
    class ValidateDeleteTests {

        @Test
        @DisplayName("Should allow delete when no appointments exist")
        void shouldAllowDelete_whenNoAppointmentsExist() {
            // Given
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenReturn(ApiResponse.ok(0));

            // When/Then - should not throw
            assertDoesNotThrow(() -> scheduleHook.validateDelete(SCHEDULE_ID));
        }

        @Test
        @DisplayName("Should block delete when appointments exist")
        void shouldBlockDelete_whenAppointmentsExist() {
            // Given
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenReturn(ApiResponse.ok(3)); // 3 appointments exist

            // When/Then
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateDelete(SCHEDULE_ID));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
            assertThat(exception.getMessage()).contains("3 active appointment(s)");
        }

        @Test
        @DisplayName("Should block delete when appointment service fails (fail-safe)")
        void shouldBlockDelete_whenAppointmentServiceFails() {
            // Given
            EmployeeSchedule schedule = createSchedule();
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));
            
            // Use doThrow for void-returning or exception scenarios
            doThrow(new RuntimeException("Appointment service unavailable"))
                .when(appointmentClient).countByDoctorAndDate(any(), any());

            // When/Then - FAIL-SAFE: block delete when we can't verify
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateDelete(SCHEDULE_ID));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
            assertThat(exception.getMessage()).contains("Unable to verify");
        }

        @Test
        @DisplayName("Should block delete when appointment service returns null")
        void shouldBlockDelete_whenAppointmentServiceReturnsNull() {
            // Given
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenReturn(ApiResponse.ok(null));

            // When/Then - treat null as 0 appointments, allow delete
            assertDoesNotThrow(() -> scheduleHook.validateDelete(SCHEDULE_ID));
        }

        @Test
        @DisplayName("Should skip validation when schedule not found")
        void shouldSkipValidation_whenScheduleNotFound() {
            // Given
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.empty());

            // When/Then - let generic service handle not found
            assertDoesNotThrow(() -> scheduleHook.validateDelete(SCHEDULE_ID));
            verify(appointmentClient, never()).countByDoctorAndDate(anyString(), any());
        }

        @Test
        @DisplayName("Should block delete when appointment service times out (fail-safe)")
        void shouldBlockDelete_whenAppointmentServiceTimesOut() {
            // Given
            EmployeeSchedule schedule = createSchedule();
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));
            
            // Simulate timeout with FeignException.GatewayTimeout
            doThrow(new feign.FeignException.GatewayTimeout(
                "Gateway Timeout", 
                feign.Request.create(feign.Request.HttpMethod.GET, "/test", 
                    java.util.Collections.emptyMap(), null, null, null),
                null, null))
                .when(appointmentClient).countByDoctorAndDate(any(), any());

            // When/Then - FAIL-SAFE
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateDelete(SCHEDULE_ID));

            assertThat(exception.getMessage()).contains("Unable to verify");
        }
    }

    // ========================================================================
    // 2. VALIDATE BULK DELETE TESTS
    // ========================================================================

    @Nested
    @DisplayName("2. Validate Bulk Delete - Multiple Schedules Check")
    class ValidateBulkDeleteTests {

        private EmployeeSchedule createScheduleWithId(String id, String employeeId, LocalDate date) {
            EmployeeSchedule schedule = new EmployeeSchedule();
            schedule.setId(id);
            schedule.setEmployeeId(employeeId);
            schedule.setWorkDate(date);
            schedule.setStartTime(LocalTime.of(8, 0));
            schedule.setEndTime(LocalTime.of(17, 0));
            schedule.setStatus(ScheduleStatus.AVAILABLE);
            return schedule;
        }

        @Test
        @DisplayName("Should allow bulk delete when no appointments exist for any schedule")
        void shouldAllowBulkDelete_whenNoAppointmentsExist() {
            // Given
            List<String> scheduleIds = List.of("sch-1", "sch-2", "sch-3");
            
            EmployeeSchedule sch1 = createScheduleWithId("sch-1", "emp-1", LocalDate.of(2026, 1, 10));
            EmployeeSchedule sch2 = createScheduleWithId("sch-2", "emp-2", LocalDate.of(2026, 1, 11));
            EmployeeSchedule sch3 = createScheduleWithId("sch-3", "emp-3", LocalDate.of(2026, 1, 12));

            when(scheduleRepository.findById("sch-1")).thenReturn(Optional.of(sch1));
            when(scheduleRepository.findById("sch-2")).thenReturn(Optional.of(sch2));
            when(scheduleRepository.findById("sch-3")).thenReturn(Optional.of(sch3));

            when(appointmentClient.countByDoctorAndDate("emp-1", LocalDate.of(2026, 1, 10)))
                .thenReturn(ApiResponse.ok(0));
            when(appointmentClient.countByDoctorAndDate("emp-2", LocalDate.of(2026, 1, 11)))
                .thenReturn(ApiResponse.ok(0));
            when(appointmentClient.countByDoctorAndDate("emp-3", LocalDate.of(2026, 1, 12)))
                .thenReturn(ApiResponse.ok(0));

            // When/Then
            assertDoesNotThrow(() -> scheduleHook.validateBulkDelete(scheduleIds));
        }

        @Test
        @DisplayName("Should block bulk delete when any schedule has appointments")
        void shouldBlockBulkDelete_whenAnyScheduleHasAppointments() {
            // Given
            List<String> scheduleIds = List.of("sch-1", "sch-2");
            
            EmployeeSchedule sch1 = createScheduleWithId("sch-1", "emp-1", LocalDate.of(2026, 1, 10));
            EmployeeSchedule sch2 = createScheduleWithId("sch-2", "emp-2", LocalDate.of(2026, 1, 11));

            when(scheduleRepository.findById("sch-1")).thenReturn(Optional.of(sch1));
            when(scheduleRepository.findById("sch-2")).thenReturn(Optional.of(sch2));

            when(appointmentClient.countByDoctorAndDate("emp-1", LocalDate.of(2026, 1, 10)))
                .thenReturn(ApiResponse.ok(0));
            when(appointmentClient.countByDoctorAndDate("emp-2", LocalDate.of(2026, 1, 11)))
                .thenReturn(ApiResponse.ok(5)); // Has 5 appointments

            // When/Then
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateBulkDelete(scheduleIds));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
            assertThat(exception.getMessage()).contains("1 schedule(s)");
        }

        @Test
        @DisplayName("Should block bulk delete when appointment service fails for any schedule (fail-safe)")
        void shouldBlockBulkDelete_whenServiceFailsForAnySchedule() {
            // Given
            List<String> scheduleIds = List.of("sch-1", "sch-2");
            
            EmployeeSchedule sch1 = createScheduleWithId("sch-1", "emp-1", LocalDate.of(2026, 1, 10));
            EmployeeSchedule sch2 = createScheduleWithId("sch-2", "emp-2", LocalDate.of(2026, 1, 11));

            when(scheduleRepository.findById("sch-1")).thenReturn(Optional.of(sch1));
            when(scheduleRepository.findById("sch-2")).thenReturn(Optional.of(sch2));

            when(appointmentClient.countByDoctorAndDate("emp-1", LocalDate.of(2026, 1, 10)))
                .thenReturn(ApiResponse.ok(0));
            when(appointmentClient.countByDoctorAndDate("emp-2", LocalDate.of(2026, 1, 11)))
                .thenThrow(new RuntimeException("Appointment service unavailable"));

            // When/Then - FAIL-SAFE: block when we can't verify
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateBulkDelete(scheduleIds));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        @Test
        @DisplayName("Should skip non-existent schedules in bulk validation")
        void shouldSkipNonExistentSchedules_inBulkValidation() {
            // Given
            List<String> scheduleIds = List.of("sch-exists", "sch-not-found");
            
            EmployeeSchedule sch = createScheduleWithId("sch-exists", "emp-1", LocalDate.of(2026, 1, 10));

            when(scheduleRepository.findById("sch-exists")).thenReturn(Optional.of(sch));
            when(scheduleRepository.findById("sch-not-found")).thenReturn(Optional.empty());

            when(appointmentClient.countByDoctorAndDate("emp-1", LocalDate.of(2026, 1, 10)))
                .thenReturn(ApiResponse.ok(0));

            // When/Then - should only check the existing schedule
            assertDoesNotThrow(() -> scheduleHook.validateBulkDelete(scheduleIds));
            
            verify(appointmentClient, times(1)).countByDoctorAndDate(anyString(), any());
        }
    }

    // ========================================================================
    // 3. EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("3. Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle FeignException correctly (fail-safe)")
        void shouldHandleFeignException_failSafe() {
            // Given
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenThrow(new feign.FeignException.ServiceUnavailable(
                    "Service Unavailable", 
                    feign.Request.create(feign.Request.HttpMethod.GET, "/test", 
                        java.util.Collections.emptyMap(), null, null, null),
                    null, null));

            // When/Then - FAIL-SAFE
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateDelete(SCHEDULE_ID));

            assertThat(exception.getMessage()).contains("Unable to verify");
        }

        @Test
        @DisplayName("Should handle API error response from appointment service")
        void shouldHandleApiErrorResponse() {
            // Given
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            
            ApiResponse<Integer> errorResponse = new ApiResponse<>();
            errorResponse.setCode(5000);
            errorResponse.setMessage("Internal error");
            errorResponse.setData(null);
            
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenReturn(errorResponse);

            // When/Then - null data treated as 0 appointments
            assertDoesNotThrow(() -> scheduleHook.validateDelete(SCHEDULE_ID));
        }
    }
}
