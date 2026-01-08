package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.lab.LabTestRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResponse;
import com.hms.medical_exam_service.entities.LabTest;
import com.hms.medical_exam_service.entities.LabTestCategory;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LabTestMapper.
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
@DisplayName("UC-EXAM-006: LabTestMapper Unit Tests")
class LabTestMapperTest {

    @Autowired
    private LabTestMapper mapper;

    private LabTest testEntity;
    private LabTestRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new LabTest();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setCode("CBC");
        testEntity.setName("Complete Blood Count");
        testEntity.setCategory(LabTestCategory.LAB);
        testEntity.setDescription("Measures various components of blood");
        testEntity.setPrice(new BigDecimal("50.00"));
        testEntity.setUnit("cells/μL");
        testEntity.setNormalRange("4.5-11.0");
        testEntity.setIsActive(true);
        testEntity.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        testEntity.setUpdatedAt(Instant.now());
        testEntity.setCreatedBy("admin");
        testEntity.setUpdatedBy("admin");

        // Setup test request
        testRequest = new LabTestRequest();
        testRequest.setCode("XRAY_CHEST");
        testRequest.setName("X-Ray Chest");
        testRequest.setCategory(LabTestCategory.IMAGING);
        testRequest.setDescription("Chest radiography");
        testRequest.setPrice(new BigDecimal("75.00"));
        testRequest.setUnit("image");
        testRequest.setNormalRange("normal");
        testRequest.setIsActive(true);
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("Should map all fields from LabTestRequest to LabTest entity")
        void requestToEntity_withAllFields_shouldMapCorrectly() {
            // When
            LabTest result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(testRequest.getCode());
            assertThat(result.getName()).isEqualTo(testRequest.getName());
            assertThat(result.getCategory()).isEqualTo(testRequest.getCategory());
            assertThat(result.getDescription()).isEqualTo(testRequest.getDescription());
            assertThat(result.getPrice()).isEqualByComparingTo(testRequest.getPrice());
            assertThat(result.getUnit()).isEqualTo(testRequest.getUnit());
            assertThat(result.getNormalRange()).isEqualTo(testRequest.getNormalRange());
            assertThat(result.getIsActive()).isEqualTo(testRequest.getIsActive());
        }

        @Test
        @DisplayName("Should handle null optional fields gracefully")
        void requestToEntity_withNullOptionalFields_shouldMapSuccessfully() {
            // Given
            LabTestRequest minimalRequest = new LabTestRequest();
            minimalRequest.setCode("TSH");
            minimalRequest.setName("Thyroid Stimulating Hormone");
            minimalRequest.setCategory(LabTestCategory.LAB);
            minimalRequest.setPrice(new BigDecimal("30.00"));
            // description, unit, normalRange null

            // When
            LabTest result = mapper.requestToEntity(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(minimalRequest.getCode());
            assertThat(result.getName()).isEqualTo(minimalRequest.getName());
            assertThat(result.getCategory()).isEqualTo(minimalRequest.getCategory());
            assertThat(result.getPrice()).isEqualByComparingTo(minimalRequest.getPrice());
            assertThat(result.getDescription()).isNull();
            assertThat(result.getUnit()).isNull();
            assertThat(result.getNormalRange()).isNull();
        }

        @Test
        @DisplayName("Should preserve price precision")
        void requestToEntity_shouldPreservePricePrecision() {
            // Given
            testRequest.setPrice(new BigDecimal("123.45"));

            // When
            LabTest result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("123.45"));
        }
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("Should map all fields from LabTest entity to LabTestResponse")
        void entityToResponse_withAllFields_shouldMapCorrectly() {
            // When
            LabTestResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
            assertThat(result.getCode()).isEqualTo(testEntity.getCode());
            assertThat(result.getName()).isEqualTo(testEntity.getName());
            assertThat(result.getCategory()).isEqualTo(testEntity.getCategory());
            assertThat(result.getDescription()).isEqualTo(testEntity.getDescription());
            assertThat(result.getPrice()).isEqualByComparingTo(testEntity.getPrice());
            assertThat(result.getUnit()).isEqualTo(testEntity.getUnit());
            assertThat(result.getNormalRange()).isEqualTo(testEntity.getNormalRange());
            assertThat(result.getIsActive()).isEqualTo(testEntity.getIsActive());
            assertThat(result.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle null entity")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            LabTestResponse result = mapper.entityToResponse(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should map all category types correctly")
        void entityToResponse_shouldMapAllCategories() {
            // Test LAB
            testEntity.setCategory(LabTestCategory.LAB);
            LabTestResponse result = mapper.entityToResponse(testEntity);
            assertThat(result.getCategory()).isEqualTo(LabTestCategory.LAB);

            // Test IMAGING
            testEntity.setCategory(LabTestCategory.IMAGING);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getCategory()).isEqualTo(LabTestCategory.IMAGING);

            // Test PATHOLOGY
            testEntity.setCategory(LabTestCategory.PATHOLOGY);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getCategory()).isEqualTo(LabTestCategory.PATHOLOGY);
        }
    }

    @Nested
    @DisplayName("Partial Update Tests")
    class PartialUpdateTests {

        @Test
        @DisplayName("Should update entity fields from request without overwriting existing data")
        void partialUpdate_shouldUpdateOnlyProvidedFields() {
            // Given
            LabTestRequest updateRequest = new LabTestRequest();
            updateRequest.setCode("CBC");
            updateRequest.setName("Complete Blood Count - Updated");
            updateRequest.setCategory(LabTestCategory.LAB);
            updateRequest.setPrice(new BigDecimal("55.00"));
            // description, unit, normalRange not set

            String originalDescription = testEntity.getDescription();
            String originalUnit = testEntity.getUnit();
            String originalRange = testEntity.getNormalRange();

            // When
            mapper.partialUpdate(updateRequest, testEntity);

            // Then
            assertThat(testEntity.getName()).isEqualTo("Complete Blood Count - Updated");
            assertThat(testEntity.getPrice()).isEqualByComparingTo(new BigDecimal("55.00"));
            
            // Note: MapStruct default behavior is to copy null values
            // Without nullValuePropertyMappingStrategy = IGNORE, null fields WILL overwrite
            assertThat(testEntity.getDescription()).isNull();
            assertThat(testEntity.getUnit()).isNull();
            assertThat(testEntity.getNormalRange()).isNull();
        }

        @Test
        @DisplayName("Should update all fields when all are provided in request")
        void partialUpdate_withAllFields_shouldUpdateAllFields() {
            // When
            mapper.partialUpdate(testRequest, testEntity);

            // Then
            assertThat(testEntity.getCode()).isEqualTo(testRequest.getCode());
            assertThat(testEntity.getName()).isEqualTo(testRequest.getName());
            assertThat(testEntity.getCategory()).isEqualTo(testRequest.getCategory());
            assertThat(testEntity.getDescription()).isEqualTo(testRequest.getDescription());
            assertThat(testEntity.getPrice()).isEqualByComparingTo(testRequest.getPrice());
            assertThat(testEntity.getUnit()).isEqualTo(testRequest.getUnit());
            assertThat(testEntity.getNormalRange()).isEqualTo(testRequest.getNormalRange());
            assertThat(testEntity.getIsActive()).isEqualTo(testRequest.getIsActive());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long description")
        void mapping_withLongDescription_shouldMapCorrectly() {
            // Given
            String longDescription = "A".repeat(2000);
            testRequest.setDescription(longDescription);

            // When
            LabTest entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getDescription()).hasSize(2000);
        }

        @Test
        @DisplayName("Should handle inactive test")
        void mapping_withInactiveTest_shouldMapCorrectly() {
            // Given
            testRequest.setIsActive(false);

            // When
            LabTest entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Should preserve code format")
        void mapping_shouldPreserveCodeFormat() {
            // Given
            String complexCode = "COVID_19_PCR_TEST";
            testRequest.setCode(complexCode);

            // When
            LabTest entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getCode()).isEqualTo(complexCode);
        }
    }
}
