package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.MedicalExam;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalExamRepository extends SimpleRepository<MedicalExam, String> {
    
    /**
     * Find exam by appointment ID (UNIQUE constraint - one exam per appointment)
     */
    Optional<MedicalExam> findByAppointmentId(String appointmentId);
    
    /**
     * Check if exam exists for appointment (for uniqueness validation)
     */
    boolean existsByAppointmentId(String appointmentId);
    
    /**
     * Find all exams for a patient
     */
    List<MedicalExam> findByPatientId(String patientId);
    
    /**
     * Find all exams by a doctor
     */
    List<MedicalExam> findByDoctorId(String doctorId);
    
    /**
     * Find exams within a date range
     */
    List<MedicalExam> findByExamDateBetween(Instant startDate, Instant endDate);
    
    /**
     * Find exams for a patient within a date range
     */
    List<MedicalExam> findByPatientIdAndExamDateBetween(String patientId, Instant startDate, Instant endDate);
    
    /**
     * Find exams by a doctor within a date range
     */
    List<MedicalExam> findByDoctorIdAndExamDateBetween(String doctorId, Instant startDate, Instant endDate);
}
