package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.LabOrderStatus;
import com.hms.medical_exam_service.entities.OrderPriority;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for lab order with all test results
 */
@Data
public class LabOrderResponse {

    private String id;
    private String orderNumber;
    private String medicalExamId;

    // Patient info
    private String patientId;
    private String patientName;

    // Doctor info
    private String orderingDoctorId;
    private String orderingDoctorName;

    private Instant orderDate;
    private LabOrderStatus status;
    private OrderPriority priority;
    private String notes;

    // Test results
    private List<LabTestResultResponse> results;

    // Summary info
    private int totalTests;
    private int completedTests;
    private int pendingTests;

    // Audit
    private Instant createdAt;
    private Instant updatedAt;
}
