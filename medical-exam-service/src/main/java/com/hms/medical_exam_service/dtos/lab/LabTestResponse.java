package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.LabTestCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class LabTestResponse {
    
    private String id;
    private String code;
    private String name;
    private LabTestCategory category;
    private String description;
    private BigDecimal price;
    private String unit;
    private String normalRange;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
