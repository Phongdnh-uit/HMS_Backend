package com.hms.appointment_service.services;

import com.hms.appointment_service.clients.HrClient;
import com.hms.appointment_service.clients.PatientClient;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.dtos.appointment.TimeSlotResponse;
import com.hms.appointment_service.dtos.WalkInRequest;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.mappers.AppointmentMapper;
import com.hms.appointment_service.repositories.AppointmentRepository;
import com.hms.common.dtos.ApiResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for graceful degradation behavior in AppointmentService.
 * 
 * Verifies:
 * 1. getAvailableSlots - returns empty list when HR service fails
 * 2. registerWalkIn - uses default names when external services fail
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Graceful Degradation Tests")
class AppointmentServiceGracefulDegradationTest {

    @InjectMocks
    private AppointmentService appointmentService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private HrClient hrClient;

    @Mock
    private PatientClient patientClient;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private QueueService queueService;

    private static final String DOCTOR_ID = "doc-123";
    private static final String PATIENT_ID = "patient-456";
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 1, 15);

    // ========================================================================
    // 1. GET AVAILABLE SLOTS TESTS
    // ========================================================================
    
    @Nested
    @DisplayName("1. Get Available Slots - HR Service Integration")
    class GetAvailableSlotsTests {

        @Test
        @DisplayName("Should return slots when HR service responds successfully")
        void shouldReturnSlots_whenHrServiceSucceeds() {
            // Given
            HrClient.ScheduleInfo schedule = new HrClient.ScheduleInfo(
                "schedule-1",
                DOCTOR_ID,
                TEST_DATE,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                "AVAILABLE"
            );
            when(hrClient.getScheduleByDoctorAndDate(DOCTOR_ID, TEST_DATE))
                .thenReturn(ApiResponse.ok(schedule));
            when(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(anyString(), any(), any()))
                .thenReturn(List.of());

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(DOCTOR_ID, TEST_DATE);

            // Then
            assertThat(slots).isNotEmpty();
            // 8:00 to 12:00 = 4 hours = 8 slots (30 min each)
            assertThat(slots).hasSize(8);
            assertThat(slots.get(0).getTime()).isEqualTo("08:00");
            assertThat(slots.get(0).isAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should return empty list when HR service throws exception (graceful degradation)")
        void shouldReturnEmptyList_whenHrServiceFails() {
            // Given - HR service throws RuntimeException
            when(hrClient.getScheduleByDoctorAndDate(DOCTOR_ID, TEST_DATE))
                .thenThrow(new RuntimeException("HR service unavailable"));

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(DOCTOR_ID, TEST_DATE);

            // Then - graceful degradation: return empty list, don't throw
            assertThat(slots).isEmpty();
            verify(hrClient).getScheduleByDoctorAndDate(DOCTOR_ID, TEST_DATE);
        }

        @Test
        @DisplayName("Should return empty list when schedule not found")
        void shouldReturnEmptyList_whenScheduleNotFound() {
            // Given - HR service returns null data
            when(hrClient.getScheduleByDoctorAndDate(DOCTOR_ID, TEST_DATE))
                .thenReturn(ApiResponse.ok(null));

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(DOCTOR_ID, TEST_DATE);

            // Then
            assertThat(slots).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when HR service returns error response")
        void shouldReturnEmptyList_whenHrServiceReturnsError() {
            // Given - HR service returns error ApiResponse
            ApiResponse<HrClient.ScheduleInfo> errorResponse = new ApiResponse<>();
            errorResponse.setCode(5000);
            errorResponse.setMessage("Internal error");
            when(hrClient.getScheduleByDoctorAndDate(DOCTOR_ID, TEST_DATE))
                .thenReturn(errorResponse);

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(DOCTOR_ID, TEST_DATE);

            // Then
            assertThat(slots).isEmpty();
        }
    }

    // ========================================================================
    // 2. REGISTER WALK-IN TESTS
    // ========================================================================

    @Nested
    @DisplayName("2. Register Walk-In - External Service Integration")
    class RegisterWalkInTests {

        private WalkInRequest createWalkInRequest() {
            WalkInRequest request = new WalkInRequest();
            request.setPatientId(PATIENT_ID);
            request.setDoctorId(DOCTOR_ID);
            request.setReason("Walk-in consultation");
            request.setPriorityReason(null);
            return request;
        }

        @BeforeEach
        void setUp() {
            // Setup queue service - no arguments version
            when(queueService.getNextQueueNumber()).thenReturn(1);
        }

        @Test
        @DisplayName("Should register walk-in with patient and doctor names when services succeed")
        void shouldRegisterWalkIn_withNames_whenServicesSucceed() {
            // Given
            WalkInRequest request = createWalkInRequest();
            
            PatientClient.PatientInfo patient = new PatientClient.PatientInfo(
                PATIENT_ID, "John Doe", "0123456789", "acc-1"
            );
            when(patientClient.getPatientById(PATIENT_ID))
                .thenReturn(ApiResponse.ok(patient));
            
            HrClient.EmployeeInfo doctor = new HrClient.EmployeeInfo(
                DOCTOR_ID, "Dr. Smith", "DOCTOR", "dept-1", "Internal Medicine"
            );
            when(hrClient.getEmployeeById(DOCTOR_ID))
                .thenReturn(ApiResponse.ok(doctor));

            Appointment savedAppointment = new Appointment();
            savedAppointment.setId("apt-new");
            savedAppointment.setPatientName("John Doe");
            savedAppointment.setDoctorName("Dr. Smith");
            when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> {
                    Appointment apt = inv.getArgument(0);
                    apt.setId("apt-new");
                    return apt;
                });

            AppointmentResponse response = new AppointmentResponse();
            response.setId("apt-new");
            when(appointmentMapper.entityToResponse(any(Appointment.class)))
                .thenReturn(response);

            // When
            AppointmentResponse result = appointmentService.registerWalkIn(request);

            // Then
            assertThat(result).isNotNull();
            verify(patientClient).getPatientById(PATIENT_ID);
            verify(hrClient).getEmployeeById(DOCTOR_ID);
            
            // Verify names were set
            verify(appointmentRepository).save(argThat(apt -> 
                "John Doe".equals(apt.getPatientName()) && 
                "Dr. Smith".equals(apt.getDoctorName())
            ));
        }

        @Test
        @DisplayName("Should use default 'Walk-in Patient' when patient service fails (graceful degradation)")
        void shouldUseDefaultPatientName_whenPatientServiceFails() {
            // Given
            WalkInRequest request = createWalkInRequest();
            
            // Patient service fails
            when(patientClient.getPatientById(PATIENT_ID))
                .thenThrow(new RuntimeException("Patient service unavailable"));
            
            // Doctor service succeeds
            HrClient.EmployeeInfo doctor = new HrClient.EmployeeInfo(
                DOCTOR_ID, "Dr. Smith", "DOCTOR", "dept-1", "Internal Medicine"
            );
            when(hrClient.getEmployeeById(DOCTOR_ID))
                .thenReturn(ApiResponse.ok(doctor));

            when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> {
                    Appointment apt = inv.getArgument(0);
                    apt.setId("apt-new");
                    return apt;
                });
            when(appointmentMapper.entityToResponse(any(Appointment.class)))
                .thenReturn(new AppointmentResponse());

            // When - should NOT throw
            assertDoesNotThrow(() -> appointmentService.registerWalkIn(request));

            // Then - verify default name was used
            verify(appointmentRepository).save(argThat(apt -> 
                "Walk-in Patient".equals(apt.getPatientName())
            ));
        }

        @Test
        @DisplayName("Should use default 'Doctor' when HR service fails (graceful degradation)")
        void shouldUseDefaultDoctorName_whenHrServiceFails() {
            // Given
            WalkInRequest request = createWalkInRequest();
            
            // Patient service succeeds
            PatientClient.PatientInfo patient = new PatientClient.PatientInfo(
                PATIENT_ID, "John Doe", "0123456789", "acc-1"
            );
            when(patientClient.getPatientById(PATIENT_ID))
                .thenReturn(ApiResponse.ok(patient));
            
            // HR service fails
            when(hrClient.getEmployeeById(DOCTOR_ID))
                .thenThrow(new RuntimeException("HR service unavailable"));

            when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> {
                    Appointment apt = inv.getArgument(0);
                    apt.setId("apt-new");
                    return apt;
                });
            when(appointmentMapper.entityToResponse(any(Appointment.class)))
                .thenReturn(new AppointmentResponse());

            // When - should NOT throw
            assertDoesNotThrow(() -> appointmentService.registerWalkIn(request));

            // Then - verify default name was used
            verify(appointmentRepository).save(argThat(apt -> 
                "Doctor".equals(apt.getDoctorName())
            ));
        }

        @Test
        @DisplayName("Should still succeed when both external services fail (graceful degradation)")
        void shouldSucceed_whenBothServicesFail() {
            // Given
            WalkInRequest request = createWalkInRequest();
            
            // Both services fail
            when(patientClient.getPatientById(PATIENT_ID))
                .thenThrow(new RuntimeException("Patient service unavailable"));
            when(hrClient.getEmployeeById(DOCTOR_ID))
                .thenThrow(new RuntimeException("HR service unavailable"));

            when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> {
                    Appointment apt = inv.getArgument(0);
                    apt.setId("apt-new");
                    return apt;
                });
            when(appointmentMapper.entityToResponse(any(Appointment.class)))
                .thenReturn(new AppointmentResponse());

            // When - should NOT throw, appointment is still registered
            AppointmentResponse result = assertDoesNotThrow(() -> 
                appointmentService.registerWalkIn(request));

            // Then
            assertThat(result).isNotNull();
            
            // Verify default names were used
            verify(appointmentRepository).save(argThat(apt -> 
                "Walk-in Patient".equals(apt.getPatientName()) && 
                "Doctor".equals(apt.getDoctorName())
            ));
        }

        @Test
        @DisplayName("Should use default name when patient service returns null data")
        void shouldUseDefaultName_whenPatientServiceReturnsNull() {
            // Given
            WalkInRequest request = createWalkInRequest();
            
            when(patientClient.getPatientById(PATIENT_ID))
                .thenReturn(ApiResponse.ok(null));
            
            HrClient.EmployeeInfo doctor = new HrClient.EmployeeInfo(
                DOCTOR_ID, "Dr. Smith", "DOCTOR", "dept-1", "Internal Medicine"
            );
            when(hrClient.getEmployeeById(DOCTOR_ID))
                .thenReturn(ApiResponse.ok(doctor));

            when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> {
                    Appointment apt = inv.getArgument(0);
                    apt.setId("apt-new");
                    return apt;
                });
            when(appointmentMapper.entityToResponse(any(Appointment.class)))
                .thenReturn(new AppointmentResponse());

            // When
            assertDoesNotThrow(() -> appointmentService.registerWalkIn(request));

            // Then
            verify(appointmentRepository).save(argThat(apt -> 
                "Walk-in Patient".equals(apt.getPatientName())
            ));
        }
    }
}
