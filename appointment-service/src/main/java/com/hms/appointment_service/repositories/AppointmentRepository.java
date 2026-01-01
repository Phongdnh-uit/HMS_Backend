package com.hms.appointment_service.repositories;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AppointmentRepository extends SimpleRepository<Appointment, String> {

    /**
     * Find appointments by doctor and date range with specific status.
     * Used for bulk cancel operations.
     */
    List<Appointment> findByDoctorIdAndAppointmentTimeBetweenAndStatus(
            String doctorId, Instant startTime, Instant endTime, AppointmentStatus status);

    /**
     * Find appointments by doctor and date range (any status).
     */
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
            String doctorId, Instant startTime, Instant endTime);
    
    /**
     * Find appointments by patient ID with pagination.
     * Used by patient detail page to show only that patient's appointments.
     */
    org.springframework.data.domain.Page<Appointment> findByPatientId(
            String patientId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find all appointments within date range for stats calculation.
     */
    List<Appointment> findByAppointmentTimeBetween(Instant startTime, Instant endTime);
    
    /**
     * Count appointments by status within date range.
     */
    @Query("SELECT a.status, COUNT(a) FROM Appointment a WHERE a.appointmentTime >= :startTime AND a.appointmentTime < :endTime GROUP BY a.status")
    List<Object[]> countByStatusInDateRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Count appointments by type within date range.
     */
    @Query("SELECT a.type, COUNT(a) FROM Appointment a WHERE a.appointmentTime >= :startTime AND a.appointmentTime < :endTime AND a.type IS NOT NULL GROUP BY a.type")
    List<Object[]> countByTypeInDateRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Count appointments by department (from snapshot) within date range.
     */
    @Query("SELECT a.doctorDepartment, COUNT(a) FROM Appointment a WHERE a.appointmentTime >= :startTime AND a.appointmentTime < :endTime AND a.doctorDepartment IS NOT NULL GROUP BY a.doctorDepartment")
    List<Object[]> countByDepartmentInDateRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Count total appointments within date range.
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.appointmentTime >= :startTime AND a.appointmentTime < :endTime")
    long countInDateRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Count appointments by date within date range (for daily trend).
     */
    @Query("SELECT FUNCTION('DATE', a.appointmentTime), COUNT(a) FROM Appointment a WHERE a.appointmentTime >= :startTime AND a.appointmentTime < :endTime GROUP BY FUNCTION('DATE', a.appointmentTime) ORDER BY FUNCTION('DATE', a.appointmentTime)")
    List<Object[]> countByDateInDateRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    // ========== Queue-related queries ==========
    
    /**
     * Find max queue number for a specific date range (used to generate next queue number).
     */
    @Query("SELECT MAX(a.queueNumber) FROM Appointment a WHERE a.createdAt >= :startTime AND a.createdAt < :endTime AND a.queueNumber IS NOT NULL")
    Integer findMaxQueueNumberForDate(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Find doctor's queue for a specific date, ordered by priority and queue number.
     * Returns appointments with queue numbers (walk-in and emergency patients).
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.createdAt >= :startTime AND a.createdAt < :endTime AND a.queueNumber IS NOT NULL ORDER BY a.priority ASC, a.queueNumber ASC")
    List<Appointment> findDoctorQueueForDate(@Param("doctorId") String doctorId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    /**
     * Find all queues for a specific date across all doctors.
     * Returns appointments with queue numbers, ordered by priority and queue number.
     */
    @Query("SELECT a FROM Appointment a WHERE a.createdAt >= :startTime AND a.createdAt < :endTime AND a.queueNumber IS NOT NULL ORDER BY a.priority ASC, a.queueNumber ASC")
    List<Appointment> findAllQueuesForDate(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}


