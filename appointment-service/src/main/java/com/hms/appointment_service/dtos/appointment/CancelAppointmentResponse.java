package com.hms.appointment_service.dtos.appointment;

import com.hms.appointment_service.constants.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Response DTO for appointment cancellation.
 * Contains only cancellation-relevant information.
 */
@Getter
@Setter
@Builder
public class CancelAppointmentResponse {
    private String id;
    private PatientResponse patient;
    private DoctorResponse doctor;
    private AppointmentStatus status;
    private String cancelReason;
    private Instant cancelledAt;
    private Instant updatedAt;
    private String updatedBy;
}
