package com.hms.medicine_service.mappers;

import com.hms.medicine_service.dtos.medicine.MedicineRequest;
import com.hms.medicine_service.dtos.medicine.MedicineResponse;
import com.hms.medicine_service.entities.Category;
import com.hms.medicine_service.entities.Medicine;
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
 * Unit tests for MedicineMapper.
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
@DisplayName("UC-MED-001/002: MedicineMapper Unit Tests")
class MedicineMapperTest {

    @Autowired
    private MedicineMapper mapper;

    private Medicine testEntity;
    private MedicineRequest testRequest;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Setup test category
        testCategory = new Category();
        testCategory.setId(TestDataFactory.uuid());
        testCategory.setName("Antibiotics");
        testCategory.setDescription("Antibiotic medications");

        // Setup test entity
        testEntity = new Medicine();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setName("Amoxicillin");
        testEntity.setActiveIngredient("Amoxicillin Trihydrate");
        testEntity.setUnit("capsule");
        testEntity.setDescription("Broad-spectrum antibiotic");
        testEntity.setQuantity(100L);
        testEntity.setConcentration("500mg");
        testEntity.setPackaging("Blister pack");
        testEntity.setPurchasePrice(new BigDecimal("5.50"));
        testEntity.setSellingPrice(new BigDecimal("8.75"));
        testEntity.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        testEntity.setManufacturer("PharmaCorp");
        testEntity.setSideEffects("Nausea, diarrhea");
        testEntity.setStorageConditions("Store at room temperature");
        testEntity.setCategory(testCategory);

        // Setup test request
        testRequest = new MedicineRequest();
        testRequest.setName("Ibuprofen");
        testRequest.setActiveIngredient("Ibuprofen");
        testRequest.setUnit("tablet");
        testRequest.setDescription("Pain reliever");
        testRequest.setQuantity(200L);
        testRequest.setConcentration("200mg");
        testRequest.setPackaging("Bottle");
        testRequest.setPurchasePrice(new BigDecimal("3.00"));
        testRequest.setSellingPrice(new BigDecimal("5.00"));
        testRequest.setExpiresAt(Instant.now().plus(730, ChronoUnit.DAYS));
        testRequest.setManufacturer("MediPharm");
        testRequest.setSideEffects("Stomach upset");
        testRequest.setStorageConditions("Keep dry");
        testRequest.setCategoryId(TestDataFactory.uuid());
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-MED-001: Should map entity to response correctly")
        void entityToResponse_shouldMapAllFields() {
            // When
            MedicineResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testEntity.getId());
            assertThat(response.getName()).isEqualTo(testEntity.getName());
            assertThat(response.getActiveIngredient()).isEqualTo(testEntity.getActiveIngredient());
            assertThat(response.getUnit()).isEqualTo(testEntity.getUnit());
            assertThat(response.getDescription()).isEqualTo(testEntity.getDescription());
            assertThat(response.getQuantity()).isEqualTo(testEntity.getQuantity());
            assertThat(response.getConcentration()).isEqualTo(testEntity.getConcentration());
            assertThat(response.getPackaging()).isEqualTo(testEntity.getPackaging());
            assertThat(response.getPurchasePrice()).isEqualByComparingTo(testEntity.getPurchasePrice());
            assertThat(response.getSellingPrice()).isEqualByComparingTo(testEntity.getSellingPrice());
            assertThat(response.getExpiresAt()).isEqualTo(testEntity.getExpiresAt());
            assertThat(response.getManufacturer()).isEqualTo(testEntity.getManufacturer());
            assertThat(response.getSideEffects()).isEqualTo(testEntity.getSideEffects());
            assertThat(response.getStorageConditions()).isEqualTo(testEntity.getStorageConditions());
        }

        @Test
        @DisplayName("Should handle null entity gracefully")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            MedicineResponse response = mapper.entityToResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Should map category fields correctly via AfterMapping")
        void entityToResponse_shouldMapCategory() {
            // When
            MedicineResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getCategoryId()).isEqualTo(testCategory.getId());
            assertThat(response.getCategoryName()).isEqualTo(testCategory.getName());
        }

        @Test
        @DisplayName("Should handle entity without category gracefully")
        void entityToResponse_withNullCategory_shouldNotMapCategory() {
            // Given
            testEntity.setCategory(null);

            // When
            MedicineResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getCategoryId()).isNull();
            assertThat(response.getCategoryName()).isNull();
        }
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-MED-002: Should map request to entity correctly")
        void requestToEntity_shouldMapAllFields() {
            // When
            Medicine entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo(testRequest.getName());
            assertThat(entity.getActiveIngredient()).isEqualTo(testRequest.getActiveIngredient());
            assertThat(entity.getUnit()).isEqualTo(testRequest.getUnit());
            assertThat(entity.getDescription()).isEqualTo(testRequest.getDescription());
            assertThat(entity.getQuantity()).isEqualTo(testRequest.getQuantity());
            assertThat(entity.getConcentration()).isEqualTo(testRequest.getConcentration());
            assertThat(entity.getPackaging()).isEqualTo(testRequest.getPackaging());
            assertThat(entity.getPurchasePrice()).isEqualByComparingTo(testRequest.getPurchasePrice());
            assertThat(entity.getSellingPrice()).isEqualByComparingTo(testRequest.getSellingPrice());
            assertThat(entity.getExpiresAt()).isEqualTo(testRequest.getExpiresAt());
            assertThat(entity.getManufacturer()).isEqualTo(testRequest.getManufacturer());
            assertThat(entity.getSideEffects()).isEqualTo(testRequest.getSideEffects());
            assertThat(entity.getStorageConditions()).isEqualTo(testRequest.getStorageConditions());
            // ID should be null (generated by DB)
            assertThat(entity.getId()).isNull();
            // Category should be null (set via hook)
            assertThat(entity.getCategory()).isNull();
        }

        @Test
        @DisplayName("Should handle null request gracefully")
        void requestToEntity_withNullRequest_shouldReturnNull() {
            // When
            Medicine entity = mapper.requestToEntity(null);

            // Then
            assertThat(entity).isNull();
        }

        @Test
        @DisplayName("Should handle request with minimal required fields")
        void requestToEntity_withMinimalFields_shouldMapRequiredFields() {
            // Given
            MedicineRequest minimalRequest = new MedicineRequest();
            minimalRequest.setName("Paracetamol");
            minimalRequest.setActiveIngredient("Acetaminophen");
            minimalRequest.setUnit("tablet");
            minimalRequest.setQuantity(50L);
            minimalRequest.setPurchasePrice(new BigDecimal("2.00"));
            minimalRequest.setSellingPrice(new BigDecimal("3.50"));
            minimalRequest.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            minimalRequest.setCategoryId(TestDataFactory.uuid());

            // When
            Medicine entity = mapper.requestToEntity(minimalRequest);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo(minimalRequest.getName());
            assertThat(entity.getActiveIngredient()).isEqualTo(minimalRequest.getActiveIngredient());
            assertThat(entity.getUnit()).isEqualTo(minimalRequest.getUnit());
            assertThat(entity.getQuantity()).isEqualTo(minimalRequest.getQuantity());
            assertThat(entity.getPurchasePrice()).isEqualByComparingTo(minimalRequest.getPurchasePrice());
            assertThat(entity.getSellingPrice()).isEqualByComparingTo(minimalRequest.getSellingPrice());
            assertThat(entity.getDescription()).isNull();
            assertThat(entity.getConcentration()).isNull();
        }
    }

    @Nested
    @DisplayName("Partial Update Mapping")
    class PartialUpdateTests {

        @Test
        @DisplayName("Should update entity fields from request")
        void partialUpdate_shouldModifyEntity() {
            // Given
            Medicine existingEntity = new Medicine();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setName("OldName");
            existingEntity.setActiveIngredient("OldIngredient");
            existingEntity.setUnit("tablet");
            existingEntity.setQuantity(50L);
            existingEntity.setPurchasePrice(new BigDecimal("2.00"));
            existingEntity.setSellingPrice(new BigDecimal("3.00"));
            existingEntity.setExpiresAt(Instant.now());

            MedicineRequest updateRequest = new MedicineRequest();
            updateRequest.setName("UpdatedName");
            updateRequest.setActiveIngredient("UpdatedIngredient");
            updateRequest.setUnit("capsule");
            updateRequest.setQuantity(100L);
            updateRequest.setPurchasePrice(new BigDecimal("5.00"));
            updateRequest.setSellingPrice(new BigDecimal("8.00"));
            updateRequest.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            updateRequest.setCategoryId(TestDataFactory.uuid());

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getName()).isEqualTo(updateRequest.getName());
            assertThat(existingEntity.getActiveIngredient()).isEqualTo(updateRequest.getActiveIngredient());
            assertThat(existingEntity.getUnit()).isEqualTo(updateRequest.getUnit());
            assertThat(existingEntity.getQuantity()).isEqualTo(updateRequest.getQuantity());
            assertThat(existingEntity.getPurchasePrice()).isEqualByComparingTo(updateRequest.getPurchasePrice());
            assertThat(existingEntity.getSellingPrice()).isEqualByComparingTo(updateRequest.getSellingPrice());
            // ID should remain unchanged
            assertThat(existingEntity.getId()).isNotNull();
        }

        @Test
        @DisplayName("Should preserve ID during partial update")
        void partialUpdate_shouldPreserveId() {
            // Given
            String originalId = TestDataFactory.uuid();
            Medicine existingEntity = new Medicine();
            existingEntity.setId(originalId);
            existingEntity.setName("Original");

            MedicineRequest updateRequest = new MedicineRequest();
            updateRequest.setName("Updated");
            updateRequest.setActiveIngredient("UpdatedIngredient");
            updateRequest.setUnit("tablet");
            updateRequest.setQuantity(100L);
            updateRequest.setPurchasePrice(new BigDecimal("5.00"));
            updateRequest.setSellingPrice(new BigDecimal("8.00"));
            updateRequest.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            updateRequest.setCategoryId(TestDataFactory.uuid());

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getId()).isEqualTo(originalId);
        }

        @Test
        @DisplayName("Should update optional fields correctly")
        void partialUpdate_shouldUpdateOptionalFields() {
            // Given
            Medicine existingEntity = new Medicine();
            existingEntity.setId(TestDataFactory.uuid());
            existingEntity.setName("Medicine");
            existingEntity.setDescription("Old Description");
            existingEntity.setManufacturer("Old Manufacturer");

            MedicineRequest updateRequest = new MedicineRequest();
            updateRequest.setName("Medicine");
            updateRequest.setActiveIngredient("Ingredient");
            updateRequest.setUnit("tablet");
            updateRequest.setQuantity(100L);
            updateRequest.setPurchasePrice(new BigDecimal("5.00"));
            updateRequest.setSellingPrice(new BigDecimal("8.00"));
            updateRequest.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            updateRequest.setDescription("New Description");
            updateRequest.setManufacturer("New Manufacturer");
            updateRequest.setConcentration("250mg");
            updateRequest.setCategoryId(TestDataFactory.uuid());

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getDescription()).isEqualTo("New Description");
            assertThat(existingEntity.getManufacturer()).isEqualTo("New Manufacturer");
            assertThat(existingEntity.getConcentration()).isEqualTo("250mg");
        }
    }
}
