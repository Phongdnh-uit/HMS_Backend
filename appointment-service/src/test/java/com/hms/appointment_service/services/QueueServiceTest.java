package com.hms.appointment_service.services;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.constants.AppointmentType;
import com.hms.appointment_service.dtos.WalkInRequest;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.repositories.AppointmentRepository;
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
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for QueueService.
 * Tests queue management logic for walk-in patients.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-APT-007/008: QueueService Unit Tests")
class QueueServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private QueueService queueService;

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private String testDoctorId;
    private LocalDate today;
    private Instant startOfDay;
    private Instant endOfDay;

    @BeforeEach
    void setUp() {
        testDoctorId = TestDataFactory.uuid();
        today = LocalDate.now(VIETNAM_ZONE);
        startOfDay = today.atStartOfDay(VIETNAM_ZONE).toInstant();
        endOfDay = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
    }

    @Nested
    @DisplayName("Method: getNextQueueNumber()")
    class GetNextQueueNumberTests {

        @Test
        @DisplayName("UC-APT-007: Should return 1 when no appointments exist today")
        void getNextQueueNumber_withNoAppointments_shouldReturnOne() {
            // Given
            given(appointmentRepository.findMaxQueueNumberForDate(any(Instant.class), any(Instant.class)))
                    .willReturn(null);

            // When
            int queueNumber = queueService.getNextQueueNumber();

            // Then
            assertThat(queueNumber).isEqualTo(1);
            then(appointmentRepository).should().findMaxQueueNumberForDate(any(Instant.class), any(Instant.class));
        }

        @Test
        @DisplayName("Should increment from max queue number")
        void getNextQueueNumber_withExistingAppointments_shouldIncrement() {
            // Given - Max queue number is 5
            given(appointmentRepository.findMaxQueueNumberForDate(any(Instant.class), any(Instant.class)))
                    .willReturn(5);

            // When
            int queueNumber = queueService.getNextQueueNumber();

            // Then
            assertThat(queueNumber).isEqualTo(6);
        }

        @Test
        @DisplayName("Should handle large queue numbers")
        void getNextQueueNumber_withLargeNumber_shouldIncrementCorrectly() {
            // Given - Max queue number is 99
            given(appointmentRepository.findMaxQueueNumberForDate(any(Instant.class), any(Instant.class)))
                    .willReturn(99);

            // When
            int queueNumber = queueService.getNextQueueNumber();

            // Then
            assertThat(queueNumber).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Method: calculatePriority()")
    class CalculatePriorityTests {

        @Test
        @DisplayName("UC-APT-007: Should assign priority 10 for EMERGENCY type")
        void calculatePriority_withEmergencyType_shouldReturnHighestPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason(null);

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.EMERGENCY);

            // Then
            assertThat(priority).isEqualTo(10); // Highest priority
        }

        @Test
        @DisplayName("Should assign priority 10 for EMERGENCY reason regardless of type")
        void calculatePriority_withEmergencyReason_shouldReturnHighestPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason("EMERGENCY");

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(10);
        }

        @Test
        @DisplayName("Should assign priority 50 for ELDERLY reason")
        void calculatePriority_withElderlyReason_shouldReturnElderlyPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason("ELDERLY");

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(50);
        }

        @Test
        @DisplayName("Should assign priority 60 for PREGNANT reason")
        void calculatePriority_withPregnantReason_shouldReturnPregnantPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason("PREGNANT");

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(60);
        }

        @Test
        @DisplayName("Should assign priority 70 for DISABILITY reason")
        void calculatePriority_withDisabilityReason_shouldReturnDisabilityPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason("DISABILITY");

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(70);
        }

        @Test
        @DisplayName("Should assign priority 80 for other priority reasons")
        void calculatePriority_withOtherReason_shouldReturnOtherPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason("CHRONIC_DISEASE");

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(80);
        }

        @Test
        @DisplayName("Should assign priority 100 for normal walk-in")
        void calculatePriority_withNoPriorityReason_shouldReturnNormalPriority() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason(null);

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(100); // Lowest priority
        }

        @Test
        @DisplayName("Should handle case-insensitive priority reasons")
        void calculatePriority_withLowercaseReason_shouldRecognizeCorrectly() {
            // Given
            WalkInRequest request = new WalkInRequest();
            request.setPriorityReason("elderly"); // lowercase

            // When
            int priority = queueService.calculatePriority(request, AppointmentType.WALK_IN);

            // Then
            assertThat(priority).isEqualTo(50); // Should still recognize as ELDERLY
        }
    }

    @Nested
    @DisplayName("Method: getDoctorQueueToday()")
    class GetDoctorQueueTodayTests {

        @Test
        @DisplayName("Should return ordered queue for doctor")
        void getDoctorQueueToday_withMultipleAppointments_shouldReturnOrdered() {
            // Given
            Appointment appointment1 = new Appointment();
            appointment1.setQueueNumber(2);
            appointment1.setPriority(100);

            Appointment appointment2 = new Appointment();
            appointment2.setQueueNumber(1);
            appointment2.setPriority(50);

            Appointment appointment3 = new Appointment();
            appointment3.setQueueNumber(3);
            appointment3.setPriority(10);

            List<Appointment> queue = Arrays.asList(appointment3, appointment2, appointment1);
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(queue);

            // When
            List<Appointment> result = queueService.getDoctorQueueToday(testDoctorId);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).isEqualTo(queue);
            then(appointmentRepository).should().findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class));
        }

        @Test
        @DisplayName("Should return empty list when no queue")
        void getDoctorQueueToday_withNoAppointments_shouldReturnEmptyList() {
            // Given
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of());

            // When
            List<Appointment> result = queueService.getDoctorQueueToday(testDoctorId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: getNextInQueue()")
    class GetNextInQueueTests {

        @Test
        @DisplayName("UC-APT-008: Should return highest priority scheduled patient")
        void getNextInQueue_withMultipleWaiting_shouldReturnHighestPriority() {
            // Given
            Appointment appointment1 = new Appointment();
            appointment1.setId(TestDataFactory.uuid());
            appointment1.setQueueNumber(2);
            appointment1.setPriority(100); // Normal
            appointment1.setStatus(AppointmentStatus.SCHEDULED);

            Appointment appointment2 = new Appointment();
            appointment2.setId(TestDataFactory.uuid());
            appointment2.setQueueNumber(1);
            appointment2.setPriority(10); // Emergency
            appointment2.setStatus(AppointmentStatus.SCHEDULED);

            Appointment appointment3 = new Appointment();
            appointment3.setId(TestDataFactory.uuid());
            appointment3.setQueueNumber(3);
            appointment3.setPriority(50); // Elderly
            appointment3.setStatus(AppointmentStatus.SCHEDULED);

            // Repository returns sorted by priority, queueNumber
            List<Appointment> queue = Arrays.asList(appointment2, appointment3, appointment1);
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(queue);

            // When
            Appointment next = queueService.getNextInQueue(testDoctorId);

            // Then
            assertThat(next).isNotNull();
            assertThat(next.getId()).isEqualTo(appointment2.getId()); // Emergency first
            assertThat(next.getPriority()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should skip non-SCHEDULED appointments")
        void getNextInQueue_withInProgressAppointments_shouldSkipThem() {
            // Given
            Appointment appointment1 = new Appointment();
            appointment1.setQueueNumber(1);
            appointment1.setPriority(10);
            appointment1.setStatus(AppointmentStatus.IN_PROGRESS); // Currently being seen

            Appointment appointment2 = new Appointment();
            appointment2.setId(TestDataFactory.uuid());
            appointment2.setQueueNumber(2);
            appointment2.setPriority(50);
            appointment2.setStatus(AppointmentStatus.SCHEDULED); // Waiting

            List<Appointment> queue = Arrays.asList(appointment1, appointment2);
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(queue);

            // When
            Appointment next = queueService.getNextInQueue(testDoctorId);

            // Then
            assertThat(next).isNotNull();
            assertThat(next.getId()).isEqualTo(appointment2.getId()); // Skip IN_PROGRESS
        }

        @Test
        @DisplayName("Should skip completed appointments")
        void getNextInQueue_withCompletedAppointments_shouldSkipThem() {
            // Given
            Appointment appointment1 = new Appointment();
            appointment1.setQueueNumber(1);
            appointment1.setPriority(10);
            appointment1.setStatus(AppointmentStatus.COMPLETED);

            Appointment appointment2 = new Appointment();
            appointment2.setId(TestDataFactory.uuid());
            appointment2.setQueueNumber(2);
            appointment2.setPriority(50);
            appointment2.setStatus(AppointmentStatus.SCHEDULED);

            List<Appointment> queue = Arrays.asList(appointment1, appointment2);
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(queue);

            // When
            Appointment next = queueService.getNextInQueue(testDoctorId);

            // Then
            assertThat(next).isNotNull();
            assertThat(next.getId()).isEqualTo(appointment2.getId());
        }

        @Test
        @DisplayName("Should return null when no appointments have queue numbers")
        void getNextInQueue_withNoQueueNumbers_shouldReturnNull() {
            // Given - Appointments without queue numbers (pre-booked)
            Appointment appointment1 = new Appointment();
            appointment1.setQueueNumber(null);
            appointment1.setStatus(AppointmentStatus.SCHEDULED);

            List<Appointment> queue = Arrays.asList(appointment1);
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(queue);

            // When
            Appointment next = queueService.getNextInQueue(testDoctorId);

            // Then
            assertThat(next).isNull();
        }

        @Test
        @DisplayName("Should return null when queue is empty")
        void getNextInQueue_withEmptyQueue_shouldReturnNull() {
            // Given
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(List.of());

            // When
            Appointment next = queueService.getNextInQueue(testDoctorId);

            // Then
            assertThat(next).isNull();
        }

        @Test
        @DisplayName("Should return null when all appointments are non-SCHEDULED")
        void getNextInQueue_withOnlyNonScheduledAppointments_shouldReturnNull() {
            // Given
            Appointment appointment1 = new Appointment();
            appointment1.setQueueNumber(1);
            appointment1.setPriority(10);
            appointment1.setStatus(AppointmentStatus.COMPLETED);

            Appointment appointment2 = new Appointment();
            appointment2.setQueueNumber(2);
            appointment2.setPriority(50);
            appointment2.setStatus(AppointmentStatus.CANCELLED);

            List<Appointment> queue = Arrays.asList(appointment1, appointment2);
            given(appointmentRepository.findDoctorQueueForDate(eq(testDoctorId), any(Instant.class), any(Instant.class)))
                    .willReturn(queue);

            // When
            Appointment next = queueService.getNextInQueue(testDoctorId);

            // Then
            assertThat(next).isNull();
        }
    }

    @Nested
    @DisplayName("Method: getAllQueuesForToday()")
    class GetAllQueuesForTodayTests {

        @Test
        @DisplayName("Should return all queues for today across all doctors")
        void getAllQueuesForToday_withMultipleDoctors_shouldReturnAll() {
            // Given
            Appointment appointment1 = new Appointment();
            appointment1.setDoctorId("doctor-1");
            appointment1.setQueueNumber(1);
            appointment1.setPriority(10);

            Appointment appointment2 = new Appointment();
            appointment2.setDoctorId("doctor-2");
            appointment2.setQueueNumber(2);
            appointment2.setPriority(50);

            List<Appointment> allQueues = Arrays.asList(appointment1, appointment2);
            given(appointmentRepository.findAllQueuesForDate(any(Instant.class), any(Instant.class)))
                    .willReturn(allQueues);

            // When
            List<Appointment> result = queueService.getAllQueuesForToday();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(appointment1, appointment2);
            then(appointmentRepository).should().findAllQueuesForDate(any(Instant.class), any(Instant.class));
        }

        @Test
        @DisplayName("Should return empty list when no queues exist")
        void getAllQueuesForToday_withNoQueues_shouldReturnEmptyList() {
            // Given
            given(appointmentRepository.findAllQueuesForDate(any(Instant.class), any(Instant.class)))
                    .willReturn(List.of());

            // When
            List<Appointment> result = queueService.getAllQueuesForToday();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
