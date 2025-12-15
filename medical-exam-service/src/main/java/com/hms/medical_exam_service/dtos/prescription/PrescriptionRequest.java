package com.hms.medical_exam_service.dtos.prescription;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO for creating/updating prescriptions.
 * Note: medicalExamId comes from URL path variable, not request body.
 * Controller sets it in context before calling hook.
 */
@Getter
@Setter
public class PrescriptionRequest {
    
    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;
    
    @NotEmpty(message = "Prescription must have at least one item")
    @Valid
    private List<PrescriptionItemRequest> items;
}
