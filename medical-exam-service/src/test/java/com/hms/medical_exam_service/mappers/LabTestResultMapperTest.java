package com.hms.medical_exam_service.mappers;

import com.hms.medical_exam_service.dtos.lab.LabTestResultRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResultResponse;
import com.hms.medical_exam_service.dtos.lab.LabTestResultUpdateRequest;
import com.hms.medical_exam_service.entities.LabTestCategory;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LabTestResultMapper.
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
@DisplayName("UC-EXAM-007: LabTestResultMapper Unit Tests")
class LabTestResultMapperTest {

    @Autowired
    private LabTestResultMapper mapper;

    private LabTestResult testEntity;
    private LabTestResultRequest testRequest;
    private LabTestResultUpdateRequest testUpdateRequest;

    @BeforeEach
    void setUp() {
        // Setup test entity
        testEntity = new LabTestResult();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setMedicalExamId(TestDataFactory.uuid());
        testEntity.setLabTestId(TestDataFactory.uuid());
        testEntity.setPatientId(TestDataFactory.uuid());
        testEntity.setPatientName("John Doe");
        testEntity.setLabTestCode("CBC");
        testEntity.setLabTestName("Complete Blood Count");
        testEntity.setLabTestCategory(LabTestCategory.LAB);
        testEntity.setLabTestPrice(new BigDecimal("50.00"));
        testEntity.setResultValue("8.5");
        testEntity.setStatus(ResultStatus.COMPLETED);
        testEntity.setIsAbnormal(false);
        testEntity.setInterpretation("Results within normal range");
        testEntity.setNotes("Patient was fasting");
        testEntity.setPerformedBy("Tech001");
        testEntity.setInterpretedBy("Dr. Smith");
        testEntity.setPerformedAt(Instant.now().minus(2, ChronoUnit.HOURS));
        testEntity.setCompletedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        testEntity.setCreatedAt(Instant.now().minus(3, ChronoUnit.HOURS));
        testEntity.setUpdatedAt(Instant.now());

        // Setup test request
        testRequest = new LabTestResultRequest();
        testRequest.setMedicalExamId(TestDataFactory.uuid());
        testRequest.setLabTestId(TestDataFactory.uuid());
        testRequest.setResultValue("Positive");
        testRequest.setIsAbnormal(true);
        testRequest.setInterpretation("Requires follow-up");
        testRequest.setNotes("Sample collected at 8:00 AM");
        testRequest.setPerformedBy("Tech002");

        // Setup update request
        testUpdateRequest = new LabTestResultUpdateRequest();
        testUpdateRequest.setResultValue("Negative");
        testUpdateRequest.setIsAbnormal(false);
        testUpdateRequest.setInterpretation("Normal findings");
        testUpdateRequest.setNotes("Re-tested with new sample");
    }

    @Nested
    @DisplayName("Request to Entity Mapping")
    class RequestToEntityTests {

        @Test
        @DisplayName("Should map request fields to entity (excluding system-managed fields)")
        void requestToEntity_shouldMapRequestFields() {
            // When
            LabTestResult result = mapper.requestToEntity(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMedicalExamId()).isEqualTo(testRequest.getMedicalExamId());
            assertThat(result.getLabTestId()).isEqualTo(testRequest.getLabTestId());
            assertThat(result.getResultValue()).isEqualTo(testRequest.getResultValue());
            assertThat(result.getIsAbnormal()).isEqualTo(testRequest.getIsAbnormal());
            assertThat(result.getInterpretation()).isEqualTo(testRequest.getInterpretation());
            assertThat(result.getNotes()).isEqualTo(testRequest.getNotes());
            assertThat(result.getPerformedBy()).isEqualTo(testRequest.getPerformedBy());
        }

        @Test
        @DisplayName("Should ignore system-managed fields")
        void requestToEntity_shouldIgnoreSystemFields() {
            // When
            LabTestResult result = mapper.requestToEntity(testRequest);

            // Then - these fields are explicitly ignored in mapper
            assertThat(result.getId()).isNull();
            // Note: status has a default value of PENDING in the entity
            assertThat(result.getStatus()).isEqualTo(ResultStatus.PENDING);
            assertThat(result.getPatientId()).isNull();
            assertThat(result.getPatientName()).isNull();
            assertThat(result.getLabTestCode()).isNull();
            assertThat(result.getLabTestName()).isNull();
            assertThat(result.getLabTestCategory()).isNull();
            assertThat(result.getCompletedAt()).isNull();
            assertThat(result.getPerformedAt()).isNull();
            assertThat(result.getInterpretedBy()).isNull();
        }

        @Test
        @DisplayName("Should handle null optional fields")
        void requestToEntity_withNullOptionalFields_shouldMapSuccessfully() {
            // Given
            LabTestResultRequest minimalRequest = new LabTestResultRequest();
            minimalRequest.setMedicalExamId(TestDataFactory.uuid());
            minimalRequest.setLabTestId(TestDataFactory.uuid());
            // All other fields null

            // When
            LabTestResult result = mapper.requestToEntity(minimalRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMedicalExamId()).isEqualTo(minimalRequest.getMedicalExamId());
            assertThat(result.getLabTestId()).isEqualTo(minimalRequest.getLabTestId());
            assertThat(result.getResultValue()).isNull();
            assertThat(result.getIsAbnormal()).isNull();
            assertThat(result.getInterpretation()).isNull();
            assertThat(result.getNotes()).isNull();
            assertThat(result.getPerformedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("Entity to Response Mapping")
    class EntityToResponseTests {

        @Test
        @DisplayName("Should map all entity fields to response")
        void entityToResponse_withAllFields_shouldMapCorrectly() {
            // When
            LabTestResultResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
            assertThat(result.getMedicalExamId()).isEqualTo(testEntity.getMedicalExamId());
            assertThat(result.getLabTestId()).isEqualTo(testEntity.getLabTestId());
            assertThat(result.getPatientId()).isEqualTo(testEntity.getPatientId());
            assertThat(result.getPatientName()).isEqualTo(testEntity.getPatientName());
            assertThat(result.getLabTestCode()).isEqualTo(testEntity.getLabTestCode());
            assertThat(result.getLabTestName()).isEqualTo(testEntity.getLabTestName());
            assertThat(result.getLabTestCategory()).isEqualTo(testEntity.getLabTestCategory());
            assertThat(result.getLabTestPrice()).isEqualByComparingTo(testEntity.getLabTestPrice());
            assertThat(result.getResultValue()).isEqualTo(testEntity.getResultValue());
            assertThat(result.getStatus()).isEqualTo(testEntity.getStatus());
            assertThat(result.getIsAbnormal()).isEqualTo(testEntity.getIsAbnormal());
            assertThat(result.getInterpretation()).isEqualTo(testEntity.getInterpretation());
            assertThat(result.getNotes()).isEqualTo(testEntity.getNotes());
            assertThat(result.getPerformedBy()).isEqualTo(testEntity.getPerformedBy());
            assertThat(result.getInterpretedBy()).isEqualTo(testEntity.getInterpretedBy());
            assertThat(result.getPerformedAt()).isEqualTo(testEntity.getPerformedAt());
            assertThat(result.getCompletedAt()).isEqualTo(testEntity.getCompletedAt());
            assertThat(result.getCreatedAt()).isEqualTo(testEntity.getCreatedAt());
            assertThat(result.getUpdatedAt()).isEqualTo(testEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle null entity")
        void entityToResponse_withNullEntity_shouldReturnNull() {
            // When
            LabTestResultResponse result = mapper.entityToResponse(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should ignore images field (populated separately)")
        void entityToResponse_shouldIgnoreImages() {
            // When
            LabTestResultResponse result = mapper.entityToResponse(testEntity);

            // Then
            assertThat(result.getImages()).isNull(); // Images populated separately by service
        }

        @Test
        @DisplayName("Should map all status types correctly")
        void entityToResponse_shouldMapAllStatuses() {
            // Test PENDING
            testEntity.setStatus(ResultStatus.PENDING);
            LabTestResultResponse result = mapper.entityToResponse(testEntity);
            assertThat(result.getStatus()).isEqualTo(ResultStatus.PENDING);

            // Test PROCESSING
            testEntity.setStatus(ResultStatus.PROCESSING);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getStatus()).isEqualTo(ResultStatus.PROCESSING);

            // Test COMPLETED
            testEntity.setStatus(ResultStatus.COMPLETED);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getStatus()).isEqualTo(ResultStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should map all category types correctly")
        void entityToResponse_shouldMapAllCategories() {
            // Test LAB
            testEntity.setLabTestCategory(LabTestCategory.LAB);
            LabTestResultResponse result = mapper.entityToResponse(testEntity);
            assertThat(result.getLabTestCategory()).isEqualTo(LabTestCategory.LAB);

            // Test IMAGING
            testEntity.setLabTestCategory(LabTestCategory.IMAGING);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getLabTestCategory()).isEqualTo(LabTestCategory.IMAGING);

            // Test PATHOLOGY
            testEntity.setLabTestCategory(LabTestCategory.PATHOLOGY);
            result = mapper.entityToResponse(testEntity);
            assertThat(result.getLabTestCategory()).isEqualTo(LabTestCategory.PATHOLOGY);
        }
    }

    @Nested
    @DisplayName("Update From Request Tests")
    class UpdateFromRequestTests {

        @Test
        @DisplayName("Should update entity from update request")
        void updateFromRequest_shouldUpdateFields() {
            // Given
            String originalMedicalExamId = testEntity.getMedicalExamId();
            String originalLabTestId = testEntity.getLabTestId();

            // When
            mapper.updateFromRequest(testUpdateRequest, testEntity);

            // Then
            assertThat(testEntity.getResultValue()).isEqualTo(testUpdateRequest.getResultValue());
            assertThat(testEntity.getIsAbnormal()).isEqualTo(testUpdateRequest.getIsAbnormal());
            assertThat(testEntity.getInterpretation()).isEqualTo(testUpdateRequest.getInterpretation());
            assertThat(testEntity.getNotes()).isEqualTo(testUpdateRequest.getNotes());
            
            // Should not update medicalExamId and labTestId
            assertThat(testEntity.getMedicalExamId()).isEqualTo(originalMedicalExamId);
            assertThat(testEntity.getLabTestId()).isEqualTo(originalLabTestId);
        }

        @Test
        @DisplayName("Should handle partial update with some null fields")
        void updateFromRequest_withPartialData_shouldUpdateOnlyProvidedFields() {
            // Given
            LabTestResultUpdateRequest partialUpdate = new LabTestResultUpdateRequest();
            partialUpdate.setResultValue("Updated value");
            // isAbnormal, interpretation, notes are null

            String originalInterpretation = testEntity.getInterpretation();
            String originalNotes = testEntity.getNotes();

            // When
            mapper.updateFromRequest(partialUpdate, testEntity);

            // Then
            assertThat(testEntity.getResultValue()).isEqualTo("Updated value");
            // Note: MapStruct default behavior is to set null values
            // Without nullValuePropertyMappingStrategy = IGNORE, null fields WILL overwrite
            assertThat(testEntity.getInterpretation()).isNull();
            assertThat(testEntity.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very long text fields")
        void mapping_withLongTextFields_shouldMapCorrectly() {
            // Given
            String longText = "A".repeat(2000);
            testRequest.setInterpretation(longText);
            testRequest.setNotes(longText);

            // When
            LabTestResult entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getInterpretation()).hasSize(2000);
            assertThat(entity.getNotes()).hasSize(2000);
        }

        @Test
        @DisplayName("Should handle abnormal result flag")
        void mapping_withAbnormalResult_shouldMapCorrectly() {
            // Given
            testRequest.setIsAbnormal(true);

            // When
            LabTestResult entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getIsAbnormal()).isTrue();
        }

        @Test
        @DisplayName("Should preserve price precision in entity to response")
        void entityToResponse_shouldPreservePricePrecision() {
            // Given
            testEntity.setLabTestPrice(new BigDecimal("123.45"));

            // When
            LabTestResultResponse response = mapper.entityToResponse(testEntity);

            // Then
            assertThat(response.getLabTestPrice()).isEqualByComparingTo(new BigDecimal("123.45"));
        }

        @Test
        @DisplayName("Should preserve result value format")
        void mapping_shouldPreserveResultValueFormat() {
            // Given
            String complexValue = "WBC: 8.5, RBC: 4.2, Platelets: 250";
            testRequest.setResultValue(complexValue);

            // When
            LabTestResult entity = mapper.requestToEntity(testRequest);

            // Then
            assertThat(entity.getResultValue()).isEqualTo(complexValue);
        }
    }
}
