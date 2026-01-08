package com.hms.medical_exam_service.services;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.test.TestDataFactory;
import com.hms.medical_exam_service.dtos.lab.LabTestResultRequest;
import com.hms.medical_exam_service.dtos.lab.LabTestResultResponse;
import com.hms.medical_exam_service.entities.*;
import com.hms.medical_exam_service.mappers.DiagnosticImageMapper;
import com.hms.medical_exam_service.mappers.LabTestResultMapper;
import com.hms.medical_exam_service.repositories.DiagnosticImageRepository;
import com.hms.medical_exam_service.repositories.LabTestRepository;
import com.hms.medical_exam_service.repositories.LabTestResultRepository;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for LabTestResultService.
 * Tests CRUD operations for lab test result management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-EXAM-013: LabTestResultService Unit Tests")
class LabTestResultServiceTest {

    @Mock
    private LabTestResultRepository resultRepository;

    @Mock
    private LabTestRepository labTestRepository;

    @Mock
    private MedicalExamRepository medicalExamRepository;

    @Mock
    private DiagnosticImageRepository imageRepository;

    @Mock
    private LabTestResultMapper resultMapper;

    @Mock
    private DiagnosticImageMapper imageMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private LabTestResultService labTestResultService;

    private LabTestResultRequest testRequest;
    private LabTestResult testEntity;
    private LabTestResultResponse testResponse;
    private MedicalExam testExam;
    private LabTest testLabTest;

    @BeforeEach
    void setUp() {
        // Setup medical exam
        testExam = new MedicalExam();
        testExam.setId(TestDataFactory.uuid());
        testExam.setPatientId(TestDataFactory.uuid());
        testExam.setPatientName("John Doe");

        // Setup lab test
        testLabTest = new LabTest();
        testLabTest.setId(TestDataFactory.uuid());
        testLabTest.setCode("CBC");
        testLabTest.setName("Complete Blood Count");
        testLabTest.setCategory(LabTestCategory.LAB);
        testLabTest.setPrice(new BigDecimal("50.00"));

        // Setup request
        testRequest = new LabTestResultRequest();
        testRequest.setMedicalExamId(testExam.getId());
        testRequest.setLabTestId(testLabTest.getId());
        testRequest.setResultValue("8.5");
        testRequest.setIsAbnormal(false);
        testRequest.setInterpretation("Normal findings");
        testRequest.setNotes("Fasting sample");
        testRequest.setPerformedBy("Tech001");

        // Setup entity
        testEntity = new LabTestResult();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setMedicalExamId(testExam.getId());
        testEntity.setLabTestId(testLabTest.getId());
        testEntity.setPatientId(testExam.getPatientId());
        testEntity.setPatientName(testExam.getPatientName());
        testEntity.setLabTestCode(testLabTest.getCode());
        testEntity.setLabTestName(testLabTest.getName());
        testEntity.setLabTestCategory(testLabTest.getCategory());
        testEntity.setLabTestPrice(testLabTest.getPrice());
        testEntity.setStatus(ResultStatus.PENDING);
        testEntity.setIsAbnormal(false);

        // Setup response
        testResponse = new LabTestResultResponse();
        testResponse.setId(testEntity.getId());
        testResponse.setMedicalExamId(testEntity.getMedicalExamId());
        testResponse.setLabTestId(testEntity.getLabTestId());
        testResponse.setStatus(ResultStatus.PENDING);
    }

    @Nested
    @DisplayName("Method: create()")
    class CreateTests {

        @Test
        @DisplayName("Should create lab test result successfully")
        void create_withValidData_shouldCreateResult() {
            // Given
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.of(testLabTest));
            given(resultMapper.requestToEntity(testRequest)).willReturn(testEntity);
            given(resultRepository.save(any(LabTestResult.class))).willReturn(testEntity);
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(testEntity.getId())).willReturn(Arrays.asList());

            // When
            LabTestResultResponse result = labTestResultService.create(testRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testEntity.getId());
            then(medicalExamRepository).should().findById(testExam.getId());
            then(labTestRepository).should().findById(testLabTest.getId());
            then(resultRepository).should().save(any(LabTestResult.class));
        }

        @Test
        @DisplayName("Should throw exception when medical exam not found")
        void create_withNonExistentExam_shouldThrowException() {
            // Given
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> labTestResultService.create(testRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Medical exam not found");
            
            then(labTestRepository).should(never()).findById(anyString());
            then(resultRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when lab test not found")
        void create_withNonExistentLabTest_shouldThrowException() {
            // Given
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> labTestResultService.create(testRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Lab test not found");
            
            then(resultRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should set status to PENDING on create")
        void create_shouldSetStatusToPending() {
            // Given
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.of(testLabTest));
            given(resultMapper.requestToEntity(testRequest)).willReturn(testEntity);
            given(resultRepository.save(any(LabTestResult.class))).willReturn(testEntity);
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(testEntity.getId())).willReturn(Arrays.asList());

            // When
            labTestResultService.create(testRequest);

            // Then
            then(resultRepository).should().save(argThat(result -> 
                result.getStatus() == ResultStatus.PENDING
            ));
        }

        @Test
        @DisplayName("Should copy denormalized data from exam and lab test")
        void create_shouldCopyDenormalizedData() {
            // Given
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.of(testLabTest));
            given(resultMapper.requestToEntity(testRequest)).willReturn(testEntity);
            given(resultRepository.save(any(LabTestResult.class))).willReturn(testEntity);
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(testEntity.getId())).willReturn(Arrays.asList());

            // When
            labTestResultService.create(testRequest);

            // Then
            then(resultRepository).should().save(argThat(result -> 
                result.getPatientId().equals(testExam.getPatientId()) &&
                result.getPatientName().equals(testExam.getPatientName()) &&
                result.getLabTestCode().equals(testLabTest.getCode()) &&
                result.getLabTestName().equals(testLabTest.getName())
            ));
        }

        @Test
        @DisplayName("Should default isAbnormal to false when null")
        void create_withNullIsAbnormal_shouldDefaultToFalse() {
            // Given
            testEntity.setIsAbnormal(null); // Simulate null from mapper
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.of(testLabTest));
            given(resultMapper.requestToEntity(testRequest)).willReturn(testEntity);
            given(resultRepository.save(any(LabTestResult.class))).willReturn(testEntity);
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(testEntity.getId())).willReturn(Arrays.asList());

            // When
            labTestResultService.create(testRequest);

            // Then
            then(resultRepository).should().save(argThat(result -> 
                result.getIsAbnormal() != null && result.getIsAbnormal() == false
            ));
        }
    }

    @Nested
    @DisplayName("Method: findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return lab test result when found by ID")
        void findById_withExistingId_shouldReturnResult() {
            // Given
            String resultId = testEntity.getId();
            given(resultRepository.findById(resultId)).willReturn(Optional.of(testEntity));
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(resultId)).willReturn(Arrays.asList());

            // When
            LabTestResultResponse result = labTestResultService.findById(resultId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(resultId);
            then(resultRepository).should().findById(resultId);
        }

        @Test
        @DisplayName("Should throw exception when result not found")
        void findById_withNonExistentId_shouldThrowException() {
            // Given
            String nonExistentId = TestDataFactory.uuid();
            given(resultRepository.findById(nonExistentId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> labTestResultService.findById(nonExistentId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Method: findByMedicalExam()")
    class FindByMedicalExamTests {

        @Test
        @DisplayName("Should return all results for a medical exam")
        void findByMedicalExam_shouldReturnAllResults() {
            // Given
            String examId = testExam.getId();
            
            LabTestResult result1 = new LabTestResult();
            result1.setId(TestDataFactory.uuid());
            result1.setMedicalExamId(examId);
            
            LabTestResult result2 = new LabTestResult();
            result2.setId(TestDataFactory.uuid());
            result2.setMedicalExamId(examId);
            
            List<LabTestResult> results = Arrays.asList(result1, result2);

            given(resultRepository.findByMedicalExamId(examId)).willReturn(results);
            given(resultMapper.entityToResponse(any())).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(anyString())).willReturn(Arrays.asList());

            // When
            List<LabTestResultResponse> resultList = labTestResultService.findByMedicalExam(examId);

            // Then
            assertThat(resultList).hasSize(2);
            then(resultRepository).should().findByMedicalExamId(examId);
        }

        @Test
        @DisplayName("Should return empty list when no results for exam")
        void findByMedicalExam_withNoResults_shouldReturnEmptyList() {
            // Given
            String examId = testExam.getId();
            given(resultRepository.findByMedicalExamId(examId)).willReturn(Arrays.asList());

            // When
            List<LabTestResultResponse> resultList = labTestResultService.findByMedicalExam(examId);

            // Then
            assertThat(resultList).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: findByPatient()")
    class FindByPatientTests {

        @Test
        @DisplayName("Should return all results for a patient")
        void findByPatient_shouldReturnAllResults() {
            // Given
            String patientId = testExam.getPatientId();
            
            LabTestResult result1 = new LabTestResult();
            result1.setId(TestDataFactory.uuid());
            result1.setPatientId(patientId);
            
            List<LabTestResult> results = Arrays.asList(result1);

            given(resultRepository.findByPatientId(patientId)).willReturn(results);
            given(resultMapper.entityToResponse(any())).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(anyString())).willReturn(Arrays.asList());

            // When
            List<LabTestResultResponse> resultList = labTestResultService.findByPatient(patientId);

            // Then
            assertThat(resultList).hasSize(1);
            then(resultRepository).should().findByPatientId(patientId);
        }

        @Test
        @DisplayName("Should return empty list when patient has no results")
        void findByPatient_withNoResults_shouldReturnEmptyList() {
            // Given
            String patientId = TestDataFactory.uuid();
            given(resultRepository.findByPatientId(patientId)).willReturn(Arrays.asList());

            // When
            List<LabTestResultResponse> resultList = labTestResultService.findByPatient(patientId);

            // Then
            assertThat(resultList).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle creating result with minimal data")
        void create_withMinimalData_shouldSucceed() {
            // Given
            LabTestResultRequest minimalRequest = new LabTestResultRequest();
            minimalRequest.setMedicalExamId(testExam.getId());
            minimalRequest.setLabTestId(testLabTest.getId());

            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.of(testLabTest));
            given(resultMapper.requestToEntity(minimalRequest)).willReturn(testEntity);
            given(resultRepository.save(any(LabTestResult.class))).willReturn(testEntity);
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(testEntity.getId())).willReturn(Arrays.asList());

            // When
            LabTestResultResponse result = labTestResultService.create(minimalRequest);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should preserve lab test price snapshot")
        void create_shouldPreserveLabTestPrice() {
            // Given
            BigDecimal expectedPrice = new BigDecimal("75.50");
            testLabTest.setPrice(expectedPrice);
            
            given(medicalExamRepository.findById(testExam.getId())).willReturn(Optional.of(testExam));
            given(labTestRepository.findById(testLabTest.getId())).willReturn(Optional.of(testLabTest));
            given(resultMapper.requestToEntity(testRequest)).willReturn(testEntity);
            given(resultRepository.save(any(LabTestResult.class))).willReturn(testEntity);
            given(resultMapper.entityToResponse(testEntity)).willReturn(testResponse);
            given(imageRepository.findByLabTestResultIdOrderBySequenceNumberAsc(testEntity.getId())).willReturn(Arrays.asList());

            // When
            labTestResultService.create(testRequest);

            // Then
            then(resultRepository).should().save(argThat(result -> 
                result.getLabTestPrice().compareTo(expectedPrice) == 0
            ));
        }
    }
}
