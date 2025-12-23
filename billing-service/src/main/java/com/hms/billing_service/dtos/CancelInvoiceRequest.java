package com.hms.billing_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelInvoiceRequest {

    @NotBlank(message = "cancelReason is required")
    @Size(min = 10, max = 500, message = "cancelReason must be between 10 and 500 characters")
    private String cancelReason;
}
