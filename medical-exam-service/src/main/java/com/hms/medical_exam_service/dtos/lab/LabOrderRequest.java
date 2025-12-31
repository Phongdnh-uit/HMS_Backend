package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.OrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for creating a new lab order with multiple tests
 */
@Data
public class LabOrderRequest {

    @NotBlank(message = "Medical exam ID is required")
    private String medicalExamId;

    @NotEmpty(message = "At least one lab test is required")
    private List<String> labTestIds;  // List of LabTest IDs to order

    private OrderPriority priority = OrderPriority.NORMAL;

    private String notes;

    // Patient info (can be provided or fetched from exam)
    private String patientId;
    private String patientName;

    // Doctor info (can be provided or fetched from context)
    private String orderingDoctorId;
    private String orderingDoctorName;
}
