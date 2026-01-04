package com.hms.medical_exam_service.entities;

/**
 * Status of a lab test result
 */
public enum ResultStatus {
    PENDING,      // Waiting to be performed
    PROCESSING,   // Being processed
    COMPLETED,    // Completed with results
    CANCELLED     // Cancelled
}
