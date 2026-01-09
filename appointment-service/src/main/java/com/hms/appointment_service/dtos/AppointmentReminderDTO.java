package com.hms.appointment_service.dtos;

import java.time.Instant;

/**
 * DTO for appointment reminder notifications.
 * Contains essential info for sending reminder emails.
 */
public record AppointmentReminderDTO(
    String id,
    String patientId,
    String patientName,
    String doctorName,
    String doctorDepartment,
    Instant appointmentTime,
    String reason
) {}
