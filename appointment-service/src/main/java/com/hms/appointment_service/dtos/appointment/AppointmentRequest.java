package com.hms.appointment_service.dtos.appointment;

import com.hms.appointment_service.constants.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppointmentRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotBlank(message = "Appointment time is required")
    private String appointmentTime;

    @NotNull(message = "Appointment type is required")
    private AppointmentType type;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}
