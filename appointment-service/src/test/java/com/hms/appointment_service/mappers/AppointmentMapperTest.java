package com.hms.appointment_service.mappers;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.constants.AppointmentType;
import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AppointmentMapper.
 * Tests MapStruct mapper methods for correct field mapping.
 * 
 * Note: MapStruct mappers are generated at compile time and need Spring context
 * for dependency injection when using componentModel = "spring".
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("UC-APT-001/002: AppointmentMapper Unit Tests")
class AppointmentMapperTest {

    @Autowired
    private AppointmentMapper mapper;

    private Appointment testEntity;
    private AppointmentRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new Appointment();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setPatientId(TestDataFactory.uuid());
        testEntity.setPatientName(TestDataFactory.fullName());
        testEntity.setDoctorId(TestDataFactory.uuid());
        testEntity.setDoctorName(TestDataFactory.fullName());
        testEntity.setDoctorDepartment("Cardiology");
        testEntity.setAppointmentTime(Instant.now().plus(2, ChronoUnit.DAYS));
        testEntity.setStatus(AppointmentStatus.SCHEDULED);
        testEntity.setType(AppointmentType.CONSULTATION);
        testEntity.setReason("Regular checkup");
        testEntity.setNotes("Patient needs blood pressure monitoring");
        testEntity.setQueueNumber(5);
        testEntity.setPriority(100);
        testEntity.setPriorityReason("NORMAL");
        testEntity.setCreatedAt(Instant.now());
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setCreatedBy("admin");
        testEntity.setUpdatedBy("admin");

        // Setup test request
        testRequest = new AppointmentRequest();
        testRequest.setPatientId(TestDataFactory.uuid());
        testRequest.setDoctorId(TestDataFactory.uuid());
        testRequest.setAppointmentTime(Instant.now().plus(3, ChronoUnit.DAYS).toString());
        testRequest.setType(AppointmentType.FOLLOW_UP);
        testRequest.setReason("Follow-up consultation");
        testRequest.setNotes("Check lab results");
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-APT-001: Should map appointment entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            AppointmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getAppointmentTime()).isEqualTo(testEntity.getAppointmentTime());
            assertThat(response.getStatus()).isEqualTo(testEntity.getStatus());
            assertThat(response.getType()).isEqualTo(testEntity.getType());
            assertThat(response.getReason()).isEqualTo(testEntity.getReason());
            assertThat(response.getNotes()).isEqualTo(testEntity.getNotes());
            assertThat(response.getQueueNumber()).isEqualTo(testEntity.getQueueNumber());
            assertThat(response.getPriority()).isEqualTo(testEntity.getPriority());
            assertThat(response.getPriorityReason()).isEqualTo(testEntity.getPriorityReason());
            assertThat(response.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
            assertThat(response.getCreatedBy()).isEqualTo(testEntity.getCreatedBy());
            assertThat(response.getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
        }

        @Test
        @DisplayName("Should map patient information to nested PatientResponse")
        void entityToResponse_shouldMapPatientInformation() {
            // When
            AppointmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getPatient()).isNotNull();
            assertThat(response.getPatient().getId()).isEqualTo(testEntity.getPatientId());
            assertThat(response.getPatient().getFullName()).isEqualTo(testEntity.getPatientName());
        }

        @Test
        @DisplayName("Should map doctor information to nested DoctorResponse")
        void entityToResponse_shouldMapDoctorInformation() {
            // When
            AppointmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getDoctor()).isNotNull();
            assertThat(response.getDoctor().getId()).isEqualTo(testEntity.getDoctorId());
            assertThat(response.getDoctor().getFullName()).isEqualTo(testEntity.getDoctorName());
            assertThat(response.getDoctor().getDepartment()).isEqualTo(testEntity.getDoctorDepartment());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            AppointmentResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should map all appointment statuses correctly")
        void entityToResponse_shouldMapAllStatuses() {
            // Given - Test each status
            AppointmentStatus[] statuses = {
                AppointmentStatus.SCHEDULED, 
                AppointmentStatus.IN_PROGRESS,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.NO_SHOW
            };

            for (AppointmentStatus status : statuses) {
                testEntity.setStatus(status);

                // When
                AppointmentResponse response = mapper.entityToResponse(testEntity);

                // Then
                assertThat(response.getStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("Should map all appointment types correctly")
        void entityToResponse_shouldMapAllTypes() {
            // Given - Test each type
            AppointmentType[] types = {
                AppointmentType.CONSULTATION,
                AppointmentType.FOLLOW_UP,
                AppointmentType.WALK_IN,
                AppointmentType.EMERGENCY
            };

            for (AppointmentType type : types) {
                testEntity.setType(type);

                // When
                AppointmentResponse response = mapper.entityToResponse(testEntity);

                // Then
                assertThat(response.getType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("Should handle null queue fields")
        void entityToResponse_withNullQueueFields_shouldMapCorrectly() {
            // Given
            testEntity.setQueueNumber(null);
            testEntity.setPriority(null);
            testEntity.setPriorityReason(null);

            // When
            AppointmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getQueueNumber()).isNull();
            assertThat(response.getPriority()).isNull();
            assertThat(response.getPriorityReason()).isNull();
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testEntity.setNotes(null);
            testEntity.setReason(null);

            // When
            AppointmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getNotes()).isNull();
            assertThat(response.getReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-APT-002: Should map appointment request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            Appointment entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getPatientId()).isEqualTo(testRequest.getPatientId());
            assertThat(entity.getDoctorId()).isEqualTo(testRequest.getDoctorId());
            assertThat(entity.getType()).isEqualTo(testRequest.getType());
            assertThat(entity.getReason()).isEqualTo(testRequest.getReason());
            assertThat(entity.getNotes()).isEqualTo(testRequest.getNotes());
        }

        @Test
        @DisplayName("Should ignore ID when mapping request to entity")
        void requestToEntity_shouldIgnoreId() {
            // When
            Appointment entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getId()).isNull();
        }

        @Test
        @DisplayName("Should ignore audit fields when mapping request to entity")
        void requestToEntity_shouldIgnoreAuditFields() {
            // When
            Appointment entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getCreatedBy()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("Should ignore patient/doctor names (set by hook)")
        void requestToEntity_shouldIgnoreSnapshotFields() {
            // When
            Appointment entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getPatientName()).isNull();
            assertThat(entity.getDoctorName()).isNull();
            assertThat(entity.getDoctorDepartment()).isNull();
        }

        @Test
        @DisplayName("Should ignore status (set by hook)")
        void requestToEntity_shouldIgnoreStatus() {
            // When
            Appointment entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getStatus()).isNull();
        }

        @Test
        @DisplayName("Should ignore cancellation fields")
        void requestToEntity_shouldIgnoreCancellationFields() {
            // When
            Appointment entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getCancelledAt()).isNull();
            assertThat(entity.getCancelReason()).isNull();
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            Appointment entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should map all appointment types correctly")
        void requestToEntity_shouldMapAllTypes() {
            // Given - Test each type
            AppointmentType[] types = {
                AppointmentType.CONSULTATION,
                AppointmentType.FOLLOW_UP,
                AppointmentType.WALK_IN,
                AppointmentType.EMERGENCY
            };

            for (AppointmentType type : types) {
                testRequest.setType(type);

                // When
                Appointment entity = mapper.requestToEntity(testRequest);

                // Then
                assertThat(entity.getType()).isEqualTo(type);
            }
        }
    }

    @Nested
    @DisplayName("Partial Update Mapping")
    class PartialUpdateTests {

        @Test
        @DisplayName("Should update only modifiable fields from request")
        void partialUpdate_shouldUpdateOnlyModifiableFields() {
            // Given - Create existing entity
            Appointment existingEntity = new Appointment();
            String originalId = TestDataFactory.uuid();
            existingEntity.setId(originalId);
            existingEntity.setPatientId("original-patient-id");
            existingEntity.setDoctorId("original-doctor-id");
            existingEntity.setStatus(AppointmentStatus.SCHEDULED);
            existingEntity.setType(AppointmentType.CONSULTATION);
            existingEntity.setReason("Original reason");
            existingEntity.setNotes("Original notes");

            // When - Update with new request
            mapper.partialUpdate(testRequest, existingEntity);

            // Then - Fields should be updated according to mapper configuration
            assertThat(existingEntity.getId()).isEqualTo(originalId); // ID should not change
            assertThat(existingEntity.getPatientId()).isEqualTo("original-patient-id"); // Should not change (ignored)
            assertThat(existingEntity.getDoctorId()).isEqualTo("original-doctor-id"); // Should not change (ignored)
            assertThat(existingEntity.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED); // Should not change (ignored)
            // These fields CAN be updated by partialUpdate (not ignored)
            assertThat(existingEntity.getType()).isEqualTo(testRequest.getType()); // Should update
            assertThat(existingEntity.getReason()).isEqualTo(testRequest.getReason()); // Should update
            assertThat(existingEntity.getNotes()).isEqualTo(testRequest.getNotes()); // Should update
        }

        @Test
        @DisplayName("Should not modify patient/doctor IDs during partial update")
        void partialUpdate_shouldNotModifyPatientAndDoctorIds() {
            // Given
            Appointment existingEntity = new Appointment();
            String originalPatientId = TestDataFactory.uuid();
            String originalDoctorId = TestDataFactory.uuid();
            existingEntity.setPatientId(originalPatientId);
            existingEntity.setDoctorId(originalDoctorId);

            // When
            mapper.partialUpdate(testRequest, existingEntity);

            // Then
            assertThat(existingEntity.getPatientId()).isEqualTo(originalPatientId);
            assertThat(existingEntity.getDoctorId()).isEqualTo(originalDoctorId);
        }

        @Test
        @DisplayName("Should not modify status during partial update")
        void partialUpdate_shouldNotModifyStatus() {
            // Given
            Appointment existingEntity = new Appointment();
            existingEntity.setStatus(AppointmentStatus.IN_PROGRESS);

            // When
            mapper.partialUpdate(testRequest, existingEntity);

            // Then
            assertThat(existingEntity.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should handle null values in request using IGNORE strategy")
        void partialUpdate_withNullValues_shouldNotOverwrite() {
            // Given
            Appointment existingEntity = new Appointment();
            existingEntity.setReason("Original reason");
            existingEntity.setNotes("Original notes");

            AppointmentRequest requestWithNulls = new AppointmentRequest();
            requestWithNulls.setPatientId(TestDataFactory.uuid());
            requestWithNulls.setDoctorId(TestDataFactory.uuid());
            requestWithNulls.setAppointmentTime(Instant.now().toString());
            requestWithNulls.setType(AppointmentType.CONSULTATION);
            requestWithNulls.setReason(null); // null reason
            requestWithNulls.setNotes(null); // null notes

            // When
            mapper.partialUpdate(requestWithNulls, existingEntity);

            // Then - Original values should remain (IGNORE strategy)
            assertThat(existingEntity.getReason()).isEqualTo("Original reason");
            assertThat(existingEntity.getNotes()).isEqualTo("Original notes");
        }
    }

    @Nested
    @DisplayName("Cancel Response Mapping")
    class CancelResponseTests {

        @Test
        @DisplayName("Should map appointment to cancel response correctly")
        void toCancelResponse_shouldMapAllFields() {
            // Given
            testEntity.setStatus(AppointmentStatus.CANCELLED);
            testEntity.setCancelReason("Patient requested cancellation");
            testEntity.setCancelledAt(Instant.now());

            // When
            var response = mapper.toCancelResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(response.getCancelReason()).isEqualTo("Patient requested cancellation");
            assertThat(response.getCancelledAt()).isEqualTo(testEntity.getCancelledAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
            assertThat(response.getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
        }

        @Test
        @DisplayName("Should map patient and doctor in cancel response")
        void toCancelResponse_shouldMapPatientAndDoctor() {
            // Given
            testEntity.setStatus(AppointmentStatus.CANCELLED);
            testEntity.setCancelReason("Doctor unavailable");

            // When
            var response = mapper.toCancelResponse(testEntity);

            // Then
            assertThat(response.getPatient()).isNotNull();
            assertThat(response.getPatient().getId()).isEqualTo(testEntity.getPatientId());
            assertThat(response.getPatient().getFullName()).isEqualTo(testEntity.getPatientName());
            assertThat(response.getDoctor()).isNotNull();
            assertThat(response.getDoctor().getId()).isEqualTo(testEntity.getDoctorId());
        }

        @Test
        @DisplayName("Should handle null entity in cancel response")
        void toCancelResponse_withNullEntity_shouldReturnNull() {
            // When
            var response = mapper.toCancelResponse(null);

            // Then
            assertThat(response).isNull();
        }
    }
}
