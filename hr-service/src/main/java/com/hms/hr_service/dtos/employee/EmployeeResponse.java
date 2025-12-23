package com.hms.hr_service.dtos.employee;

import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EmployeeResponse {
    private String id;
    private String accountId;
    private String fullName;
    private EmployeeRole role;
    private String departmentId;
    private String departmentName;
    private String specialization;
    private String licenseNumber;
    private String phoneNumber;
    private String address;
    private EmployeeStatus status;
    private Instant hiredAt;
    private Instant deletedAt;
    private String deletedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
