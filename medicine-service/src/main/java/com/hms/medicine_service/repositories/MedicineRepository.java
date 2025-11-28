package com.hms.patient_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.patient_service.entities.Medicine;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicineRepository extends SimpleRepository<Medicine, String> {
}
