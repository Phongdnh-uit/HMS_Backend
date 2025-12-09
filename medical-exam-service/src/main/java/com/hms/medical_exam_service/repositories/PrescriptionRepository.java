package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends SimpleRepository<Prescription, String> {
    
    /**
     * Find prescription by medical exam ID (one prescription per exam)
     */
    Optional<Prescription> findByMedicalExamId(String medicalExamId);
    
    /**
     * Check if prescription exists for exam (for uniqueness validation)
     */
    boolean existsByMedicalExamId(String medicalExamId);
    
    /**
     * Find all prescriptions for a patient (with pagination)
     */
    Page<Prescription> findByPatientId(String patientId, Pageable pageable);
    
    /**
     * Find all prescriptions for a patient with status filter (with pagination)
     */
    Page<Prescription> findByPatientIdAndStatus(String patientId, Prescription.Status status, Pageable pageable);
    
    /**
     * Find all prescriptions for a patient (list - for backward compatibility)
     */
    List<Prescription> findByPatientId(String patientId);
    
    /**
     * Find all prescriptions by a doctor
     */
    List<Prescription> findByDoctorId(String doctorId);
    
    /**
     * Find prescriptions within a date range
     */
    List<Prescription> findByPrescribedAtBetween(Instant startDate, Instant endDate);
    
    /**
     * Find prescriptions for a patient within a date range
     */
    List<Prescription> findByPatientIdAndPrescribedAtBetween(String patientId, Instant startDate, Instant endDate);
}
