package com.hms.appointment_service.dtos.appointment;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.constants.AppointmentType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AppointmentResponse {
    private String id;
    private PatientResponse patient;
    private DoctorResponse doctor;
    private Instant appointmentTime;
    private AppointmentStatus status;
    private AppointmentType type;
    private String reason;
    private String notes;
    
    // Queue fields for walk-in patients
    private Integer queueNumber;
    private Integer priority;
    private String priorityReason;
    
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
