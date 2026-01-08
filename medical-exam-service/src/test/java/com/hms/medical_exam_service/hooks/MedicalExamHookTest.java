package com.hms.medical_exam_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.test.TestDataFactory;
import com.hms.medical_exam_service.clients.BillingClient;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.dtos.external.AppointmentResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import com.hms.medical_exam_service.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for MedicalExamHook.
 * Tests lifecycle hooks for MedicalExam entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-EXAM-008/009/010: MedicalExamHook Unit Tests")
class MedicalExamHookTest {

    @Mock
    private MedicalExamRepository medicalExamRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private BillingClient billingClient;

    @InjectMocks
    private MedicalExamHook medicalExamHook;

    private MedicalExamRequest testRequest;
    private MedicalExam testEntity;
    private Map<String, Object> context;
    private String testAppointmentId;
    private String testExamId;

    @BeforeEach
    void setUp() {
        context = new HashMap<>();
        testAppointmentId = TestDataFactory.uuid();
        testExamId = TestDataFactory.uuid();

        testRequest = new MedicalExamRequest();
        testRequest.setAppointmentId(testAppointmentId);
        testRequest.setDiagnosis("Common cold");
        testRequest.setSymptoms("Fever, cough");
        testRequest.setTreatment("Rest and hydration");
        testRequest.setTemperature(37.5);
        testRequest.setBloodPressureSystolic(120);
        testRequest.setBloodPressureDiastolic(80);
        testRequest.setHeartRate(72);
        testRequest.setNotes("Patient should return if symptoms worsen");

        testEntity = new MedicalExam();
        testEntity.setId(testExamId);
        testEntity.setAppointmentId(testAppointmentId);
        testEntity.setPatientId(TestDataFactory.uuid());
        testEntity.setPatientName("John Doe");
        testEntity.setDoctorId(TestDataFactory.uuid());
        testEntity.setDoctorName("Dr. Smith");
        testEntity.setDiagnosis("Common cold");
        testEntity.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        testEntity.setExamDate(Instant.now());
    }

    @Nested
    @DisplayName("UC-EXAM-008: Method: enrichFindAll()")
    class EnrichFindAllTests {

        @Test
        @DisplayName("Should populate hasPrescription flag for all exams")
        void enrichFindAll_shouldPopulateHasPrescriptionForAllExams() {
            // Given
            MedicalExamResponse exam1 = new MedicalExamResponse();
            exam1.setId(TestDataFactory.uuid());
            
            MedicalExamResponse exam2 = new MedicalExamResponse();
            exam2.setId(TestDataFactory.uuid());
            
            List<MedicalExamResponse> content = new ArrayList<>();
            content.add(exam1);
            content.add(exam2);
            
            PageResponse<MedicalExamResponse> response = new PageResponse<>();
            response.setContent(content);
            
            given(prescriptionRepository.existsByMedicalExamId(exam1.getId())).willReturn(true);
            given(prescriptionRepository.existsByMedicalExamId(exam2.getId())).willReturn(false);

            // When
            medicalExamHook.enrichFindAll(response);

            // Then
            assertThat(exam1.getHasPrescription()).isTrue();
            assertThat(exam2.getHasPrescription()).isFalse();
            then(prescriptionRepository).should(times(2)).existsByMedicalExamId(anyString());
        }

        @Test
        @DisplayName("Should handle empty response list")
        void enrichFindAll_withEmptyList_shouldNotCallRepository() {
            // Given
            PageResponse<MedicalExamResponse> emptyResponse = new PageResponse<>();
            emptyResponse.setContent(new ArrayList<>());

            // When
            medicalExamHook.enrichFindAll(emptyResponse);

            // Then
            then(prescriptionRepository).should(never()).existsByMedicalExamId(anyString());
        }
    }

    @Nested
    @DisplayName("UC-EXAM-008: Method: enrichFindById()")
    class EnrichFindByIdTests {

        @Test
        @DisplayName("Should populate hasPrescription flag for single exam")
        void enrichFindById_shouldPopulateHasPrescription() {
            // Given
            MedicalExamResponse response = new MedicalExamResponse();
            response.setId(testExamId);
            
            given(prescriptionRepository.existsByMedicalExamId(testExamId)).willReturn(true);

            // When
            medicalExamHook.enrichFindById(response);

            // Then
            assertThat(response.getHasPrescription()).isTrue();
            then(prescriptionRepository).should().existsByMedicalExamId(testExamId);
        }

        @Test
        @DisplayName("Should set hasPrescription to false when no prescription exists")
        void enrichFindById_withNoPrescription_shouldSetFalse() {
            // Given
            MedicalExamResponse response = new MedicalExamResponse();
            response.setId(testExamId);
            
            given(prescriptionRepository.existsByMedicalExamId(testExamId)).willReturn(false);

            // When
            medicalExamHook.enrichFindById(response);

            // Then
            assertThat(response.getHasPrescription()).isFalse();
        }
    }

    @Nested
    @DisplayName("UC-EXAM-009: Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("Should throw exception when exam already exists for appointment")
        void validateCreate_withExistingExam_shouldThrowException() {
            // Given
            given(medicalExamRepository.existsByAppointmentId(testAppointmentId)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> medicalExamHook.validateCreate(testRequest, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Medical exam already exists for appointment");
        }

        @Test
        @DisplayName("Should pass validation when appointment exists and exam does not exist")
        void validateCreate_withValidData_shouldPassAndStoreAppointmentInContext() {
            // Given
            given(medicalExamRepository.existsByAppointmentId(testAppointmentId)).willReturn(false);
            // Mock appointment response will be created by the hook using AppointmentResponse.createMock()

            // When
            medicalExamHook.validateCreate(testRequest, context);

            // Then
            then(medicalExamRepository).should().existsByAppointmentId(testAppointmentId);
            assertThat(context).containsKey("appointment");
            assertThat(context.get("appointment")).isNotNull();
        }
    }

    @Nested
    @DisplayName("UC-EXAM-009: Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("Should set examDate and copy snapshot data from appointment")
        void enrichCreate_shouldPopulateSnapshotData() {
            // Given
            AppointmentResponse mockAppointment = AppointmentResponse.createMock(testAppointmentId);
            context.put("appointment", mockAppointment);

            // When
            medicalExamHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getExamDate()).isNotNull();
            assertThat(testEntity.getPatientId()).isEqualTo(mockAppointment.patientId());
            assertThat(testEntity.getPatientName()).isEqualTo(mockAppointment.patientName());
            assertThat(testEntity.getDoctorId()).isEqualTo(mockAppointment.doctorId());
            assertThat(testEntity.getDoctorName()).isEqualTo(mockAppointment.doctorName());
        }

        @Test
        @DisplayName("Should set hasPrescription from request")
        void enrichCreate_shouldSetHasPrescriptionFlag() {
            // Given
            testRequest.setHasPrescription(true);
            AppointmentResponse mockAppointment = AppointmentResponse.createMock(testAppointmentId);
            context.put("appointment", mockAppointment);

            // When
            medicalExamHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getHasPrescription()).isTrue();
        }

        @Test
        @DisplayName("Should default hasPrescription to false when not specified")
        void enrichCreate_withNullHasPrescription_shouldDefaultToFalse() {
            // Given
            testRequest.setHasPrescription(null);
            AppointmentResponse mockAppointment = AppointmentResponse.createMock(testAppointmentId);
            context.put("appointment", mockAppointment);

            // When
            medicalExamHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getHasPrescription()).isFalse();
        }

        @Test
        @DisplayName("Should handle missing appointment in context gracefully")
        void enrichCreate_withMissingAppointment_shouldNotThrowException() {
            // Given
            context.clear(); // No appointment in context

            // When & Then - Should not throw exception
            assertThatCode(() -> medicalExamHook.enrichCreate(testRequest, testEntity, context))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("UC-EXAM-009: Method: afterCreate()")
    class AfterCreateTests {

        @Test
        @DisplayName("Should log successful creation")
        void afterCreate_shouldCompleteSuccessfully() {
            // Given
            MedicalExamResponse response = new MedicalExamResponse();
            response.setId(testExamId);

            // When & Then - Should not throw exception
            assertThatCode(() -> medicalExamHook.afterCreate(testEntity, response, context))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("UC-EXAM-009: Method: validateUpdate()")
    class ValidateUpdateTests {

        @Test
        @DisplayName("Should throw exception when updating exam older than 24 hours")
        void validateUpdate_afterModificationWindow_shouldThrowException() {
            // Given
            testEntity.setCreatedAt(Instant.now().minus(25, ChronoUnit.HOURS));

            // When & Then
            assertThatThrownBy(() -> medicalExamHook.validateUpdate(testExamId, testRequest, testEntity, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot be modified after 24 hours");
        }

        @Test
        @DisplayName("Should pass validation when updating exam within 24 hours")
        void validateUpdate_withinModificationWindow_shouldPass() {
            // Given
            testEntity.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS));

            // When & Then
            assertThatCode(() -> medicalExamHook.validateUpdate(testExamId, testRequest, testEntity, context))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass validation when createdAt is null")
        void validateUpdate_withNullCreatedAt_shouldPass() {
            // Given
            testEntity.setCreatedAt(null);

            // When & Then
            assertThatCode(() -> medicalExamHook.validateUpdate(testExamId, testRequest, testEntity, context))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass validation for newly created exam")
        void validateUpdate_forNewExam_shouldPass() {
            // Given
            testEntity.setCreatedAt(Instant.now().minus(1, ChronoUnit.HOURS));

            // When & Then
            assertThatCode(() -> medicalExamHook.validateUpdate(testExamId, testRequest, testEntity, context))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("UC-EXAM-010: Method: validateDelete()")
    class ValidateDeleteTests {

        @Test
        @DisplayName("Should throw exception when attempting to delete exam")
        void validateDelete_shouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> medicalExamHook.validateDelete(testExamId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Medical exams cannot be deleted");
        }

        @Test
        @DisplayName("Should prevent deletion for audit/legal compliance")
        void validateDelete_shouldBlockForAuditCompliance() {
            // When & Then
            assertThatThrownBy(() -> medicalExamHook.validateDelete(testExamId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("audit/legal requirement");
        }
    }

    @Nested
    @DisplayName("UC-EXAM-010: Method: validateBulkDelete()")
    class ValidateBulkDeleteTests {

        @Test
        @DisplayName("Should throw exception when attempting bulk delete")
        void validateBulkDelete_shouldThrowException() {
            // Given
            List<String> ids = List.of(TestDataFactory.uuid(), TestDataFactory.uuid());

            // When & Then
            assertThatThrownBy(() -> medicalExamHook.validateBulkDelete(ids))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Medical exams cannot be deleted");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exam at exactly 24 hours")
        void validateUpdate_atExactly24Hours_shouldPass() {
            // Given
            testEntity.setCreatedAt(Instant.now().minus(24, ChronoUnit.HOURS));

            // When & Then - At exactly 24 hours should pass (only > 24 hours throws)
            assertThatCode(() -> medicalExamHook.validateUpdate(testExamId, testRequest, testEntity, context))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle exam just under 24 hours")
        void validateUpdate_justUnder24Hours_shouldPass() {
            // Given - 23 hours 59 minutes
            testEntity.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES));

            // When & Then
            assertThatCode(() -> medicalExamHook.validateUpdate(testExamId, testRequest, testEntity, context))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should enrich response with hasPrescription correctly")
        void enrichFindById_withPrescription_shouldSetCorrectFlag() {
            // Given
            MedicalExamResponse response = new MedicalExamResponse();
            response.setId(testExamId);
            response.setHasPrescription(null); // Initially null
            
            given(prescriptionRepository.existsByMedicalExamId(testExamId)).willReturn(true);

            // When
            medicalExamHook.enrichFindById(response);

            // Then
            assertThat(response.getHasPrescription())
                .isNotNull()
                .isTrue();
        }
    }
}
