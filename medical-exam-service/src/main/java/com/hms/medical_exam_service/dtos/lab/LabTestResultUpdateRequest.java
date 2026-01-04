package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.ResultStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LabTestResultUpdateRequest {
    
    @Size(max = 255, message = "Result value cannot exceed 255 characters")
    private String resultValue;
    
    private ResultStatus status;
    
    private Boolean isAbnormal;
    
    @Size(max = 2000, message = "Interpretation cannot exceed 2000 characters")
    private String interpretation;
    
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
    
    @Size(max = 255, message = "Performed by cannot exceed 255 characters")
    private String performedBy;
    
    @Size(max = 255, message = "Interpreted by cannot exceed 255 characters")
    private String interpretedBy;
}
