package com.hms.hr_service.dtos.department;

import com.hms.hr_service.enums.DepartmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class DepartmentResponse {
    private String id;
    private String name;
    private String description;
    private String headDoctorId;
    private String location;
    private String phoneExtension;
    private DepartmentStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
