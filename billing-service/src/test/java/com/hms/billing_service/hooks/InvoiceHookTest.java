package com.hms.billing_service.hooks;

import com.hms.billing_service.clients.AppointmentClient;
import com.hms.billing_service.clients.HrClient;
import com.hms.billing_service.clients.MedicalExamClient;
import com.hms.billing_service.clients.PatientClient;
import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.InvoiceItem;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for InvoiceHook.
 * Tests invoice validation, enrichment, and external service integration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-BILL-005/006: InvoiceHook Unit Tests")
class InvoiceHookTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private MedicalExamClient medicalExamClient;

    @Mock
    private AppointmentClient appointmentClient;

    @Mock
    private PatientClient patientClient;

    @Mock
    private HrClient hrClient;

    @InjectMocks
    private InvoiceHook invoiceHook;

    private InvoiceRequest testRequest;
    private Invoice testInvoice;
    private MedicalExamClient.MedicalExamResponse examResponse;
    private MedicalExamClient.PrescriptionResponse prescriptionResponse;
    private HrClient.EmployeeResponse doctorResponse;

    @BeforeEach
    void setUp() {
        String appointmentId = TestDataFactory.uuid();
        String examId = TestDataFactory.uuid();
        String patientId = TestDataFactory.uuid();
        String doctorId = TestDataFactory.uuid();

        // Setup request
        testRequest = new InvoiceRequest();
        testRequest.setAppointmentId(appointmentId);
        testRequest.setExamId(examId);
        testRequest.setNotes("Test invoice");

        // Setup exam response
        examResponse = new MedicalExamClient.MedicalExamResponse(
                examId,
                new MedicalExamClient.MedicalExamResponse.AppointmentInfo(appointmentId, LocalDateTime.now()),
                new MedicalExamClient.MedicalExamResponse.PatientInfo(patientId, TestDataFactory.fullName()),
                new MedicalExamClient.MedicalExamResponse.DoctorInfo(doctorId, "Dr. " + TestDataFactory.fullName()),
                "Common cold",
                Instant.now(),
                Instant.now()
        );

        // Setup prescription response
        List<MedicalExamClient.PrescriptionItemResponse> prescriptionItems = List.of(
                new MedicalExamClient.PrescriptionItemResponse(
                        TestDataFactory.uuid(),
                        new MedicalExamClient.PrescriptionItemResponse.MedicineInfo(
                                TestDataFactory.uuid(),
                                "Paracetamol 500mg"
                        ),
                        10,
                        new BigDecimal("5000"),
                        "1 tablet",
                        7,
                        "Take after meals"
                )
        );

        prescriptionResponse = new MedicalExamClient.PrescriptionResponse(
                TestDataFactory.uuid(),
                new MedicalExamClient.PrescriptionResponse.MedicalExamInfo(examId),
                new MedicalExamClient.PrescriptionResponse.PatientInfo(patientId, TestDataFactory.fullName()),
                new MedicalExamClient.PrescriptionResponse.DoctorInfo(doctorId, "Dr. " + TestDataFactory.fullName()),
                "ACTIVE",
                Instant.now(),
                "Take as prescribed",
                prescriptionItems
        );

        // Setup doctor response with consultation fee
        HrClient.EmployeeResponse.DepartmentInfo department = 
            new HrClient.EmployeeResponse.DepartmentInfo(
                TestDataFactory.uuid(),
                "Cardiology",
                new BigDecimal("300000")
            );
        
        doctorResponse = new HrClient.EmployeeResponse(
            doctorId,
            "Dr. " + TestDataFactory.fullName(),
            TestDataFactory.uniqueEmail(),
            department
        );

        // Setup invoice entity
        testInvoice = Invoice.builder()
                .appointmentId(appointmentId)
                .medicalExamId(examId)
                .items(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("UC-BILL-005: Should validate and fetch exam by examId")
        void validateCreate_withExamId_shouldFetchExam() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_EXAM)).isEqualTo(examResponse);
            then(medicalExamClient).should().getExamById(testRequest.getExamId());
        }

        @Test
        @DisplayName("UC-BILL-005: Should fetch exam by appointmentId when examId is null")
        void validateCreate_withoutExamId_shouldFetchByAppointment() {
            // Given
            testRequest.setExamId(null);
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamByAppointment(testRequest.getAppointmentId()))
                    .willReturn(ApiResponse.ok(examResponse));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_EXAM)).isEqualTo(examResponse);
            then(medicalExamClient).should().getExamByAppointment(testRequest.getAppointmentId());
        }

        @Test
        @DisplayName("UC-BILL-005: Should throw exception when exam not found")
        void validateCreate_withInvalidExamId_shouldThrowException() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(null));

            // When & Then
            assertThatThrownBy(() -> invoiceHook.validateCreate(testRequest, context))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXAM_NOT_FOUND);
        }

        @Test
        @DisplayName("UC-BILL-005: Should fetch prescription and add to context")
        void validateCreate_withPrescription_shouldAddToContext() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));
            given(medicalExamClient.getPrescriptionByExam(examResponse.id()))
                    .willReturn(ApiResponse.ok(prescriptionResponse));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_PRESCRIPTION)).isEqualTo(prescriptionResponse);
            then(medicalExamClient).should().getPrescriptionByExam(examResponse.id());
        }

        @Test
        @DisplayName("UC-BILL-005: Should handle missing prescription gracefully")
        void validateCreate_withoutPrescription_shouldContinue() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));
            given(medicalExamClient.getPrescriptionByExam(examResponse.id()))
                    .willThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Prescription not found"));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_EXAM)).isEqualTo(examResponse);
            assertThat(context.get(InvoiceHook.CONTEXT_PRESCRIPTION)).isNull();
        }

        @Test
        @DisplayName("UC-BILL-006: Should fetch consultation fee from doctor's department")
        void validateCreate_shouldFetchConsultationFee() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));
            given(hrClient.getEmployeeById(examResponse.doctor().id()))
                    .willReturn(ApiResponse.ok(doctorResponse));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_CONSULTATION_FEE))
                    .isEqualTo(new BigDecimal("300000"));
            then(hrClient).should().getEmployeeById(examResponse.doctor().id());
        }

        @Test
        @DisplayName("Should use default consultation fee when doctor not found")
        void validateCreate_withoutDoctorFee_shouldUseDefault() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));
            given(hrClient.getEmployeeById(examResponse.doctor().id()))
                    .willThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Doctor not found"));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            // Should not throw exception and should not have consultation fee in context
            assertThat(context.get(InvoiceHook.CONTEXT_CONSULTATION_FEE)).isNull();
        }

        @Test
        @DisplayName("UC-BILL-005: Should detect existing invoice for appointment")
        void validateCreate_withExistingInvoice_shouldAddToContext() {
            // Given
            Map<String, Object> context = new HashMap<>();
            Invoice existingInvoice = Invoice.builder()
                    .id(TestDataFactory.uuid())
                    .appointmentId(testRequest.getAppointmentId())
                    .build();
            
            given(invoiceRepository.findByAppointmentId(testRequest.getAppointmentId()))
                    .willReturn(Optional.of(existingInvoice));
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get("EXISTING_INVOICE")).isEqualTo(existingInvoice);
            then(invoiceRepository).should().findByAppointmentId(testRequest.getAppointmentId());
        }

        @Test
        @DisplayName("UC-BILL-006: Should fetch lab test results when available")
        void validateCreate_withLabTests_shouldAddToContext() {
            // Given
            Map<String, Object> context = new HashMap<>();
            List<MedicalExamClient.LabTestResultResponse> labTests = List.of(
                    new MedicalExamClient.LabTestResultResponse(
                            TestDataFactory.uuid(),
                            TestDataFactory.uuid(),
                            "LAB-001",
                            "Blood Test",
                            new BigDecimal("150000"),
                            "COMPLETED",
                            Instant.now()
                    )
            );
            
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));
            given(medicalExamClient.getLabResultsByExam(examResponse.id()))
                    .willReturn(ApiResponse.ok(labTests));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_LAB_TESTS)).isEqualTo(labTests);
            then(medicalExamClient).should().getLabResultsByExam(examResponse.id());
        }

        @Test
        @DisplayName("Should handle missing lab tests gracefully")
        void validateCreate_withoutLabTests_shouldContinue() {
            // Given
            Map<String, Object> context = new HashMap<>();
            given(medicalExamClient.getExamById(testRequest.getExamId()))
                    .willReturn(ApiResponse.ok(examResponse));
            given(medicalExamClient.getLabResultsByExam(examResponse.id()))
                    .willThrow(new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "No lab tests"));

            // When
            invoiceHook.validateCreate(testRequest, context);

            // Then
            assertThat(context.get(InvoiceHook.CONTEXT_LAB_TESTS)).isNull();
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("UC-BILL-006: Should enrich invoice with basic info")
        void enrichCreate_shouldSetBasicInfo() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, new BigDecimal("300000"));

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            assertThat(testInvoice.getInvoiceNumber()).isNotNull();
            assertThat(testInvoice.getInvoiceNumber()).startsWith("INV-");
            assertThat(testInvoice.getMedicalExamId()).isEqualTo(examResponse.id());
            assertThat(testInvoice.getAppointmentId()).isEqualTo(testRequest.getAppointmentId());
            assertThat(testInvoice.getPatientId()).isEqualTo(examResponse.patient().id());
            assertThat(testInvoice.getPatientName()).isEqualTo(examResponse.patient().fullName());
            assertThat(testInvoice.getInvoiceDate()).isNotNull();
            assertThat(testInvoice.getDueDate()).isNotNull();
            assertThat(testInvoice.getNotes()).isEqualTo(testRequest.getNotes());
        }

        @Test
        @DisplayName("UC-BILL-005: Should add consultation fee item")
        void enrichCreate_shouldAddConsultationItem() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            BigDecimal consultationFee = new BigDecimal("300000");
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, consultationFee);

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            List<InvoiceItem> items = testInvoice.getItems();
            assertThat(items).isNotEmpty();
            
            InvoiceItem consultationItem = items.stream()
                    .filter(item -> item.getType() == InvoiceItem.ItemType.CONSULTATION)
                    .findFirst()
                    .orElseThrow();
            
            assertThat(consultationItem.getDescription()).isEqualTo("Consultation Fee");
            assertThat(consultationItem.getQuantity()).isEqualTo(1);
            assertThat(consultationItem.getUnitPrice()).isEqualByComparingTo(consultationFee);
            assertThat(consultationItem.getAmount()).isEqualByComparingTo(consultationFee);
        }

        @Test
        @DisplayName("UC-BILL-005: Should use default consultation fee when not provided")
        void enrichCreate_withoutConsultationFee_shouldUseDefault() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            // No consultation fee in context

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            InvoiceItem consultationItem = testInvoice.getItems().stream()
                    .filter(item -> item.getType() == InvoiceItem.ItemType.CONSULTATION)
                    .findFirst()
                    .orElseThrow();
            
            assertThat(consultationItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("200000"));
        }

        @Test
        @DisplayName("UC-BILL-006: Should add medicine items from prescription")
        void enrichCreate_withPrescription_shouldAddMedicineItems() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            context.put(InvoiceHook.CONTEXT_PRESCRIPTION, prescriptionResponse);
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, new BigDecimal("300000"));

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            List<InvoiceItem> medicineItems = testInvoice.getItems().stream()
                    .filter(item -> item.getType() == InvoiceItem.ItemType.MEDICINE)
                    .toList();
            
            assertThat(medicineItems).hasSize(1);
            
            InvoiceItem medicineItem = medicineItems.get(0);
            assertThat(medicineItem.getDescription()).isEqualTo("Paracetamol 500mg");
            assertThat(medicineItem.getQuantity()).isEqualTo(10);
            assertThat(medicineItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(medicineItem.getAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(medicineItem.getReferenceId()).isNotNull();
        }

        @Test
        @DisplayName("UC-BILL-005: Should create consultation-only invoice without prescription")
        void enrichCreate_withoutPrescription_shouldCreateConsultationOnly() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, new BigDecimal("300000"));
            // No prescription in context

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            assertThat(testInvoice.getItems()).hasSize(1);
            assertThat(testInvoice.getItems().get(0).getType()).isEqualTo(InvoiceItem.ItemType.CONSULTATION);
        }

        @Test
        @DisplayName("UC-BILL-006: Should add lab test items when available")
        void enrichCreate_withLabTests_shouldAddTestItems() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, new BigDecimal("300000"));
            
            List<MedicalExamClient.LabTestResultResponse> labTests = List.of(
                    new MedicalExamClient.LabTestResultResponse(
                            TestDataFactory.uuid(),
                            TestDataFactory.uuid(),
                            "LAB-001",
                            "Blood Test",
                            new BigDecimal("150000"),
                            "COMPLETED",
                            Instant.now()
                    )
            );
            context.put(InvoiceHook.CONTEXT_LAB_TESTS, labTests);

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            List<InvoiceItem> labTestItems = testInvoice.getItems().stream()
                    .filter(item -> item.getType() == InvoiceItem.ItemType.TEST)
                    .toList();
            
            assertThat(labTestItems).hasSize(1);
            
            InvoiceItem labTestItem = labTestItems.get(0);
            assertThat(labTestItem.getDescription()).isEqualTo("Lab Test: Blood Test");
            assertThat(labTestItem.getQuantity()).isEqualTo(1);
            assertThat(labTestItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(labTestItem.getAmount()).isEqualByComparingTo(new BigDecimal("150000"));
        }

        @Test
        @DisplayName("UC-BILL-005: Should calculate totals correctly")
        void enrichCreate_shouldCalculateTotals() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            context.put(InvoiceHook.CONTEXT_PRESCRIPTION, prescriptionResponse);
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, new BigDecimal("300000"));

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            // Consultation: 300000, Medicine: 50000 (10 * 5000)
            BigDecimal expectedSubtotal = new BigDecimal("350000");
            BigDecimal expectedTax = expectedSubtotal.multiply(new BigDecimal("0.10")); // 35000
            BigDecimal expectedTotal = expectedSubtotal.add(expectedTax); // 385000
            
            assertThat(testInvoice.getSubtotal()).isEqualByComparingTo(expectedSubtotal);
            assertThat(testInvoice.getTax()).isEqualByComparingTo(expectedTax);
            assertThat(testInvoice.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("UC-BILL-005: Should calculate totals with discount")
        void enrichCreate_withDiscount_shouldCalculateTotals() {
            // Given
            Map<String, Object> context = new HashMap<>();
            context.put(InvoiceHook.CONTEXT_EXAM, examResponse);
            context.put(InvoiceHook.CONTEXT_CONSULTATION_FEE, new BigDecimal("300000"));
            testInvoice.setDiscount(new BigDecimal("50000"));

            // When
            invoiceHook.enrichCreate(testRequest, testInvoice, context);

            // Then
            BigDecimal expectedSubtotal = new BigDecimal("300000");
            BigDecimal expectedTax = new BigDecimal("30000"); // 300000 * 0.10
            // Total = Subtotal + Tax - Discount = 300000 + 30000 - 50000 = 280000
            BigDecimal expectedTotal = new BigDecimal("280000");
            
            assertThat(testInvoice.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        }
    }

    @Nested
    @DisplayName("Method: recalculateTotals()")
    class RecalculateTotalsTests {

        @Test
        @DisplayName("UC-BILL-005: Should recalculate invoice totals")
        void recalculateTotals_shouldCalculateCorrectly() {
            // Given
            testInvoice.addItem(InvoiceItem.builder()
                    .type(InvoiceItem.ItemType.CONSULTATION)
                    .amount(new BigDecimal("200000"))
                    .invoice(testInvoice)
                    .build());
            testInvoice.addItem(InvoiceItem.builder()
                    .type(InvoiceItem.ItemType.MEDICINE)
                    .amount(new BigDecimal("50000"))
                    .invoice(testInvoice)
                    .build());

            // When
            invoiceHook.recalculateTotals(testInvoice);

            // Then
            assertThat(testInvoice.getSubtotal()).isEqualByComparingTo(new BigDecimal("250000"));
            assertThat(testInvoice.getTax()).isEqualByComparingTo(new BigDecimal("25000")); // 10%
            assertThat(testInvoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("275000"));
        }

        @Test
        @DisplayName("Should handle empty items list")
        void recalculateTotals_withNoItems_shouldSetZero() {
            // Given - invoice with no items

            // When
            invoiceHook.recalculateTotals(testInvoice);

            // Then
            assertThat(testInvoice.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(testInvoice.getTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(testInvoice.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Method: validateCancel()")
    class ValidateCancelTests {

        @Test
        @DisplayName("Should validate cancellation of unpaid invoice")
        void validateCancel_withUnpaidInvoice_shouldPass() {
            // Given
            testInvoice.setStatus(Invoice.InvoiceStatus.UNPAID);

            // When & Then
            assertThatCode(() -> invoiceHook.validateCancel(testInvoice))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject cancellation of already cancelled invoice")
        void validateCancel_withCancelledInvoice_shouldThrowException() {
            // Given
            testInvoice.setStatus(Invoice.InvoiceStatus.CANCELLED);

            // When & Then
            assertThatThrownBy(() -> invoiceHook.validateCancel(testInvoice))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("Should reject cancellation of paid invoice")
        void validateCancel_withPaidInvoice_shouldThrowException() {
            // Given
            testInvoice.setStatus(Invoice.InvoiceStatus.PAID);

            // When & Then
            assertThatThrownBy(() -> invoiceHook.validateCancel(testInvoice))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPERATION_NOT_ALLOWED)
                    .hasMessageContaining("fully paid");
        }
    }
}
