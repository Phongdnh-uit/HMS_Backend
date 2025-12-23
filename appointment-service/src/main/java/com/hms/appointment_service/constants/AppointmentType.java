package com.hms.appointment_service.constants;

/**
 * Types of appointments in the HMS system.
 * - CONSULTATION: First visit or regular checkup
 * - FOLLOW_UP: Return visit after initial treatment
 * - EMERGENCY: Urgent/priority cases
 */
public enum AppointmentType {
    CONSULTATION,
    FOLLOW_UP,
    EMERGENCY
}
