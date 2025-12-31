package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.LabTestCategory;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LabTestRequest {
    
    @NotBlank(message = "Lab test code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;
    
    @NotBlank(message = "Lab test name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;
    
    @NotNull(message = "Category is required")
    private LabTestCategory category;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    private BigDecimal price;
    
    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;
    
    @Size(max = 100, message = "Normal range cannot exceed 100 characters")
    private String normalRange;
    
    private Boolean isActive = true;
}
