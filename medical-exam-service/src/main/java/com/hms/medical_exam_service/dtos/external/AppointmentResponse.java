package com.hms.medical_exam_service.dtos.external;

import java.time.LocalDateTime;

/**
 * DTO for deserializing Appointment data from appointment-service.
 * Used to propagate snapshot data (patientId, patientName, doctorId, doctorName) 
 * to MedicalExam entity when creating an exam from an appointment.
 * 
 * IMPORTANT: This DTO must match the actual JSON structure returned by appointment-service!
 * JSON format: {"patient": {"id": "...", "fullName": "..."}, "doctor": {"id": "...", "fullName": "..."}, ...}
 */
public record AppointmentResponse(
    String id,
    PatientInfo patient,
    DoctorInfo doctor,
    LocalDateTime appointmentTime,
    String status,
    String type,
    String reason,
    String notes
) {
    /** Nested DTO for patient info in appointment response */
    public record PatientInfo(String id, String fullName) {}
    
    /** Nested DTO for doctor info in appointment response */
    public record DoctorInfo(String id, String fullName) {}
    
    /** Helper method to get patient ID, null-safe */
    public String patientId() {
        return patient != null ? patient.id() : null;
    }
    
    /** Helper method to get patient name, null-safe */
    public String patientName() {
        return patient != null ? patient.fullName() : null;
    }
    
    /** Helper method to get doctor ID, null-safe */
    public String doctorId() {
        return doctor != null ? doctor.id() : null;
    }
    
    /** Helper method to get doctor name, null-safe */
    public String doctorName() {
        return doctor != null ? doctor.fullName() : null;
    }

    /**
     * Creates mock appointment response for MVP testing.
     * In production, this comes from appointment-service via WebClient.
     */
    public static AppointmentResponse createMock(String appointmentId) {
        return new AppointmentResponse(
            appointmentId,
            new PatientInfo("patient-mock-001", "Mock Patient Name"),
            new DoctorInfo("doctor-mock-001", "BS. Mock Doctor"),
            LocalDateTime.now().minusHours(1), // past time = appointment completed
            "COMPLETED",
            "CONSULTATION",
            "Mock appointment reason",
            "Mock notes"
        );
    }
}

