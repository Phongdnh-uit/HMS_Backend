package com.hms.medical_exam_service.dtos.lab;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabTestResultRequest {
    
    @NotBlank(message = "Medical exam ID is required")
    private String medicalExamId;
    
    @NotBlank(message = "Lab test ID is required")
    private String labTestId;
    
    // Result data - optional on create, updated later
    @Size(max = 255, message = "Result value cannot exceed 255 characters")
    private String resultValue;
    
    private Boolean isAbnormal;
    
    @Size(max = 2000, message = "Interpretation cannot exceed 2000 characters")
    private String interpretation;
    
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
    
    @Size(max = 255, message = "Performed by cannot exceed 255 characters")
    private String performedBy;
}
