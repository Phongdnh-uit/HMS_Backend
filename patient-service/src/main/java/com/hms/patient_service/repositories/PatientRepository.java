package com.hms.patient_service.repositories;

import com.hms.common.repositories.SimpleRepository;
import com.hms.patient_service.entities.Patient;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends SimpleRepository<Patient, String> {
    Optional<Patient> findByEmail(String name);
    Optional<Patient> findByAccountId(String accountId);
    
    /**
     * Count patients by gender for stats.
     */
    @Query("SELECT p.gender, COUNT(p) FROM Patient p WHERE p.gender IS NOT NULL GROUP BY p.gender")
    List<Object[]> countByGender();
    
    /**
     * Count patients by blood type for stats.
     */
    @Query("SELECT p.bloodType, COUNT(p) FROM Patient p WHERE p.bloodType IS NOT NULL GROUP BY p.bloodType")
    List<Object[]> countByBloodType();
    
    /**
     * Count patients created after a specific date.
     */
    long countByCreatedAtAfter(Instant date);
    
    /**
     * Get all date of births for average age calculation.
     */
    @Query("SELECT p.dateOfBirth FROM Patient p WHERE p.dateOfBirth IS NOT NULL")
    List<java.time.LocalDate> findAllDateOfBirths();
    
    /**
     * Count patients by registration date within date range (for registration trend).
     */
    @Query("SELECT FUNCTION('DATE', p.createdAt), COUNT(p) FROM Patient p WHERE p.createdAt >= :startTime AND p.createdAt < :endTime GROUP BY FUNCTION('DATE', p.createdAt) ORDER BY FUNCTION('DATE', p.createdAt)")
    List<Object[]> countByCreatedAtGroupedByDate(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
