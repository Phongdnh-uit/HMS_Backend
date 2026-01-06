package com.hms.medical_exam_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lab Test Definition - Types of tests available (e.g., CBC, X-Ray Chest, MRI Brain)
 */
@Getter
@Setter
@Entity
@Table(name = "lab_tests")
@EntityListeners(AuditingEntityListener.class)
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;  // e.g., "CBC", "XRAY_CHEST", "MRI_BRAIN"

    @Column(nullable = false)
    private String name;  // e.g., "Complete Blood Count", "X-Ray Chest"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabTestCategory category;  // LAB, IMAGING, PATHOLOGY

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    private String unit;  // e.g., "mg/dL", "cells/μL"

    private String normalRange;  // e.g., "4.5-11.0", "negative"

    @Column(nullable = false)
    private Boolean isActive = true;

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
