package com.hms.medical_exam_service.hooks;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.entities.Prescription;
import com.hms.medical_exam_service.entities.PrescriptionItem;
import com.hms.medical_exam_service.mappers.PrescriptionItemMapper;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import com.hms.medical_exam_service.repositories.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private PrescriptionHook prescriptionHook;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(prescriptionHook, "medicineServiceUrl", "http://medicine-service");
    }

    @Test
    @DisplayName("validateCreate: Success when exam exists and stock sufficient")
    void validateCreate_Success() {
        // Arrange
        String examId = "exam-1";
        Map<String, Object> context = new HashMap<>();
        context.put(PrescriptionHook.CONTEXT_EXAM_ID, examId);
        
        PrescriptionRequest request = new PrescriptionRequest();
        PrescriptionItemRequest itemRequest = new PrescriptionItemRequest();
        itemRequest.setMedicineId("med-1");
        itemRequest.setQuantity(10);
        request.setItems(List.of(itemRequest));

        MedicalExam exam = new MedicalExam();
        exam.setId(examId);

        // Mock Exam
        when(medicalExamRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(prescriptionRepository.existsByMedicalExamId(examId)).thenReturn(false);

        // Mock WebClient for Medicine Validation
        mockWebClientGetChain();
        mockMedicineResponse("med-1", 100); // 100 in stock

        // Act
        prescriptionHook.validateCreate(request, context);

        // Assert
        assertNotNull(context.get("medicalExam"));
        assertTrue(context.containsKey("medicine_med-1"));
    }

    @Test
    @DisplayName("validateCreate: Fail when exam does not exist")
    void validateCreate_Fail_ExamNotFound() {
        // Arrange
        String examId = "exam-unknown";
        Map<String, Object> context = new HashMap<>();
        context.put(PrescriptionHook.CONTEXT_EXAM_ID, examId);

        when(medicalExamRepository.findById(examId)).thenReturn(Optional.empty());

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, 
            () -> prescriptionHook.validateCreate(new PrescriptionRequest(), context));
        assertEquals(ErrorCode.EXAM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("validateCreate: Fail when insufficient stock")
    void validateCreate_Fail_InsufficientStock() {
        // Arrange
        String examId = "exam-1";
        Map<String, Object> context = new HashMap<>();
        context.put(PrescriptionHook.CONTEXT_EXAM_ID, examId);

        PrescriptionRequest request = new PrescriptionRequest();
        PrescriptionItemRequest itemRequest = new PrescriptionItemRequest();
        itemRequest.setMedicineId("med-1");
        itemRequest.setQuantity(50);
        request.setItems(List.of(itemRequest));

        when(medicalExamRepository.findById(examId)).thenReturn(Optional.of(new MedicalExam()));
        when(prescriptionRepository.existsByMedicalExamId(examId)).thenReturn(false);

        mockWebClientGetChain();
        mockMedicineResponse("med-1", 10); // Only 10 in stock

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, 
            () -> prescriptionHook.validateCreate(request, context));
        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
    }

    @Test
    @DisplayName("cancelPrescription: Success when ACTIVE")
    void cancelPrescription_Success() {
        // Arrange
        Prescription prescription = new Prescription();
        prescription.setId("rx-1");
        prescription.setStatus(Prescription.Status.ACTIVE);
        
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicineId("med-1");
        item.setQuantity(5);
        prescription.setItems(List.of(item));

        // Mock WebClient for Stock Restoration
        mockWebClientPatchChain();

        // Act
        prescriptionHook.cancelPrescription(prescription, "Wrong pill", "user-1");

        // Assert
        assertEquals(Prescription.Status.CANCELLED, prescription.getStatus());
        assertNotNull(prescription.getCancelledAt());
        assertEquals("user-1", prescription.getCancelledBy());
        assertEquals("Wrong pill", prescription.getCancelReason());
        
        // Verify web client call (patch for restoration)
        verify(requestBodySpec, times(1)).bodyValue(anyMap());
        verify(responseSpec, times(1)).toBodilessEntity();
    }
    
    @Test
    @DisplayName("cancelPrescription: Fail when DISPENSED")
    void cancelPrescription_Fail_Dispensed() {
        // Arrange
        Prescription prescription = new Prescription();
        prescription.setId("rx-1");
        prescription.setStatus(Prescription.Status.DISPENSED);

        // Act & Assert
        ApiException exception = assertThrows(ApiException.class, 
            () -> prescriptionHook.cancelPrescription(prescription, "reason", "user"));
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }

    // --- Helper Methods to Mock Fluent WebClient API ---

    private void mockWebClientGetChain() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), (Object[]) any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private void mockWebClientPatchChain() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), (Object[]) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());
    }

    // Since MedicineResponse is a private record in the Hook, we have to mock the json response binding
    // However, the hook uses .bodyToMono(MedicineResponse.class).
    // The easiest way to mock this without reflection hacks on the record is to just rely on the fact 
    // that the hook gets the response and calls accessors. 
    // BUT the record is private inside PrescriptionHook!
    // Solution: We can't mock private record return types easily. 
    // ALTERNATIVE: Verify behavior by exception or success flow relying on what bodyToMono returns.
    // Ideally, the DTO should be public or package-private for testing.
    // For this test, assuming the JSON mapping works, we need 'bodyToMono' to return a mock that behaves like the record.
    // But we can't instantiate a private record here.
    // FIX: Refactor Hook to use a public/package-private DTO or simulate via reflection (messy).
    // Better: Assumption -> The test cannot easily create the private record instance to return.
    // However, we can use reflection to instantiate the private record and return it in the Mono.
    
    // Actually, checking the code provided: 'private record MedicineResponse'.
    // We can use Reflection to invoke the constructor if needed, OR better, 
    // since we are writing new code, let's assume I can't change the hook production code right now 
    // (unless I do a refactor tool call).
    // Let's try to construct it using standard Java reflection for the test data setup.

    private void mockMedicineResponse(String id, int quantity) {
        try {
            // Get the private inner record class
            Class<?> clazz = Class.forName("com.hms.medical_exam_service.hooks.PrescriptionHook$MedicineResponse");
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class, BigDecimal.class, Integer.class);
            constructor.setAccessible(true);
            Object recordInstance = constructor.newInstance(id, "Test Med", new BigDecimal("10.0"), quantity);
            
            // when(responseSpec.bodyToMono(eq(clazz))).thenReturn((Mono) Mono.just(recordInstance));
            // doReturn(Mono.just(recordInstance)).when(responseSpec).bodyToMono(any());
            doReturn(Mono.just(recordInstance)).when(responseSpec).bodyToMono(any(Class.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock private record", e);
        }
    }
}
