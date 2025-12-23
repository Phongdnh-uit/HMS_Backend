package com.hms.medical_exam_service.dtos.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for cancelling a prescription.
 * Requires a reason for audit trail compliance.
 */
@Getter
@Setter
public class CancelPrescriptionRequest {
    
    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
}
