package com.hms.medicine_service.dtos.medicine;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Response DTO for stock adjustment showing updated quantity.
 */
@Getter
@Setter
@Builder
public class StockUpdateResponse {
    private String id;
    private String name;
    private Long quantity;
    private Instant updatedAt;
}
