package com.hms.hr_service.dtos.schedule;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for schedule cancellation.
 */
@Getter
@Setter
public class CancelScheduleRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
