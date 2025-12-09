package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.PrescriptionItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends SimpleRepository<PrescriptionItem, String> {
    
    /**
     * Find all items for a prescription
     */
    List<PrescriptionItem> findByPrescriptionId(String prescriptionId);
    
    /**
     * Find items by medicine ID (useful for tracking medicine usage)
     */
    List<PrescriptionItem> findByMedicineId(String medicineId);
    
    /**
     * Delete all items for a prescription (used when prescription is deleted)
     */
    void deleteByPrescriptionId(String prescriptionId);
}
