package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.LabTest;
import com.hms.medical_exam_service.entities.LabTestCategory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestRepository extends SimpleRepository<LabTest, String> {
    
    /**
     * Find lab test by unique code
     */
    Optional<LabTest> findByCode(String code);
    
    /**
     * Check if code exists (for uniqueness validation)
     */
    boolean existsByCode(String code);
    
    /**
     * Find all active lab tests
     */
    List<LabTest> findByIsActiveTrue();
    
    /**
     * Find lab tests by category
     */
    List<LabTest> findByCategory(LabTestCategory category);
    
    /**
     * Find active lab tests by category
     */
    List<LabTest> findByCategoryAndIsActiveTrue(LabTestCategory category);
}
