package com.hms.appointment_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for registering a walk-in patient.
 * Used by receptionist to create appointment for patients who come directly.
 */
@Data
public class WalkInRequest {
    
    @NotBlank(message = "Patient ID is required")
    private String patientId;
    
    @NotBlank(message = "Doctor ID is required")
    private String doctorId;
    
    private String reason;
    
    /**
     * Optional priority reason for special cases.
     * Values: ELDERLY, PREGNANT, DISABILITY, etc.
     * If null, will be treated as normal walk-in.
     */
    private String priorityReason;
}
