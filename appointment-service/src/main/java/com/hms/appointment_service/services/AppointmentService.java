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
    private final com.hms.appointment_service.clients.HrClient hrClient;
    private final com.hms.appointment_service.mappers.AppointmentMapper appointmentMapper;

    /**
     * Get available time slots for a doctor on a specific date.
     * Integrates with hr-service to get schedule and checks existing appointments.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to check
     * @return List of time slots with availability status
     */
    public List<com.hms.appointment_service.dtos.appointment.TimeSlotResponse> getAvailableSlots(String doctorId, LocalDate date) {
        log.info("🔍 [getAvailableSlots] Fetching slots for doctor {} on {}", doctorId, date);

        // 1. Get Doctor Schedule from HR Service
        com.hms.common.dtos.ApiResponse<com.hms.appointment_service.clients.HrClient.ScheduleInfo> scheduleResponse;
        try {
            log.info("📞 [getAvailableSlots] Calling HR service for schedule: doctorId={}, date={}", doctorId, date);
            scheduleResponse = com.hms.common.helpers.FeignHelper.safeCall(() -> 
                hrClient.getScheduleByDoctorAndDate(doctorId, date));
            log.info("✅ [getAvailableSlots] HR service response received: {}", scheduleResponse != null ? "Not null" : "NULL");
            if (scheduleResponse != null) {
                log.info("📊 [getAvailableSlots] Schedule response data: {}", scheduleResponse.getData());
            }
        } catch (Exception e) {
            log.error("❌ [getAvailableSlots] Failed to fetch schedule for doctor {}: {}", doctorId, e.getMessage(), e);
            return List.of(); // Return empty if no schedule or error
        }

        if (scheduleResponse == null || scheduleResponse.getData() == null) {
            log.warn("⚠️ [getAvailableSlots] No schedule found for doctor {} on {}. Response was: {}", 
                doctorId, date, scheduleResponse);
            return List.of();
        }

        var schedule = scheduleResponse.getData();
        log.info("✅ [getAvailableSlots] Found schedule: startTime={}, endTime={}", schedule.startTime(), schedule.endTime());

        // 2. Get existing booked appointments
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<Appointment> bookedAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
        
        log.info("📅 [getAvailableSlots] Found {} booked appointments for doctor on {}", bookedAppointments.size(), date);
        
        // Filter out CANCELLED
        List<String> bookedTimes = bookedAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .map(a -> a.getAppointmentTime().atZone(zoneId).toLocalTime().toString().substring(0, 5)) // HH:mm
                .toList();

        log.info("🚫 [getAvailableSlots] Booked times (excluding cancelled): {}", bookedTimes);

        // 3. Generate Slots
        List<com.hms.appointment_service.dtos.appointment.TimeSlotResponse> slots = new java.util.ArrayList<>();
        java.time.LocalTime start = schedule.startTime(); // Already LocalTime
        java.time.LocalTime end = schedule.endTime(); // Already LocalTime

        while (start.isBefore(end)) {
            String timeStr = start.toString();
            if (timeStr.length() == 8) timeStr = timeStr.substring(0, 5); // Ensure HH:mm

            boolean isBooked = bookedTimes.contains(timeStr);
            slots.add(com.hms.appointment_service.dtos.appointment.TimeSlotResponse.builder()
                    .time(timeStr)
                    .available(!isBooked)
                    .build());

            start = start.plusMinutes(30);
        }

        log.info("✅ [getAvailableSlots] Generated {} total slots", slots.size());
        return slots;
    }

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
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
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
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
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
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
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

    /**
     * Get appointments for a specific patient with pagination.
     * Used by patient detail page to show only that patient's appointments.
     *
     * @param patientId The patient ID
     * @param pageable  Pagination parameters
     * @return PageResponse of appointments for this patient
     */
    public com.hms.common.dtos.PageResponse<com.hms.appointment_service.dtos.appointment.AppointmentResponse> getByPatientId(
            String patientId, org.springframework.data.domain.Pageable pageable) {
        log.debug("Getting appointments for patient: {}", patientId);
        
        org.springframework.data.domain.Page<Appointment> appointments = 
                appointmentRepository.findByPatientId(patientId, pageable);
        
        // Convert to response DTOs using the mapper
        org.springframework.data.domain.Page<com.hms.appointment_service.dtos.appointment.AppointmentResponse> responsePageData = 
                appointments.map(appointmentMapper::entityToResponse);
        
        return com.hms.common.dtos.PageResponse.fromPage(responsePageData);
    }
}
