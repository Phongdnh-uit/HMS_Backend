package com.hms.hr_service.dtos.schedule;

import lombok.Getter;
import lombok.Setter;

/**
 * Nested department info for schedule responses.
 */
@Getter
@Setter
public class ScheduleDepartmentInfo {
    private String id;
    private String name;
}
