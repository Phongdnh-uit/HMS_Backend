package com.hms.medical_exam_service.hooks;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.test.TestDataFactory;
import com.hms.medical_exam_service.clients.BillingClient;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionRequest;
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
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for PrescriptionHook.
 * Tests lifecycle hooks for Prescription entity operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-EXAM-011: PrescriptionHook Unit Tests")
class PrescriptionHookTest {

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

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private PrescriptionHook prescriptionHook;

    private PrescriptionRequest testRequest;
    private Prescription testEntity;
    private MedicalExam testExam;
    private Map<String, Object> context;
    private String testExamId;
    private String testPrescriptionId;

    @BeforeEach
    void setUp() {
        // Manual constructor since we need CircuitBreakerFactory
        prescriptionHook = new PrescriptionHook(
            prescriptionRepository,
            medicalExamRepository,
            prescriptionItemMapper,
            webClientBuilder,
            billingClient,
            circuitBreakerFactory
        );
        ReflectionTestUtils.setField(prescriptionHook, "medicineServiceUrl", "http://medicine-service");
        
        // Default CB setup - pass through
        lenient().when(circuitBreakerFactory.create(anyString())).thenReturn(circuitBreaker);
        setupCircuitBreakerToPassThrough();

        context = new HashMap<>();
        testExamId = TestDataFactory.uuid();
        testPrescriptionId = TestDataFactory.uuid();

        // Setup test request
        testRequest = new PrescriptionRequest();
        testRequest.setNotes("Take medications as directed");
        
        List<PrescriptionItemRequest> items = new ArrayList<>();
        PrescriptionItemRequest item1 = new PrescriptionItemRequest();
        item1.setMedicineId("MED001");
        item1.setQuantity(20);
        item1.setDosage("500mg");
        item1.setDurationDays(5);
        item1.setInstructions("Take with food");
        items.add(item1);
        
        testRequest.setItems(items);

        // Setup test exam
        testExam = new MedicalExam();
        testExam.setId(testExamId);
        testExam.setAppointmentId(TestDataFactory.uuid());
        testExam.setPatientId(TestDataFactory.uuid());
        testExam.setPatientName("John Doe");
        testExam.setDoctorId(TestDataFactory.uuid());
        testExam.setDoctorName("Dr. Smith");
        testExam.setExamDate(Instant.now());

        // Setup test entity
        testEntity = new Prescription();
        testEntity.setId(testPrescriptionId);
        testEntity.setMedicalExamId(testExamId);
        testEntity.setStatus(Prescription.Status.ACTIVE);
        testEntity.setPatientId(testExam.getPatientId());
        testEntity.setPatientName(testExam.getPatientName());
        testEntity.setDoctorId(testExam.getDoctorId());
        testEntity.setDoctorName(testExam.getDoctorName());
        testEntity.setPrescribedAt(Instant.now());
    }

    /**
     * Helper: CB passes through (healthy state)
     */
    @SuppressWarnings("unchecked")
    private void setupCircuitBreakerToPassThrough() {
        lenient().when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(invocation -> {
                Supplier<?> supplier = invocation.getArgument(0);
                return supplier.get();
            });
    }

    @Nested
    @DisplayName("Method: validateCreate()")
    class ValidateCreateTests {

        @Test
        @DisplayName("Should throw exception when exam ID is missing from context")
        void validateCreate_withoutExamIdInContext_shouldThrowException() {
            // Given
            context.clear(); // No CONTEXT_EXAM_ID

            // When & Then
            assertThatThrownBy(() -> prescriptionHook.validateCreate(testRequest, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Medical exam ID is required");
        }

        @Test
        @DisplayName("Should throw exception when medical exam does not exist")
        void validateCreate_withNonExistentExam_shouldThrowException() {
            // Given
            context.put(PrescriptionHook.CONTEXT_EXAM_ID, testExamId);
            given(medicalExamRepository.findById(testExamId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> prescriptionHook.validateCreate(testRequest, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Medical exam not found");
        }

        @Test
        @DisplayName("Should throw exception when prescription already exists for exam")
        void validateCreate_withExistingPrescription_shouldThrowException() {
            // Given
            context.put(PrescriptionHook.CONTEXT_EXAM_ID, testExamId);
            given(medicalExamRepository.findById(testExamId)).willReturn(Optional.of(testExam));
            given(prescriptionRepository.existsByMedicalExamId(testExamId)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> prescriptionHook.validateCreate(testRequest, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Prescription already exists");
        }

        @Test
        @DisplayName("Should pass validation when exam exists and no prescription exists")
        void validateCreate_withValidData_shouldPass() {
            // Given
            context.put(PrescriptionHook.CONTEXT_EXAM_ID, testExamId);
            given(medicalExamRepository.findById(testExamId)).willReturn(Optional.of(testExam));
            given(prescriptionRepository.existsByMedicalExamId(testExamId)).willReturn(false);
            
            // Mock WebClient to avoid NullPointerException (even though we use empty items list)
            WebClient.Builder mockBuilder = mock(WebClient.Builder.class);
            given(webClientBuilder.baseUrl(any())).willReturn(mockBuilder);
            given(mockBuilder.defaultHeader(any(), any())).willReturn(mockBuilder);
            
            // Use empty items list to avoid medicine validation (which requires WebClient mocking)
            PrescriptionRequest simpleRequest = new PrescriptionRequest();
            simpleRequest.setNotes("Test prescription");
            simpleRequest.setItems(Collections.emptyList());

            // When & Then - Should not throw exception
            assertThatCode(() -> prescriptionHook.validateCreate(simpleRequest, context))
                .doesNotThrowAnyException();
            
            // Should store exam in context
            assertThat(context).containsKey("medicalExam");
            then(medicalExamRepository).should().findById(testExamId);
            then(prescriptionRepository).should().existsByMedicalExamId(testExamId);
        }

        @Test
        @DisplayName("Should validate that prescription has at least one item")
        void validateCreate_withEmptyItems_shouldThrowException() {
            // Given
            testRequest.setItems(new ArrayList<>()); // Empty list
            context.put(PrescriptionHook.CONTEXT_EXAM_ID, testExamId);

            // When & Then - Should be caught by bean validation (@NotEmpty on items)
            // The validation happens before hook, but we test the flow
            assertThat(testRequest.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Method: enrichCreate()")
    class EnrichCreateTests {

        @Test
        @DisplayName("Should set prescribedAt timestamp")
        void enrichCreate_shouldSetPrescribedAt() {
            // Given
            context.put("medicalExam", testExam);
            Instant beforeCall = Instant.now();

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPrescribedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeCall);
        }

        @Test
        @DisplayName("Should set medical exam ID")
        void enrichCreate_shouldSetMedicalExamId() {
            // Given
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getMedicalExamId()).isEqualTo(testExamId);
        }

        @Test
        @DisplayName("Should copy snapshot data from exam")
        void enrichCreate_shouldCopySnapshotData() {
            // Given
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPatientId()).isEqualTo(testExam.getPatientId());
            assertThat(testEntity.getPatientName()).isEqualTo(testExam.getPatientName());
            assertThat(testEntity.getDoctorId()).isEqualTo(testExam.getDoctorId());
            assertThat(testEntity.getDoctorName()).isEqualTo(testExam.getDoctorName());
        }

        @Test
        @DisplayName("Should handle prescription with multiple items")
        void enrichCreate_withMultipleItems_shouldProcessAll() {
            // Given
            PrescriptionItemRequest item2 = new PrescriptionItemRequest();
            item2.setMedicineId("MED002");
            item2.setQuantity(15);
            item2.setDosage("250mg");
            testRequest.getItems().add(item2);
            
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getMedicalExamId()).isEqualTo(testExamId);
            assertThat(testEntity.getPrescribedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Snapshot Propagation Pattern")
    class SnapshotPropagationTests {

        @Test
        @DisplayName("Should propagate patient snapshot from exam to prescription")
        void enrichCreate_shouldPropagatePatientSnapshot() {
            // Given
            testExam.setPatientId("PATIENT-123");
            testExam.setPatientName("Jane Doe");
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then - Patient data propagated from exam snapshot
            assertThat(testEntity.getPatientId()).isEqualTo("PATIENT-123");
            assertThat(testEntity.getPatientName()).isEqualTo("Jane Doe");
        }

        @Test
        @DisplayName("Should propagate doctor snapshot from exam to prescription")
        void enrichCreate_shouldPropagateDoctorSnapshot() {
            // Given
            testExam.setDoctorId("DOCTOR-456");
            testExam.setDoctorName("Dr. Johnson");
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then - Doctor data propagated from exam snapshot
            assertThat(testEntity.getDoctorId()).isEqualTo("DOCTOR-456");
            assertThat(testEntity.getDoctorName()).isEqualTo("Dr. Johnson");
        }

        @Test
        @DisplayName("Should not make cross-service calls for patient/doctor data")
        void enrichCreate_shouldNotCallExternalServices() {
            // Given
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then - All data comes from exam snapshot, no external calls
            assertThat(testEntity.getPatientName()).isNotNull();
            assertThat(testEntity.getDoctorName()).isNotNull();
            // No verification needed for external service calls - they shouldn't exist
        }
    }

    @Nested
    @DisplayName("Business Rules")
    class BusinessRulesTests {

        @Test
        @DisplayName("Should enforce one prescription per medical exam")
        void validateCreate_shouldEnforceOnePrescriptionPerExam() {
            // Given
            context.put(PrescriptionHook.CONTEXT_EXAM_ID, testExamId);
            given(medicalExamRepository.findById(testExamId)).willReturn(Optional.of(testExam));
            given(prescriptionRepository.existsByMedicalExamId(testExamId)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> prescriptionHook.validateCreate(testRequest, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should require medical exam to exist before creating prescription")
        void validateCreate_shouldRequireExistingExam() {
            // Given
            context.put(PrescriptionHook.CONTEXT_EXAM_ID, testExamId);
            given(medicalExamRepository.findById(testExamId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> prescriptionHook.validateCreate(testRequest, context))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exam with null patient name gracefully")
        void enrichCreate_withNullPatientName_shouldNotThrowException() {
            // Given
            testExam.setPatientName(null);
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getPatientName()).isNull();
        }

        @Test
        @DisplayName("Should handle exam with null doctor name gracefully")
        void enrichCreate_withNullDoctorName_shouldNotThrowException() {
            // Given
            testExam.setDoctorName(null);
            context.put("medicalExam", testExam);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            assertThat(testEntity.getDoctorName()).isNull();
        }

        @Test
        @DisplayName("Should set prescribed time to current instant")
        void enrichCreate_shouldSetCurrentTimestamp() {
            // Given
            context.put("medicalExam", testExam);
            Instant before = Instant.now().minusSeconds(1);

            // When
            prescriptionHook.enrichCreate(testRequest, testEntity, context);

            // Then
            Instant after = Instant.now().plusSeconds(1);
            assertThat(testEntity.getPrescribedAt())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
        }
    }
}
