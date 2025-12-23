package com.hms.hr_service.dtos.schedule;

import com.hms.hr_service.enums.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for schedule cancellation.
 * Includes information about cancelled appointments.
 */
@Getter
@Setter
@Builder
public class CancelScheduleResponse {
    private String scheduleId;
    private String employeeId;
    private String employeeName;
    private LocalDate workDate;
    private ScheduleStatus status;
    private String cancelReason;
    private int cancelledAppointments;
    private Instant cancelledAt;
}
