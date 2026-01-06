package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.DiagnosticImage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticImageRepository extends SimpleRepository<DiagnosticImage, String> {
    
    /**
     * Find all images for a lab test result
     */
    List<DiagnosticImage> findByLabTestResultIdOrderBySequenceNumberAsc(String labTestResultId);
    
    /**
     * Count images for a lab test result
     */
    long countByLabTestResultId(String labTestResultId);
    
    /**
     * Delete all images for a lab test result
     */
    void deleteByLabTestResultId(String labTestResultId);
}
