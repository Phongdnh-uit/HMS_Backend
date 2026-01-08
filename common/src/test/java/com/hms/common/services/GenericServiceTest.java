package com.hms.common.services;

import com.hms.common.dtos.PageResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for GenericService.
 * Tests generic service layer with repository, mapper, and hook integration.
 * Note: Testing focuses on individual operations as findAll/create are
 * comprehensively tested via GenericController integration tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-CMN-002: GenericService Unit Tests")
class GenericServiceTest {

    @Mock
    private SimpleRepository<TestEntity, String> repository;

    @Mock(lenient = true)
    private GenericMapper<TestEntity, TestRequest, TestResponse> mapper;

    @Mock
    private GenericHook<TestEntity, String, TestRequest, TestResponse> hook;

    private GenericService<TestEntity, String, TestRequest, TestResponse> service;

    // Test DTOs
    record TestRequest(String name, String description) {}
    record TestResponse(String id, String name, String description) {}
    
    static class TestEntity {
        private String id;
        private String name;
        private String description;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @BeforeEach
    void setUp() {
        service = new GenericService<>(repository, mapper, hook);
    }

    @Nested
    @DisplayName("Method: findById()")
    class FindByIdTests {

        @Test
        @DisplayName("UC-CMN-002: Should find entity by ID")
        void findById_withValidId_shouldReturnEntity() {
            // Given
            String entityId = "test-123";
            TestEntity entity = new TestEntity();
            entity.setId(entityId);
            entity.setName("Test Entity");
            entity.setDescription("Test Description");

            TestResponse response = new TestResponse(entityId, "Test Entity", "Test Description");

            given(repository.findById(entityId)).willReturn(Optional.of(entity));
            given(mapper.entityToResponse(entity)).willReturn(response);

            // When
            TestResponse result = service.findById(entityId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(entityId);
            assertThat(result.name()).isEqualTo("Test Entity");

            then(repository).should().findById(entityId);
            then(mapper).should().entityToResponse(entity);
            then(hook).should().enrichFindById(response);
        }
    }

    @Nested
    @DisplayName("Method: create()")
    class CreateTests {

        @Test
        @DisplayName("UC-CMN-002: Should create new entity")
        void create_withValidInput_shouldCreateEntity() {
            // Given
            TestRequest request = new TestRequest("New Item", "New Description");
            
            TestEntity entity = new TestEntity();
            entity.setName("New Item");
            entity.setDescription("New Description");

            TestEntity savedEntity = new TestEntity();
            savedEntity.setId("new-123");
            savedEntity.setName("New Item");
            savedEntity.setDescription("New Description");

            TestResponse response = new TestResponse("new-123", "New Item", "New Description");

            given(mapper.requestToEntity(request)).willReturn(entity);
            given(repository.save(any(TestEntity.class))).willReturn(savedEntity);
            given(mapper.entityToResponse(any(TestEntity.class))).willReturn(response);

            // When
            TestResponse result = service.create(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo("new-123");
            assertThat(result.name()).isEqualTo("New Item");

            then(hook).should().validateCreate(eq(request), any());
            then(mapper).should().requestToEntity(request);
            then(hook).should().enrichCreate(eq(request), any(TestEntity.class), any());
            then(repository).should().save(any(TestEntity.class));
            then(mapper).should().entityToResponse(any(TestEntity.class));
            then(hook).should().afterCreate(any(TestEntity.class), eq(response), any());
        }
    }

    @Nested
    @DisplayName("Method: update()")
    class UpdateTests {

        @Test
        @DisplayName("UC-CMN-002: Should update existing entity")
        void update_withValidId_shouldUpdateEntity() {
            // Given
            String entityId = "update-123";
            TestRequest request = new TestRequest("Updated Name", "Updated Description");

            TestEntity existingEntity = new TestEntity();
            existingEntity.setId(entityId);
            existingEntity.setName("Old Name");
            existingEntity.setDescription("Old Description");

            TestEntity updatedEntity = new TestEntity();
            updatedEntity.setId(entityId);
            updatedEntity.setName("Updated Name");
            updatedEntity.setDescription("Updated Description");

            TestResponse response = new TestResponse(entityId, "Updated Name", "Updated Description");

            given(repository.findById(entityId)).willReturn(Optional.of(existingEntity));
            given(repository.save(existingEntity)).willReturn(updatedEntity);
            given(mapper.entityToResponse(updatedEntity)).willReturn(response);

            // When
            TestResponse result = service.update(entityId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(entityId);
            assertThat(result.name()).isEqualTo("Updated Name");

            then(repository).should().findById(entityId);
            then(hook).should().validateUpdate(eq(entityId), eq(request), eq(existingEntity), any());
            then(mapper).should().partialUpdate(request, existingEntity);
            then(hook).should().enrichUpdate(eq(request), eq(existingEntity), any());
            then(repository).should().save(existingEntity);
            then(hook).should().afterUpdate(eq(updatedEntity), eq(response), any());
        }
    }

    @Nested
    @DisplayName("Method: delete()")
    class DeleteTests {

        @Test
        @DisplayName("UC-CMN-002: Should delete entity by ID")
        void delete_withValidId_shouldDeleteEntity() {
            // Given
            String entityId = "delete-123";

            // When
            service.delete(entityId);

            // Then
            then(hook).should().validateDelete(entityId);
            then(repository).should().deleteById(entityId);
            then(hook).should().afterDelete(entityId);
        }
    }

    @Nested
    @DisplayName("Method: deleteAll()")
    class DeleteAllTests {

        @Test
        @DisplayName("UC-CMN-002: Should delete multiple entities")
        void deleteAll_withMultipleIds_shouldDeleteAll() {
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
        @DisplayName("Should handle single ID deletion")
        void deleteAll_withSingleId_shouldDelete() {
            // Given
            List<String> singleId = List.of("single-id");

            // When
            service.deleteAll(singleId);

            // Then
            then(repository).should().deleteAllByIdInBatch(singleId);
        }
    }
}
