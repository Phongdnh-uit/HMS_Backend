package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.LabTestResult;
import com.hms.medical_exam_service.entities.ResultStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabTestResultRepository extends SimpleRepository<LabTestResult, String> {
    
    /**
     * Find all results for a medical exam
     */
    List<LabTestResult> findByMedicalExamId(String medicalExamId);
    
    /**
     * Find all results for a patient
     */
    List<LabTestResult> findByPatientId(String patientId);
    
    /**
     * Find results by status
     */
    List<LabTestResult> findByStatus(ResultStatus status);
    
    /**
     * Find pending results for a patient
     */
    List<LabTestResult> findByPatientIdAndStatus(String patientId, ResultStatus status);
    
    /**
     * Count results by status for a medical exam
     */
    long countByMedicalExamIdAndStatus(String medicalExamId, ResultStatus status);
    
    /**
     * Check if any abnormal results exist for a medical exam
     */
    boolean existsByMedicalExamIdAndIsAbnormalTrue(String medicalExamId);

    /**
     * Find results that are not yet grouped into a lab order
     */
    List<LabTestResult> findByLabOrderIsNull();

    /**
     * Find results by lab order ID
     */
    List<LabTestResult> findByLabOrderId(String labOrderId);
}
