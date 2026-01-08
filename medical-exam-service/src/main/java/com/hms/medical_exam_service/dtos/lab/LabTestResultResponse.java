package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.LabTestCategory;
import com.hms.medical_exam_service.entities.ResultStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class LabTestResultResponse {
    
    private String id;
    private String medicalExamId;
    private String labTestId;
    
    // Patient info
    private String patientId;
    private String patientName;
    
    // Lab test info
    private String labTestCode;
    private String labTestName;
    private LabTestCategory labTestCategory;
    private java.math.BigDecimal labTestPrice;
    
    // Result data
    private String resultValue;
    private ResultStatus status;
    private Boolean isAbnormal;
    private String interpretation;
    private String notes;
    
    // Technician/Doctor info
    private String performedBy;
    private String interpretedBy;
    
    private Instant performedAt;
    private Instant completedAt;
    
    // Images attached to this result
    private List<DiagnosticImageResponse> images;
    
    private Instant createdAt;
    private Instant updatedAt;
}
