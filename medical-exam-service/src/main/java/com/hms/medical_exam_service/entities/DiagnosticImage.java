package com.hms.medical_exam_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Diagnostic Image - Images/files attached to lab test results (X-Ray, MRI, etc.)
 */
@Getter
@Setter
@Entity
@Table(name = "diagnostic_images")
@EntityListeners(AuditingEntityListener.class)
public class DiagnosticImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String labTestResultId;  // FK to LabTestResult

    @Column(nullable = false)
    private String fileName;  // Original filename

    @Column(nullable = false)
    private String storagePath;  // MinIO object path: "lab-images/{patientId}/{resultId}/{uuid}.jpg"

    @Column(nullable = false)
    private String contentType;  // "image/jpeg", "image/png", "application/pdf"

    private Long fileSize;  // bytes

    private String thumbnailPath;  // Thumbnail for preview (optional)

    @Enumerated(EnumType.STRING)
    private ImageType imageType;  // XRAY, CT, MRI, ULTRASOUND, PHOTO

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer sequenceNumber;  // Order in series (for CT/MRI slices)

    private String uploadedBy;

    @CreatedDate
    private Instant createdAt;
}
