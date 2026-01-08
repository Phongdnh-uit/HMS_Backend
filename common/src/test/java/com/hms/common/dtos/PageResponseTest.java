package com.hms.common.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PageResponse.
 * Tests pagination wrapping and conversion from Spring Page.
 */
@DisplayName("UC-CMN-009: PageResponse Unit Tests")
class PageResponseTest {

    @Nested
    @DisplayName("Method: fromPage(Page<T>)")
    class FromPageTests {

        @Test
        @DisplayName("UC-CMN-009: Should create PageResponse from Spring Page")
        void fromPage_withValidPage_shouldCreatePageResponse() {
            // Given
            List<String> content = List.of("item1", "item2", "item3");
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<String> page = new PageImpl<>(content, pageRequest, 25);

            // When
            PageResponse<String> response = PageResponse.fromPage(page);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isEqualTo(25L);
            assertThat(response.getTotalPages()).isEqualTo(3);
            assertThat(response.getNumberOfElements()).isEqualTo(3);
            assertThat(response.getContent()).containsExactly("item1", "item2", "item3");
        }

        @Test
        @DisplayName("Should handle empty page")
        void fromPage_withEmptyPage_shouldCreateEmptyPageResponse() {
            // Given
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<String> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

            // When
            PageResponse<String> response = PageResponse.fromPage(emptyPage);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPage()).isEqualTo(0);
            assertThat(response.getSize()).isEqualTo(10);
            assertThat(response.getTotalElements()).isZero();
            assertThat(response.getTotalPages()).isZero();
            assertThat(response.getNumberOfElements()).isZero();
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should handle page with complex objects")
        void fromPage_withComplexObjects_shouldPreserveData() {
            // Given
            record TestDto(String id, String name) {}
            List<TestDto> content = List.of(
                    new TestDto("1", "First"),
                    new TestDto("2", "Second")
            );
            Page<TestDto> page = new PageImpl<>(content, PageRequest.of(1, 5), 12);

            // When
            PageResponse<TestDto> response = PageResponse.fromPage(page);

            // Then
            assertThat(response.getPage()).isEqualTo(1);
            assertThat(response.getSize()).isEqualTo(5);
            assertThat(response.getTotalPages()).isEqualTo(3);
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getContent().get(0).id()).isEqualTo("1");
        }

        @Test
        @DisplayName("Should handle last page")
        void fromPage_withLastPage_shouldCalculateCorrectly() {
            // Given
            List<String> content = List.of("last-item");
            Page<String> lastPage = new PageImpl<>(content, PageRequest.of(4, 10), 41);

            // When
            PageResponse<String> response = PageResponse.fromPage(lastPage);

            // Then
            assertThat(response.getPage()).isEqualTo(4);
            assertThat(response.getTotalPages()).isEqualTo(5);
            assertThat(response.getNumberOfElements()).isEqualTo(1);
            assertThat(response.getTotalElements()).isEqualTo(41);
        }
    }

    @Nested
    @DisplayName("Method: empty()")
    class EmptyTests {

        @Test
        @DisplayName("UC-CMN-009: Should create empty PageResponse")
        void empty_shouldCreateEmptyResponse() {
            // When
            PageResponse<String> response = PageResponse.empty();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getPage()).isZero();
            assertThat(response.getSize()).isZero();
            assertThat(response.getTotalElements()).isZero();
            assertThat(response.getTotalPages()).isZero();
            assertThat(response.getNumberOfElements()).isZero();
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should create empty response with correct type")
        void empty_shouldMaintainGenericType() {
            // When
            PageResponse<Integer> intResponse = PageResponse.empty();
            PageResponse<String> stringResponse = PageResponse.empty();

            // Then
            assertThat(intResponse.getContent()).isEmpty();
            assertThat(stringResponse.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Setters and Getters")
    class SettersAndGettersTests {

        @Test
        @DisplayName("Should allow manual field setting")
        void setters_shouldAllowManualConfiguration() {
            // Given
            PageResponse<String> response = new PageResponse<>();
            List<String> content = List.of("a", "b", "c");

            // When
            response.setPage(2);
            response.setSize(20);
            response.setTotalElements(100L);
            response.setTotalPages(5);
            response.setNumberOfElements(20);
            response.setContent(content);

            // Then
            assertThat(response.getPage()).isEqualTo(2);
            assertThat(response.getSize()).isEqualTo(20);
            assertThat(response.getTotalElements()).isEqualTo(100L);
            assertThat(response.getTotalPages()).isEqualTo(5);
            assertThat(response.getNumberOfElements()).isEqualTo(20);
            assertThat(response.getContent()).isEqualTo(content);
        }
    }
}
