package com.hms.billing_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvoiceRequest {

    @NotBlank(message = "appointmentId is required")
    private String appointmentId;
    
    // Optional: exam ID for direct prescription lookup
    private String examId;

    private String notes;
}
