package com.hms.common.mappers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GenericMapper interface.
 * Tests basic mapping contract and implementation verification.
 */
@DisplayName("UC-CMN-004: GenericMapper Unit Tests")
class GenericMapperTest {

    private TestMapper mapper;

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

    // Test mapper implementation
    static class TestMapper implements GenericMapper<TestEntity, TestRequest, TestResponse> {
        @Override
        public TestEntity requestToEntity(TestRequest request) {
            TestEntity entity = new TestEntity();
            entity.setName(request.name());
            entity.setValue(request.value());
            return entity;
        }

        @Override
        public TestResponse entityToResponse(TestEntity entity) {
            return new TestResponse(entity.getId(), entity.getName(), entity.getValue());
        }

        @Override
        public void partialUpdate(TestRequest request, TestEntity entity) {
            if (request.name() != null) {
                entity.setName(request.name());
            }
            if (request.value() != null) {
                entity.setValue(request.value());
            }
        }
    }

    @BeforeEach
    void setUp() {
        mapper = new TestMapper();
    }

    @Nested
    @DisplayName("Method: requestToEntity()")
    class RequestToEntityTests {

        @Test
        @DisplayName("UC-CMN-004: Should map request to entity")
        void requestToEntity_withValidRequest_shouldMapToEntity() {
            // Given
            TestRequest request = new TestRequest("Test Name", "Test Value");

            // When
            TestEntity entity = mapper.requestToEntity(request);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("Test Name");
            assertThat(entity.getValue()).isEqualTo("Test Value");
            assertThat(entity.getId()).isNull(); // ID is not set from request
        }

        @Test
        @DisplayName("Should handle request with null values")
        void requestToEntity_withNullValues_shouldMapWithNulls() {
            // Given
            TestRequest request = new TestRequest(null, null);

            // When
            TestEntity entity = mapper.requestToEntity(request);

            // Then
            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isNull();
            assertThat(entity.getValue()).isNull();
        }
    }

    @Nested
    @DisplayName("Method: entityToResponse()")
    class EntityToResponseTests {

        @Test
        @DisplayName("UC-CMN-004: Should map entity to response")
        void entityToResponse_withValidEntity_shouldMapToResponse() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId("entity-123");
            entity.setName("Entity Name");
            entity.setValue("Entity Value");

            // When
            TestResponse response = mapper.entityToResponse(entity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo("entity-123");
            assertThat(response.name()).isEqualTo("Entity Name");
            assertThat(response.value()).isEqualTo("Entity Value");
        }

        @Test
        @DisplayName("Should handle entity with null values")
        void entityToResponse_withNullValues_shouldMapWithNulls() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId(null);
            entity.setName(null);
            entity.setValue(null);

            // When
            TestResponse response = mapper.entityToResponse(entity);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isNull();
            assertThat(response.name()).isNull();
            assertThat(response.value()).isNull();
        }
    }

    @Nested
    @DisplayName("Method: partialUpdate()")
    class PartialUpdateTests {

        @Test
        @DisplayName("UC-CMN-004: Should perform partial update on entity")
        void partialUpdate_withValidRequest_shouldUpdateEntity() {
            // Given
            TestEntity existingEntity = new TestEntity();
            existingEntity.setId("existing-123");
            existingEntity.setName("Old Name");
            existingEntity.setValue("Old Value");

            TestRequest updateRequest = new TestRequest("New Name", "New Value");

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getId()).isEqualTo("existing-123"); // ID unchanged
            assertThat(existingEntity.getName()).isEqualTo("New Name");
            assertThat(existingEntity.getValue()).isEqualTo("New Value");
        }

        @Test
        @DisplayName("Should only update non-null fields")
        void partialUpdate_withNullValues_shouldOnlyUpdateNonNullFields() {
            // Given
            TestEntity existingEntity = new TestEntity();
            existingEntity.setId("existing-456");
            existingEntity.setName("Original Name");
            existingEntity.setValue("Original Value");

            TestRequest updateRequest = new TestRequest("Updated Name", null);

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getName()).isEqualTo("Updated Name");
            assertThat(existingEntity.getValue()).isEqualTo("Original Value"); // Unchanged
        }

        @Test
        @DisplayName("Should preserve entity ID during update")
        void partialUpdate_shouldPreserveEntityId() {
            // Given
            TestEntity existingEntity = new TestEntity();
            existingEntity.setId("preserve-id");
            existingEntity.setName("Name");
            existingEntity.setValue("Value");

            TestRequest updateRequest = new TestRequest("New", "New");

            // When
            mapper.partialUpdate(updateRequest, existingEntity);

            // Then
            assertThat(existingEntity.getId()).isEqualTo("preserve-id");
        }

        @Test
        @DisplayName("Should handle all null values in update request")
        void partialUpdate_withAllNullValues_shouldNotChangeEntity() {
            // Given
            TestEntity existingEntity = new TestEntity();
            existingEntity.setId("unchanged-id");
            existingEntity.setName("Unchanged Name");
            existingEntity.setValue("Unchanged Value");

            TestRequest nullRequest = new TestRequest(null, null);

            // When
            mapper.partialUpdate(nullRequest, existingEntity);

            // Then - All values should remain unchanged
            assertThat(existingEntity.getId()).isEqualTo("unchanged-id");
            assertThat(existingEntity.getName()).isEqualTo("Unchanged Name");
            assertThat(existingEntity.getValue()).isEqualTo("Unchanged Value");
        }
    }

    @Nested
    @DisplayName("Mapper Contract Verification")
    class MapperContractTests {

        @Test
        @DisplayName("Should provide consistent round-trip mapping")
        void mapper_shouldProvideConsistentRoundTrip() {
            // Given
            TestRequest request = new TestRequest("Round Trip", "Value");

            // When
            TestEntity entity = mapper.requestToEntity(request);
            entity.setId("generated-id"); // Simulate ID generation
            TestResponse response = mapper.entityToResponse(entity);

            // Then
            assertThat(response.name()).isEqualTo(request.name());
            assertThat(response.value()).isEqualTo(request.value());
            assertThat(response.id()).isEqualTo("generated-id");
        }

        @Test
        @DisplayName("Should support update-then-response flow")
        void mapper_shouldSupportUpdateFlow() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId("flow-test-id");
            entity.setName("Original");
            entity.setValue("Original");

            TestRequest updateRequest = new TestRequest("Updated", "Updated");

            // When
            mapper.partialUpdate(updateRequest, entity);
            TestResponse response = mapper.entityToResponse(entity);

            // Then
            assertThat(response.id()).isEqualTo("flow-test-id");
            assertThat(response.name()).isEqualTo("Updated");
            assertThat(response.value()).isEqualTo("Updated");
        }
    }
}
