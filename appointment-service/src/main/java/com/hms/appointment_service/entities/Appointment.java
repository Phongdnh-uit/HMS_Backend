package com.hms.appointment_service.entities;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.constants.AppointmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Table
@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String patientId;

    // Snapshot at creation for historical accuracy
    private String patientName;

    private String doctorId;

    // Snapshot at creation for historical accuracy
    private String doctorName;

    // Snapshot at creation for historical accuracy
    private String doctorDepartment;

    private Instant appointmentTime;

    private AppointmentStatus status;
    
    @Enumerated(EnumType.STRING)
    private AppointmentType type;

    private String reason;

    private String notes;

    private Instant cancelledAt;

    private String cancelReason;

    // Queue fields for walk-in patients
    private Integer queueNumber;  // Daily queue number (1, 2, 3...)
    
    private Integer priority;  // Lower = higher priority (10=Emergency, 100=Normal)
    
    private String priorityReason;  // EMERGENCY, ELDERLY, PREGNANT, APPOINTMENT, etc.

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

}

