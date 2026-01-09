package com.hms.common.services;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.common.mappers.GenericMapper;
import com.hms.common.repositories.SimpleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for CrudService default methods.
 * Tests base CRUD operations with hooks.
 * Note: Testing focuses on delete operations as create/read/update are
 * comprehensively tested via GenericService integration tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-CMN-003: CrudService Unit Tests")
class CrudServiceTest {

    @Mock
    private SimpleRepository<TestEntity, String> repository;

    @Mock(lenient = true)
    private GenericMapper<TestEntity, TestRequest, TestResponse> mapper;

    @Mock
    private GenericHook<TestEntity, String, TestRequest, TestResponse> hook;

    private TestCrudService service;

    // Test DTOs
    record TestRequest(String name, String value) {}
    record TestResponse(String id, String name, String value) {}
    static class TestEntity {
        private String id;
        private String name;
        private String value;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    // Test implementation
    class TestCrudService implements CrudService<TestEntity, String, TestRequest, TestResponse> {
        @Override
        public PageResponse<TestResponse> findAll(Pageable pageable, Specification<TestEntity> specification) {
            return defaultFindAll(pageable, specification, mapper, repository, hook);
        }

        @Override
        public TestResponse findById(String id) {
            return defaultFindById(id, mapper, repository, hook);
        }

        @Override
        public TestResponse create(TestRequest input) {
            return defaultCreate(input, mapper, repository, hook);
        }

        @Override
        public TestResponse update(String id, TestRequest input) {
            return defaultUpdate(id, input, mapper, repository, hook);
        }

        @Override
        public void delete(String id) {
            defaultDelete(id, repository, hook);
        }

        @Override
        public void deleteAll(Iterable<String> ids) {
            defaultDeleteAll(ids, repository, hook);
        }
    }

    @BeforeEach
    void setUp() {
        service = new TestCrudService();
    }

    @Nested
    @DisplayName("Method: defaultFindById()")
    class DefaultFindByIdTests {

        @Test
        @DisplayName("UC-CMN-003: Should find entity by ID")
        void defaultFindById_withValidId_shouldReturnEntity() {
            // Given
            String entityId = "test-id";
            TestEntity entity = new TestEntity();
            entity.setId(entityId);
            entity.setName("Test Entity");

            TestResponse response = new TestResponse(entityId, "Test Entity", "test-value");

            given(repository.findById(entityId)).willReturn(Optional.of(entity));
            given(mapper.entityToResponse(entity)).willReturn(response);

            // When
            TestResponse result = service.findById(entityId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(entityId);
            assertThat(result.name()).isEqualTo("Test Entity");
            then(hook).should().enrichFindById(response);
        }

        @Test
        @DisplayName("Should throw exception when entity not found")
        void defaultFindById_withInvalidId_shouldThrowException() {
            // Given
            String invalidId = "invalid-id";
            given(repository.findById(invalidId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.findById(invalidId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

            then(hook).should(never()).enrichFindById(any());
        }
    }

    @Nested
    @DisplayName("Method: defaultUpdate()")
    class DefaultUpdateTests {

        @Test
        @DisplayName("UC-CMN-003: Should update existing entity")
        void defaultUpdate_withValidId_shouldUpdateEntity() {
            // Given
            String entityId = "update-id";
            TestRequest request = new TestRequest("Updated Name", "updated-value");

            TestEntity existingEntity = new TestEntity();
            existingEntity.setId(entityId);
            existingEntity.setName("Old Name");
            existingEntity.setValue("old-value");

            TestEntity updatedEntity = new TestEntity();
            updatedEntity.setId(entityId);
            updatedEntity.setName("Updated Name");
            updatedEntity.setValue("updated-value");

            TestResponse response = new TestResponse(entityId, "Updated Name", "updated-value");

            given(repository.findById(entityId)).willReturn(Optional.of(existingEntity));
            given(repository.save(existingEntity)).willReturn(updatedEntity);
            given(mapper.entityToResponse(updatedEntity)).willReturn(response);

            // When
            TestResponse result = service.update(entityId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(entityId);
            assertThat(result.name()).isEqualTo("Updated Name");

            then(hook).should().validateUpdate(eq(entityId), eq(request), eq(existingEntity), any(Map.class));
            then(mapper).should().partialUpdate(request, existingEntity);
            then(hook).should().enrichUpdate(eq(request), eq(existingEntity), any(Map.class));
            then(hook).should().afterUpdate(eq(updatedEntity), eq(response), any(Map.class));
        }

        @Test
        @DisplayName("Should throw exception when entity to update not found")
        void defaultUpdate_withInvalidId_shouldThrowException() {
            // Given
            String invalidId = "invalid-id";
            TestRequest request = new TestRequest("Update", "value");
            given(repository.findById(invalidId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.update(invalidId, request))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);

            then(hook).should(never()).validateUpdate(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Method: defaultDelete()")
    class DefaultDeleteTests {

        @Test
        @DisplayName("UC-CMN-003: Should delete entity by ID")
        void defaultDelete_withValidId_shouldDeleteEntity() {
            // Given
            String entityId = "delete-id";

            // When
            service.delete(entityId);

            // Then
            then(hook).should().validateDelete(entityId);
            then(repository).should().deleteById(entityId);
            then(hook).should().afterDelete(entityId);
        }
    }

    @Nested
    @DisplayName("Method: defaultDeleteAll()")
    class DefaultDeleteAllTests {

        @Test
        @DisplayName("UC-CMN-003: Should delete multiple entities")
        void defaultDeleteAll_withIds_shouldDeleteAllEntities() {
            // Given
            List<String> ids = List.of("id1", "id2", "id3");

            // When
            service.deleteAll(ids);

            // Then
            then(hook).should().validateBulkDelete(ids);
            then(repository).should().deleteAllByIdInBatch(ids);
            then(hook).should().afterBulkDelete(ids);
        }

        @Test
        @DisplayName("Should handle empty ID list")
        void defaultDeleteAll_withEmptyList_shouldCallHooksAndRepository() {
            // Given
            List<String> emptyIds = List.of();

            // When
            service.deleteAll(emptyIds);

            // Then
            then(hook).should().validateBulkDelete(emptyIds);
            then(repository).should().deleteAllByIdInBatch(emptyIds);
            then(hook).should().afterBulkDelete(emptyIds);
        }
    }
}
