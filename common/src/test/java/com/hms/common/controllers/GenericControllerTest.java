package com.hms.common.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.services.CrudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for GenericController.
 * Tests generic CRUD operations and API response handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-CMN-001: GenericController Unit Tests")
class GenericControllerTest {

    @Mock
    private CrudService<TestEntity, String, TestRequest, TestResponse> service;

    private TestController controller;

    // Test DTOs
    record TestRequest(String name, String value) {}
    record TestResponse(String id, String name, String value) {}
    static class TestEntity {
        private String id;
        private String name;
    }

    // Test controller implementation
    class TestController extends GenericController<TestEntity, String, TestRequest, TestResponse> {
        public TestController(CrudService<TestEntity, String, TestRequest, TestResponse> service) {
            super(service);
        }
    }

    @BeforeEach
    void setUp() {
        controller = new TestController(service);
    }

    @Nested
    @DisplayName("Method: findAll()")
    class FindAllTests {

        @Test
        @DisplayName("UC-CMN-001: Should find all entities with pagination")
        void findAll_withPagination_shouldReturnPageResponse() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<TestResponse> pageResponse = new PageResponse<>();
            pageResponse.setContent(List.of(
                    new TestResponse("1", "Item 1", "value1"),
                    new TestResponse("2", "Item 2", "value2")
            ));
            pageResponse.setTotalElements(2L);
            pageResponse.setPage(0);
            pageResponse.setSize(10);

            given(service.findAll(any(Pageable.class), any())).willReturn(pageResponse);

            // When
            ResponseEntity<ApiResponse<PageResponse<TestResponse>>> response = 
                    controller.findAll(pageable, null, false);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(1000);
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().getContent()).hasSize(2);

            then(service).should().findAll(any(Pageable.class), any(Specification.class));
        }

        @Test
        @DisplayName("Should apply filter parameter")
        void findAll_withFilter_shouldApplyFilter() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            String filter = "name==test";
            PageResponse<TestResponse> pageResponse = new PageResponse<>();
            pageResponse.setContent(List.of(new TestResponse("1", "test", "value")));

            given(service.findAll(any(Pageable.class), any())).willReturn(pageResponse);

            // When
            ResponseEntity<ApiResponse<PageResponse<TestResponse>>> response = 
                    controller.findAll(pageable, filter, false);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getContent()).hasSize(1);

            then(service).should().findAll(any(Pageable.class), any(Specification.class));
        }

        @Test
        @DisplayName("Should return all results when all parameter is true")
        void findAll_withAllTrue_shouldReturnUnpaged() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<TestResponse> pageResponse = PageResponse.empty();

            given(service.findAll(any(Pageable.class), any())).willReturn(pageResponse);

            // When
            ResponseEntity<ApiResponse<PageResponse<TestResponse>>> response = 
                    controller.findAll(pageable, null, true);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(service).should().findAll(any(Pageable.class), any());
        }

        @Test
        @DisplayName("Should handle empty results")
        void findAll_withNoResults_shouldReturnEmptyPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            PageResponse<TestResponse> emptyPage = PageResponse.empty();

            given(service.findAll(any(Pageable.class), any())).willReturn(emptyPage);

            // When
            ResponseEntity<ApiResponse<PageResponse<TestResponse>>> response = 
                    controller.findAll(pageable, null, false);

            // Then
            assertThat(response.getBody().getData().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: findById()")
    class FindByIdTests {

        @Test
        @DisplayName("UC-CMN-001: Should find entity by ID")
        void findById_withValidId_shouldReturnEntity() {
            // Given
            String entityId = "test-123";
            TestResponse entityResponse = new TestResponse(entityId, "Test Entity", "test-value");

            given(service.findById(entityId)).willReturn(entityResponse);

            // When
            ResponseEntity<ApiResponse<TestResponse>> response = controller.findById(entityId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(1000);
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().id()).isEqualTo(entityId);

            then(service).should().findById(entityId);
        }
    }

    @Nested
    @DisplayName("Method: create()")
    class CreateTests {

        @Test
        @DisplayName("UC-CMN-001: Should create new entity")
        void create_withValidInput_shouldCreateEntity() {
            // Given
            TestRequest request = new TestRequest("New Entity", "new-value");
            TestResponse response = new TestResponse("new-123", "New Entity", "new-value");

            given(service.create(request)).willReturn(response);

            // When
            ResponseEntity<ApiResponse<TestResponse>> result = controller.create(request);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getCode()).isEqualTo(1000);
            assertThat(result.getBody().getData()).isNotNull();
            assertThat(result.getBody().getData().id()).isEqualTo("new-123");

            then(service).should().create(request);
        }
    }

    @Nested
    @DisplayName("Method: update()")
    class UpdateTests {

        @Test
        @DisplayName("UC-CMN-001: Should update existing entity")
        void update_withValidId_shouldUpdateEntity() {
            // Given
            String entityId = "update-123";
            TestRequest request = new TestRequest("Updated Entity", "updated-value");
            TestResponse response = new TestResponse(entityId, "Updated Entity", "updated-value");

            given(service.update(entityId, request)).willReturn(response);

            // When
            ResponseEntity<ApiResponse<TestResponse>> result = controller.update(entityId, request);

            // Then
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getCode()).isEqualTo(1000);
            assertThat(result.getBody().getData()).isNotNull();
            assertThat(result.getBody().getData().id()).isEqualTo(entityId);
            assertThat(result.getBody().getData().name()).isEqualTo("Updated Entity");

            then(service).should().update(entityId, request);
        }
    }

    @Nested
    @DisplayName("Method: delete()")
    class DeleteTests {

        @Test
        @DisplayName("UC-CMN-001: Should delete entity by ID")
        void delete_withValidId_shouldDeleteEntity() {
            // Given
            String entityId = "delete-123";
            willDoNothing().given(service).delete(entityId);

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.delete(entityId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(1000);
            assertThat(response.getBody().getData()).isNull();

            then(service).should().delete(entityId);
        }
    }

    @Nested
    @DisplayName("Method: deleteAll()")
    class DeleteAllTests {

        @Test
        @DisplayName("UC-CMN-001: Should delete multiple entities")
        void deleteAll_withMultipleIds_shouldDeleteAll() {
            // Given
            List<String> ids = List.of("id1", "id2", "id3");
            willDoNothing().given(service).deleteAll(ids);

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.deleteAll(ids);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(1000);
            assertThat(response.getBody().getData()).isNull();

            then(service).should().deleteAll(ids);
        }

        @Test
        @DisplayName("Should handle empty ID list")
        void deleteAll_withEmptyList_shouldCallService() {
            // Given
            List<String> emptyIds = List.of();
            willDoNothing().given(service).deleteAll(emptyIds);

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.deleteAll(emptyIds);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(service).should().deleteAll(emptyIds);
        }

        @Test
        @DisplayName("Should handle single ID deletion")
        void deleteAll_withSingleId_shouldDelete() {
            // Given
            List<String> singleId = List.of("single-123");
            willDoNothing().given(service).deleteAll(singleId);

            // When
            ResponseEntity<ApiResponse<Void>> response = controller.deleteAll(singleId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(service).should().deleteAll(singleId);
        }
    }
}
