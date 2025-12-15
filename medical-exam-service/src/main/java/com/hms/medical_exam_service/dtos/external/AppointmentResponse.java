package com.hms.medical_exam_service.dtos.external;

import java.time.LocalDateTime;

/**
 * DTO for deserializing Appointment data from appointment-service.
 * Used to propagate snapshot data (patientId, patientName, doctorId, doctorName) 
 * to MedicalExam entity when creating an exam from an appointment.
 * 
 * Snapshot Propagation Pattern:
 * - Appointment captures patientName/doctorName at booking time
 * - MedicalExam copies these snapshots at creation (no cross-service calls)
 * - Prescription copies snapshots from MedicalExam at creation
 */
public record AppointmentResponse(
    String id,
    String patientId,
    String patientName,
    String doctorId,
    String doctorName,
    LocalDateTime appointmentTime,
    String status,
    String type,
    String reason,
    String notes
) {
    /**
     * Creates mock appointment response for MVP testing.
     * In production, this comes from appointment-service via WebClient.
     */
    public static AppointmentResponse createMock(String appointmentId) {
        return new AppointmentResponse(
            appointmentId,
            "patient-mock-001",
            "Mock Patient Name",
            "doctor-mock-001",
            "BS. Mock Doctor",
            LocalDateTime.now().minusHours(1), // past time = appointment completed
            "COMPLETED",
            "CONSULTATION",
            "Mock appointment reason",
            "Mock notes"
        );
    }
}
