package com.hms.patient_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.patient_service.entities.Patient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends SimpleRepository<Patient, String> {
    Optional<Patient> findByEmail(String name);
}
