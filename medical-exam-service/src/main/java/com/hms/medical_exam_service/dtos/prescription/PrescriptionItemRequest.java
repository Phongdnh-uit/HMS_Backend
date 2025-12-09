package com.hms.medical_exam_service.dtos.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrescriptionItemRequest {
    
    @NotBlank(message = "Medicine ID is required")
    private String medicineId;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
    
    @NotBlank(message = "Dosage is required")
    @Size(max = 255, message = "Dosage cannot exceed 255 characters")
    private String dosage;
    
    @Positive(message = "Duration days must be positive")
    private Integer durationDays;
    
    @Size(max = 1000, message = "Instructions cannot exceed 1000 characters")
    private String instructions;
}
