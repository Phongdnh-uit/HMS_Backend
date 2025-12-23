package com.hms.medical_exam_service.dtos.exam;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class MedicalExamResponse {
    
    private String id;
    
    // Nested appointment info
    private AppointmentInfo appointment;
    
    // Nested patient info
    private PatientInfo patient;
    
    // Nested doctor info
    private DoctorInfo doctor;
    
    private String diagnosis;
    private String symptoms;
    private String treatment;
    
    // Vitals as nested object per API contract
    private VitalsInfo vitals;
    
    private String notes;
    private Instant examDate;
    
    // Audit fields
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Whether this exam has a prescription (populated by hook)
    private Boolean hasPrescription;
    
    // Follow-up date for scheduling reminder notification
    private LocalDate followUpDate;
    
    // Nested DTOs
    @Getter
    @Setter
    public static class AppointmentInfo {
        private String id;
        private Instant appointmentTime;
    }
    
    @Getter
    @Setter
    public static class PatientInfo {
        private String id;
        private String fullName;
        private String phoneNumber;
    }
    
    @Getter
    @Setter
    public static class DoctorInfo {
        private String id;
        private String fullName;
        private String department;
        private String phoneNumber;
    }
    
    @Getter
    @Setter
    public static class VitalsInfo {
        private Double temperature;
        private Integer bloodPressureSystolic;
        private Integer bloodPressureDiastolic;
        private Integer heartRate;
        private Double weight;
        private Double height;
    }
}
