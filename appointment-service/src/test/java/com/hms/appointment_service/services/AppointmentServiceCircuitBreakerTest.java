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
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
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
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Circuit Breaker behavior in AppointmentService.
 * 
 * Verifies:
 * 1. getAvailableSlots - FAIL FAST when HR service is unavailable (CB open)
 * 2. registerWalkIn - FAIL FAST when external services are unavailable (CB open)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Circuit Breaker Tests")
class AppointmentServiceCircuitBreakerTest {

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

    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private static final String DOCTOR_ID = "doc-123";
    private static final String PATIENT_ID = "patient-456";
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 1, 15);

    @BeforeEach
    void setUp() {
        // Create AppointmentService with all dependencies including CircuitBreakerFactory
        appointmentService = new AppointmentService(
            appointmentRepository,
            hrClient,
            patientClient,
            appointmentMapper,
            queueService,
            circuitBreakerFactory
        );

        // Default: circuit breaker factory returns mock circuit breaker
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
     * Helper: CB triggers fallback (open state / service down)
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
    // 1. GET AVAILABLE SLOTS - FAIL FAST (CB throws when open)
    // ========================================================================
    
    @Nested
    @DisplayName("1. Get Available Slots - HR Service (FAIL FAST)")
    class GetAvailableSlotsTests {

        @Test
        @DisplayName("Should return slots when HR service responds successfully")
        void shouldReturnSlots_whenHrServiceSucceeds() {
            // Given: CB passes through
            setupCircuitBreakerToPassThrough();
            
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
            assertThat(slots).hasSize(8); // 8:00 to 12:00 = 8 slots (30 min each)
            assertThat(slots.get(0).getTime()).isEqualTo("08:00");
        }

        @Test
        @DisplayName("Should throw SERVICE_UNAVAILABLE when CB is open (HR service down)")
        void shouldThrowServiceUnavailable_whenCircuitBreakerOpen() {
            // Given: CB is open (fallback throws exception)
            setupCircuitBreakerToFallback(new RuntimeException("Connection refused"));

            // When & Then: FAIL FAST - should throw
            ApiException exception = assertThrows(ApiException.class,
                () -> appointmentService.getAvailableSlots(DOCTOR_ID, TEST_DATE));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("HR service");
        }

        @Test
        @DisplayName("Should return empty list when schedule not found (service up, no data)")
        void shouldReturnEmptyList_whenNoScheduleFound() {
            // Given: CB passes through, but service returns null
            setupCircuitBreakerToPassThrough();
            when(hrClient.getScheduleByDoctorAndDate(DOCTOR_ID, TEST_DATE))
                .thenReturn(ApiResponse.ok(null));

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(DOCTOR_ID, TEST_DATE);

            // Then: Empty list (not an error - just no schedule)
            assertThat(slots).isEmpty();
        }
    }

    // ========================================================================
    // 2. REGISTER WALK-IN - FAIL FAST (CB throws when services unavailable)
    // ========================================================================
    
    @Nested
    @DisplayName("2. Register Walk-In - External Services (FAIL FAST)")
    class RegisterWalkInTests {

        private WalkInRequest createWalkInRequest() {
            WalkInRequest request = new WalkInRequest();
            request.setPatientId(PATIENT_ID);
            request.setDoctorId(DOCTOR_ID);
            request.setReason("Walk-in consultation");
            return request;
        }

        @Test
        @DisplayName("Should register walk-in with real names when all services succeed")
        void shouldRegisterWalkIn_whenServicesSucceed() {
            // Given: CB passes through for all calls
            setupCircuitBreakerToPassThrough();
            
            PatientClient.PatientInfo patient = new PatientClient.PatientInfo(
                PATIENT_ID, "John Doe", "0912345678", "acc-123"
            );
            HrClient.EmployeeInfo doctor = new HrClient.EmployeeInfo(
                DOCTOR_ID, "Dr. Smith", "DOCTOR", "dept-1", "Internal Medicine"
            );

            when(patientClient.getPatientById(PATIENT_ID))
                .thenReturn(ApiResponse.ok(patient));
            when(hrClient.getEmployeeById(DOCTOR_ID))
                .thenReturn(ApiResponse.ok(doctor));
            when(queueService.getNextQueueNumber()).thenReturn(1);
            when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(inv -> {
                    Appointment apt = inv.getArgument(0);
                    apt.setId("apt-123");
                    return apt;
                });
            when(appointmentMapper.entityToResponse(any(Appointment.class)))
                .thenReturn(new AppointmentResponse());

            // When
            AppointmentResponse response = appointmentService.registerWalkIn(createWalkInRequest());

            // Then
            assertThat(response).isNotNull();
            verify(patientClient).getPatientById(PATIENT_ID);
            verify(hrClient).getEmployeeById(DOCTOR_ID);
        }

        @Test
        @DisplayName("Should throw SERVICE_UNAVAILABLE when patient service CB is open")
        void shouldThrowServiceUnavailable_whenPatientServiceCBOpen() {
            // Given: CB triggers fallback (throws exception)
            setupCircuitBreakerToFallback(new RuntimeException("Patient service down"));
            when(queueService.getNextQueueNumber()).thenReturn(1);

            // When & Then: FAIL FAST
            ApiException exception = assertThrows(ApiException.class,
                () -> appointmentService.registerWalkIn(createWalkInRequest()));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("Patient service");
        }

        @Test
        @DisplayName("Should throw SERVICE_UNAVAILABLE when HR service CB is open")
        void shouldThrowServiceUnavailable_whenHrServiceCBOpen() {
            // Given: First CB call (patient) passes, second (HR) triggers fallback
            int[] callCount = {0};
            when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    callCount[0]++;
                    if (callCount[0] == 1) { // Patient lookup - pass through
                        Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    } else { // HR lookup - fallback (throws)
                        Function<Throwable, ?> fallback = invocation.getArgument(1);
                        return fallback.apply(new RuntimeException("HR service down"));
                    }
                });

            PatientClient.PatientInfo patient = new PatientClient.PatientInfo(
                PATIENT_ID, "John Doe", "0912345678", "acc-123"
            );
            when(patientClient.getPatientById(PATIENT_ID))
                .thenReturn(ApiResponse.ok(patient));
            when(queueService.getNextQueueNumber()).thenReturn(1);

            // When & Then: FAIL FAST
            ApiException exception = assertThrows(ApiException.class,
                () -> appointmentService.registerWalkIn(createWalkInRequest()));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("HR service");
        }

        @Test
        @DisplayName("Should throw RESOURCE_NOT_FOUND when patient not found (service up)")
        void shouldThrowResourceNotFound_whenPatientNotFound() {
            // Given: CB passes through, but patient service returns null
            setupCircuitBreakerToPassThrough();
            when(patientClient.getPatientById(PATIENT_ID))
                .thenReturn(ApiResponse.ok(null));
            when(queueService.getNextQueueNumber()).thenReturn(1);

            // When & Then
            ApiException exception = assertThrows(ApiException.class,
                () -> appointmentService.registerWalkIn(createWalkInRequest()));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            assertThat(exception.getMessage()).contains("Patient not found");
        }
    }
}
