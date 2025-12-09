package com.hms.medicine_service.dtos.medicine;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for stock adjustment.
 * Uses delta value: positive to add stock, negative to deduct.
 */
@Getter
@Setter
public class StockUpdateRequest {
    
    @NotNull(message = "Delta is required")
    private Integer delta;  // Positive to add, negative to deduct
}
