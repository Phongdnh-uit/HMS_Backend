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

/**
 * Lab Test Result - Actual result of a test ordered for a patient
 */
@Getter
@Setter
@Entity
@Table(name = "lab_test_results")
@EntityListeners(AuditingEntityListener.class)
public class LabTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String medicalExamId;  // FK to MedicalExam

    // Reference to the lab order this result belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id")
    private LabOrder labOrder;

    @Column(nullable = false)
    private String labTestId;  // FK to LabTest

    // Denormalized for query performance
    private String patientId;
    private String patientName;

    // Lab test info snapshot
    private String labTestCode;
    private String labTestName;

    @Enumerated(EnumType.STRING)
    private LabTestCategory labTestCategory;
    
    private java.math.BigDecimal labTestPrice;

    // Result data
    private String resultValue;  // e.g., "8.5", "Positive", "Normal"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultStatus status = ResultStatus.PENDING;

    @Column(nullable = false)
    private Boolean isAbnormal = false;  // Flag for abnormal results

    @Column(columnDefinition = "TEXT")
    private String interpretation;  // Doctor's interpretation

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Technician/Doctor info
    private String performedBy;  // Technician who performed test
    private String interpretedBy;  // Doctor who interpreted

    private Instant performedAt;
    private Instant completedAt;

    // Audit
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
