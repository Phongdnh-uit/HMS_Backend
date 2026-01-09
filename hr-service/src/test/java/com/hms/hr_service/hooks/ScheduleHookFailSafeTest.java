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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Circuit Breaker behavior in ScheduleHook.
 * 
 * Key Pattern: FAIL-SAFE
 * When appointment-service CB is open, ScheduleHook blocks delete
 * to prevent data integrity issues (orphaned appointments).
 * 
 * Verifies:
 * 1. validateDelete - blocks delete when appointment service CB is open (fail-safe)
 * 2. validateBulkDelete - blocks bulk delete when CB is open
 * 3. Normal operations when CB is closed (service healthy)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleHook Circuit Breaker Tests")
class ScheduleHookFailSafeTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private AppointmentClient appointmentClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

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
            appointmentClient,
            circuitBreakerFactory
        );
        
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
     * The fallback function receives the exception and handles it.
     * For validateDelete, fallback throws ApiException.
     * For validateBulkDelete, fallback returns -1 (sentinel value).
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
            setupCircuitBreakerToPassThrough();
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
            setupCircuitBreakerToPassThrough();
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
        @DisplayName("Should block delete with SERVICE_UNAVAILABLE when CB is open (fail-safe)")
        void shouldBlockDelete_whenCircuitBreakerIsOpen() {
            // Given
            EmployeeSchedule schedule = createSchedule();
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule));
            
            // CB is open - fallback throws SERVICE_UNAVAILABLE
            setupCircuitBreakerToFallback(new RuntimeException("Appointment service unavailable"));

            // When/Then - FAIL-SAFE: block delete when CB is open
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateDelete(SCHEDULE_ID));

            // CB fallback throws SERVICE_UNAVAILABLE, not OPERATION_NOT_ALLOWED
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("Appointment service unavailable");
        }

        @Test
        @DisplayName("Should allow delete when appointment service returns null count")
        void shouldAllowDelete_whenAppointmentServiceReturnsNullCount() {
            // Given
            setupCircuitBreakerToPassThrough();
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenReturn(ApiResponse.ok(null));

            // When/Then - null treated as 0, allow delete
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
            setupCircuitBreakerToPassThrough();
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
            setupCircuitBreakerToPassThrough();
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
        @DisplayName("Should block bulk delete with OPERATION_NOT_ALLOWED when CB is open (fail-safe returns -1)")
        void shouldBlockBulkDelete_whenCircuitBreakerIsOpen() {
            // Given
            List<String> scheduleIds = List.of("sch-1");
            
            EmployeeSchedule sch1 = createScheduleWithId("sch-1", "emp-1", LocalDate.of(2026, 1, 10));

            when(scheduleRepository.findById("sch-1")).thenReturn(Optional.of(sch1));

            // CB is open - fallback returns -1 (sentinel value for "unknown")
            setupCircuitBreakerToFallback(new RuntimeException("Appointment service unavailable"));

            // When/Then - FAIL-SAFE: block when CB is open
            // The fallback returns -1, which counts as "has appointments" (count != 0)
            ApiException exception = assertThrows(ApiException.class, 
                () -> scheduleHook.validateBulkDelete(scheduleIds));

            // Bulk delete CB fallback returns -1, not throws, so OPERATION_NOT_ALLOWED
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
        }

        @Test
        @DisplayName("Should skip non-existent schedules in bulk validation")
        void shouldSkipNonExistentSchedules_inBulkValidation() {
            // Given
            setupCircuitBreakerToPassThrough();
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
        @DisplayName("Should block delete with SERVICE_UNAVAILABLE when service fails inside CB")
        void shouldBlockDelete_whenServiceFailsInsideCircuitBreaker() {
            // Given
            setupCircuitBreakerToPassThrough();
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenThrow(new feign.FeignException.ServiceUnavailable(
                    "Service Unavailable", 
                    feign.Request.create(feign.Request.HttpMethod.GET, "/test", 
                        java.util.Collections.emptyMap(), null, null, null),
                    null, null));

            // When/Then - FeignHelper.safeCall wraps FeignException and throws
            // The CB supplier will throw, triggering fallback
            // But since we use pass-through, the exception propagates
            assertThrows(feign.FeignException.class, 
                () -> scheduleHook.validateDelete(SCHEDULE_ID));
        }

        @Test
        @DisplayName("Should allow delete when API returns null count (treated as 0)")
        void shouldAllowDelete_whenApiReturnsNullCount() {
            // Given
            setupCircuitBreakerToPassThrough();
            when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(createSchedule()));
            
            ApiResponse<Integer> response = new ApiResponse<>();
            response.setCode(2000);
            response.setMessage("OK");
            response.setData(null);
            
            when(appointmentClient.countByDoctorAndDate(EMPLOYEE_ID, WORK_DATE))
                .thenReturn(response);

            // When/Then - null data treated as 0 appointments
            assertDoesNotThrow(() -> scheduleHook.validateDelete(SCHEDULE_ID));
        }
    }
}
