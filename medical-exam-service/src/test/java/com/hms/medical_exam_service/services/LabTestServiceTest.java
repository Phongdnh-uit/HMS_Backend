package com.hms.medical_exam_service.services;

import com.hms.medical_exam_service.dtos.lab.LabTestResponse;
import com.hms.medical_exam_service.entities.LabTest;
import com.hms.medical_exam_service.entities.LabTestCategory;
import com.hms.medical_exam_service.mappers.LabTestMapper;
import com.hms.medical_exam_service.repositories.LabTestRepository;
import com.hms.common.test.TestDataFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for LabTestService.
 * Tests CRUD operations for lab test catalog management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-EXAM-012: LabTestService Unit Tests")
class LabTestServiceTest {

    @Mock
    private LabTestRepository repository;

    @Mock
    private LabTestMapper mapper;

    @InjectMocks
    private LabTestService labTestService;

    private LabTest testEntity;
    private LabTestResponse testResponse;

    @BeforeEach
    void setUp() {
        testEntity = new LabTest();
        testEntity.setId(TestDataFactory.uuid());
        testEntity.setCode("CBC");
        testEntity.setName("Complete Blood Count");
        testEntity.setCategory(LabTestCategory.LAB);
        testEntity.setDescription("Measures various components of blood");
        testEntity.setPrice(new BigDecimal("50.00"));
        testEntity.setUnit("cells/μL");
        testEntity.setNormalRange("4.5-11.0");
        testEntity.setIsActive(true);

        testResponse = new LabTestResponse();
        testResponse.setId(testEntity.getId());
        testResponse.setCode(testEntity.getCode());
        testResponse.setName(testEntity.getName());
        testResponse.setCategory(testEntity.getCategory());
        testResponse.setPrice(testEntity.getPrice());
        testResponse.setIsActive(true);
    }

    @Nested
    @DisplayName("Method: findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return lab test when found by ID")
        void findById_withExistingId_shouldReturnLabTest() {
            // Given
            String testId = testEntity.getId();
            given(repository.findById(testId)).willReturn(Optional.of(testEntity));

            // When
            Optional<LabTest> result = labTestService.findById(testId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testId);
            assertThat(result.get().getCode()).isEqualTo("CBC");
            then(repository).should().findById(testId);
        }

        @Test
        @DisplayName("Should return empty when lab test not found")
        void findById_withNonExistentId_shouldReturnEmpty() {
            // Given
            String nonExistentId = TestDataFactory.uuid();
            given(repository.findById(nonExistentId)).willReturn(Optional.empty());

            // When
            Optional<LabTest> result = labTestService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            then(repository).should().findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Method: findByCode()")
    class FindByCodeTests {

        @Test
        @DisplayName("Should return lab test response when found by code")
        void findByCode_withExistingCode_shouldReturnResponse() {
            // Given
            String code = "CBC";
            given(repository.findByCode(code)).willReturn(Optional.of(testEntity));
            given(mapper.entityToResponse(testEntity)).willReturn(testResponse);

            // When
            LabTestResponse result = labTestService.findByCode(code);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(code);
            then(repository).should().findByCode(code);
            then(mapper).should().entityToResponse(testEntity);
        }

        @Test
        @DisplayName("Should return null when lab test not found by code")
        void findByCode_withNonExistentCode_shouldReturnNull() {
            // Given
            String nonExistentCode = "INVALID";
            given(repository.findByCode(nonExistentCode)).willReturn(Optional.empty());

            // When
            LabTestResponse result = labTestService.findByCode(nonExistentCode);

            // Then
            assertThat(result).isNull();
            then(repository).should().findByCode(nonExistentCode);
            then(mapper).should(never()).entityToResponse(any());
        }
    }

    @Nested
    @DisplayName("Method: findAllActive()")
    class FindAllActiveTests {

        @Test
        @DisplayName("Should return all active lab tests")
        void findAllActive_shouldReturnActiveTests() {
            // Given
            LabTest activeTest1 = new LabTest();
            activeTest1.setId(TestDataFactory.uuid());
            activeTest1.setCode("CBC");
            activeTest1.setName("Complete Blood Count");
            activeTest1.setIsActive(true);

            LabTest activeTest2 = new LabTest();
            activeTest2.setId(TestDataFactory.uuid());
            activeTest2.setCode("XRAY_CHEST");
            activeTest2.setName("X-Ray Chest");
            activeTest2.setIsActive(true);

            List<LabTest> activeTests = Arrays.asList(activeTest1, activeTest2);

            LabTestResponse response1 = new LabTestResponse();
            response1.setId(activeTest1.getId());
            response1.setCode("CBC");

            LabTestResponse response2 = new LabTestResponse();
            response2.setId(activeTest2.getId());
            response2.setCode("XRAY_CHEST");

            given(repository.findByIsActiveTrue()).willReturn(activeTests);
            given(mapper.entityToResponse(activeTest1)).willReturn(response1);
            given(mapper.entityToResponse(activeTest2)).willReturn(response2);

            // When
            List<LabTestResponse> result = labTestService.findAllActive();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("CBC");
            assertThat(result.get(1).getCode()).isEqualTo("XRAY_CHEST");
            then(repository).should().findByIsActiveTrue();
            then(mapper).should(times(2)).entityToResponse(any(LabTest.class));
        }

        @Test
        @DisplayName("Should return empty list when no active tests exist")
        void findAllActive_withNoActiveTests_shouldReturnEmptyList() {
            // Given
            given(repository.findByIsActiveTrue()).willReturn(Arrays.asList());

            // When
            List<LabTestResponse> result = labTestService.findAllActive();

            // Then
            assertThat(result).isEmpty();
            then(repository).should().findByIsActiveTrue();
        }
    }

    @Nested
    @DisplayName("Method: findByCategory()")
    class FindByCategoryTests {

        @Test
        @DisplayName("Should return lab tests for LAB category")
        void findByCategory_withLabCategory_shouldReturnLabTests() {
            // Given
            LabTest labTest = new LabTest();
            labTest.setId(TestDataFactory.uuid());
            labTest.setCode("CBC");
            labTest.setCategory(LabTestCategory.LAB);
            labTest.setIsActive(true);

            LabTestResponse response = new LabTestResponse();
            response.setId(labTest.getId());
            response.setCode("CBC");
            response.setCategory(LabTestCategory.LAB);

            given(repository.findByCategoryAndIsActiveTrue(LabTestCategory.LAB))
                .willReturn(Arrays.asList(labTest));
            given(mapper.entityToResponse(labTest)).willReturn(response);

            // When
            List<LabTestResponse> result = labTestService.findByCategory(LabTestCategory.LAB);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(LabTestCategory.LAB);
            then(repository).should().findByCategoryAndIsActiveTrue(LabTestCategory.LAB);
        }

        @Test
        @DisplayName("Should return imaging tests for IMAGING category")
        void findByCategory_withImagingCategory_shouldReturnImagingTests() {
            // Given
            LabTest imagingTest = new LabTest();
            imagingTest.setId(TestDataFactory.uuid());
            imagingTest.setCode("XRAY_CHEST");
            imagingTest.setCategory(LabTestCategory.IMAGING);
            imagingTest.setIsActive(true);

            LabTestResponse response = new LabTestResponse();
            response.setId(imagingTest.getId());
            response.setCode("XRAY_CHEST");
            response.setCategory(LabTestCategory.IMAGING);

            given(repository.findByCategoryAndIsActiveTrue(LabTestCategory.IMAGING))
                .willReturn(Arrays.asList(imagingTest));
            given(mapper.entityToResponse(imagingTest)).willReturn(response);

            // When
            List<LabTestResponse> result = labTestService.findByCategory(LabTestCategory.IMAGING);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategory()).isEqualTo(LabTestCategory.IMAGING);
        }

        @Test
        @DisplayName("Should return empty list when no tests in category")
        void findByCategory_withNoTestsInCategory_shouldReturnEmptyList() {
            // Given
            given(repository.findByCategoryAndIsActiveTrue(LabTestCategory.PATHOLOGY))
                .willReturn(Arrays.asList());

            // When
            List<LabTestResponse> result = labTestService.findByCategory(LabTestCategory.PATHOLOGY);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: existsByCode()")
    class ExistsByCodeTests {

        @Test
        @DisplayName("Should return true when code exists")
        void existsByCode_withExistingCode_shouldReturnTrue() {
            // Given
            String code = "CBC";
            given(repository.existsByCode(code)).willReturn(true);

            // When
            boolean result = labTestService.existsByCode(code);

            // Then
            assertThat(result).isTrue();
            then(repository).should().existsByCode(code);
        }

        @Test
        @DisplayName("Should return false when code does not exist")
        void existsByCode_withNonExistentCode_shouldReturnFalse() {
            // Given
            String code = "INVALID";
            given(repository.existsByCode(code)).willReturn(false);

            // When
            boolean result = labTestService.existsByCode(code);

            // Then
            assertThat(result).isFalse();
            then(repository).should().existsByCode(code);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle multiple tests with same category")
        void findByCategory_withMultipleTests_shouldReturnAll() {
            // Given
            LabTest test1 = new LabTest();
            test1.setId(TestDataFactory.uuid());
            test1.setCode("CBC");
            test1.setCategory(LabTestCategory.LAB);

            LabTest test2 = new LabTest();
            test2.setId(TestDataFactory.uuid());
            test2.setCode("GLUCOSE");
            test2.setCategory(LabTestCategory.LAB);

            given(repository.findByCategoryAndIsActiveTrue(LabTestCategory.LAB))
                .willReturn(Arrays.asList(test1, test2));
            given(mapper.entityToResponse(any(LabTest.class)))
                .willReturn(testResponse);

            // When
            List<LabTestResponse> result = labTestService.findByCategory(LabTestCategory.LAB);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should only return active tests in category")
        void findByCategory_shouldOnlyReturnActiveTests() {
            // Given
            LabTest activeTest = new LabTest();
            activeTest.setIsActive(true);
            activeTest.setCategory(LabTestCategory.LAB);

            given(repository.findByCategoryAndIsActiveTrue(LabTestCategory.LAB))
                .willReturn(Arrays.asList(activeTest));
            given(mapper.entityToResponse(activeTest)).willReturn(testResponse);

            // When
            List<LabTestResponse> result = labTestService.findByCategory(LabTestCategory.LAB);

            // Then
            assertThat(result).hasSize(1);
            // Verify repository method filters by isActive=true
            then(repository).should().findByCategoryAndIsActiveTrue(LabTestCategory.LAB);
        }
    }
}
