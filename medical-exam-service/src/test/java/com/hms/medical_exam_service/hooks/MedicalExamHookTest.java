package com.hms.medical_exam_service.hooks;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.external.AppointmentResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalExamHookTest {

    @Mock
    private MedicalExamRepository medicalExamRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private MedicalExamHook medicalExamHook;

    private MedicalExamRequest request;
    private MedicalExam entity;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        request = new MedicalExamRequest();
        request.setAppointmentId("appt-123");
        
        entity = new MedicalExam();
        entity.setId("exam-123");
        entity.setAppointmentId("appt-123");
        
        context = new HashMap<>();
    }

    @Test
    @DisplayName("validateCreate: should succeed when appointment is valid and completed")
    void validateCreateSucces() {
        // Given
        when(medicalExamRepository.existsByAppointmentId(anyString())).thenReturn(false);
        // Note: fetchAppointment in hook uses a static mock creation if flag is true, 
        // effectively bypassing the webclient mock needs for the default MVP path.
        // We rely on the internal mock behavior of the hook for now.
        
        // When
        assertDoesNotThrow(() -> medicalExamHook.validateCreate(request, context));
        
        // Then
        assertTrue(context.containsKey("appointment"));
    }

    @Test
    @DisplayName("validateCreate: should throw exception if exam already exists for appointment")
    void validateCreateExists() {
        // Given
        when(medicalExamRepository.existsByAppointmentId(anyString())).thenReturn(true);

        // When & Then
        ApiException exception = assertThrows(ApiException.class, 
            () -> medicalExamHook.validateCreate(request, context));
        
        assertEquals(ErrorCode.EXAM_EXISTS, exception.getErrorCode());
        verify(medicalExamRepository).existsByAppointmentId("appt-123");
    }

    @Test
    @DisplayName("enrichCreate: should copy snapshot data from appointment to exam")
    void enrichCreateSnapshot() {
        // Given
        // Simulate context populated by validateCreate with a mock appointment
        AppointmentResponse mockAppt = new AppointmentResponse(
                "appt-123", 
                new AppointmentResponse.PatientInfo("p-1", "John Doe"),
                new AppointmentResponse.DoctorInfo("doc-1", "Dr. Smith"),
                java.time.LocalDateTime.now(), 
                "COMPLETED", "CONSULTATION", "Checkup", "None"
        );
        context.put("appointment", mockAppt);

        // When
        medicalExamHook.enrichCreate(request, entity, context);

        // Then
        assertNotNull(entity.getExamDate());
        assertEquals("p-1", entity.getPatientId());
        assertEquals("John Doe", entity.getPatientName());
        assertEquals("doc-1", entity.getDoctorId());
        assertEquals("Dr. Smith", entity.getDoctorName());
    }

    @Test
    @DisplayName("validateUpdate: should throw exception if exam is older than 24 hours")
    void validateUpdateTooOld() {
        // Given
        entity.setCreatedAt(Instant.now().minus(25, ChronoUnit.HOURS));

        // When & Then
        ApiException exception = assertThrows(ApiException.class, 
            () -> medicalExamHook.validateUpdate(entity.getId(), request, entity, context));
            
        assertEquals(ErrorCode.EXAM_NOT_MODIFIABLE, exception.getErrorCode());
    }

    @Test
    @DisplayName("validateUpdate: should succeed if exam is recent")
    void validateUpdateRecent() {
        // Given
        entity.setCreatedAt(Instant.now().minus(23, ChronoUnit.HOURS));

        // When & Then
        assertDoesNotThrow(() -> medicalExamHook.validateUpdate(entity.getId(), request, entity, context));
    }

    @Test
    @DisplayName("validateDelete: should always block deletion")
    void validateDeleteBlocked() {
        // When & Then
        ApiException exception = assertThrows(ApiException.class, 
            () -> medicalExamHook.validateDelete(entity.getId()));
            
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }
}
