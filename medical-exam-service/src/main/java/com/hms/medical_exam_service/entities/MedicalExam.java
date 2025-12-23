package com.hms.medical_exam_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "medical_exams")
@EntityListeners(AuditingEntityListener.class)
public class MedicalExam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String appointmentId;

    // Denormalized fields (query performance + snapshot for historical accuracy)
    private String patientId;
    private String patientName;  // Snapshot at exam creation
    
    private String doctorId;
    private String doctorName;   // Snapshot at exam creation

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String treatment;

    // Vitals
    private Double temperature;

    private Integer bloodPressureSystolic;

    private Integer bloodPressureDiastolic;

    private Integer heartRate;

    private Double weight;

    private Double height;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Denormalized flag for invoice generation workflow
    // true = has prescription, invoice generated after DISPENSED
    // false = no prescription, invoice can be generated immediately on exam finalization
    @Column(nullable = false)
    private Boolean hasPrescription = false;

    private Instant examDate;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    // Follow-up notification fields
    // Doctor sets this when completing exam to schedule a follow-up reminder
    private LocalDate followUpDate;
    
    // Flag to track if notification has been sent (prevents duplicate emails)
    @Column(nullable = false)
    private Boolean followUpNotificationSent = false;
}
