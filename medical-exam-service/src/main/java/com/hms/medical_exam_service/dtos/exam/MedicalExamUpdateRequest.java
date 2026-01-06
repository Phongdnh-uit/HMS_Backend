package com.hms.medical_exam_service.dtos.exam;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for updating an existing medical exam.
 * Unlike MedicalExamRequest, appointmentId is NOT required since
 * the appointment cannot be changed after exam creation.
 */
@Getter
@Setter
public class MedicalExamUpdateRequest {
    
    @Size(max = 2000, message = "Diagnosis cannot exceed 2000 characters")
    private String diagnosis;
    
    @Size(max = 2000, message = "Symptoms cannot exceed 2000 characters")
    private String symptoms;
    
    @Size(max = 2000, message = "Treatment cannot exceed 2000 characters")
    private String treatment;
    
    // Vitals - all optional but validated if provided
    @DecimalMin(value = "30.0", message = "Temperature must be at least 30.0°C")
    @DecimalMax(value = "45.0", message = "Temperature must be at most 45.0°C")
    private Double temperature;
    
    @Min(value = 50, message = "Systolic blood pressure must be at least 50 mmHg")
    @Max(value = 250, message = "Systolic blood pressure must be at most 250 mmHg")
    private Integer bloodPressureSystolic;
    
    @Min(value = 30, message = "Diastolic blood pressure must be at least 30 mmHg")
    @Max(value = 150, message = "Diastolic blood pressure must be at most 150 mmHg")
    private Integer bloodPressureDiastolic;
    
    @Min(value = 30, message = "Heart rate must be at least 30 bpm")
    @Max(value = 200, message = "Heart rate must be at most 200 bpm")
    private Integer heartRate;
    
    @Positive(message = "Weight must be positive")
    private Double weight;
    
    @Positive(message = "Height must be positive")
    private Double height;
    
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    /**
     * Optional follow-up date for scheduling a reminder notification.
     * When set, patient will receive an email reminder on this date.
     */
    @FutureOrPresent(message = "Follow-up date must be today or in the future")
    private LocalDate followUpDate;
    
    /**
     * Exam status update.
     */
    private String status;
}
