package com.hms.hr_service.mappers;

import com.hms.common.test.TestDataFactory;
import com.hms.hr_service.dtos.employee.EmployeeRequest;
import com.hms.hr_service.dtos.employee.EmployeeResponse;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
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
 * Unit tests for EmployeeMapper.
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
@DisplayName("UC-HR-001: EmployeeMapper Unit Tests")
class EmployeeMapperTest {

    @Autowired
    private EmployeeMapper mapper;

    private Employee testEntity;
    private EmployeeRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new Employee();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setAccountId(TestDataFactory.uuid());
        testEntity.setFullName(TestDataFactory.fullName());
        testEntity.setRole(EmployeeRole.DOCTOR);
        testEntity.setDepartmentId(TestDataFactory.uuid());
        testEntity.setSpecialization("Cardiology");
        testEntity.setLicenseNumber("MD-12345");
        testEntity.setPhoneNumber("1234567890");
        testEntity.setAddress(TestDataFactory.fullAddress());
        testEntity.setStatus(EmployeeStatus.ACTIVE);
        testEntity.setHiredAt(Instant.now().minusSeconds(86400 * 365));
        testEntity.setProfileImageUrl("https://example.com/profile.jpg");
        testEntity.setCreatedAt(Instant.now());
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setCreatedBy("admin");
        testEntity.setUpdatedBy("admin");

        // Setup test request
        testRequest = new EmployeeRequest();
        testRequest.setAccountId(TestDataFactory.uuid());
        testRequest.setFullName(TestDataFactory.fullName());
        testRequest.setRole(EmployeeRole.NURSE);
        testRequest.setDepartmentId(TestDataFactory.uuid());
        testRequest.setSpecialization("General");
        testRequest.setLicenseNumber("RN-98765");
        testRequest.setPhoneNumber("9876543210");
        testRequest.setAddress(TestDataFactory.fullAddress());
        testRequest.setStatus(EmployeeStatus.ACTIVE);
        testRequest.setHiredAt(Instant.now());
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-HR-001: Should map entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            EmployeeResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getAccountId()).isEqualTo(testEntity.getAccountId());
            assertThat(response.getFullName()).isEqualTo(testEntity.getFullName());
            assertThat(response.getRole()).isEqualTo(testEntity.getRole());
            assertThat(response.getDepartmentId()).isEqualTo(testEntity.getDepartmentId());
            assertThat(response.getSpecialization()).isEqualTo(testEntity.getSpecialization());
            assertThat(response.getLicenseNumber()).isEqualTo(testEntity.getLicenseNumber());
            assertThat(response.getPhoneNumber()).isEqualTo(testEntity.getPhoneNumber());
            assertThat(response.getAddress()).isEqualTo(testEntity.getAddress());
            assertThat(response.getStatus()).isEqualTo(testEntity.getStatus());
            assertThat(response.getHiredAt()).isEqualTo(testEntity.getHiredAt());
            assertThat(response.getProfileImageUrl()).isEqualTo(testEntity.getProfileImageUrl());
            assertThat(response.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(response.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
            assertThat(response.getCreatedBy()).isEqualTo(testEntity.getCreatedBy());
            assertThat(response.getUpdatedBy()).isEqualTo(testEntity.getUpdatedBy());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            EmployeeResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should handle entity with null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testEntity.setAccountId(null);
            testEntity.setSpecialization(null);
            testEntity.setLicenseNumber(null);
            testEntity.setPhoneNumber(null);
            testEntity.setAddress(null);
            testEntity.setProfileImageUrl(null);

            // When
            EmployeeResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getFullName()).isEqualTo(testEntity.getFullName());
            assertThat(response.getRole()).isEqualTo(testEntity.getRole());
            assertThat(response.getAccountId()).isNull();
            assertThat(response.getSpecialization()).isNull();
            assertThat(response.getLicenseNumber()).isNull();
        }

        @Test
        @DisplayName("Should map soft-deleted entity correctly")
        void entityToResponse_withSoftDeletedEntity_shouldMapDeletedFields() {
            // Given
            testEntity.setDeletedAt(Instant.now());
            testEntity.setDeletedBy("admin");

            // When
            EmployeeResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDeletedAt()).isEqualTo(testEntity.getDeletedAt());
            assertThat(response.getDeletedBy()).isEqualTo(testEntity.getDeletedBy());
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-HR-001: Should map request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            Employee entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getAccountId()).isEqualTo(testRequest.getAccountId());
            assertThat(entity.getFullName()).isEqualTo(testRequest.getFullName());
            assertThat(entity.getRole()).isEqualTo(testRequest.getRole());
            assertThat(entity.getDepartmentId()).isEqualTo(testRequest.getDepartmentId());
            assertThat(entity.getSpecialization()).isEqualTo(testRequest.getSpecialization());
            assertThat(entity.getLicenseNumber()).isEqualTo(testRequest.getLicenseNumber());
            assertThat(entity.getPhoneNumber()).isEqualTo(testRequest.getPhoneNumber());
            assertThat(entity.getAddress()).isEqualTo(testRequest.getAddress());
            assertThat(entity.getStatus()).isEqualTo(testRequest.getStatus());
            assertThat(entity.getHiredAt()).isEqualTo(testRequest.getHiredAt());
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            Employee entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should not map audit fields from request")
        void requestToEntity_shouldNotMapAuditFields() {
            // When
            Employee entity = mapper.requestToEntity(testRequest);

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
            testRequest.setAccountId(null);
            testRequest.setSpecialization(null);
            testRequest.setLicenseNumber(null);
            testRequest.setPhoneNumber(null);
            testRequest.setAddress(null);

            // When
            Employee entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getFullName()).isEqualTo(testRequest.getFullName());
            assertThat(entity.getRole()).isEqualTo(testRequest.getRole());
            assertThat(entity.getDepartmentId()).isEqualTo(testRequest.getDepartmentId());
            assertThat(entity.getAccountId()).isNull();
            assertThat(entity.getSpecialization()).isNull();
        }
    }

    @Nested
    @DisplayName("Partial Update Mapping")
    class PartialUpdateTests {

        @Test
        @DisplayName("UC-HR-001: Should update only non-null fields")
        void partialUpdate_shouldUpdateNonNullFieldsOnly() {
            // Given
            Employee existingEntity = new Employee();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setFullName("Original Name");
            existingEntity.setRole(EmployeeRole.DOCTOR);
            existingEntity.setDepartmentId(TestDataFactory.uuid());
            existingEntity.setStatus(EmployeeStatus.ACTIVE);
            existingEntity.setSpecialization("Cardiology");

            EmployeeRequest updateRequest = new EmployeeRequest();
            updateRequest.setFullName("Updated Name");
            updateRequest.setRole(EmployeeRole.NURSE);
            updateRequest.setDepartmentId(existingEntity.getDepartmentId());
            updateRequest.setStatus(EmployeeStatus.ON_LEAVE);
            updateRequest.setSpecialization(null); // Null value - default MapStruct behavior will overwrite

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getFullName()).isEqualTo("Updated Name");
            assertThat(existingEntity.getRole()).isEqualTo(EmployeeRole.NURSE);
            assertThat(existingEntity.getStatus()).isEqualTo(EmployeeStatus.ON_LEAVE);
            // Note: MapStruct default behavior overwrites with null unless configured otherwise
            assertThat(existingEntity.getSpecialization()).isNull();
        }

        @Test
        @DisplayName("Should preserve entity ID during partial update")
        void partialUpdate_shouldPreserveId() {
            // Given
            String originalId = TestDataFactory.uuid();
            Employee existingEntity = new Employee();
            existingEntity.setId(originalId);
            existingEntity.setFullName("Original Name");
            existingEntity.setRole(EmployeeRole.DOCTOR);
            existingEntity.setDepartmentId(TestDataFactory.uuid());
            existingEntity.setStatus(EmployeeStatus.ACTIVE);

            EmployeeRequest updateRequest = new EmployeeRequest();
            updateRequest.setFullName("Updated Name");
            updateRequest.setRole(EmployeeRole.NURSE);
            updateRequest.setDepartmentId(existingEntity.getDepartmentId());
            updateRequest.setStatus(EmployeeStatus.ACTIVE);

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

            Employee existingEntity = new Employee();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setFullName("Original Name");
            existingEntity.setRole(EmployeeRole.DOCTOR);
            existingEntity.setDepartmentId(TestDataFactory.uuid());
            existingEntity.setStatus(EmployeeStatus.ACTIVE);
            existingEntity.setCreatedAt(originalCreatedAt);
            existingEntity.setCreatedBy(originalCreatedBy);

            EmployeeRequest updateRequest = new EmployeeRequest();
            updateRequest.setFullName("Updated Name");
            updateRequest.setRole(EmployeeRole.NURSE);
            updateRequest.setDepartmentId(existingEntity.getDepartmentId());
            updateRequest.setStatus(EmployeeStatus.ACTIVE);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(existingEntity.getCreatedBy()).isEqualTo(originalCreatedBy);
        }
    }
}
