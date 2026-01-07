package com.hms.appointment_service.services;

import com.hms.appointment_service.clients.HrClient;
import com.hms.appointment_service.clients.PatientClient;
import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.dtos.appointment.TimeSlotResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.mappers.AppointmentMapper;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AppointmentService.
 * Tests business logic methods for appointment management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-APT-003/004/005/006: AppointmentService Unit Tests")
class AppointmentServiceTest {

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

    @InjectMocks
    private AppointmentService appointmentService;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private String testDoctorId;
    private LocalDate testDate;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        testDoctorId = TestDataFactory.uuid();
        testDate = LocalDate.now().plusDays(1);

        testAppointment = new Appointment();
        testAppointment.setId(TestDataFactory.uuid());
        testAppointment.setPatientId(TestDataFactory.uuid());
        testAppointment.setDoctorId(testDoctorId);
        testAppointment.setStatus(AppointmentStatus.SCHEDULED);
        testAppointment.setAppointmentTime(Instant.now().plusSeconds(86400));
    }

    @Nested
    @DisplayName("Method: getAvailableSlots()")
    class GetAvailableSlotsTests {

        @Test
        @DisplayName("UC-APT-006: Should return available time slots for a doctor")
        void getAvailableSlots_withValidSchedule_shouldReturnSlots() {
            // Given
            HrClient.ScheduleInfo schedule = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    testDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(11, 0),
                    "AVAILABLE"
            );
            ApiResponse<HrClient.ScheduleInfo> scheduleResponse = ApiResponse.ok(schedule);

            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, testDate))
                    .willReturn(scheduleResponse);
            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of());

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(testDoctorId, testDate);

            // Then
            assertThat(slots).isNotEmpty();
            assertThat(slots).hasSize(4); // 9:00, 9:30, 10:00, 10:30 (30-min intervals)
            assertThat(slots.get(0).getTime()).isEqualTo("09:00");
            assertThat(slots.get(0).isAvailable()).isTrue();
            assertThat(slots.get(1).getTime()).isEqualTo("09:30");
            assertThat(slots.get(2).getTime()).isEqualTo("10:00");
            assertThat(slots.get(3).getTime()).isEqualTo("10:30");

            then(hrClient).should().getScheduleByDoctorAndDate(testDoctorId, testDate);
            then(appointmentRepository).should().findByDoctorIdAndAppointmentTimeBetween(
                    eq(testDoctorId), any(Instant.class), any(Instant.class));
        }

        @Test
        @DisplayName("Should mark booked slots as unavailable")
        void getAvailableSlots_withBookedSlots_shouldMarkUnavailable() {
            // Given
            HrClient.ScheduleInfo schedule = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    testDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0),
                    "AVAILABLE"
            );
            ApiResponse<HrClient.ScheduleInfo> scheduleResponse = ApiResponse.ok(schedule);

            // Create booked appointment at 9:00
            Instant bookedTime = testDate.atTime(9, 0).atZone(VIETNAM_ZONE).toInstant();
            Appointment bookedAppointment = new Appointment();
            bookedAppointment.setAppointmentTime(bookedTime);
            bookedAppointment.setStatus(AppointmentStatus.SCHEDULED);

            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, testDate))
                    .willReturn(scheduleResponse);
            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of(bookedAppointment));

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(testDoctorId, testDate);

            // Then
            assertThat(slots).hasSize(2); // 9:00, 9:30
            assertThat(slots.get(0).getTime()).isEqualTo("09:00");
            assertThat(slots.get(0).isAvailable()).isFalse(); // Booked
            assertThat(slots.get(1).getTime()).isEqualTo("09:30");
            assertThat(slots.get(1).isAvailable()).isTrue(); // Available
        }

        @Test
        @DisplayName("Should ignore cancelled appointments when calculating availability")
        void getAvailableSlots_withCancelledAppointments_shouldIgnoreThem() {
            // Given
            HrClient.ScheduleInfo schedule = new HrClient.ScheduleInfo(
                    TestDataFactory.uuid(),
                    testDoctorId,
                    testDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0),
                    "AVAILABLE"
            );
            ApiResponse<HrClient.ScheduleInfo> scheduleResponse = ApiResponse.ok(schedule);

            // Create cancelled appointment at 9:00
            Instant cancelledTime = testDate.atTime(9, 0).atZone(VIETNAM_ZONE).toInstant();
            Appointment cancelledAppointment = new Appointment();
            cancelledAppointment.setAppointmentTime(cancelledTime);
            cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);

            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, testDate))
                    .willReturn(scheduleResponse);
            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of(cancelledAppointment));

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(testDoctorId, testDate);

            // Then - Cancelled slot should be available again
            assertThat(slots).hasSize(2);
            assertThat(slots.get(0).getTime()).isEqualTo("09:00");
            assertThat(slots.get(0).isAvailable()).isTrue(); // Available (cancelled ignored)
        }

        @Test
        @DisplayName("Should return empty list when doctor has no schedule")
        void getAvailableSlots_withNoSchedule_shouldReturnEmptyList() {
            // Given
            ApiResponse<HrClient.ScheduleInfo> emptyResponse = ApiResponse.ok(null);

            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, testDate))
                    .willReturn(emptyResponse);

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(testDoctorId, testDate);

            // Then
            assertThat(slots).isEmpty();
            then(appointmentRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should return empty list when HR service fails")
        void getAvailableSlots_whenHrServiceFails_shouldReturnEmptyList() {
            // Given
            given(hrClient.getScheduleByDoctorAndDate(testDoctorId, testDate))
                    .willThrow(new RuntimeException("HR service unavailable"));

            // When
            List<TimeSlotResponse> slots = appointmentService.getAvailableSlots(testDoctorId, testDate);

            // Then
            assertThat(slots).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: cancelAppointment()")
    class CancelAppointmentTests {

        @Test
        @DisplayName("UC-APT-005: Should cancel a scheduled appointment successfully")
        void cancelAppointment_withScheduledAppointment_shouldCancelSuccessfully() {
            // Given
            String appointmentId = testAppointment.getId();
            String cancelReason = "Patient requested cancellation";

            given(appointmentRepository.findById(appointmentId))
                    .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            Appointment result = appointmentService.cancelAppointment(appointmentId, cancelReason);

            // Then
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(result.getCancelReason()).isEqualTo(cancelReason);
            assertThat(result.getCancelledAt()).isNotNull();
            assertThat(result.getCancelledAt()).isBeforeOrEqualTo(Instant.now());

            then(appointmentRepository).should().findById(appointmentId);
            then(appointmentRepository).should().save(testAppointment);
        }

        @Test
        @DisplayName("Should throw exception when appointment not found")
        void cancelAppointment_withNonExistentAppointment_shouldThrowException() {
            // Given
            String appointmentId = "non-existent-id";
            given(appointmentRepository.findById(appointmentId))
                    .willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId, "reason"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Appointment not found");

            then(appointmentRepository).should().findById(appointmentId);
            then(appointmentRepository).should(never()).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Should throw exception when appointment is already cancelled")
        void cancelAppointment_withAlreadyCancelledAppointment_shouldThrowException() {
            // Given
            testAppointment.setStatus(AppointmentStatus.CANCELLED);
            given(appointmentRepository.findById(testAppointment.getId()))
                    .willReturn(Optional.of(testAppointment));

            // When/Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(testAppointment.getId(), "reason"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already cancelled");

            then(appointmentRepository).should(never()).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Should throw exception when appointment is completed")
        void cancelAppointment_withCompletedAppointment_shouldThrowException() {
            // Given
            testAppointment.setStatus(AppointmentStatus.COMPLETED);
            given(appointmentRepository.findById(testAppointment.getId()))
                    .willReturn(Optional.of(testAppointment));

            // When/Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(testAppointment.getId(), "reason"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Cannot cancel appointment with status: COMPLETED");

            then(appointmentRepository).should(never()).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Should throw exception when appointment is in progress")
        void cancelAppointment_withInProgressAppointment_shouldThrowException() {
            // Given
            testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
            given(appointmentRepository.findById(testAppointment.getId()))
                    .willReturn(Optional.of(testAppointment));

            // When/Then
            assertThatThrownBy(() -> appointmentService.cancelAppointment(testAppointment.getId(), "reason"))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Cannot cancel appointment with status: IN_PROGRESS");

            then(appointmentRepository).should(never()).save(any(Appointment.class));
        }
    }

    @Nested
    @DisplayName("Method: cancelByDoctorAndDate()")
    class CancelByDoctorAndDateTests {

        @Test
        @DisplayName("UC-APT-004: Should cancel all scheduled appointments for doctor on date")
        void cancelByDoctorAndDate_withScheduledAppointments_shouldCancelAll() {
            // Given
            Instant startOfDay = testDate.atStartOfDay(VIETNAM_ZONE).toInstant();
            Instant endOfDay = testDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

            Appointment appointment1 = new Appointment();
            appointment1.setId(TestDataFactory.uuid());
            appointment1.setStatus(AppointmentStatus.SCHEDULED);

            Appointment appointment2 = new Appointment();
            appointment2.setId(TestDataFactory.uuid());
            appointment2.setStatus(AppointmentStatus.SCHEDULED);

            List<Appointment> appointments = Arrays.asList(appointment1, appointment2);
            String cancelReason = "Doctor unavailable";

            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                    testDoctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED))
                    .willReturn(appointments);
            given(appointmentRepository.saveAll(appointments))
                    .willReturn(appointments);

            // When
            int cancelledCount = appointmentService.cancelByDoctorAndDate(testDoctorId, testDate, cancelReason);

            // Then
            assertThat(cancelledCount).isEqualTo(2);
            assertThat(appointment1.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment1.getCancelReason()).isEqualTo(cancelReason);
            assertThat(appointment1.getCancelledAt()).isNotNull();
            assertThat(appointment2.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment2.getCancelReason()).isEqualTo(cancelReason);
            assertThat(appointment2.getCancelledAt()).isNotNull();

            then(appointmentRepository).should().findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                    testDoctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED);
            then(appointmentRepository).should().saveAll(appointments);
        }

        @Test
        @DisplayName("Should return zero when no scheduled appointments found")
        void cancelByDoctorAndDate_withNoAppointments_shouldReturnZero() {
            // Given
            Instant startOfDay = testDate.atStartOfDay(VIETNAM_ZONE).toInstant();
            Instant endOfDay = testDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                    testDoctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED))
                    .willReturn(List.of());

            // When
            int cancelledCount = appointmentService.cancelByDoctorAndDate(testDoctorId, testDate, "reason");

            // Then
            assertThat(cancelledCount).isZero();
            then(appointmentRepository).should(never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Method: completeAppointment()")
    class CompleteAppointmentTests {

        @Test
        @DisplayName("UC-APT-003: Should complete a scheduled appointment successfully")
        void completeAppointment_withScheduledAppointment_shouldCompleteSuccessfully() {
            // Given
            String appointmentId = testAppointment.getId();

            given(appointmentRepository.findById(appointmentId))
                    .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            Appointment result = appointmentService.completeAppointment(appointmentId);

            // Then
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);

            then(appointmentRepository).should().findById(appointmentId);
            then(appointmentRepository).should().save(testAppointment);
        }

        @Test
        @DisplayName("Should complete an in-progress appointment successfully")
        void completeAppointment_withInProgressAppointment_shouldCompleteSuccessfully() {
            // Given
            testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
            String appointmentId = testAppointment.getId();

            given(appointmentRepository.findById(appointmentId))
                    .willReturn(Optional.of(testAppointment));
            given(appointmentRepository.save(any(Appointment.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // When
            Appointment result = appointmentService.completeAppointment(appointmentId);

            // Then
            assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should throw exception when appointment not found")
        void completeAppointment_withNonExistentAppointment_shouldThrowException() {
            // Given
            String appointmentId = "non-existent-id";
            given(appointmentRepository.findById(appointmentId))
                    .willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> appointmentService.completeAppointment(appointmentId))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Appointment not found");
        }

        @Test
        @DisplayName("Should throw exception when appointment is already completed")
        void completeAppointment_withAlreadyCompletedAppointment_shouldThrowException() {
            // Given
            testAppointment.setStatus(AppointmentStatus.COMPLETED);
            given(appointmentRepository.findById(testAppointment.getId()))
                    .willReturn(Optional.of(testAppointment));

            // When/Then
            assertThatThrownBy(() -> appointmentService.completeAppointment(testAppointment.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already completed");

            then(appointmentRepository).should(never()).save(any(Appointment.class));
        }

        @Test
        @DisplayName("Should throw exception when appointment is cancelled")
        void completeAppointment_withCancelledAppointment_shouldThrowException() {
            // Given
            testAppointment.setStatus(AppointmentStatus.CANCELLED);
            given(appointmentRepository.findById(testAppointment.getId()))
                    .willReturn(Optional.of(testAppointment));

            // When/Then
            assertThatThrownBy(() -> appointmentService.completeAppointment(testAppointment.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Cannot complete appointment with status: CANCELLED");
        }
    }

    @Nested
    @DisplayName("Method: countByDoctorAndDate()")
    class CountByDoctorAndDateTests {

        @Test
        @DisplayName("Should count scheduled appointments for doctor on date")
        void countByDoctorAndDate_withScheduledAppointments_shouldReturnCount() {
            // Given
            Instant startOfDay = testDate.atStartOfDay(VIETNAM_ZONE).toInstant();
            Instant endOfDay = testDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

            List<Appointment> appointments = Arrays.asList(
                    new Appointment(),
                    new Appointment(),
                    new Appointment()
            );

            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                    testDoctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED))
                    .willReturn(appointments);

            // When
            int count = appointmentService.countByDoctorAndDate(testDoctorId, testDate);

            // Then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero when no appointments found")
        void countByDoctorAndDate_withNoAppointments_shouldReturnZero() {
            // Given
            Instant startOfDay = testDate.atStartOfDay(VIETNAM_ZONE).toInstant();
            Instant endOfDay = testDate.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

            given(appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                    testDoctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED))
                    .willReturn(List.of());

            // When
            int count = appointmentService.countByDoctorAndDate(testDoctorId, testDate);

            // Then
            assertThat(count).isZero();
        }
    }
}
