package com.hms.hr_service.dtos.schedule;

import com.hms.hr_service.enums.ScheduleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating/updating employee schedules.
 */
@Getter
@Setter
public class ScheduleRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private ScheduleStatus status;

    private String notes;
}
