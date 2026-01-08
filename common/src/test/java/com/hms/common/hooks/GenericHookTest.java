package com.hms.common.hooks;

import com.hms.common.dtos.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GenericHook interface.
 * Tests default empty implementations and contract verification.
 */
@DisplayName("UC-CMN-005: GenericHook Unit Tests")
class GenericHookTest {

    private TestHook hook;

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

    // Default hook implementation (all methods are no-ops)
    static class TestHook implements GenericHook<TestEntity, String, TestRequest, TestResponse> {
        // All methods use default implementations
    }

    @BeforeEach
    void setUp() {
        hook = new TestHook();
    }

    @Nested
    @DisplayName("View Hooks")
    class ViewHooksTests {

        @Test
        @DisplayName("UC-CMN-005: enrichFindAll should have default empty implementation")
        void enrichFindAll_defaultImplementation_shouldDoNothing() {
            // Given
            PageResponse<TestResponse> response = new PageResponse<>();
            response.setContent(List.of(
                    new TestResponse("1", "Item 1", "value1"),
                    new TestResponse("2", "Item 2", "value2")
            ));
            response.setTotalElements(2L);

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.enrichFindAll(response)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("enrichFindById should have default empty implementation")
        void enrichFindById_defaultImplementation_shouldDoNothing() {
            // Given
            TestResponse response = new TestResponse("1", "Test", "value");

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.enrichFindById(response)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Create Hooks")
    class CreateHooksTests {

        @Test
        @DisplayName("UC-CMN-005: validateCreate should have default empty implementation")
        void validateCreate_defaultImplementation_shouldDoNothing() {
            // Given
            TestRequest input = new TestRequest("New Item", "new-value");
            Map<String, Object> context = new HashMap<>();

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.validateCreate(input, context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("enrichCreate should have default empty implementation")
        void enrichCreate_defaultImplementation_shouldDoNothing() {
            // Given
            TestRequest input = new TestRequest("New Item", "new-value");
            TestEntity entity = new TestEntity();
            entity.setName("New Item");
            Map<String, Object> context = new HashMap<>();

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.enrichCreate(input, entity, context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("afterCreate should have default empty implementation")
        void afterCreate_defaultImplementation_shouldDoNothing() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId("new-123");
            TestResponse response = new TestResponse("new-123", "Created", "value");
            Map<String, Object> context = new HashMap<>();

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.afterCreate(entity, response, context)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Update Hooks")
    class UpdateHooksTests {

        @Test
        @DisplayName("UC-CMN-005: validateUpdate should have default empty implementation")
        void validateUpdate_defaultImplementation_shouldDoNothing() {
            // Given
            String id = "update-123";
            TestRequest input = new TestRequest("Updated", "updated-value");
            TestEntity existingEntity = new TestEntity();
            existingEntity.setId(id);
            Map<String, Object> context = new HashMap<>();

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.validateUpdate(id, input, existingEntity, context))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("enrichUpdate should have default empty implementation")
        void enrichUpdate_defaultImplementation_shouldDoNothing() {
            // Given
            TestRequest input = new TestRequest("Updated", "updated-value");
            TestEntity entity = new TestEntity();
            entity.setId("update-123");
            Map<String, Object> context = new HashMap<>();

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.enrichUpdate(input, entity, context)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("afterUpdate should have default empty implementation")
        void afterUpdate_defaultImplementation_shouldDoNothing() {
            // Given
            TestEntity entity = new TestEntity();
            entity.setId("update-123");
            TestResponse response = new TestResponse("update-123", "Updated", "value");
            Map<String, Object> context = new HashMap<>();

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.afterUpdate(entity, response, context)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Delete Hooks")
    class DeleteHooksTests {

        @Test
        @DisplayName("UC-CMN-005: validateDelete should have default empty implementation")
        void validateDelete_defaultImplementation_shouldDoNothing() {
            // Given
            String id = "delete-123";

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.validateDelete(id)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("afterDelete should have default empty implementation")
        void afterDelete_defaultImplementation_shouldDoNothing() {
            // Given
            String id = "delete-123";

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.afterDelete(id)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("validateBulkDelete should have default empty implementation")
        void validateBulkDelete_defaultImplementation_shouldDoNothing() {
            // Given
            List<String> ids = List.of("id1", "id2", "id3");

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.validateBulkDelete(ids)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("afterBulkDelete should have default empty implementation")
        void afterBulkDelete_defaultImplementation_shouldDoNothing() {
            // Given
            List<String> ids = List.of("id1", "id2", "id3");

            // When & Then - Should not throw exception
            assertThatCode(() -> hook.afterBulkDelete(ids)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Custom Hook Implementation")
    class CustomHookImplementationTests {

        // Custom hook that actually implements some methods
        static class CustomHook implements GenericHook<TestEntity, String, TestRequest, TestResponse> {
            public boolean validateCreateCalled = false;
            public boolean enrichCreateCalled = false;
            public boolean afterCreateCalled = false;

            @Override
            public void validateCreate(TestRequest input, Map<String, Object> context) {
                validateCreateCalled = true;
                context.put("validated", true);
            }

            @Override
            public void enrichCreate(TestRequest input, TestEntity entity, Map<String, Object> context) {
                enrichCreateCalled = true;
                entity.setValue("enriched-" + entity.getValue());
            }

            @Override
            public void afterCreate(TestEntity entity, TestResponse response, Map<String, Object> context) {
                afterCreateCalled = true;
            }
        }

        @Test
        @DisplayName("Should support custom implementations of hook methods")
        void customHook_shouldExecuteCustomLogic() {
            // Given
            CustomHook customHook = new CustomHook();
            TestRequest request = new TestRequest("Custom", "value");
            TestEntity entity = new TestEntity();
            entity.setName("Custom");
            entity.setValue("value");
            TestResponse response = new TestResponse("custom-123", "Custom", "enriched-value");
            Map<String, Object> context = new HashMap<>();

            // When
            customHook.validateCreate(request, context);
            customHook.enrichCreate(request, entity, context);
            customHook.afterCreate(entity, response, context);

            // Then
            assertThat(customHook.validateCreateCalled).isTrue();
            assertThat(customHook.enrichCreateCalled).isTrue();
            assertThat(customHook.afterCreateCalled).isTrue();
            assertThat(context).containsEntry("validated", true);
            assertThat(entity.getValue()).isEqualTo("enriched-value");
        }
    }

    @Nested
    @DisplayName("Hook Contract Verification")
    class HookContractTests {

        @Test
        @DisplayName("All view hooks should accept null parameters safely")
        void viewHooks_withNullParameters_shouldNotThrow() {
            // When & Then
            assertThatCode(() -> {
                hook.enrichFindAll(null);
                hook.enrichFindById(null);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("All create hooks should accept null parameters safely")
        void createHooks_withNullParameters_shouldNotThrow() {
            // When & Then
            assertThatCode(() -> {
                hook.validateCreate(null, null);
                hook.enrichCreate(null, null, null);
                hook.afterCreate(null, null, null);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("All update hooks should accept null parameters safely")
        void updateHooks_withNullParameters_shouldNotThrow() {
            // When & Then
            assertThatCode(() -> {
                hook.validateUpdate(null, null, null, null);
                hook.enrichUpdate(null, null, null);
                hook.afterUpdate(null, null, null);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("All delete hooks should accept null parameters safely")
        void deleteHooks_withNullParameters_shouldNotThrow() {
            // When & Then
            assertThatCode(() -> {
                hook.validateDelete(null);
                hook.afterDelete(null);
                hook.validateBulkDelete(null);
                hook.afterBulkDelete(null);
            }).doesNotThrowAnyException();
        }
    }
}
