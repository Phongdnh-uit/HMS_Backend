package com.hms.medicine_service.dtos.medicine;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class MedicineResponse {
    private String id;
    private String name;
    private String activeIngredient;
    private String unit;
    private String description;
    private Long quantity;
    private String concentration;
    private String packaging;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
