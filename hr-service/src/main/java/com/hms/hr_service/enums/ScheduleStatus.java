package com.hms.hr_service.enums;

/**
 * Status of an employee schedule.
 * 
 * Lifecycle:
 * AVAILABLE → BOOKED (when all slots filled)
 * AVAILABLE/BOOKED → PENDING_CANCEL → CANCELLED (saga pattern)
 * PENDING_CANCEL → AVAILABLE/BOOKED (rollback on failure)
 * 
 * - AVAILABLE: Schedule is open for appointments
 * - BOOKED: All time slots are filled
 * - PENDING_CANCEL: Intermediate state during saga - appointments being cancelled
 * - CANCELLED: Schedule was cancelled, all appointments were cancelled
 */
public enum ScheduleStatus {
    AVAILABLE,
    BOOKED,
    PENDING_CANCEL,
    CANCELLED
}

