package com.hms.hr_service.dtos.employee;

import com.hms.hr_service.enums.EmployeeRole;
import com.hms.hr_service.enums.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EmployeeRequest {
    private String accountId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role is required")
    private EmployeeRole role;

    @NotBlank(message = "Department ID is required")
    private String departmentId;

    private String specialization;

    @Pattern(regexp = "^[A-Z]{2}-[0-9]{5}$", message = "License number must be format XX-12345")
    private String licenseNumber;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;

    private String address;

    @jakarta.validation.constraints.Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Status is required")
    private EmployeeStatus status;

    private Instant hiredAt;
}
