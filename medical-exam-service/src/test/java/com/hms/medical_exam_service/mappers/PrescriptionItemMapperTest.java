package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemResponse;
import com.hms.medical_exam_service.entities.PrescriptionItem;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PrescriptionItemMapper.
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
@DisplayName("UC-EXAM-004: PrescriptionItemMapper Unit Tests")
class PrescriptionItemMapperTest {

    @Autowired
    private PrescriptionItemMapper mapper;

    private PrescriptionItem testEntity;
    private PrescriptionItemRequest testRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new PrescriptionItem();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setMedicineId("MED001");
        testEntity.setMedicineName("Paracetamol 500mg");
        testEntity.setUnitPrice(new BigDecimal("2.50"));
        testEntity.setQuantity(20);
        testEntity.setDosage("500mg");
        testEntity.setDurationDays(5);
        testEntity.setInstructions("Take with food");
        testEntity.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        testEntity.setUpdatedAt(Instant.now());

        // Setup test request
        testRequest = new PrescriptionItemRequest();
        testRequest.setMedicineId("MED002");
        testRequest.setQuantity(15);
        testRequest.setDosage("250mg");
        testRequest.setDurationDays(7);
        testRequest.setInstructions("Take before meals");
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("Should map request fields to entity (excluding snapshots)")
        void requestToEntity_shouldMapRequestFields() {
            // When
            PrescriptionItem result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMedicineId()).isEqualTo(testRequest.getMedicineId());
            assertThat(result.getQuantity()).isEqualTo(testRequest.getQuantity());
            assertThat(result.getDosage()).isEqualTo(testRequest.getDosage());
            assertThat(result.getDurationDays()).isEqualTo(testRequest.getDurationDays());
            assertThat(result.getInstructions()).isEqualTo(testRequest.getInstructions());
        }

        @Test
        @DisplayName("Should ignore medicineName (set by hook)")
        void requestToEntity_shouldIgnoreMedicineName() {
            // When
            PrescriptionItem result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result.getMedicineName()).isNull(); // Set by hook
        }

        @Test
        @DisplayName("Should ignore unitPrice (set by hook)")
        void requestToEntity_shouldIgnoreUnitPrice() {
            // When
            PrescriptionItem result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result.getUnitPrice()).isNull(); // Set by hook
        }

        @Test
        @DisplayName("Should ignore id (auto-generated)")
        void requestToEntity_shouldIgnoreId() {
            // When
            PrescriptionItem result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result.getId()).isNull(); // Auto-generated
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void requestToEntity_withNullOptionalFields_shouldMapSuccessfully() {
            // Given
            PrescriptionItemRequest minimalRequest = new PrescriptionItemRequest();
            minimalRequest.setMedicineId("MED003");
            minimalRequest.setQuantity(10);
            minimalRequest.setDosage("100mg");
            // durationDays and instructions are null

            // When
            PrescriptionItem result = mapper.requestToEntity(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMedicineId()).isEqualTo(minimalRequest.getMedicineId());
            assertThat(result.getQuantity()).isEqualTo(minimalRequest.getQuantity());
            assertThat(result.getDosage()).isEqualTo(minimalRequest.getDosage());
            assertThat(result.getDurationDays()).isNull();
            assertThat(result.getInstructions()).isNull();
        }

        @Test
        @DisplayName("Should handle empty instructions")
        void requestToEntity_withEmptyInstructions_shouldMapSuccessfully() {
            // Given
            testRequest.setInstructions("");

            // When
            PrescriptionItem result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result.getInstructions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("Should map all entity fields to response")
        void entityToResponse_withAllFields_shouldMapCorrectly() {
            // When
            PrescriptionItemResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
            assertThat(result.getQuantity()).isEqualTo(testEntity.getQuantity());
            assertThat(result.getUnitPrice()).isEqualTo(testEntity.getUnitPrice());
            assertThat(result.getDosage()).isEqualTo(testEntity.getDosage());
            assertThat(result.getDurationDays()).isEqualTo(testEntity.getDurationDays());
            assertThat(result.getInstructions()).isEqualTo(testEntity.getInstructions());
            assertThat(result.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should map medicine info to nested object")
        void entityToResponse_shouldMapMedicineInfo() {
            // When
            PrescriptionItemResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getMedicine()).isNotNull();
            assertThat(result.getMedicine().getId()).isEqualTo(testEntity.getMedicineId());
            assertThat(result.getMedicine().getName()).isEqualTo(testEntity.getMedicineName());
        }

        @Test
        @DisplayName("Should handle null entity")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            PrescriptionItemResponse result = mapper.entityToResponse(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle entity with null optional fields")
        void entityToResponse_withNullOptionalFields_shouldMapSuccessfully() {
            // Given
            testEntity.setDurationDays(null);
            testEntity.setInstructions(null);

            // When
            PrescriptionItemResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDurationDays()).isNull();
            assertThat(result.getInstructions()).isNull();
        }

        @Test
        @DisplayName("Should preserve price precision")
        void entityToResponse_shouldPreservePricePrecision() {
            // Given
            testEntity.setUnitPrice(new BigDecimal("12.99"));

            // When
            PrescriptionItemResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getUnitPrice()).isEqualByComparingTo(new BigDecimal("12.99"));
        }
    }

    @Nested
    @DisplayName("List Mapping")
    class ListMappingTests {

        @Test
        @DisplayName("Should map list of requests to list of entities")
        void requestsToEntities_shouldMapList() {
            // Given
            PrescriptionItemRequest request1 = new PrescriptionItemRequest();
            request1.setMedicineId("MED001");
            request1.setQuantity(10);
            request1.setDosage("100mg");

            PrescriptionItemRequest request2 = new PrescriptionItemRequest();
            request2.setMedicineId("MED002");
            request2.setQuantity(20);
            request2.setDosage("200mg");

            List<PrescriptionItemRequest> requests = Arrays.asList(request1, request2);

            // When
            List<PrescriptionItem> result = mapper.requestsToEntities(requests);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMedicineId()).isEqualTo("MED001");
            assertThat(result.get(0).getQuantity()).isEqualTo(10);
            assertThat(result.get(1).getMedicineId()).isEqualTo("MED002");
            assertThat(result.get(1).getQuantity()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should map list of entities to list of responses")
        void entitiesToResponses_shouldMapList() {
            // Given
            PrescriptionItem entity1 = new PrescriptionItem();
            entity1.setId(TestDataFactory.uuid());
            entity1.setMedicineId("MED001");
            entity1.setMedicineName("Medicine A");
            entity1.setUnitPrice(new BigDecimal("5.00"));
            entity1.setQuantity(10);
            entity1.setDosage("100mg");

            PrescriptionItem entity2 = new PrescriptionItem();
            entity2.setId(TestDataFactory.uuid());
            entity2.setMedicineId("MED002");
            entity2.setMedicineName("Medicine B");
            entity2.setUnitPrice(new BigDecimal("10.00"));
            entity2.setQuantity(20);
            entity2.setDosage("200mg");

            List<PrescriptionItem> entities = Arrays.asList(entity1, entity2);

            // When
            List<PrescriptionItemResponse> result = mapper.entitiesToResponses(entities);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(entity1.getId());
            assertThat(result.get(0).getMedicine().getName()).isEqualTo("Medicine A");
            assertThat(result.get(1).getId()).isEqualTo(entity2.getId());
            assertThat(result.get(1).getMedicine().getName()).isEqualTo("Medicine B");
        }

        @Test
        @DisplayName("Should handle empty request list")
        void requestsToEntities_withEmptyList_shouldReturnEmptyList() {
            // Given
            List<PrescriptionItemRequest> emptyList = Arrays.asList();

            // When
            List<PrescriptionItem> result = mapper.requestsToEntities(emptyList);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty entity list")
        void entitiesToResponses_withEmptyList_shouldReturnEmptyList() {
            // Given
            List<PrescriptionItem> emptyList = Arrays.asList();

            // When
            List<PrescriptionItemResponse> result = mapper.entitiesToResponses(emptyList);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Medicine Info Mapping")
    class MedicineInfoMappingTests {

        @Test
        @DisplayName("Should create MedicineInfo from entity fields")
        void mapMedicineInfo_shouldCreateNestedObject() {
            // When
            PrescriptionItemResponse.MedicineInfo result = mapper.mapMedicineInfo(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getMedicineId());
            assertThat(result.getName()).isEqualTo(testEntity.getMedicineName());
        }

        @Test
        @DisplayName("Should handle null entity in mapMedicineInfo")
        void mapMedicineInfo_withNullEntity_shouldReturnNull() {
            // When
            PrescriptionItemResponse.MedicineInfo result = mapper.mapMedicineInfo(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should preserve medicine snapshot data")
        void mapMedicineInfo_shouldPreserveSnapshots() {
            // Given
            testEntity.setMedicineId("MED999");
            testEntity.setMedicineName("Historical Medicine Name");

            // When
            PrescriptionItemResponse.MedicineInfo result = mapper.mapMedicineInfo(testEntity);

            // Then
            assertThat(result.getId()).isEqualTo("MED999");
            assertThat(result.getName()).isEqualTo("Historical Medicine Name");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long instructions")
        void mapping_withLongInstructions_shouldMapCorrectly() {
            // Given
            String longInstructions = "A".repeat(1000);
            testRequest.setInstructions(longInstructions);

            // When
            PrescriptionItem entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getInstructions()).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle large quantity values")
        void mapping_withLargeQuantity_shouldMapCorrectly() {
            // Given
            testRequest.setQuantity(9999);

            // When
            PrescriptionItem entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getQuantity()).isEqualTo(9999);
        }

        @Test
        @DisplayName("Should handle high precision prices")
        void mapping_withHighPrecisionPrice_shouldPreservePrecision() {
            // Given
            testEntity.setUnitPrice(new BigDecimal("123.45"));

            // When
            PrescriptionItemResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getUnitPrice()).isEqualByComparingTo(new BigDecimal("123.45"));
        }

        @Test
        @DisplayName("Should preserve dosage format")
        void mapping_shouldPreserveDosageFormat() {
            // Given
            String complexDosage = "500mg + 250mg combination";
            testRequest.setDosage(complexDosage);

            // When
            PrescriptionItem entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getDosage()).isEqualTo(complexDosage);
        }
    }
}
