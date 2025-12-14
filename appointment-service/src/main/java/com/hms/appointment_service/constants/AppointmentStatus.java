package com.hms.appointment_service.constants;

/**
 * Appointment status lifecycle.
 * - SCHEDULED: Appointment booked, waiting to happen
 * - COMPLETED: Consultation finished, can create medical exam
 * - CANCELLED: Appointment cancelled by patient/staff
 * - NO_SHOW: Patient did not attend
 */
public enum AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}