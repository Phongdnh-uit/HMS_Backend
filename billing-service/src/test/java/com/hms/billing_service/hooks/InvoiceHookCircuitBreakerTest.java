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
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvoiceHook with Circuit Breaker pattern.
 * 
 * Tests verify:
 * 1. Circuit Breaker fallback behavior for critical services (FAIL FAST)
 * 2. Circuit Breaker fallback behavior for optional services (GRACEFUL DEGRADATION)
 * 3. Normal operations when services respond correctly
 * 4. Context enrichment with external service data
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceHook Circuit Breaker Tests")
class InvoiceHookCircuitBreakerTest {

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

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private static final String APPOINTMENT_ID = "apt-123";
    private static final String EXAM_ID = "exam-456";
    private static final String PATIENT_ID = "patient-789";

    @BeforeEach
    void setUp() {
        // Create InvoiceHook with all dependencies
        invoiceHook = new InvoiceHook(
            invoiceRepository,
            medicalExamClient,
            appointmentClient,
            patientClient,
            hrClient,
            circuitBreakerFactory
        );

        // Default: no existing invoice
        lenient().when(invoiceRepository.findByAppointmentId(anyString())).thenReturn(Optional.empty());

        // Setup circuit breaker factory to return mock circuit breaker
        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
    }

    /**
     * Helper method to setup circuit breaker to execute supplier directly (CB closed/healthy)
     */
    @SuppressWarnings("unchecked")
    private <T> void setupCircuitBreakerToPassThrough() {
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(invocation -> {
                Supplier<T> supplier = invocation.getArgument(0);
                return supplier.get();
            });
    }

    /**
     * Helper method to setup circuit breaker to call fallback (CB open/service down)
     */
    @SuppressWarnings("unchecked")
    private <T> void setupCircuitBreakerToFallback(RuntimeException exception) {
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(invocation -> {
                Function<Throwable, T> fallback = invocation.getArgument(1);
                return fallback.apply(exception);
            });
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
            "DISPENSED",
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

    private LabTestResultResponse createMockLabTest() {
        return new LabTestResultResponse(
            "lab-1",
            "test-type-1",
            "BT001",
            "Blood Test",
            new BigDecimal("150000"),
            "COMPLETED",
            Instant.now()
        );
    }

    // ========================================================================
    // 1. CRITICAL SERVICE TESTS - Medical Exam (FAIL FAST)
    // ========================================================================

    @Nested
    @DisplayName("1. Medical Exam Service - Critical (Fail Fast)")
    class MedicalExamCriticalTests {

        @Test
        @DisplayName("Should throw SERVICE_UNAVAILABLE when circuit breaker triggers fallback for exam fetch")
        void shouldThrowServiceUnavailable_whenCircuitBreakerOpenForExam() {
            // Given: CB is open (service unavailable)
            setupCircuitBreakerToFallback(new RuntimeException("Connection refused"));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When & Then: Should fail fast
            ApiException exception = assertThrows(ApiException.class,
                () -> invoiceHook.validateCreate(request, context));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("Medical exam service unavailable");
        }

        @Test
        @DisplayName("Should throw EXAM_NOT_FOUND when service responds but exam doesn't exist")
        void shouldThrowExamNotFound_whenServiceRespondsButNoData() {
            // Given: CB passes through, service returns null
            setupCircuitBreakerToPassThrough();
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));

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
            // Given: All services respond normally
            setupCircuitBreakerToPassThrough();
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
    }

    // ========================================================================
    // 2. CRITICAL SERVICE TESTS - Prescription (FAIL FAST when CB open)
    // ========================================================================

    @Nested
    @DisplayName("2. Prescription Service - Critical (Fail Fast when CB open)")
    class PrescriptionCriticalTests {

        @Test
        @DisplayName("Should throw SERVICE_UNAVAILABLE when prescription service CB is open")
        void shouldThrowServiceUnavailable_whenPrescriptionCBOpen() {
            // Given: First call (exam) succeeds, second call (prescription) triggers CB fallback
            var exam = createMockExam();
            
            // Use a counter to make first call succeed, second call fail
            when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(0);
                    return supplier.get(); // First call - exam - succeeds
                })
                .thenAnswer(invocation -> {
                    Function<Throwable, ?> fallback = invocation.getArgument(1);
                    return fallback.apply(new RuntimeException("Prescription service down"));
                });
            
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When & Then: Should fail fast - no incomplete invoices
            ApiException exception = assertThrows(ApiException.class,
                () -> invoiceHook.validateCreate(request, context));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("prescription");
        }

        @Test
        @DisplayName("Should continue without prescription when service responds but no prescription exists")
        void shouldContinue_whenNoPrescriptionExists() {
            // Given: All services respond, prescription doesn't exist
            setupCircuitBreakerToPassThrough();
            var exam = createMockExam();
            
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null)); // No prescription
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then: No prescription in context, but should proceed
            assertThat(context.get(InvoiceHook.CONTEXT_PRESCRIPTION)).isNull();
        }

        @Test
        @DisplayName("Should put prescription in context when found")
        void shouldPutPrescriptionInContext_whenFound() {
            // Given
            setupCircuitBreakerToPassThrough();
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
    }

    // ========================================================================
    // 3. CRITICAL SERVICE TESTS - Lab Tests (FAIL FAST when CB open)
    // ========================================================================

    @Nested
    @DisplayName("3. Lab Tests Service - Critical (Fail Fast when CB open)")
    class LabTestsCriticalTests {

        @Test
        @DisplayName("Should throw SERVICE_UNAVAILABLE when lab tests service CB is open")
        void shouldThrowServiceUnavailable_whenLabTestsCBOpen() {
            // Given: Exam and prescription succeed, lab tests triggers CB fallback
            var exam = createMockExam();
            
            // Track call count to make first two succeed, third fail
            int[] callCount = {0};
            when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    callCount[0]++;
                    if (callCount[0] <= 2) { // Exam and Prescription calls succeed
                        Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    } else { // Lab tests call fails
                        Function<Throwable, ?> fallback = invocation.getArgument(1);
                        return fallback.apply(new RuntimeException("Lab service down"));
                    }
                });
            
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When & Then: Should fail fast
            ApiException exception = assertThrows(ApiException.class,
                () -> invoiceHook.validateCreate(request, context));

            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
            assertThat(exception.getMessage()).contains("lab test");
        }

        @Test
        @DisplayName("Should continue without lab tests when service responds but no tests exist")
        void shouldContinue_whenNoLabTestsExist() {
            // Given
            setupCircuitBreakerToPassThrough();
            var exam = createMockExam();
            
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of())); // Empty list

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_LAB_TESTS)).isNull();
        }

        @Test
        @DisplayName("Should put lab tests in context when found")
        void shouldPutLabTestsInContext_whenFound() {
            // Given
            setupCircuitBreakerToPassThrough();
            var exam = createMockExam();
            var labTests = List.of(createMockLabTest());
            
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
    }

    // ========================================================================
    // 4. NON-CRITICAL SERVICE TESTS - HR/Consultation Fee (GRACEFUL DEGRADATION)
    // ========================================================================

    @Nested
    @DisplayName("4. HR Service - Optional (Graceful Degradation)")
    class HrServiceGracefulDegradationTests {

        @Test
        @DisplayName("Should use default consultation fee when HR service CB is open")
        void shouldUseDefaultFee_whenHrServiceCBOpen() {
            // Given: Medical exam services work, HR triggers CB fallback
            var exam = createMockExam();
            
            // Track call count: first 3 (exam, prescription, lab) succeed, 4th (HR) returns null (fallback)
            int[] callCount = {0};
            when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    callCount[0]++;
                    if (callCount[0] <= 3) { // Medical exam calls succeed
                        Supplier<?> supplier = invocation.getArgument(0);
                        return supplier.get();
                    } else { // HR call returns null from fallback
                        Function<Throwable, ?> fallback = invocation.getArgument(1);
                        return fallback.apply(new RuntimeException("HR service down"));
                    }
                });
            
            when(medicalExamClient.getExamById(EXAM_ID))
                .thenReturn(ApiResponse.ok(exam));
            when(medicalExamClient.getPrescriptionByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(null));
            when(medicalExamClient.getLabResultsByExam(EXAM_ID))
                .thenReturn(ApiResponse.ok(List.of()));

            InvoiceRequest request = createInvoiceRequest();
            Map<String, Object> context = new HashMap<>();

            // When: Should NOT throw - graceful degradation
            assertDoesNotThrow(() -> invoiceHook.validateCreate(request, context));

            // Then: No consultation fee in context (will use default in enrichCreate)
            assertThat(context.get(InvoiceHook.CONTEXT_CONSULTATION_FEE)).isNull();
        }

        @Test
        @DisplayName("Should fetch consultation fee from HR service when available")
        void shouldFetchConsultationFee_whenHrServiceAvailable() {
            // Given
            setupCircuitBreakerToPassThrough();
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
    }

    // ========================================================================
    // 5. EXISTING INVOICE TESTS
    // ========================================================================

    @Nested
    @DisplayName("5. Existing Invoice Detection")
    class ExistingInvoiceTests {

        @Test
        @DisplayName("Should mark existing invoice for update instead of create")
        void shouldMarkExistingInvoice_forUpdate() {
            // Given
            setupCircuitBreakerToPassThrough();
            var exam = createMockExam();
            Invoice existingInvoice = new Invoice();
            existingInvoice.setId("inv-existing");
            
            when(invoiceRepository.findByAppointmentId(APPOINTMENT_ID))
                .thenReturn(Optional.of(existingInvoice));
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
            assertThat(context.get("EXISTING_INVOICE")).isEqualTo(existingInvoice);
        }
    }
}
