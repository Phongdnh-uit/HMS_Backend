package com.hms.medical_exam_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.medical_exam_service.entities.MedicalExam;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
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
    
    /**
     * Count exams grouped by diagnosis (for top diagnoses report).
     * Returns diagnosis text and count, ordered by count descending.
     */
    @Query("SELECT e.diagnosis, COUNT(e) as cnt FROM MedicalExam e WHERE e.diagnosis IS NOT NULL AND e.diagnosis <> '' GROUP BY e.diagnosis ORDER BY cnt DESC")
    List<Object[]> countTopDiagnoses();
    
    /**
     * Count total exams for percentage calculation.
     */
    @Query("SELECT COUNT(e) FROM MedicalExam e WHERE e.diagnosis IS NOT NULL AND e.diagnosis <> ''")
    long countWithDiagnosis();
    
    /**
     * Find exams where followUpDate matches the given date and notification not yet sent.
     * Used by notification service to send follow-up reminders.
     */
    List<MedicalExam> findByFollowUpDateAndFollowUpNotificationSentFalse(LocalDate followUpDate);
}
