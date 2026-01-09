package com.hms.hr_service.mappers;

import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.dtos.department.DepartmentRequest;
import com.hms.hr_service.dtos.department.DepartmentResponse;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.enums.DepartmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DepartmentMapper.
 * Tests MapStruct mapper methods for correct field mapping.
 */
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@DisplayName("UC-HR-002: DepartmentMapper Unit Tests")
class DepartmentMapperTest {

    @Autowired
    private DepartmentMapper mapper;

    private Department testEntity;
    private DepartmentRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new Department();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setName("Cardiology");
        testEntity.setDescription("Heart and cardiovascular care");
        testEntity.setHeadDoctorId(TestDataFactory.uuid());
        testEntity.setLocation("Building A, Floor 3");
        testEntity.setPhoneExtension("1234");
        testEntity.setStatus(DepartmentStatus.ACTIVE);
        testEntity.setCreatedAt(Instant.now());
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setCreatedBy("admin");
        testEntity.setUpdatedBy("admin");

        // Setup test request
        testRequest = new DepartmentRequest();
        testRequest.setName("Emergency");
        testRequest.setDescription("24/7 emergency care");
        testRequest.setHeadDoctorId(TestDataFactory.uuid());
        testRequest.setLocation("Building B, Ground Floor");
        testRequest.setPhoneExtension("9999");
        testRequest.setStatus(DepartmentStatus.ACTIVE);
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-HR-002: Should map entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            DepartmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getName()).isEqualTo(testEntity.getName());
            assertThat(response.getDescription()).isEqualTo(testEntity.getDescription());
            assertThat(response.getHeadDoctorId()).isEqualTo(testEntity.getHeadDoctorId());
            assertThat(response.getLocation()).isEqualTo(testEntity.getLocation());
            assertThat(response.getPhoneExtension()).isEqualTo(testEntity.getPhoneExtension());
            assertThat(response.getStatus()).isEqualTo(testEntity.getStatus());
            assertThat(response.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
            assertThat(response.getCreatedBy()).isEqualTo(testEntity.getCreatedBy());
            assertThat(response.getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            DepartmentResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should handle entity with null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testEntity.setDescription(null);
            testEntity.setHeadDoctorId(null);

            // When
            DepartmentResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getName()).isEqualTo(testEntity.getName());
            assertThat(response.getDescription()).isNull();
            assertThat(response.getHeadDoctorId()).isNull();
            assertThat(response.getLocation()).isEqualTo(testEntity.getLocation());
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-HR-002: Should map request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            Department entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo(testRequest.getName());
            assertThat(entity.getDescription()).isEqualTo(testRequest.getDescription());
            assertThat(entity.getHeadDoctorId()).isEqualTo(testRequest.getHeadDoctorId());
            assertThat(entity.getLocation()).isEqualTo(testRequest.getLocation());
            assertThat(entity.getPhoneExtension()).isEqualTo(testRequest.getPhoneExtension());
            assertThat(entity.getStatus()).isEqualTo(testRequest.getStatus());
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            Department entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should not map audit fields from request")
        void requestToEntity_shouldNotMapAuditFields() {
            // When
            Department entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getId()).isNull();
            assertThat(entity.getCreatedAt()).isNull();
            assertThat(entity.getUpdatedAt()).isNull();
            assertThat(entity.getCreatedBy()).isNull();
            assertThat(entity.getUpdatedBy()).isNull();
        }

        @Test
        @DisplayName("Should handle request with null optional fields")
        void requestToEntity_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testRequest.setDescription(null);
            testRequest.setHeadDoctorId(null);

            // When
            Department entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo(testRequest.getName());
            assertThat(entity.getDescription()).isNull();
            assertThat(entity.getHeadDoctorId()).isNull();
            assertThat(entity.getLocation()).isEqualTo(testRequest.getLocation());
        }
    }

    @Nested
    @DisplayName("Partial Update Mapping")
    class PartialUpdateTests {

        @Test
        @DisplayName("UC-HR-002: Should update only non-null fields")
        void partialUpdate_shouldUpdateNonNullFieldsOnly() {
            // Given
            Department existingEntity = new Department();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setName("Original Department");
            existingEntity.setDescription("Original description");
            existingEntity.setLocation("Original location");
            existingEntity.setPhoneExtension("0000");
            existingEntity.setStatus(DepartmentStatus.ACTIVE);

            DepartmentRequest updateRequest = new DepartmentRequest();
            updateRequest.setName("Updated Department");
            updateRequest.setDescription(null); // MapStruct default behavior will overwrite with null
            updateRequest.setLocation("Updated location");
            updateRequest.setPhoneExtension("1111");
            updateRequest.setStatus(DepartmentStatus.INACTIVE);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getName()).isEqualTo("Updated Department");
            assertThat(existingEntity.getDescription()).isNull(); // Overwritten
            assertThat(existingEntity.getLocation()).isEqualTo("Updated location");
            assertThat(existingEntity.getPhoneExtension()).isEqualTo("1111");
            assertThat(existingEntity.getStatus()).isEqualTo(DepartmentStatus.INACTIVE);
        }

        @Test
        @DisplayName("Should preserve entity ID during partial update")
        void partialUpdate_shouldPreserveId() {
            // Given
            String originalId = TestDataFactory.uuid();
            Department existingEntity = new Department();
            existingEntity.setId(originalId);
            existingEntity.setName("Original Department");
            existingEntity.setLocation("Original location");
            existingEntity.setPhoneExtension("0000");
            existingEntity.setStatus(DepartmentStatus.ACTIVE);

            DepartmentRequest updateRequest = new DepartmentRequest();
            updateRequest.setName("Updated Department");
            updateRequest.setLocation("Updated location");
            updateRequest.setPhoneExtension("1111");
            updateRequest.setStatus(DepartmentStatus.ACTIVE);

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

            Department existingEntity = new Department();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setName("Original Department");
            existingEntity.setLocation("Original location");
            existingEntity.setPhoneExtension("0000");
            existingEntity.setStatus(DepartmentStatus.ACTIVE);
            existingEntity.setCreatedAt(originalCreatedAt);
            existingEntity.setCreatedBy(originalCreatedBy);

            DepartmentRequest updateRequest = new DepartmentRequest();
            updateRequest.setName("Updated Department");
            updateRequest.setLocation("Updated location");
            updateRequest.setPhoneExtension("1111");
            updateRequest.setStatus(DepartmentStatus.ACTIVE);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(existingEntity.getCreatedBy()).isEqualTo(originalCreatedBy);
        }
    }
}
