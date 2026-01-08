package com.hms.billing_service.hooks;

import com.hms.billing_service.clients.MedicalExamClient;
import com.hms.billing_service.clients.MedicalExamClient.MedicalExamResponse;
import com.hms.billing_service.clients.MedicalExamClient.PrescriptionResponse;
import com.hms.billing_service.clients.MedicalExamClient.PrescriptionItemResponse;
import com.hms.billing_service.clients.MedicalExamClient.LabTestResultResponse;
import com.hms.billing_service.clients.AppointmentClient;
import com.hms.billing_service.clients.PatientClient;
import com.hms.billing_service.clients.HrClient;
import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceHook.
 * 
 * Tests verify:
 * 1. Validation logic for invoice creation
 * 2. Context enrichment with external service data
 * 3. Error handling for service failures
 * 4. Distinction between data not found vs service errors
 */
@ExtendWith(MockitoExtension.class)
class InvoiceHookTest {

    @InjectMocks
    private InvoiceHook invoiceHook;

    @Mock
    private MedicalExamClient medicalExamClient;

    @Mock
    private AppointmentClient appointmentClient;

    @Mock
    private PatientClient patientClient;

    @Mock
    private HrClient hrClient;

    @Mock
    private InvoiceRepository invoiceRepository;

    private static final String APPOINTMENT_ID = "apt-123";
    private static final String EXAM_ID = "exam-456";
    private static final String PATIENT_ID = "patient-789";

    @BeforeEach
    void setUp() {
        // Default: no existing invoice
        when(invoiceRepository.findByAppointmentId(anyString())).thenReturn(Optional.empty());
    }

    // ========================================================================
    // TEST DATA BUILDERS
    // ========================================================================

    private InvoiceRequest createInvoiceRequest() {
        InvoiceRequest request = new InvoiceRequest();
        request.setAppointmentId(APPOINTMENT_ID);
        request.setExamId(EXAM_ID);
        return request;
    }

    private MedicalExamResponse createMockExam() {
        return new MedicalExamResponse(
            EXAM_ID,
            new MedicalExamResponse.AppointmentInfo(APPOINTMENT_ID, null),
            new MedicalExamResponse.PatientInfo(PATIENT_ID, "John Doe"),
            new MedicalExamResponse.DoctorInfo("doc-1", "Dr. Smith"),
            "Common cold",
            Instant.now(),
            Instant.now()
        );
    }

    private PrescriptionResponse createMockPrescription() {
        return new PrescriptionResponse(
            "presc-1",
            new PrescriptionResponse.MedicalExamInfo(EXAM_ID),
            new PrescriptionResponse.PatientInfo(PATIENT_ID, "John Doe"),
            new PrescriptionResponse.DoctorInfo("doc-1", "Dr. Smith"),
            "ACTIVE",
            Instant.now(),
            "Take with food",
            List.of(
                new PrescriptionItemResponse(
                    "item-1",
                    new PrescriptionItemResponse.MedicineInfo("med-1", "Paracetamol"),
                    10,
                    new BigDecimal("5000"),
                    "500mg",
                    5,
                    "After meals"
                )
            )
        );
    }

    private HrClient.EmployeeResponse createMockDoctor() {
        return new HrClient.EmployeeResponse(
            "doc-1",
            "Dr. Smith",
            "dr.smith@hospital.com",
            new HrClient.EmployeeResponse.DepartmentInfo("dept-1", "Internal Medicine", new BigDecimal("200000"))
        );
    }

    // ========================================================================
    // 1. EXAM FETCH TESTS
    // ========================================================================

    @Nested
    @DisplayName("1. Exam Fetch Tests")
    class ExamFetchTests {

        @Test
        @DisplayName("Should throw EXAM_NOT_FOUND when exam doesn't exist")
        void shouldThrowExamNotFound_whenExamDoesNotExist() {
            // Given: service responds but exam not found
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(null)); // Service up, but no data

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When & Then
            ApiException exception = assertThrows(ApiException.class,
                () -> invoiceHook.validateCreate(request, context));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXAM_NOT_FOUND);
        }

        @Test
        @DisplayName("Should put exam in context when found")
        void shouldPutExamInContext_whenFound() {
            // Given
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            invoiceHook.validateCreate(request, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_EXAM)).isEqualTo(exam);
        }

        @Test
        @DisplayName("Should use examId from request when provided")
        void shouldUseExamIdFromRequest_whenProvided() {
            // Given
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            request.setExamId(EXAM_ID);
            Map<String, Object> context = new HashMap<>();

            // When
            invoiceHook.validateCreate(request, context);

            // Then: should call getExamById, not getExamByAppointment
            verify(medicalExamClient).getExamById(EXAM_ID);
            verify(medicalExamClient, never()).getExamByAppointment(anyString());
        }
    }

    // ========================================================================
    // 2. PRESCRIPTION FETCH TESTS
    // ========================================================================

    @Nested
    @DisplayName("2. Prescription Fetch Tests")
    class PrescriptionFetchTests {

        @Test
        @DisplayName("Should continue when no prescription exists (consultation-only)")
        void shouldContinue_whenNoPrescriptionExists() {
            // Given: exam exists but no prescription
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null)); // No prescription
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When: should NOT throw
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then: prescription should be null (consultation-only invoice)
            assertThat(context.get(InvoiceHook.CONTEXT_PRESCRIPTION)).isNull();
        }

        @Test
        @DisplayName("Should put prescription in context when found")
        void shouldPutPrescriptionInContext_whenFound() {
            // Given
            var exam = createMockExam();
            var prescription = createMockPrescription();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(prescription));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            invoiceHook.validateCreate(request, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_PRESCRIPTION)).isEqualTo(prescription);
        }

        @Test
        @DisplayName("Should handle prescription service failure gracefully")
        void shouldHandlePrescriptionServiceFailure_gracefully() {
            // Given: exam works but prescription throws
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenThrow(new RuntimeException("Service down"));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When: should NOT throw - graceful degradation
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then: exam present, no prescription (creates consultation-only invoice)
            assertThat(context.get(InvoiceHook.CONTEXT_EXAM)).isEqualTo(exam);
            assertThat(context.get(InvoiceHook.CONTEXT_PRESCRIPTION)).isNull();
        }
    }

    // ========================================================================
    // 3. LAB TESTS FETCH TESTS
    // ========================================================================

    @Nested
    @DisplayName("3. Lab Tests Fetch Tests")
    class LabTestsFetchTests {

        @Test
        @DisplayName("Should put lab tests in context when found")
        void shouldPutLabTestsInContext_whenFound() {
            // Given
            var exam = createMockExam();
            // LabTestResultResponse(id, labTestId, labTestCode, labTestName, labTestPrice, status, createdAt)
            var labTests = List.of(
                new LabTestResultResponse(
                    "lab-1", "lt-1", "BT001", "Blood Test", 
                    new BigDecimal("50000"), "COMPLETED", Instant.now()
                )
            );
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(labTests));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            invoiceHook.validateCreate(request, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_LAB_TESTS)).isEqualTo(labTests);
        }

        @Test
        @DisplayName("Should continue when no lab tests exist")
        void shouldContinue_whenNoLabTestsExist() {
            // Given
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When: should NOT throw
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then: no lab tests in context
            assertThat(context.get(InvoiceHook.CONTEXT_LAB_TESTS)).isNull();
        }
    }

    // ========================================================================
    // 4. CONSULTATION FEE TESTS
    // ========================================================================

    @Nested
    @DisplayName("4. Consultation Fee Tests")
    class ConsultationFeeTests {

        @Test
        @DisplayName("Should fetch consultation fee from HR service")
        void shouldFetchConsultationFee_fromHrService() {
            // Given
            var exam = createMockExam();
            var doctor = createMockDoctor();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));
            when(hrClient.getEmployeeById("doc-1"))
                .thenReturn(ApiResponse.ok(doctor));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            invoiceHook.validateCreate(request, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_CONSULTATION_FEE))
                .isEqualTo(new BigDecimal("200000"));
        }

        @Test
        @DisplayName("Should use default fee when HR service fails (graceful degradation)")
        void shouldUseDefaultFee_whenHrServiceFails() {
            // Given: HR service fails
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));
            when(hrClient.getEmployeeById(anyString()))
                .thenThrow(new RuntimeException("HR service down"));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When: should NOT throw - HR failure is graceful
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then: consultation fee not set (will use default in enrichCreate)
            assertThat(context.get(InvoiceHook.CONTEXT_CONSULTATION_FEE)).isNull();
        }
    }

    // ========================================================================
    // 5. EXISTING INVOICE TESTS
    // ========================================================================

    @Nested
    @DisplayName("5. Existing Invoice Tests")
    class ExistingInvoiceTests {

        @Test
        @DisplayName("Should mark existing invoice for update")
        void shouldMarkExistingInvoice_forUpdate() {
            // Given: invoice already exists
            var existingInvoice = new com.hms.billing_service.entities.Invoice();
            existingInvoice.setId("inv-123");
            when(invoiceRepository.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(existingInvoice));
            
            var exam = createMockExam();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            invoiceHook.validateCreate(request, context);

            // Then: existing invoice marked in context
            assertThat(context.get("EXISTING_INVOICE")).isEqualTo(existingInvoice);
        }
    }
}
