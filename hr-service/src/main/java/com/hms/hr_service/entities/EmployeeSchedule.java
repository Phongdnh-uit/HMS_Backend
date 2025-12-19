package com.hms.hr_service.entities;

import com.hms.hr_service.enums.ScheduleStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Employee work schedule entity.
 * Defines when an employee (doctor, nurse, etc.) is available for work.
 * Used by appointment-service to validate doctor availability.
 */
@Getter
@Setter
@Entity
@Table(name = "employee_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedules_employee_date",
                columnNames = {"employee_id", "work_date"}
        ))
@EntityListeners(AuditingEntityListener.class)
public class EmployeeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "employee_id", nullable = false)
    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @Column(name = "work_date", nullable = false)
    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    @Column(name = "start_time", nullable = false)
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status is required")
    private ScheduleStatus status = ScheduleStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
