package com.hms.medical_exam_service.hooks;

import com.hms.common.dtos.ApiResponse;
import com.hms.medical_exam_service.clients.BillingClient;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.entities.Prescription;
import com.hms.medical_exam_service.mappers.PrescriptionItemMapper;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import com.hms.medical_exam_service.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for graceful degradation in PrescriptionHook invoice generation.
 * 
 * Key Pattern: FIRE AND FORGET
 * When billing-service is unavailable, the prescription dispense operation
 * succeeds but invoice generation fails silently (logged).
 * 
 * Verifies:
 * 1. Invoice generation is attempted after prescription dispense
 * 2. Billing service failure does NOT fail the prescription operation
 * 3. Proper logging of invoice generation failures
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionHook Invoice Generation Graceful Degradation Tests")
class PrescriptionHookInvoiceGenerationTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private MedicalExamRepository medicalExamRepository;

    @Mock
    private PrescriptionItemMapper prescriptionItemMapper;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private BillingClient billingClient;

    private PrescriptionHook prescriptionHook;

    private static final String PRESCRIPTION_ID = "presc-123";
    private static final String EXAM_ID = "exam-456";
    private static final String APPOINTMENT_ID = "apt-789";

    @BeforeEach
    void setUp() {
        prescriptionHook = new PrescriptionHook(
            prescriptionRepository,
            medicalExamRepository,
            prescriptionItemMapper,
            webClientBuilder,
            billingClient
        );
        ReflectionTestUtils.setField(prescriptionHook, "medicineServiceUrl", "http://medicine-service");
    }

    // ========================================================================
    // TEST DATA BUILDERS
    // ========================================================================

    private Prescription createDispensedPrescription() {
        Prescription prescription = new Prescription();
        prescription.setId(PRESCRIPTION_ID);
        prescription.setMedicalExamId(EXAM_ID);
        prescription.setStatus(Prescription.Status.DISPENSED);
        return prescription;
    }

    private MedicalExam createMedicalExam() {
        MedicalExam exam = new MedicalExam();
        exam.setId(EXAM_ID);
        exam.setAppointmentId(APPOINTMENT_ID);
        return exam;
    }

    // ========================================================================
    // 1. INVOICE GENERATION SUCCESS TESTS
    // ========================================================================

    @Nested
    @DisplayName("1. Invoice Generation Success Scenarios")
    class InvoiceGenerationSuccessTests {

        @Test
        @DisplayName("Should generate invoice successfully when billing service responds")
        void shouldGenerateInvoice_whenBillingServiceSucceeds() {
            // Given
            Prescription prescription = createDispensedPrescription();
            MedicalExam exam = createMedicalExam();

            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.of(prescription));
            when(medicalExamRepository.findById(EXAM_ID))
                .thenReturn(Optional.of(exam));

            BillingClient.InvoiceResponse invoiceResponse = new BillingClient.InvoiceResponse(
                "inv-001", "INV-2026-001", "patient-1", "John Doe", "PENDING"
            );
            when(billingClient.upsertInvoice(any(BillingClient.InvoiceRequest.class)))
                .thenReturn(ApiResponse.ok(invoiceResponse));

            // When
            assertDoesNotThrow(() -> 
                prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID));

            // Then
            verify(billingClient).upsertInvoice(argThat(request -> 
                APPOINTMENT_ID.equals(request.appointmentId()) &&
                EXAM_ID.equals(request.examId())
            ));
        }
    }

    // ========================================================================
    // 2. GRACEFUL DEGRADATION TESTS
    // ========================================================================

    @Nested
    @DisplayName("2. Graceful Degradation - Billing Service Failures")
    class GracefulDegradationTests {

        @Test
        @DisplayName("Should NOT throw when billing service fails (graceful degradation)")
        void shouldNotThrow_whenBillingServiceFails() {
            // Given
            Prescription prescription = createDispensedPrescription();
            MedicalExam exam = createMedicalExam();

            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.of(prescription));
            when(medicalExamRepository.findById(EXAM_ID))
                .thenReturn(Optional.of(exam));

            // Billing service throws exception
            when(billingClient.upsertInvoice(any(BillingClient.InvoiceRequest.class)))
                .thenThrow(new RuntimeException("Billing service unavailable"));

            // When/Then - should NOT throw, prescription dispense is complete
            assertDoesNotThrow(() -> 
                prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID));

            // Verify billing was attempted
            verify(billingClient).upsertInvoice(any());
        }

        @Test
        @DisplayName("Should NOT throw when FeignException occurs (service unavailable)")
        void shouldNotThrow_whenFeignExceptionOccurs() {
            // Given
            Prescription prescription = createDispensedPrescription();
            MedicalExam exam = createMedicalExam();

            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.of(prescription));
            when(medicalExamRepository.findById(EXAM_ID))
                .thenReturn(Optional.of(exam));

            // Feign client throws service unavailable
            when(billingClient.upsertInvoice(any(BillingClient.InvoiceRequest.class)))
                .thenThrow(new feign.FeignException.ServiceUnavailable(
                    "Service Unavailable",
                    feign.Request.create(feign.Request.HttpMethod.POST, "/invoices/upsert",
                        java.util.Collections.emptyMap(), null, null, null),
                    null, null));

            // When/Then - graceful degradation: prescription still complete
            assertDoesNotThrow(() -> 
                prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID));
        }

        @Test
        @DisplayName("Should NOT throw when connection timeout occurs")
        void shouldNotThrow_whenConnectionTimesOut() {
            // Given
            Prescription prescription = createDispensedPrescription();
            MedicalExam exam = createMedicalExam();

            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.of(prescription));
            when(medicalExamRepository.findById(EXAM_ID))
                .thenReturn(Optional.of(exam));

            when(billingClient.upsertInvoice(any(BillingClient.InvoiceRequest.class)))
                .thenThrow(new feign.FeignException.GatewayTimeout(
                    "Gateway Timeout", 
                    feign.Request.create(feign.Request.HttpMethod.POST, "/test", 
                        java.util.Collections.emptyMap(), null, null, null),
                    null, null));

            // When/Then
            assertDoesNotThrow(() -> 
                prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID));
        }
    }

    // ========================================================================
    // 3. SKIP SCENARIOS TESTS
    // ========================================================================

    @Nested
    @DisplayName("3. Skip Scenarios - Invoice Generation Not Attempted")
    class SkipScenariosTests {

        @Test
        @DisplayName("Should skip invoice generation when prescription not found")
        void shouldSkip_whenPrescriptionNotFound() {
            // Given
            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.empty());

            // When
            prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID);

            // Then - billing client should NOT be called
            verify(billingClient, never()).upsertInvoice(any());
        }

        @Test
        @DisplayName("Should skip invoice generation when prescription not DISPENSED")
        void shouldSkip_whenPrescriptionNotDispensed() {
            // Given
            Prescription prescription = createDispensedPrescription();
            prescription.setStatus(Prescription.Status.ACTIVE); // Not dispensed yet

            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.of(prescription));

            // When
            prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID);

            // Then - billing client should NOT be called
            verify(billingClient, never()).upsertInvoice(any());
            verify(medicalExamRepository, never()).findById(anyString());
        }

        @Test
        @DisplayName("Should skip invoice generation when medical exam not found")
        void shouldSkip_whenMedicalExamNotFound() {
            // Given
            Prescription prescription = createDispensedPrescription();

            when(prescriptionRepository.findById(PRESCRIPTION_ID))
                .thenReturn(Optional.of(prescription));
            when(medicalExamRepository.findById(EXAM_ID))
                .thenReturn(Optional.empty());

            // When
            prescriptionHook.generateInvoiceAfterDispense(PRESCRIPTION_ID);

            // Then - billing client should NOT be called
            verify(billingClient, never()).upsertInvoice(any());
        }
    }
}
