package com.hms.medical_exam_service.dtos.prescription;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class PrescriptionResponse {
    
    private String id;
    
    // Nested medical exam info
    private MedicalExamInfo medicalExam;
    
    // Nested patient info
    private PatientInfo patient;
    
    // Nested doctor info
    private DoctorInfo doctor;
    
    // Status for immutable prescription pattern
    private String status;  // ACTIVE, CANCELLED, DISPENSED

    // Cancellation info (populated when status=CANCELLED)
    private CancellationInfo cancellation;
    
    private Instant prescribedAt;
    private String notes;
    
    private List<PrescriptionItemResponse> items;
    
    // For list view - summary field
    private Integer itemCount;
    
    // Audit fields
    private Instant createdAt;
    private String createdBy;
    
    // Nested DTOs
    @Getter
    @Setter
    public static class MedicalExamInfo {
        private String id;
    }
    
    @Getter
    @Setter
    public static class PatientInfo {
        private String id;
        private String fullName;
    }
    
    @Getter
    @Setter
    public static class DoctorInfo {
        private String id;
        private String fullName;
    }
    
    @Getter
    @Setter
    public static class CancellationInfo {
        private Instant cancelledAt;
        private String cancelledBy;
        private String reason;
    }
}
