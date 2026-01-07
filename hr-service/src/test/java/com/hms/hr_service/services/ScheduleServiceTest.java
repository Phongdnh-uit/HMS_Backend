package com.hms.hr_service.services;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.clients.AppointmentClient;
import com.hms.hr_service.dtos.schedule.CancelScheduleResponse;
import com.hms.hr_service.dtos.schedule.ScheduleResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.DepartmentStatus;
import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
import com.hms.hr_service.enums.ScheduleStatus;
import com.hms.hr_service.mappers.ScheduleMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for ScheduleService.
 * Tests business logic methods for schedule management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-HR-004/005/006: ScheduleService Unit Tests")
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private AppointmentClient appointmentClient;

    @InjectMocks
    private ScheduleService scheduleService;

    private String testEmployeeId;
    private String testDoctorId;
    private String testDepartmentId;
    private LocalDate testDate;
    private EmployeeSchedule testSchedule;
    private Employee testEmployee;
    private Department testDepartment;
    private ScheduleResponse testResponse;

    @BeforeEach
    void setUp() {
        testEmployeeId = TestDataFactory.uuid();
        testDoctorId = TestDataFactory.uuid();
        testDepartmentId = TestDataFactory.uuid();
        testDate = LocalDate.now().plusDays(1);

        // Setup test department
        testDepartment = new Department();
        testDepartment.setId(testDepartmentId);
        testDepartment.setName("Cardiology");
        testDepartment.setStatus(DepartmentStatus.ACTIVE);

        // Setup test employee
        testEmployee = new Employee();
        testEmployee.setId(testEmployeeId);
        testEmployee.setFullName("Dr. John Smith");
        testEmployee.setRole(EmployeeRole.DOCTOR);
        testEmployee.setDepartmentId(testDepartmentId);
        testEmployee.setSpecialization("Cardiology");
        testEmployee.setStatus(EmployeeStatus.ACTIVE);

        // Setup test schedule
        testSchedule = new EmployeeSchedule();
        testSchedule.setId(TestDataFactory.uuid());
        testSchedule.setEmployeeId(testEmployeeId);
        testSchedule.setWorkDate(testDate);
        testSchedule.setStartTime(LocalTime.of(9, 0));
        testSchedule.setEndTime(LocalTime.of(17, 0));
        testSchedule.setStatus(ScheduleStatus.AVAILABLE);

        // Setup test response
        testResponse = new ScheduleResponse();
        testResponse.setId(testSchedule.getId());
        testResponse.setEmployeeId(testEmployeeId);
        testResponse.setWorkDate(testDate);
        testResponse.setStartTime(LocalTime.of(9, 0));
        testResponse.setEndTime(LocalTime.of(17, 0));
        testResponse.setStatus(ScheduleStatus.AVAILABLE);
    }

    @Nested
    @DisplayName("Method: getByDoctorAndDate()")
    class GetByDoctorAndDateTests {

        @Test
        @DisplayName("UC-HR-004: Should return schedule for doctor and date")
        void getByDoctorAndDate_withValidData_shouldReturnSchedule() {
            // Given
            given(scheduleRepository.findByEmployeeIdAndWorkDate(testDoctorId, testDate))
                    .willReturn(Optional.of(testSchedule));
            given(scheduleMapper.entityToResponse(testSchedule)).willReturn(testResponse);
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(testEmployee));
            given(departmentRepository.findById(testDepartmentId)).willReturn(Optional.of(testDepartment));

            // When
            ScheduleResponse result = scheduleService.getByDoctorAndDate(testDoctorId, testDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmployeeId()).isEqualTo(testEmployeeId);
            assertThat(result.getWorkDate()).isEqualTo(testDate);

            then(scheduleRepository).should().findByEmployeeIdAndWorkDate(testDoctorId, testDate);
            then(scheduleMapper).should().entityToResponse(testSchedule);
        }

        @Test
        @DisplayName("Should throw exception when schedule not found")
        void getByDoctorAndDate_withNoSchedule_shouldThrowException() {
            // Given
            given(scheduleRepository.findByEmployeeIdAndWorkDate(testDoctorId, testDate))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> scheduleService.getByDoctorAndDate(testDoctorId, testDate))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND)
                    .hasMessageContaining("No schedule found");

            then(scheduleRepository).should().findByEmployeeIdAndWorkDate(testDoctorId, testDate);
            then(scheduleMapper).should(never()).entityToResponse(any());
        }
    }

    @Nested
    @DisplayName("Method: updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("UC-HR-004: Should update schedule status successfully")
        void updateStatus_withValidId_shouldUpdateStatus() {
            // Given
            String scheduleId = testSchedule.getId();
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));
            given(scheduleRepository.save(testSchedule)).willReturn(testSchedule);

            // When
            scheduleService.updateStatus(scheduleId, ScheduleStatus.BOOKED);

            // Then
            assertThat(testSchedule.getStatus()).isEqualTo(ScheduleStatus.BOOKED);
            then(scheduleRepository).should().findById(scheduleId);
            then(scheduleRepository).should().save(testSchedule);
        }

        @Test
        @DisplayName("Should throw exception when schedule not found")
        void updateStatus_withInvalidId_shouldThrowException() {
            // Given
            String invalidId = TestDataFactory.uuid();
            given(scheduleRepository.findById(invalidId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> scheduleService.updateStatus(invalidId, ScheduleStatus.BOOKED))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND)
                    .hasMessageContaining("Schedule not found");

            then(scheduleRepository).should().findById(invalidId);
            then(scheduleRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should update status to CANCELLED")
        void updateStatus_toCancelled_shouldUpdateCorrectly() {
            // Given
            String scheduleId = testSchedule.getId();
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));
            given(scheduleRepository.save(testSchedule)).willReturn(testSchedule);

            // When
            scheduleService.updateStatus(scheduleId, ScheduleStatus.CANCELLED);

            // Then
            assertThat(testSchedule.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
            then(scheduleRepository).should().save(testSchedule);
        }
    }

    @Nested
    @DisplayName("Method: cancelSchedule()")
    class CancelScheduleTests {

        @Test
        @DisplayName("UC-HR-005: Should cancel schedule successfully")
        void cancelSchedule_withValidSchedule_shouldCancelAndReturnResponse() {
            // Given
            String scheduleId = testSchedule.getId();
            String reason = "Doctor on leave";
            
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(testEmployee));
            given(scheduleRepository.save(any(EmployeeSchedule.class))).willReturn(testSchedule);
            given(appointmentClient.cancelByDoctorAndDate(testEmployeeId, testDate, reason))
                    .willReturn(ApiResponse.ok(3)); // 3 appointments cancelled

            // When
            CancelScheduleResponse result = scheduleService.cancelSchedule(scheduleId, reason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getScheduleId()).isEqualTo(scheduleId);
            assertThat(result.getEmployeeId()).isEqualTo(testEmployeeId);
            assertThat(result.getEmployeeName()).isEqualTo("Dr. John Smith");
            assertThat(result.getWorkDate()).isEqualTo(testDate);
            assertThat(result.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
            assertThat(result.getCancelReason()).isEqualTo(reason);
            assertThat(result.getCancelledAppointments()).isEqualTo(3);
            assertThat(result.getCancelledAt()).isNotNull();

            assertThat(testSchedule.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
            assertThat(testSchedule.getNotes()).isEqualTo(reason);

            then(scheduleRepository).should(atLeast(2)).save(any(EmployeeSchedule.class));
            then(appointmentClient).should().cancelByDoctorAndDate(testEmployeeId, testDate, reason);
        }

        @Test
        @DisplayName("Should throw exception when schedule not found")
        void cancelSchedule_withInvalidId_shouldThrowException() {
            // Given
            String invalidId = TestDataFactory.uuid();
            given(scheduleRepository.findById(invalidId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> scheduleService.cancelSchedule(invalidId, "Reason"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND)
                    .hasMessageContaining("Schedule not found");

            then(scheduleRepository).should().findById(invalidId);
            then(appointmentClient).should(never()).cancelByDoctorAndDate(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when schedule already cancelled")
        void cancelSchedule_withAlreadyCancelledSchedule_shouldThrowException() {
            // Given
            testSchedule.setStatus(ScheduleStatus.CANCELLED);
            String scheduleId = testSchedule.getId();
            
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));

            // When & Then
            assertThatThrownBy(() -> scheduleService.cancelSchedule(scheduleId, "Reason"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("already cancelled");

            then(appointmentClient).should(never()).cancelByDoctorAndDate(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when schedule is pending cancel")
        void cancelSchedule_withPendingCancelSchedule_shouldThrowException() {
            // Given
            testSchedule.setStatus(ScheduleStatus.PENDING_CANCEL);
            String scheduleId = testSchedule.getId();
            
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));

            // When & Then
            assertThatThrownBy(() -> scheduleService.cancelSchedule(scheduleId, "Reason"))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("already in progress");

            then(appointmentClient).should(never()).cancelByDoctorAndDate(anyString(), any(), anyString());
        }

        @Test
        @DisplayName("Should rollback when appointment cancellation fails")
        void cancelSchedule_whenAppointmentClientFails_shouldRollback() {
            // Given
            String scheduleId = testSchedule.getId();
            String reason = "Doctor on leave";
            ScheduleStatus originalStatus = testSchedule.getStatus();
            
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(testEmployee));
            given(scheduleRepository.save(any(EmployeeSchedule.class))).willReturn(testSchedule);
            given(appointmentClient.cancelByDoctorAndDate(testEmployeeId, testDate, reason))
                    .willThrow(new RuntimeException("Appointment service unavailable"));

            // When & Then
            assertThatThrownBy(() -> scheduleService.cancelSchedule(scheduleId, reason))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR)
                    .hasMessageContaining("Failed to cancel appointments");

            // Verify rollback occurred
            assertThat(testSchedule.getStatus()).isEqualTo(originalStatus);
            assertThat(testSchedule.getNotes()).isNull();

            then(scheduleRepository).should(times(2)).save(any(EmployeeSchedule.class));
            then(appointmentClient).should().cancelByDoctorAndDate(testEmployeeId, testDate, reason);
        }

        @Test
        @DisplayName("Should handle zero cancelled appointments")
        void cancelSchedule_withZeroAppointments_shouldSucceed() {
            // Given
            String scheduleId = testSchedule.getId();
            String reason = "No appointments scheduled";
            
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(testSchedule));
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(testEmployee));
            given(scheduleRepository.save(any(EmployeeSchedule.class))).willReturn(testSchedule);
            given(appointmentClient.cancelByDoctorAndDate(testEmployeeId, testDate, reason))
                    .willReturn(ApiResponse.ok(0));

            // When
            CancelScheduleResponse result = scheduleService.cancelSchedule(scheduleId, reason);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCancelledAppointments()).isZero();
            assertThat(testSchedule.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);

            then(appointmentClient).should().cancelByDoctorAndDate(testEmployeeId, testDate, reason);
        }
    }

    @Nested
    @DisplayName("Method: getDoctorSchedules()")
    class GetDoctorSchedulesTests {

        @Test
        @DisplayName("UC-HR-006: Should return available doctor schedules")
        void getDoctorSchedules_withValidFilters_shouldReturnPageResponse() {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            ScheduleStatus status = ScheduleStatus.AVAILABLE;
            Pageable pageable = PageRequest.of(0, 10);

            List<EmployeeSchedule> schedules = Arrays.asList(testSchedule);
            Page<EmployeeSchedule> schedulePage = new PageImpl<>(schedules, pageable, 1);

            given(scheduleRepository.findDoctorSchedules(startDate, endDate, status, null, null, pageable))
                    .willReturn(schedulePage);
            given(scheduleMapper.entityToResponse(testSchedule)).willReturn(testResponse);
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(testEmployee));
            given(departmentRepository.findById(testDepartmentId)).willReturn(Optional.of(testDepartment));

            // When
            PageResponse<ScheduleResponse> result = scheduleService.getDoctorSchedules(
                    startDate, endDate, status, null, null, 0, 10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);

            then(scheduleRepository).should().findDoctorSchedules(startDate, endDate, status, null, null, pageable);
        }

        @Test
        @DisplayName("Should filter by doctor ID")
        void getDoctorSchedules_withDoctorId_shouldFilterCorrectly() {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            Pageable pageable = PageRequest.of(0, 10);

            List<EmployeeSchedule> schedules = Arrays.asList(testSchedule);
            Page<EmployeeSchedule> schedulePage = new PageImpl<>(schedules, pageable, 1);

            given(scheduleRepository.findDoctorSchedules(startDate, endDate, ScheduleStatus.AVAILABLE, 
                    testDoctorId, null, pageable))
                    .willReturn(schedulePage);
            given(scheduleMapper.entityToResponse(testSchedule)).willReturn(testResponse);
            given(employeeRepository.findById(testEmployeeId)).willReturn(Optional.of(testEmployee));
            given(departmentRepository.findById(testDepartmentId)).willReturn(Optional.of(testDepartment));

            // When
            PageResponse<ScheduleResponse> result = scheduleService.getDoctorSchedules(
                    startDate, endDate, null, testDoctorId, null, 0, 10);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            then(scheduleRepository).should().findDoctorSchedules(
                    startDate, endDate, ScheduleStatus.AVAILABLE, testDoctorId, null, pageable);
        }

        @Test
        @DisplayName("Should default to AVAILABLE status when not specified")
        void getDoctorSchedules_withoutStatus_shouldDefaultToAvailable() {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            Pageable pageable = PageRequest.of(0, 10);

            Page<EmployeeSchedule> schedulePage = new PageImpl<>(Arrays.asList(), pageable, 0);

            given(scheduleRepository.findDoctorSchedules(startDate, endDate, ScheduleStatus.AVAILABLE, 
                    null, null, pageable))
                    .willReturn(schedulePage);

            // When
            scheduleService.getDoctorSchedules(startDate, endDate, null, null, null, 0, 10);

            // Then
            then(scheduleRepository).should().findDoctorSchedules(
                    startDate, endDate, ScheduleStatus.AVAILABLE, null, null, pageable);
        }

        @Test
        @DisplayName("Should limit page size to maximum 100")
        void getDoctorSchedules_withLargePageSize_shouldLimitTo100() {
            // Given
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            Pageable expectedPageable = PageRequest.of(0, 100);

            Page<EmployeeSchedule> schedulePage = new PageImpl<>(Arrays.asList(), expectedPageable, 0);

            given(scheduleRepository.findDoctorSchedules(startDate, endDate, ScheduleStatus.AVAILABLE, 
                    null, null, expectedPageable))
                    .willReturn(schedulePage);

            // When
            scheduleService.getDoctorSchedules(startDate, endDate, null, null, null, 0, 500);

            // Then
            then(scheduleRepository).should().findDoctorSchedules(
                    startDate, endDate, ScheduleStatus.AVAILABLE, null, null, expectedPageable);
        }
    }
}
