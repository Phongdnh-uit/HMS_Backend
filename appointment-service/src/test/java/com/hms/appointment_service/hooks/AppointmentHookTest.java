package com.hms.appointment_service.hooks;

import com.hms.appointment_service.clients.HrClient;
import com.hms.appointment_service.clients.PatientClient;
import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.constants.AppointmentType;
import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.repositories.AppointmentRepository;
import com.hms.common.dtos.ApiResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AppointmentHook.
 * Tests lifecycle hooks for Appointment entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-APT-009/010: AppointmentHook Unit Tests")
class AppointmentHookTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private HrClient hrClient;

    @Mock
    private PatientClient patientClient;

    @InjectMocks
    private AppointmentHook appointmentHook;

    private AppointmentRequest testRequest;
    private Appointment testEntity;
    private Map<String, Object> context;
    private Instant futureTime;
    private LocalDate appointmentDate;
    private String testDoctorId;
    private String testPatientId;

    @BeforeEach
    void setUp() {
        context = new HashMap<>();
        testDoctorId = TestDataFactory.uuid();
        testPatientId = TestDataFactory.uuid();
        
        // Set appointment time to tomorrow at 10:00 AM Vietnam time to ensure it's within 9:00-17:00 schedule
        LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        futureTime = tomorrow.atTime(10, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        appointmentDate = futureTime.atZone(ZoneId.systemDefault()).toLocalDate();

        testRequest = new AppointmentRequest();
        testRequest.setPatientId(testPatientId);
        testRequest.setDoctorId(testDoctorId);
        testRequest.setAppointmentTime(futureTime.toString());
        testRequest.setType(AppointmentType.CONSULTATION);
        testRequest.setReason("Regular checkup");
        testRequest.setNotes("Patient needs blood pressure monitoring");

        testEntity = new Appointment();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setPatientId(testPatientId);
        testEntity.setDoctorId(testDoctorId);
        testEntity.setAppointmentTime(futureTime);
        testEntity.setType(AppointmentType.CONSULTATION);
        testEntity.setStatus(AppointmentStatus.SCHEDULED);
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("UC-APT-009: Should pass validation with valid appointment data")
        void validateCreate_withValidData_shouldPass() {
            // Given
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    TestDataFactory.fullName(),
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );
            HrClient.ScheduleInfo scheduleInfo = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    appointmentDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "AVAILABLE"
            );

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(employeeInfo));
            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willReturn(ApiResponse.ok(scheduleInfo));
            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of());

            // When & Then
            assertThatCode(() -> appointmentHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            // Verify context was populated with required data
            assertThat(context.get("appointmentInstant")).isNotNull();
            assertThat(context.get("patient")).isNotNull();
            assertThat(context.get("doctor")).isNotNull();
            assertThat(context.get("schedule")).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when appointment time is invalid format")
        void validateCreate_withInvalidTimeFormat_shouldThrowException() {
            // Given
            testRequest.setAppointmentTime("invalid-time-format");

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Invalid appointment time format");
        }

        @Test
        @DisplayName("Should throw exception when appointment time is in the past")
        void validateCreate_withPastTime_shouldThrowException() {
            // Given
            Instant pastTime = Instant.now().minus(1, ChronoUnit.DAYS);
            testRequest.setAppointmentTime(pastTime.toString());

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Appointment time must be in the future");
        }

        @Test
        @DisplayName("Should throw exception when patient not found")
        void validateCreate_withNonExistentPatient_shouldThrowException() {
            // Given
            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(null));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Patient not found");
        }

        @Test
        @DisplayName("Should throw exception when patient service fails")
        void validateCreate_whenPatientServiceFails_shouldThrowException() {
            // Given
            given(patientClient.getPatientById(testPatientId))
                    .willThrow(new RuntimeException("Service unavailable"));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Unable to verify patient");
        }

        @Test
        @DisplayName("Should throw exception when doctor not found")
        void validateCreate_withNonExistentDoctor_shouldThrowException() {
            // Given
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(null));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Doctor not found");
        }

        @Test
        @DisplayName("Should throw exception when employee is not a doctor")
        void validateCreate_withNonDoctorEmployee_shouldThrowException() {
            // Given
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    TestDataFactory.fullName(),
                    "NURSE", // Not a doctor
                    TestDataFactory.uuid(),
                    "Emergency"
            );

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(employeeInfo));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Employee is not a doctor");
        }

        @Test
        @DisplayName("Should throw exception when doctor has no schedule on date")
        void validateCreate_withNoSchedule_shouldThrowException() {
            // Given
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    TestDataFactory.fullName(),
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(employeeInfo));
            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willReturn(ApiResponse.ok(null));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Doctor has no schedule on this date");
        }

        @Test
        @DisplayName("Should throw exception when doctor's schedule is cancelled")
        void validateCreate_withCancelledSchedule_shouldThrowException() {
            // Given
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    TestDataFactory.fullName(),
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );
            HrClient.ScheduleInfo scheduleInfo = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    appointmentDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "CANCELLED"
            );

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(employeeInfo));
            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willReturn(ApiResponse.ok(scheduleInfo));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Doctor's schedule is cancelled for this date");
        }

        @Test
        @DisplayName("Should throw exception when time is outside schedule hours")
        void validateCreate_withTimeOutsideSchedule_shouldThrowException() {
            // Given
            Instant earlyTime = appointmentDate.atTime(8, 0)
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toInstant();
            testRequest.setAppointmentTime(earlyTime.toString());

            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    TestDataFactory.fullName(),
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );
            HrClient.ScheduleInfo scheduleInfo = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    appointmentDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "AVAILABLE"
            );

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(employeeInfo));
            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willReturn(ApiResponse.ok(scheduleInfo));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Appointment time is outside doctor's schedule hours");
        }

        @Test
        @DisplayName("Should throw exception when time slot is already booked")
        void validateCreate_withBookedSlot_shouldThrowException() {
            // Given
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    TestDataFactory.fullName(),
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    TestDataFactory.fullName(),
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );
            HrClient.ScheduleInfo scheduleInfo = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    appointmentDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "AVAILABLE"
            );

            Appointment existingAppointment = new Appointment();
            existingAppointment.setId(TestDataFactory.uuid());

            given(patientClient.getPatientById(testPatientId))
                    .willReturn(ApiResponse.ok(patientInfo));
            given(hrClient.getEmployeeById(testDoctorId))
                    .willReturn(ApiResponse.ok(employeeInfo));
            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willReturn(ApiResponse.ok(scheduleInfo));
            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of(existingAppointment));

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("This time slot is already booked");
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("UC-APT-009: Should set default status to SCHEDULED")
        void enrichCreate_shouldSetDefaultStatus() {
            // Given
            testEntity.setStatus(null);
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    "John Doe",
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    "Dr. Smith",
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );
            context.put("patient", patientInfo);
            context.put("doctor", employeeInfo);

            // When
            appointmentHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        }

        @Test
        @DisplayName("Should snapshot patient name from context")
        void enrichCreate_shouldSnapshotPatientName() {
            // Given
            String patientName = "Jane Doe";
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    patientName,
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    "Dr. Smith",
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    "Cardiology"
            );
            context.put("patient", patientInfo);
            context.put("doctor", employeeInfo);

            // When
            appointmentHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPatientName()).isEqualTo(patientName);
        }

        @Test
        @DisplayName("Should snapshot doctor name and department from context")
        void enrichCreate_shouldSnapshotDoctorInfo() {
            // Given
            String doctorName = "Dr. Smith";
            String department = "Cardiology";
            PatientClient.PatientInfo patientInfo = new PatientClient.PatientInfo(
                    testPatientId,
                    "John Doe",
                    "0912345678",
                    TestDataFactory.uuid()
            );
            HrClient.EmployeeInfo employeeInfo = new HrClient.EmployeeInfo(
                    testDoctorId,
                    doctorName,
                    "DOCTOR",
                    TestDataFactory.uuid(),
                    department
            );
            context.put("patient", patientInfo);
            context.put("doctor", employeeInfo);

            // When
            appointmentHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getDoctorName()).isEqualTo(doctorName);
            assertThat(testEntity.getDoctorDepartment()).isEqualTo(department);
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("Should allow update of SCHEDULED appointment")
        void validateUpdate_withScheduledAppointment_shouldPass() {
            // Given
            testEntity.setStatus(AppointmentStatus.SCHEDULED);

            // When & Then
            assertThatCode(() -> appointmentHook.validateUpdate(testEntity.getId(), testRequest, testEntity, context))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception when updating non-SCHEDULED appointment")
        void validateUpdate_withCompletedAppointment_shouldThrowException() {
            // Given
            testEntity.setStatus(AppointmentStatus.COMPLETED);

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateUpdate(testEntity.getId(), testRequest, testEntity, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Cannot update appointment with status: COMPLETED");
        }

        @Test
        @DisplayName("Should throw exception when updating cancelled appointment")
        void validateUpdate_withCancelledAppointment_shouldThrowException() {
            // Given
            testEntity.setStatus(AppointmentStatus.CANCELLED);

            // When & Then
            assertThatThrownBy(() -> appointmentHook.validateUpdate(testEntity.getId(), testRequest, testEntity, context))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Cannot update appointment with status: CANCELLED");
        }
    }

    @Nested
    @DisplayName("Method: afterUpdate()")
    class AfterUpdateTests {

        @Test
        @DisplayName("UC-APT-010: Should handle status change to CANCELLED")
        void afterUpdate_withCancelledStatus_shouldTriggerScheduleCheck() {
            // Given
            testEntity.setStatus(AppointmentStatus.CANCELLED);
            AppointmentResponse response = new AppointmentResponse();

            HrClient.ScheduleInfo scheduleInfo = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    appointmentDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    "BOOKED"
            );

            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willReturn(ApiResponse.ok(scheduleInfo));
            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                    eq(testDoctorId), any(Instant.class), any(Instant.class), eq(AppointmentStatus.SCHEDULED)))
                    .willReturn(List.of());

            // When
            appointmentHook.afterUpdate(testEntity, response, context);

            // Then - Verify schedule status was checked (method call made)
            then(hrClient).should().getScheduleByDoctorAndDate(testDoctorId, appointmentDate);
        }

        @Test
        @DisplayName("Should not fail when schedule update fails")
        void afterUpdate_whenScheduleUpdateFails_shouldNotThrowException() {
            // Given
            testEntity.setStatus(AppointmentStatus.CANCELLED);
            AppointmentResponse response = new AppointmentResponse();

            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, appointmentDate))
                    .willThrow(new RuntimeException("HR service unavailable"));

            // When & Then - Should not throw exception (graceful degradation)
            assertThatCode(() -> appointmentHook.afterUpdate(testEntity, response, context))
                    .doesNotThrowAnyException();
        }
    }
}
