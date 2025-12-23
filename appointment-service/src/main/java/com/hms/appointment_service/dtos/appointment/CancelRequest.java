package com.hms.appointment_service.dtos.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for cancelling an appointment.
 */
@Data
public class CancelRequest {

    @NotBlank(message = "Cancel reason is required")
    @Size(max = 500, message = "Cancel reason cannot exceed 500 characters")
    private String cancelReason;
}
