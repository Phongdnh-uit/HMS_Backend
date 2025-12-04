package com.hms.hr_service.dtos.department;

import com.hms.hr_service.enums.DepartmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentRequest {
    @NotBlank(message = "Department name is required")
    private String name;

    private String description;

    private String headDoctorId;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Phone extension is required")
    private String phoneExtension;

    @NotNull
    private DepartmentStatus status;
}