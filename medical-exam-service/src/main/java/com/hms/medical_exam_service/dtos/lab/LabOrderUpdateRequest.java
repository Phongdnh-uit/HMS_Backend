package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.LabOrderStatus;
import com.hms.medical_exam_service.entities.OrderPriority;
import lombok.Data;

/**
 * Request DTO for updating lab order status or priority
 */
@Data
public class LabOrderUpdateRequest {
    private LabOrderStatus status;
    private OrderPriority priority;
    private String notes;
}
