package com.hms.hr_service.mappers;

import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.dtos.schedule.ScheduleRequest;
import com.hms.hr_service.dtos.schedule.ScheduleResponse;
import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.ScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ScheduleMapper.
 * Tests MapStruct mapper methods for correct field mapping with time handling.
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("UC-HR-003: ScheduleMapper Unit Tests")
class ScheduleMapperTest {

    @Autowired
    private ScheduleMapper mapper;

    private EmployeeSchedule testEntity;
    private ScheduleRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new EmployeeSchedule();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setEmployeeId(TestDataFactory.uuid());
        testEntity.setWorkDate(LocalDate.of(2025, 12, 15));
        testEntity.setStartTime(LocalTime.of(9, 0));
        testEntity.setEndTime(LocalTime.of(17, 0));
        testEntity.setStatus(ScheduleStatus.AVAILABLE);
        testEntity.setNotes("Regular shift");
        testEntity.setCreatedAt(Instant.now());
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setCreatedBy("admin");
        testEntity.setUpdatedBy("admin");

        // Setup test request
        testRequest = new ScheduleRequest();
        testRequest.setEmployeeId(TestDataFactory.uuid());
        testRequest.setWorkDate(LocalDate.of(2025, 12, 20));
        testRequest.setStartTime(LocalTime.of(8, 0));
        testRequest.setEndTime(LocalTime.of(16, 0));
        testRequest.setStatus(ScheduleStatus.AVAILABLE);
        testRequest.setNotes("Morning shift");
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-HR-003: Should map entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            ScheduleResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getEmployeeId()).isEqualTo(testEntity.getEmployeeId());
            assertThat(response.getWorkDate()).isEqualTo(testEntity.getWorkDate());
            assertThat(response.getStartTime()).isEqualTo(testEntity.getStartTime());
            assertThat(response.getEndTime()).isEqualTo(testEntity.getEndTime());
            assertThat(response.getStatus()).isEqualTo(testEntity.getStatus());
            assertThat(response.getNotes()).isEqualTo(testEntity.getNotes());
            assertThat(response.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
            assertThat(response.getCreatedBy()).isEqualTo(testEntity.getCreatedBy());
            assertThat(response.getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            ScheduleResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should handle entity with null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testEntity.setNotes(null);

            // When
            ScheduleResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getEmployeeId()).isEqualTo(testEntity.getEmployeeId());
            assertThat(response.getNotes()).isNull();
        }

        @Test
        @DisplayName("Should map employee field as null (enriched in hook)")
        void entityToResponse_shouldMapEmployeeFieldAsNull() {
            // When
            ScheduleResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmployee()).isNull(); // Enriched in ScheduleHook
        }

        @Test
        @DisplayName("Should correctly map time fields")
        void entityToResponse_shouldMapTimeFieldsCorrectly() {
            // Given
            testEntity.setStartTime(LocalTime.of(7, 30));
            testEntity.setEndTime(LocalTime.of(15, 45));

            // When
            ScheduleResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getStartTime()).isEqualTo(LocalTime.of(7, 30));
            assertThat(response.getEndTime()).isEqualTo(LocalTime.of(15, 45));
        }

        @Test
        @DisplayName("Should correctly map midnight time boundaries")
        void entityToResponse_shouldMapMidnightTimeCorrectly() {
            // Given - Night shift
            testEntity.setStartTime(LocalTime.of(22, 0));
            testEntity.setEndTime(LocalTime.of(6, 0)); // Next day

            // When
            ScheduleResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getStartTime()).isEqualTo(LocalTime.of(22, 0));
            assertThat(response.getEndTime()).isEqualTo(LocalTime.of(6, 0));
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-HR-003: Should map request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            EmployeeSchedule entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getEmployeeId()).isEqualTo(testRequest.getEmployeeId());
            assertThat(entity.getWorkDate()).isEqualTo(testRequest.getWorkDate());
            assertThat(entity.getStartTime()).isEqualTo(testRequest.getStartTime());
            assertThat(entity.getEndTime()).isEqualTo(testRequest.getEndTime());
            assertThat(entity.getStatus()).isEqualTo(testRequest.getStatus());
            assertThat(entity.getNotes()).isEqualTo(testRequest.getNotes());
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            EmployeeSchedule entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should not map audit fields from request")
        void requestToEntity_shouldNotMapAuditFields() {
            // When
            EmployeeSchedule entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getCreatedBy()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("Should correctly map LocalDate field")
        void requestToEntity_shouldMapLocalDateCorrectly() {
            // Given
            testRequest.setWorkDate(LocalDate.of(2026, 1, 15));

            // When
            EmployeeSchedule entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getWorkDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        }

        @Test
        @DisplayName("Should correctly map LocalTime fields")
        void requestToEntity_shouldMapLocalTimeFieldsCorrectly() {
            // Given
            testRequest.setStartTime(LocalTime.of(13, 30));
            testRequest.setEndTime(LocalTime.of(21, 45));

            // When
            EmployeeSchedule entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getStartTime()).isEqualTo(LocalTime.of(13, 30));
            assertThat(entity.getEndTime()).isEqualTo(LocalTime.of(21, 45));
        }

        @Test
        @DisplayName("Should handle request with null status (default handled by entity)")
        void requestToEntity_withNullStatus_shouldMapCorrectly() {
            // Given
            testRequest.setStatus(null);

            // When
            EmployeeSchedule entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getEmployeeId()).isEqualTo(testRequest.getEmployeeId());
            // Status defaults to AVAILABLE in entity
        }
    }

    @Nested
    @DisplayName("Partial Update Mapping")
    class PartialUpdateTests {

        @Test
        @DisplayName("UC-HR-003: Should update only non-null fields")
        void partialUpdate_shouldUpdateNonNullFieldsOnly() {
            // Given
            EmployeeSchedule existingEntity = new EmployeeSchedule();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setEmployeeId(TestDataFactory.uuid());
            existingEntity.setWorkDate(LocalDate.of(2025, 12, 15));
            existingEntity.setStartTime(LocalTime.of(9, 0));
            existingEntity.setEndTime(LocalTime.of(17, 0));
            existingEntity.setStatus(ScheduleStatus.AVAILABLE);
            existingEntity.setNotes("Original notes");

            ScheduleRequest updateRequest = new ScheduleRequest();
            updateRequest.setEmployeeId(existingEntity.getEmployeeId());
            updateRequest.setWorkDate(existingEntity.getWorkDate());
            updateRequest.setStartTime(LocalTime.of(10, 0));
            updateRequest.setEndTime(LocalTime.of(18, 0));
            updateRequest.setStatus(ScheduleStatus.CANCELLED);
            // notes is null - should not update

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getStartTime()).isEqualTo(LocalTime.of(10, 0));
            assertThat(existingEntity.getEndTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(existingEntity.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
            assertThat(existingEntity.getNotes()).isEqualTo("Original notes"); // Preserved
        }

        @Test
        @DisplayName("Should preserve entity ID during partial update")
        void partialUpdate_shouldPreserveId() {
            // Given
            String originalId = TestDataFactory.uuid();
            EmployeeSchedule existingEntity = new EmployeeSchedule();
            existingEntity.setId(originalId);
            existingEntity.setEmployeeId(TestDataFactory.uuid());
            existingEntity.setWorkDate(LocalDate.of(2025, 12, 15));
            existingEntity.setStartTime(LocalTime.of(9, 0));
            existingEntity.setEndTime(LocalTime.of(17, 0));
            existingEntity.setStatus(ScheduleStatus.AVAILABLE);

            ScheduleRequest updateRequest = new ScheduleRequest();
            updateRequest.setEmployeeId(existingEntity.getEmployeeId());
            updateRequest.setWorkDate(existingEntity.getWorkDate());
            updateRequest.setStartTime(LocalTime.of(10, 0));
            updateRequest.setEndTime(LocalTime.of(18, 0));
            updateRequest.setStatus(ScheduleStatus.AVAILABLE);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getId()).isEqualTo(originalId);
        }

        @Test
        @DisplayName("Should preserve audit fields during partial update")
        void partialUpdate_shouldPreserveAuditFields() {
            // Given
            Instant originalCreatedAt = Instant.now().minusSeconds(86400);
            String originalCreatedBy = "admin";

            EmployeeSchedule existingEntity = new EmployeeSchedule();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setEmployeeId(TestDataFactory.uuid());
            existingEntity.setWorkDate(LocalDate.of(2025, 12, 15));
            existingEntity.setStartTime(LocalTime.of(9, 0));
            existingEntity.setEndTime(LocalTime.of(17, 0));
            existingEntity.setStatus(ScheduleStatus.AVAILABLE);
            existingEntity.setCreatedAt(originalCreatedAt);
            existingEntity.setCreatedBy(originalCreatedBy);

            ScheduleRequest updateRequest = new ScheduleRequest();
            updateRequest.setEmployeeId(existingEntity.getEmployeeId());
            updateRequest.setWorkDate(existingEntity.getWorkDate());
            updateRequest.setStartTime(LocalTime.of(10, 0));
            updateRequest.setEndTime(LocalTime.of(18, 0));
            updateRequest.setStatus(ScheduleStatus.AVAILABLE);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(existingEntity.getCreatedBy()).isEqualTo(originalCreatedBy);
        }
    }
}
