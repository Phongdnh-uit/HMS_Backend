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
import java.util.ArrayList;
import java.util.List;

/**
 * Lab Order - Groups multiple lab tests into a single order/requisition.
 * This represents a "Phiếu xét nghiệm" containing multiple individual tests.
 */
@Getter
@Setter
@Entity
@Table(name = "lab_orders")
@EntityListeners(AuditingEntityListener.class)
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String orderNumber;  // e.g., "XN-20241226-001"

    @Column(nullable = false)
    private String medicalExamId;  // FK to MedicalExam

    // Patient info (denormalized for query performance)
    private String patientId;
    private String patientName;

    // Ordering doctor info
    private String orderingDoctorId;
    private String orderingDoctorName;

    @Column(nullable = false)
    private Instant orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabOrderStatus status = LabOrderStatus.ORDERED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderPriority priority = OrderPriority.NORMAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // One-to-many relationship with LabTestResult
    @OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<LabTestResult> results = new ArrayList<>();

    // Computed fields for status tracking
    @Transient
    public int getTotalTests() {
        return results != null ? results.size() : 0;
    }

    @Transient
    public int getCompletedTests() {
        if (results == null) return 0;
        return (int) results.stream()
                .filter(r -> r.getStatus() == ResultStatus.COMPLETED)
                .count();
    }

    // Audit fields
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
