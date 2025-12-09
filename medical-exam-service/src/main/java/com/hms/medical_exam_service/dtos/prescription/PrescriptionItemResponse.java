package com.hms.medical_exam_service.dtos.prescription;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class PrescriptionItemResponse {
    
    private String id;
    
    // Nested medicine info (snapshot data)
    private MedicineInfo medicine;
    
    private Integer quantity;
    private BigDecimal unitPrice;
    private String dosage;
    private Integer durationDays;
    private String instructions;
    
    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
    
    // Nested DTO
    @Getter
    @Setter
    public static class MedicineInfo {
        private String id;
        private String name;
    }
}
