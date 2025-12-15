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

@Getter
@Setter
@Entity
@Table(name = "prescriptions")
@EntityListeners(AuditingEntityListener.class)
public class Prescription {

    /**
     * Prescription Status (simplified for HMS MVP)
     * 
     * ACTIVE: Prescription created, waiting for pharmacy to dispense
     * CANCELLED: Cancelled by doctor (stock restored via saga)
     * DISPENSED: Pharmacy has given medicines to patient (terminal state)
     */
    public enum Status {
        ACTIVE,
        CANCELLED,
        DISPENSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String medicalExamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // Cancellation tracking
    private Instant cancelledAt;
    private String cancelledBy;
    private String cancelReason;

    // Denormalized fields (query performance + snapshot for historical accuracy)
    private String patientId;
    private String patientName;  // Snapshot at prescription creation
    
    private String doctorId;
    private String doctorName;   // Snapshot at prescription creation

    @Column(nullable = false)
    private Instant prescribedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @CreatedBy
    private String createdBy;

    // Helper method to manage bidirectional relationship
    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }

    public void removeItem(PrescriptionItem item) {
        items.remove(item);
        item.setPrescription(null);
    }
}
