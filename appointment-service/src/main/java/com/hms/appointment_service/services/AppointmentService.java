package com.hms.appointment_service.services;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.repositories.AppointmentRepository;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Service for appointment-specific business logic.
 * Handles operations beyond generic CRUD.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    /**
     * Bulk cancel all SCHEDULED appointments for a doctor on a specific date.
     * Called by hr-service when a schedule is cancelled.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to cancel appointments for
     * @param reason   Cancellation reason
     * @return Number of appointments cancelled
     */
    @Transactional
    public int cancelByDoctorAndDate(String doctorId, LocalDate date, String reason) {
        // Convert LocalDate to Instant range for query
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        // Find all SCHEDULED appointments for this doctor on this date
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                        doctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED);

        if (appointments.isEmpty()) {
            log.info("No scheduled appointments found for doctor {} on {}", doctorId, date);
            return 0;
        }

        log.info("Cancelling {} appointments for doctor {} on {}", appointments.size(), doctorId, date);

        // Cancel each appointment
        Instant now = Instant.now();
        for (Appointment appointment : appointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelledAt(now);
            appointment.setCancelReason(reason);
        }

        appointmentRepository.saveAll(appointments);
        log.info("Successfully cancelled {} appointments", appointments.size());

        return appointments.size();
    }

    /**
     * Count SCHEDULED appointments for a doctor on a specific date.
     * Used by hr-service to validate if schedule can be deleted.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to check
     * @return Count of active appointments
     */
    public int countByDoctorAndDate(String doctorId, LocalDate date) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                        doctorId, startOfDay, endOfDay, AppointmentStatus.SCHEDULED);

        return appointments.size();
    }

    /**
     * Restore CANCELLED appointments for a doctor on a specific date back to SCHEDULED.
     * This is a COMPENSATION action called when the cancel saga fails after appointments
     * were already cancelled.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to restore appointments for
     * @return Number of appointments restored
     */
    @Transactional
    public int restoreByDoctorAndDate(String doctorId, LocalDate date) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        // Find all CANCELLED appointments for this doctor on this date
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                        doctorId, startOfDay, endOfDay, AppointmentStatus.CANCELLED);

        if (appointments.isEmpty()) {
            log.info("No cancelled appointments found to restore for doctor {} on {}", doctorId, date);
            return 0;
        }

        log.info("COMPENSATION: Restoring {} cancelled appointments for doctor {} on {}", 
                appointments.size(), doctorId, date);

        // Restore each appointment back to SCHEDULED
        for (Appointment appointment : appointments) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
            appointment.setCancelledAt(null);
            appointment.setCancelReason(null);
        }

        appointmentRepository.saveAll(appointments);
        log.info("COMPENSATION SUCCESS: Restored {} appointments to SCHEDULED", appointments.size());

        return appointments.size();
    }

    /**
     * Cancel a single appointment by ID.
     * Business Rules:
     * - Only SCHEDULED appointments can be cancelled
     * - Sets cancelledAt timestamp and cancelReason
     *
     * @param id     The appointment ID
     * @param reason Cancellation reason
     * @return The cancelled appointment
     */
    @Transactional
    public Appointment cancelAppointment(String id, String reason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Appointment not found with id: " + id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ApiException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Appointment is already cancelled");
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ApiException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot cancel appointment with status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(Instant.now());
        appointment.setCancelReason(reason);

        appointment = appointmentRepository.save(appointment);
        log.info("Cancelled appointment {} with reason: {}", id, reason);

        return appointment;
    }

    /**
     * Complete an appointment.
     * Business Rules:
     * - Only SCHEDULED appointments can be completed
     * - After completion, medical exam record can be created
     *
     * @param id The appointment ID
     * @return The completed appointment
     */
    @Transactional
    public Appointment completeAppointment(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Appointment not found with id: " + id));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ApiException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Appointment is already completed");
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ApiException(
                    ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot complete appointment with status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment = appointmentRepository.save(appointment);
        log.info("Completed appointment {}", id);

        return appointment;
    }
}
