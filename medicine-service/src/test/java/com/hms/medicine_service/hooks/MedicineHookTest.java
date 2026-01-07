package com.hms.medicine_service.hooks;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import com.hms.medicine_service.dtos.medicine.MedicineRequest;
import com.hms.medicine_service.entities.Category;
import com.hms.medicine_service.entities.Medicine;
import com.hms.medicine_service.repositories.CategoryRepository;
import com.hms.medicine_service.repositories.MedicineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for MedicineHook.
 * Tests lifecycle hooks for Medicine entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-MED-004/005: MedicineHook Unit Tests")
class MedicineHookTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private MedicineHook medicineHook;

    private MedicineRequest testRequest;
    private Medicine testEntity;
    private Category testCategory;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        context = new HashMap<>();

        testCategory = new Category();
        testCategory.setId(TestDataFactory.uuid());
        testCategory.setName("Antibiotics");

        testRequest = new MedicineRequest();
        testRequest.setName("Amoxicillin");
        testRequest.setActiveIngredient("Amoxicillin Trihydrate");
        testRequest.setUnit("capsule");
        testRequest.setQuantity(100L);
        testRequest.setPurchasePrice(new BigDecimal("5.50"));
        testRequest.setSellingPrice(new BigDecimal("8.75"));
        testRequest.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        testRequest.setCategoryId(testCategory.getId());

        testEntity = new Medicine();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setName(testRequest.getName());
        testEntity.setActiveIngredient(testRequest.getActiveIngredient());
        testEntity.setUnit(testRequest.getUnit());
        testEntity.setQuantity(testRequest.getQuantity());
        testEntity.setPurchasePrice(testRequest.getPurchasePrice());
        testEntity.setSellingPrice(testRequest.getSellingPrice());
        testEntity.setExpiresAt(testRequest.getExpiresAt());
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("UC-MED-004: Should validate create successfully when category exists")
        void validateCreate_withValidCategory_shouldPass() {
            // Given
            given(categoryRepository.existsById(testRequest.getCategoryId())).willReturn(true);

            // When & Then
            assertThatCode(() -> medicineHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().existsById(testRequest.getCategoryId());
        }

        @Test
        @DisplayName("Should throw exception when category does not exist")
        void validateCreate_withNonExistentCategory_shouldThrowException() {
            // Given
            String nonExistentCategoryId = TestDataFactory.uuid();
            testRequest.setCategoryId(nonExistentCategoryId);
            given(categoryRepository.existsById(nonExistentCategoryId)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> medicineHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting("fieldErrors")
                    .satisfies(fieldErrors -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> errorMap = (Map<String, String>) fieldErrors;
                        assertThat(errorMap).containsKey("categoryId");
                        assertThat(errorMap.get("categoryId"))
                                .contains("Category with id " + nonExistentCategoryId + " does not exist");
                    });

            then(categoryRepository).should().existsById(nonExistentCategoryId);
        }

        @Test
        @DisplayName("Should validate category reference before creation")
        void validateCreate_shouldCheckCategoryExists() {
            // Given
            given(categoryRepository.existsById(testRequest.getCategoryId())).willReturn(true);

            // When
            medicineHook.validateCreate(testRequest, context);

            // Then
            then(categoryRepository).should(times(1)).existsById(testRequest.getCategoryId());
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("Should enrich entity with category reference on create")
        void enrichCreate_shouldSetCategoryReference() {
            // Given
            given(categoryRepository.getReferenceById(testRequest.getCategoryId())).willReturn(testCategory);

            // When
            medicineHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getCategory()).isNotNull();
            assertThat(testEntity.getCategory().getId()).isEqualTo(testCategory.getId());
            assertThat(testEntity.getCategory().getName()).isEqualTo(testCategory.getName());

            then(categoryRepository).should().getReferenceById(testRequest.getCategoryId());
        }

        @Test
        @DisplayName("Should preserve other entity fields during enrichment")
        void enrichCreate_shouldPreserveOtherFields() {
            // Given
            String originalName = testEntity.getName();
            Long originalQuantity = testEntity.getQuantity();
            given(categoryRepository.getReferenceById(testRequest.getCategoryId())).willReturn(testCategory);

            // When
            medicineHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getName()).isEqualTo(originalName);
            assertThat(testEntity.getQuantity()).isEqualTo(originalQuantity);
            assertThat(testEntity.getCategory()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("UC-MED-005: Should validate update successfully when category exists")
        void validateUpdate_withValidCategory_shouldPass() {
            // Given
            String medicineId = testEntity.getId();
            given(categoryRepository.existsById(testRequest.getCategoryId())).willReturn(true);

            // When & Then
            assertThatCode(() -> medicineHook.validateUpdate(medicineId, testRequest, testEntity, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().existsById(testRequest.getCategoryId());
        }

        @Test
        @DisplayName("Should throw exception when updating with non-existent category")
        void validateUpdate_withNonExistentCategory_shouldThrowException() {
            // Given
            String medicineId = testEntity.getId();
            String nonExistentCategoryId = TestDataFactory.uuid();
            testRequest.setCategoryId(nonExistentCategoryId);
            given(categoryRepository.existsById(nonExistentCategoryId)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> medicineHook.validateUpdate(medicineId, testRequest, testEntity, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting("fieldErrors")
                    .satisfies(fieldErrors -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> errorMap = (Map<String, String>) fieldErrors;
                        assertThat(errorMap).containsKey("categoryId");
                        assertThat(errorMap.get("categoryId"))
                                .contains("Category with id " + nonExistentCategoryId + " does not exist");
                    });

            then(categoryRepository).should().existsById(nonExistentCategoryId);
        }

        @Test
        @DisplayName("Should validate category reference before update")
        void validateUpdate_shouldCheckCategoryExists() {
            // Given
            String medicineId = testEntity.getId();
            given(categoryRepository.existsById(testRequest.getCategoryId())).willReturn(true);

            // When
            medicineHook.validateUpdate(medicineId, testRequest, testEntity, context);

            // Then
            then(categoryRepository).should(times(1)).existsById(testRequest.getCategoryId());
        }
    }

    @Nested
    @DisplayName("Method: enrichUpdate()")
    class EnrichUpdateTests {

        @Test
        @DisplayName("Should enrich entity with category reference on update")
        void enrichUpdate_shouldSetCategoryReference() {
            // Given
            given(categoryRepository.getReferenceById(testRequest.getCategoryId())).willReturn(testCategory);

            // When
            medicineHook.enrichUpdate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getCategory()).isNotNull();
            assertThat(testEntity.getCategory().getId()).isEqualTo(testCategory.getId());

            then(categoryRepository).should().getReferenceById(testRequest.getCategoryId());
        }

        @Test
        @DisplayName("Should update category reference when changed")
        void enrichUpdate_withNewCategory_shouldUpdateCategoryReference() {
            // Given
            Category oldCategory = new Category();
            oldCategory.setId(TestDataFactory.uuid());
            oldCategory.setName("OldCategory");
            testEntity.setCategory(oldCategory);

            Category newCategory = new Category();
            newCategory.setId(TestDataFactory.uuid());
            newCategory.setName("NewCategory");
            testRequest.setCategoryId(newCategory.getId());

            given(categoryRepository.getReferenceById(newCategory.getId())).willReturn(newCategory);

            // When
            medicineHook.enrichUpdate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getCategory()).isNotNull();
            assertThat(testEntity.getCategory().getId()).isEqualTo(newCategory.getId());
            assertThat(testEntity.getCategory().getId()).isNotEqualTo(oldCategory.getId());

            then(categoryRepository).should().getReferenceById(newCategory.getId());
        }

        @Test
        @DisplayName("Should preserve other entity fields during update enrichment")
        void enrichUpdate_shouldPreserveOtherFields() {
            // Given
            String originalId = testEntity.getId();
            String originalName = testEntity.getName();
            BigDecimal originalPrice = testEntity.getSellingPrice();
            given(categoryRepository.getReferenceById(testRequest.getCategoryId())).willReturn(testCategory);

            // When
            medicineHook.enrichUpdate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getId()).isEqualTo(originalId);
            assertThat(testEntity.getName()).isEqualTo(originalName);
            assertThat(testEntity.getSellingPrice()).isEqualTo(originalPrice);
            assertThat(testEntity.getCategory()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty context map")
        void validate_withEmptyContext_shouldWork() {
            // Given
            Map<String, Object> emptyContext = new HashMap<>();
            given(categoryRepository.existsById(testRequest.getCategoryId())).willReturn(true);

            // When & Then
            assertThatCode(() -> medicineHook.validateCreate(testRequest, emptyContext))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle minimal valid request")
        void validateCreate_withMinimalRequest_shouldValidateCategoryOnly() {
            // Given
            MedicineRequest minimalRequest = new MedicineRequest();
            minimalRequest.setName("Minimal");
            minimalRequest.setActiveIngredient("Ingredient");
            minimalRequest.setUnit("tablet");
            minimalRequest.setQuantity(1L);
            minimalRequest.setPurchasePrice(new BigDecimal("1.00"));
            minimalRequest.setSellingPrice(new BigDecimal("2.00"));
            minimalRequest.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
            minimalRequest.setCategoryId(testCategory.getId());

            given(categoryRepository.existsById(testCategory.getId())).willReturn(true);

            // When & Then
            assertThatCode(() -> medicineHook.validateCreate(minimalRequest, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().existsById(testCategory.getId());
        }
    }
}
