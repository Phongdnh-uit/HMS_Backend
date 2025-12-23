package com.hms.appointment_service.repositories;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.repositories.SimpleRepository;
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
}
