package com.hms.appointment_service.constants;

/**
 * Appointment status lifecycle.
 * - SCHEDULED: Appointment booked, waiting to happen
 * - IN_PROGRESS: Doctor is currently seeing the patient
 * - COMPLETED: Consultation finished, can create medical exam
 * - CANCELLED: Appointment cancelled by patient/staff
 * - NO_SHOW: Patient did not attend
 */
public enum AppointmentStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
