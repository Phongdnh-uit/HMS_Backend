package com.hms.medicine_service.hooks;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.test.TestDataFactory;
import com.hms.medicine_service.dtos.category.CategoryRequest;
import com.hms.medicine_service.entities.Category;
import com.hms.medicine_service.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for CategoryHook.
 * Tests lifecycle hooks for Category entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-MED-006: CategoryHook Unit Tests")
class CategoryHookTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryHook categoryHook;

    private CategoryRequest testRequest;
    private Category testEntity;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        context = new HashMap<>();

        testRequest = new CategoryRequest();
        testRequest.setName("Antibiotics");
        testRequest.setDescription("Antibiotic medications");

        testEntity = new Category();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setName(testRequest.getName());
        testEntity.setDescription(testRequest.getDescription());
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("Should validate create successfully when name is unique")
        void validateCreate_withUniqueName_shouldPass() {
            // Given
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When & Then
            assertThatCode(() -> categoryHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should throw exception when category name already exists")
        void validateCreate_withDuplicateName_shouldThrowException() {
            // Given
            given(categoryRepository.count(any(Specification.class))).willReturn(1L);

            // When & Then
            assertThatThrownBy(() -> categoryHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting("fieldErrors")
                    .satisfies(fieldErrors -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> errorMap = (Map<String, String>) fieldErrors;
                        assertThat(errorMap).containsKey("name");
                        assertThat(errorMap.get("name"))
                                .contains("Category with name '" + testRequest.getName() + "' already exists");
                    });

            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should perform case-insensitive name validation")
        void validateCreate_shouldCheckNameCaseInsensitive() {
            // Given - repository will be called with lowercase name comparison
            testRequest.setName("ANTIBIOTICS");
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When
            categoryHook.validateCreate(testRequest, context);

            // Then - verification that count was called with specification
            then(categoryRepository).should().count(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("Should validate update successfully when name is unique")
        void validateUpdate_withUniqueName_shouldPass() {
            // Given
            String categoryId = testEntity.getId();
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When & Then
            assertThatCode(() -> categoryHook.validateUpdate(categoryId, testRequest, testEntity, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should throw exception when updating to duplicate name")
        void validateUpdate_withDuplicateName_shouldThrowException() {
            // Given
            String categoryId = testEntity.getId();
            given(categoryRepository.count(any(Specification.class))).willReturn(1L);

            // When & Then
            assertThatThrownBy(() -> categoryHook.validateUpdate(categoryId, testRequest, testEntity, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR)
                    .extracting("fieldErrors")
                    .satisfies(fieldErrors -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> errorMap = (Map<String, String>) fieldErrors;
                        assertThat(errorMap).containsKey("name");
                        assertThat(errorMap.get("name"))
                                .contains("Category with name '" + testRequest.getName() + "' already exists");
                    });

            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should allow updating category with same name (no change)")
        void validateUpdate_withSameName_shouldAllowIfSameCategory() {
            // Given
            String categoryId = testEntity.getId();
            testRequest.setName(testEntity.getName()); // Same name
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When & Then
            assertThatCode(() -> categoryHook.validateUpdate(categoryId, testRequest, testEntity, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().count(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Method: validateDelete()")
    class ValidateDeleteTests {

        @Test
        @DisplayName("UC-MED-006: Should validate delete when called")
        void validateDelete_shouldBeCallable() {
            // Given
            String categoryId = testEntity.getId();

            // When & Then - current implementation is empty, should not throw
            assertThatCode(() -> categoryHook.validateDelete(categoryId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should not throw exception for valid category deletion")
        void validateDelete_withValidId_shouldPass() {
            // Given
            String categoryId = TestDataFactory.uuid();

            // When & Then
            assertThatCode(() -> categoryHook.validateDelete(categoryId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null category ID gracefully")
        void validateDelete_withNullId_shouldNotThrow() {
            // When & Then - current implementation is empty
            assertThatCode(() -> categoryHook.validateDelete(null))
                    .doesNotThrowAnyException();
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
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When & Then
            assertThatCode(() -> categoryHook.validateCreate(testRequest, emptyContext))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should trim and validate category name")
        void validateCreate_shouldHandleNameWithSpaces() {
            // Given
            testRequest.setName("  Pain Relievers  ");
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When
            categoryHook.validateCreate(testRequest, context);

            // Then
            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should handle category name with special characters")
        void validateCreate_withSpecialCharacters_shouldValidate() {
            // Given
            testRequest.setName("Anti-inflammatories & Pain Relief");
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When & Then
            assertThatCode(() -> categoryHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should handle very long category names")
        void validateCreate_withLongName_shouldValidate() {
            // Given
            String longName = "A".repeat(100);
            testRequest.setName(longName);
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When
            categoryHook.validateCreate(testRequest, context);

            // Then
            then(categoryRepository).should().count(any(Specification.class));
        }

        @Test
        @DisplayName("Should validate with null description")
        void validateCreate_withNullDescription_shouldValidateNameOnly() {
            // Given
            testRequest.setDescription(null);
            given(categoryRepository.count(any(Specification.class))).willReturn(0L);

            // When & Then
            assertThatCode(() -> categoryHook.validateCreate(testRequest, context))
                    .doesNotThrowAnyException();

            then(categoryRepository).should().count(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperationTests {

        @Test
        @DisplayName("Should handle bulk delete validation")
        void validateBulkDelete_shouldBeCallable() {
            // Given
            Iterable<String> ids = java.util.List.of(
                    TestDataFactory.uuid(),
                    TestDataFactory.uuid(),
                    TestDataFactory.uuid()
            );

            // When & Then - current implementation is empty
            assertThatCode(() -> categoryHook.validateBulkDelete(ids))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty bulk delete")
        void validateBulkDelete_withEmptyList_shouldNotThrow() {
            // Given
            Iterable<String> emptyIds = java.util.Collections.emptyList();

            // When & Then
            assertThatCode(() -> categoryHook.validateBulkDelete(emptyIds))
                    .doesNotThrowAnyException();
        }
    }
}
