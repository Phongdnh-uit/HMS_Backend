package com.hms.medical_exam_service.dtos.exam;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicalExamRequest {
    
    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;
    
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
}
