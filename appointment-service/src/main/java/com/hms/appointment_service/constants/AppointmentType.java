package com.hms.appointment_service.constants;

/**
 * Types of appointments in the HMS system.
 * - CONSULTATION: First visit or regular checkup (pre-booked)
 * - FOLLOW_UP: Return visit after initial treatment
 * - EMERGENCY: Urgent/priority cases
 * - WALK_IN: Patient comes directly without prior appointment
 */
public enum AppointmentType {
    CONSULTATION,
    FOLLOW_UP,
    EMERGENCY,
    WALK_IN
}
