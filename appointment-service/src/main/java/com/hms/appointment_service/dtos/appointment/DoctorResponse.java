package com.hms.appointment_service.dtos.appointment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DoctorResponse {
    private String id;
    private String fullName;
    private String department;
}
