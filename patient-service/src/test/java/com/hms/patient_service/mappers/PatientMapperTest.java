package com.hms.patient_service.mappers;

import com.hms.common.test.TestDataFactory;
import com.hms.patient_service.constants.Gender;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.entities.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PatientMapper.
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
@DisplayName("UC-PAT-001/002: PatientMapper Unit Tests")
class PatientMapperTest {

    @Autowired
    private PatientMapper mapper;

    private Patient testEntity;
    private PatientRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new Patient();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setAccountId(TestDataFactory.uuid());
        testEntity.setFullName(TestDataFactory.fullName());
        testEntity.setEmail(TestDataFactory.uniqueEmail());
        testEntity.setDateOfBirth(LocalDate.of(1990, 5, 15));
        testEntity.setGender(Gender.MALE);
        testEntity.setPhoneNumber("0912345678");
        testEntity.setAddress("123 Main St, Ho Chi Minh City");
        testEntity.setIdentificationNumber("079090001234");
        testEntity.setHealthInsuranceNumber("HS1234567890");
        testEntity.setRelativeFullName("John Relative");
        testEntity.setRelativePhoneNumber("0987654321");
        testEntity.setRelativeRelationship("Spouse");
        testEntity.setBloodType("O+");
        testEntity.setAllergies("Penicillin");
        testEntity.setProfileImageUrl("http://localhost:9000/patient-images/profiles/test.jpg");

        // Setup test request
        testRequest = new PatientRequest();
        testRequest.setAccountId(TestDataFactory.uuid());
        testRequest.setFullName(TestDataFactory.fullName());
        testRequest.setEmail(TestDataFactory.uniqueEmail());
        testRequest.setDateOfBirth(LocalDate.of(1985, 3, 20));
        testRequest.setGender(Gender.FEMALE);
        testRequest.setPhoneNumber("0923456789");
        testRequest.setAddress("456 Second St, Hanoi");
        testRequest.setIdentificationNumber("001085001234");
        testRequest.setHealthInsuranceNumber("HS9876543210");
        testRequest.setRelativeFullName("Jane Relative");
        testRequest.setRelativePhoneNumber("0976543210");
        testRequest.setRelativeRelationship("Sibling");
        testRequest.setBloodType("A+");
        testRequest.setAllergies("Peanuts");
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-PAT-001: Should map patient entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            PatientResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(testEntity.getId());
            assertThat(response.accountId()).isEqualTo(testEntity.getAccountId());
            assertThat(response.fullName()).isEqualTo(testEntity.getFullName());
            assertThat(response.email()).isEqualTo(testEntity.getEmail());
            assertThat(response.dateOfBirth()).isEqualTo(testEntity.getDateOfBirth());
            assertThat(response.gender()).isEqualTo(testEntity.getGender());
            assertThat(response.phoneNumber()).isEqualTo(testEntity.getPhoneNumber());
            assertThat(response.address()).isEqualTo(testEntity.getAddress());
            assertThat(response.identificationNumber()).isEqualTo(testEntity.getIdentificationNumber());
            assertThat(response.healthInsuranceNumber()).isEqualTo(testEntity.getHealthInsuranceNumber());
            assertThat(response.relativeFullName()).isEqualTo(testEntity.getRelativeFullName());
            assertThat(response.relativePhoneNumber()).isEqualTo(testEntity.getRelativePhoneNumber());
            assertThat(response.relativeRelationship()).isEqualTo(testEntity.getRelativeRelationship());
            assertThat(response.bloodType()).isEqualTo(testEntity.getBloodType());
            assertThat(response.allergies()).isEqualTo(testEntity.getAllergies());
            assertThat(response.profileImageUrl()).isEqualTo(testEntity.getProfileImageUrl());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            PatientResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should map gender correctly")
        void entityToResponse_shouldMapGender() {
            // Given
            testEntity.setGender(Gender.OTHER);

            // When
            PatientResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.gender()).isEqualTo(Gender.OTHER);
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testEntity.setProfileImageUrl(null);
            testEntity.setAllergies(null);
            testEntity.setBloodType(null);
            testEntity.setHealthInsuranceNumber(null);

            // When
            PatientResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.profileImageUrl()).isNull();
            assertThat(response.allergies()).isNull();
            assertThat(response.bloodType()).isNull();
            assertThat(response.healthInsuranceNumber()).isNull();
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-PAT-002: Should map patient request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            Patient entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getAccountId()).isEqualTo(testRequest.getAccountId());
            assertThat(entity.getFullName()).isEqualTo(testRequest.getFullName());
            assertThat(entity.getEmail()).isEqualTo(testRequest.getEmail());
            assertThat(entity.getDateOfBirth()).isEqualTo(testRequest.getDateOfBirth());
            assertThat(entity.getGender()).isEqualTo(testRequest.getGender());
            assertThat(entity.getPhoneNumber()).isEqualTo(testRequest.getPhoneNumber());
            assertThat(entity.getAddress()).isEqualTo(testRequest.getAddress());
            assertThat(entity.getIdentificationNumber()).isEqualTo(testRequest.getIdentificationNumber());
            assertThat(entity.getHealthInsuranceNumber()).isEqualTo(testRequest.getHealthInsuranceNumber());
            assertThat(entity.getRelativeFullName()).isEqualTo(testRequest.getRelativeFullName());
            assertThat(entity.getRelativePhoneNumber()).isEqualTo(testRequest.getRelativePhoneNumber());
            assertThat(entity.getRelativeRelationship()).isEqualTo(testRequest.getRelativeRelationship());
            assertThat(entity.getBloodType()).isEqualTo(testRequest.getBloodType());
            assertThat(entity.getAllergies()).isEqualTo(testRequest.getAllergies());
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            Patient entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should not map ID from request (ID should be generated)")
        void requestToEntity_shouldNotMapId() {
            // When
            Patient entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getId()).isNull(); // ID should be null, will be generated by JPA
        }

        @Test
        @DisplayName("Should handle null optional fields in request")
        void requestToEntity_withNullOptionalFields_shouldMapCorrectly() {
            // Given
            testRequest.setHealthInsuranceNumber(null);
            testRequest.setAllergies(null);
            testRequest.setBloodType(null);

            // When
            Patient entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getHealthInsuranceNumber()).isNull();
            assertThat(entity.getAllergies()).isNull();
            assertThat(entity.getBloodType()).isNull();
        }
    }

    @Nested
    @DisplayName("Update Entity from Request")
    class UpdateEntityTests {

        @Test
        @DisplayName("Should update entity from request correctly")
        void partialUpdate_shouldUpdateAllFields() {
            // Given
            Patient existingEntity = new Patient();
            existingEntity.setId("existing-id");
            existingEntity.setFullName("Old Name");
            existingEntity.setEmail("old@example.com");

            // When
            mapper.partialUpdate(testRequest, existingEntity);

            // Then
            assertThat(existingEntity.getId()).isEqualTo("existing-id"); // ID should not change
            assertThat(existingEntity.getFullName()).isEqualTo(testRequest.getFullName());
            assertThat(existingEntity.getEmail()).isEqualTo(testRequest.getEmail());
            assertThat(existingEntity.getDateOfBirth()).isEqualTo(testRequest.getDateOfBirth());
            assertThat(existingEntity.getGender()).isEqualTo(testRequest.getGender());
            assertThat(existingEntity.getPhoneNumber()).isEqualTo(testRequest.getPhoneNumber());
            assertThat(existingEntity.getAddress()).isEqualTo(testRequest.getAddress());
        }

        @Test
        @DisplayName("Should preserve null values in update (IGNORE strategy)")
        void partialUpdate_withNullValuePropertyMappingStrategy_shouldIgnoreNulls() {
            // Given
            Patient existingEntity = new Patient();
            existingEntity.setId("existing-id");
            existingEntity.setFullName("Existing Name");
            existingEntity.setEmail("existing@example.com");
            existingEntity.setBloodType("AB+");

            PatientRequest updateRequest = new PatientRequest();
            updateRequest.setFullName("Updated Name");
            updateRequest.setBloodType(null); // Null value should be ignored

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getFullName()).isEqualTo("Updated Name");
            assertThat(existingEntity.getBloodType()).isEqualTo("AB+"); // Should be preserved due to IGNORE strategy
        }
    }
}
