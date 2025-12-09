package com.hms.appointment_service.dtos.appointment;

import com.hms.appointment_service.constants.AppointmentType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AppointmentRequest {
    String patientId;
    String doctorId;
    String appointmentTime;
    AppointmentType type;
    String reason;

}
