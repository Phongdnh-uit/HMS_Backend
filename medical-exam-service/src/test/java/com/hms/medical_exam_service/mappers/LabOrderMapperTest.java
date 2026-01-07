package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.lab.LabOrderResponse;
import com.hms.medical_exam_service.entities.LabOrder;
import com.hms.medical_exam_service.entities.LabOrderStatus;
import com.hms.medical_exam_service.entities.LabTestResult;
import com.hms.medical_exam_service.entities.ResultStatus;
import com.hms.common.test.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LabOrderMapper.
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
@DisplayName("UC-EXAM-005: LabOrderMapper Unit Tests")
class LabOrderMapperTest {

    @Autowired
    private LabOrderMapper mapper;

    private LabOrder testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new LabOrder();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setOrderNumber("XN-2024-001");
        testEntity.setMedicalExamId(TestDataFactory.uuid());
        testEntity.setPatientId(TestDataFactory.uuid());
        testEntity.setPatientName("John Doe");
        testEntity.setOrderDate(Instant.now());
        testEntity.setStatus(LabOrderStatus.ORDERED);
        testEntity.setNotes("Routine checkup");
        testEntity.setResults(new ArrayList<>());
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("Should map LabOrder entity to LabOrderResponse")
        void entityToResponse_shouldMapAllFields() {
            // When
            LabOrderResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
        }

        @Test
        @DisplayName("Should calculate totalTests correctly")
        void entityToResponse_shouldCalculateTotalTests() {
            // Given
            addTestResult(ResultStatus.COMPLETED);
            addTestResult(ResultStatus.PENDING);
            addTestResult(ResultStatus.PROCESSING);

            // When
            LabOrderResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getTotalTests()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should calculate completedTests correctly")
        void entityToResponse_shouldCalculateCompletedTests() {
            // Given
            addTestResult(ResultStatus.COMPLETED);
            addTestResult(ResultStatus.COMPLETED);
            addTestResult(ResultStatus.PENDING);

            // When
            LabOrderResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getCompletedTests()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate pendingTests correctly")
        void entityToResponse_shouldCalculatePendingTests() {
            // Given
            addTestResult(ResultStatus.PENDING);
            addTestResult(ResultStatus.PROCESSING);
            addTestResult(ResultStatus.COMPLETED);

            // When
            LabOrderResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getPendingTests()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle empty results list")
        void entityToResponse_withNoResults_shouldReturnZeroCounts() {
            // When
            LabOrderResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getTotalTests()).isZero();
            assertThat(result.getCompletedTests()).isZero();
            assertThat(result.getPendingTests()).isZero();
        }

        @Test
        @DisplayName("Should handle null results list")
        void entityToResponse_withNullResults_shouldReturnZeroCounts() {
            // Given
            testEntity.setResults(null);

            // When
            LabOrderResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getTotalTests()).isZero();
            assertThat(result.getCompletedTests()).isZero();
            assertThat(result.getPendingTests()).isZero();
        }
    }

    @Nested
    @DisplayName("List Mapping")
    class ListMappingTests {

        @Test
        @DisplayName("Should map list of entities to list of responses")
        void entitiesToResponses_shouldMapList() {
            // Given
            LabOrder order1 = new LabOrder();
            order1.setId(TestDataFactory.uuid());
            order1.setMedicalExamId(TestDataFactory.uuid());
            order1.setResults(new ArrayList<>());

            LabOrder order2 = new LabOrder();
            order2.setId(TestDataFactory.uuid());
            order2.setMedicalExamId(TestDataFactory.uuid());
            order2.setResults(new ArrayList<>());

            List<LabOrder> orders = Arrays.asList(order1, order2);

            // When
            List<LabOrderResponse> result = mapper.entitiesToResponses(orders);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(order1.getId());
            assertThat(result.get(1).getId()).isEqualTo(order2.getId());
        }

        @Test
        @DisplayName("Should handle empty list")
        void entitiesToResponses_withEmptyList_shouldReturnEmptyList() {
            // Given
            List<LabOrder> emptyList = Arrays.asList();

            // When
            List<LabOrderResponse> result = mapper.entitiesToResponses(emptyList);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Test Count Helpers")
    class CountHelpersTests {

        @Test
        @DisplayName("countCompletedTests should count only COMPLETED status")
        void countCompletedTests_shouldCountOnlyCompleted() {
            // Given
            addTestResult(ResultStatus.COMPLETED);
            addTestResult(ResultStatus.COMPLETED);
            addTestResult(ResultStatus.PENDING);
            addTestResult(ResultStatus.PROCESSING);

            // When
            int count = mapper.countCompletedTests(testEntity);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("countPendingTests should count PENDING and PROCESSING")
        void countPendingTests_shouldCountPendingAndProcessing() {
            // Given
            addTestResult(ResultStatus.PENDING);
            addTestResult(ResultStatus.PROCESSING);
            addTestResult(ResultStatus.COMPLETED);

            // When
            int count = mapper.countPendingTests(testEntity);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle null results in count helpers")
        void countHelpers_withNullResults_shouldReturnZero() {
            // Given
            testEntity.setResults(null);

            // When & Then
            assertThat(mapper.countCompletedTests(testEntity)).isZero();
            assertThat(mapper.countPendingTests(testEntity)).isZero();
        }
    }

    private void addTestResult(ResultStatus status) {
        LabTestResult result = new LabTestResult();
        result.setId(TestDataFactory.uuid());
        result.setStatus(status);
        result.setLabOrder(testEntity);
        testEntity.getResults().add(result);
    }
}
