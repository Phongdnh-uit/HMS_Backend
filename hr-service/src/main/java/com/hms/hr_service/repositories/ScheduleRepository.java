package com.hms.hr_service.repositories;

import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.ScheduleStatus;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends SimpleRepository<EmployeeSchedule, String> {

    /**
     * Find schedule by employee and date (unique constraint).
     */
    Optional<EmployeeSchedule> findByEmployeeIdAndWorkDate(String employeeId, LocalDate workDate);

    /**
     * Check if schedule exists for employee on date.
     */
    boolean existsByEmployeeIdAndWorkDate(String employeeId, LocalDate workDate);

    /**
     * Find schedules for an employee within date range.
     */
    List<EmployeeSchedule> findByEmployeeIdAndWorkDateBetween(
            String employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Find schedules for an employee within date range with status filter.
     */
    List<EmployeeSchedule> findByEmployeeIdAndWorkDateBetweenAndStatus(
            String employeeId, LocalDate startDate, LocalDate endDate, ScheduleStatus status);

    /**
     * Find doctor schedules for appointment booking.
     * Filters only employees with DOCTOR role.
     */
    @Query("""
            SELECT s FROM EmployeeSchedule s
            JOIN Employee e ON s.employeeId = e.id
            WHERE e.role = 'DOCTOR'
            AND e.deletedAt IS NULL
            AND s.workDate BETWEEN :startDate AND :endDate
            AND (:status IS NULL OR s.status = :status)
            AND (:doctorId IS NULL OR s.employeeId = :doctorId)
            AND (:departmentId IS NULL OR e.departmentId = :departmentId)
            ORDER BY s.workDate, s.startTime
            """)
    Page<EmployeeSchedule> findDoctorSchedules(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ScheduleStatus status,
            @Param("doctorId") String doctorId,
            @Param("departmentId") String departmentId,
            Pageable pageable);
}
