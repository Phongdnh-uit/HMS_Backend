package com.hms.medicine_service.dtos.medicine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class MedicineRequest {
    @NotBlank(message = "Medicine name is required")
    private String name;

    @NotBlank(message = "Active ingredient is required")
    private String activeIngredient;


    @NotBlank(message = "Unit is required")
    private String unit;

    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Long quantity;

    private String concentration;

    private String packaging;

    @NotNull(message = "Purchase price is required")
    @Min(value = 0, message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    @NotNull(message = "Selling price is required")
    @Min(value = 0, message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    @NotNull(message = "Expiration date is required")
    private Instant expiresAt;

    @NotNull(message = "Category is required")
    private String categoryId;
}
