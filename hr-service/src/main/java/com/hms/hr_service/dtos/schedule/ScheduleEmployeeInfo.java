package com.hms.hr_service.dtos.schedule;

import lombok.Getter;
import lombok.Setter;

/**
 * Nested employee info for schedule responses.
 */
@Getter
@Setter
public class ScheduleEmployeeInfo {
    private String id;
    private String fullName;
    private String role;
    private String specialization;
    private ScheduleDepartmentInfo department;
}
