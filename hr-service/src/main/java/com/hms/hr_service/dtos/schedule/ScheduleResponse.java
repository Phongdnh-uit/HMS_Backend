package com.hms.hr_service.dtos.schedule;

import com.hms.hr_service.enums.ScheduleStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for employee schedules.
 * Includes nested employee info for convenience.
 */
@Getter
@Setter
public class ScheduleResponse {
    private String id;
    private String employeeId;
    private ScheduleEmployeeInfo employee;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ScheduleStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
