package com.hms.appointment_service.services;

import com.hms.appointment_service.clients.HrClient;
import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.dtos.appointment.AppointmentStatsResponse;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.dtos.appointment.TimeSlotResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.mappers.AppointmentMapper;
import com.hms.appointment_service.repositories.AppointmentRepository;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.helpers.FeignHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for appointment-specific business logic.
 * Handles operations beyond generic CRUD.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final HrClient hrClient;
    private final AppointmentMapper appointmentMapper;
    private final QueueService queueService;

    /**
     * Get available time slots for a doctor on a specific date.
     * Integrates with hr-service to get schedule and checks existing appointments.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to check
     * @return List of time slots with availability status
     */
    public List<TimeSlotResponse> getAvailableSlots(String doctorId, LocalDate date) {
        log.info("🔍 [getAvailableSlots] Fetching slots for doctor {} on {}", doctorId, date);

        // 1. Get Doctor Schedule from HR Service
        ApiResponse<HrClient.ScheduleInfo> scheduleResponse;
        try {
            log.info("📞 [getAvailableSlots] Calling HR service for schedule: doctorId={}, date={}", doctorId, date);
            scheduleResponse = FeignHelper.safeCall(() -> 
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
        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime start = schedule.startTime();
        LocalTime end = schedule.endTime();

        while (start.isBefore(end)) {
            String timeStr = start.toString();
            if (timeStr.length() == 8) timeStr = timeStr.substring(0, 5); // Ensure HH:mm

            boolean isBooked = bookedTimes.contains(timeStr);
            slots.add(TimeSlotResponse.builder()
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
    public PageResponse<AppointmentResponse> getByPatientId(
            String patientId, org.springframework.data.domain.Pageable pageable) {
        log.debug("Getting appointments for patient: {}", patientId);
        
        org.springframework.data.domain.Page<Appointment> appointments = 
                appointmentRepository.findByPatientId(patientId, pageable);
        
        // Convert to response DTOs using the mapper
        org.springframework.data.domain.Page<AppointmentResponse> responsePageData = 
                appointments.map(appointmentMapper::entityToResponse);
        
        return PageResponse.fromPage(responsePageData);
    }
    
    /**
     * Get aggregated appointment statistics for reporting.
     * Pre-aggregates data at source for efficient reporting.
     *
     * @param startDate Start date for stats period
     * @param endDate   End date for stats period
     * @return Pre-aggregated appointment statistics
     */
    public AppointmentStatsResponse getStats(LocalDate startDate, LocalDate endDate) {
        log.info("Generating appointment statistics from {} to {}", startDate, endDate);
        
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant startInstant = startDate.atStartOfDay(zoneId).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        // Get total count
        long totalCount = appointmentRepository.countInDateRange(startInstant, endInstant);
        
        // Get counts by status
        List<Object[]> statusCounts = appointmentRepository.countByStatusInDateRange(startInstant, endInstant);
        Map<String, Integer> appointmentsByStatus = new HashMap<>();
        for (Object[] row : statusCounts) {
            String status = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            appointmentsByStatus.put(status, count);
        }
        
        // Get counts by type
        List<Object[]> typeCounts = appointmentRepository.countByTypeInDateRange(startInstant, endInstant);
        Map<String, Integer> appointmentsByType = new HashMap<>();
        for (Object[] row : typeCounts) {
            String type = row[0] != null ? row[0].toString() : "UNKNOWN";
            int count = ((Number) row[1]).intValue();
            appointmentsByType.put(type, count);
        }
        
        // Get counts by department
        List<Object[]> deptCounts = appointmentRepository.countByDepartmentInDateRange(startInstant, endInstant);
        List<AppointmentStatsResponse.DepartmentStats> departmentStats = new ArrayList<>();
        for (Object[] row : deptCounts) {
            String deptName = row[0] != null ? row[0].toString() : "Unknown";
            int count = ((Number) row[1]).intValue();
            double percentage = totalCount > 0 ? (count * 100.0 / totalCount) : 0;
            departmentStats.add(AppointmentStatsResponse.DepartmentStats.builder()
                    .departmentName(deptName)
                    .count(count)
                    .percentage(Math.round(percentage * 10) / 10.0)
                    .build());
        }
        
        // Calculate average per day
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double averagePerDay = daysBetween > 0 ? Math.round((totalCount * 10.0 / daysBetween)) / 10.0 : 0;
        
        // Get daily counts for trend
        List<Object[]> dailyCounts = appointmentRepository.countByDateInDateRange(startInstant, endInstant);
        List<AppointmentStatsResponse.DailyCount> dailyTrend = new ArrayList<>();
        for (Object[] row : dailyCounts) {
            LocalDate date = null;
            if (row[0] != null) {
                if (row[0] instanceof java.sql.Date) {
                    date = ((java.sql.Date) row[0]).toLocalDate();
                } else if (row[0] instanceof LocalDate) {
                    date = (LocalDate) row[0];
                } else {
                    String dateStr = row[0].toString();
                    // Safely parse date - handle both "2026-01-01" and "2026-01-01T..." formats
                    if (dateStr.length() >= 10) {
                        date = LocalDate.parse(dateStr.substring(0, 10));
                    } else {
                        // Try parsing the whole string if it's shorter than 10 chars
                        date = LocalDate.parse(dateStr);
                    }
                }
            }
            int count = ((Number) row[1]).intValue();
            if (date != null) {
                dailyTrend.add(AppointmentStatsResponse.DailyCount.builder()
                        .date(date)
                        .count(count)
                        .build());
            }
        }
        
        return AppointmentStatsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalAppointments((int) totalCount)
                .appointmentsByStatus(appointmentsByStatus)
                .appointmentsByType(appointmentsByType)
                .appointmentsByDepartment(departmentStats)
                .appointmentsByDoctor(new ArrayList<>())
                .dailyTrend(dailyTrend)
                .averagePerDay(averagePerDay)
                .generatedAt(Instant.now())
                .build();
    }
    
    // ========== Walk-in Queue Methods ==========
    
    /**
     * Register a walk-in patient.
     * Creates an immediate appointment with queue number.
     *
     * @param request Walk-in registration request
     * @return The created appointment with queue number
     */
    @Transactional
    public AppointmentResponse registerWalkIn(com.hms.appointment_service.dtos.WalkInRequest request) {
        log.info("Registering walk-in patient: patientId={}, doctorId={}", 
                request.getPatientId(), request.getDoctorId());
        
        // Get next queue number
        int queueNumber = queueService.getNextQueueNumber();
        
        // Calculate priority
        int priority = queueService.calculatePriority(request, 
                com.hms.appointment_service.constants.AppointmentType.WALK_IN);
        
        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setPatientId(request.getPatientId());
        appointment.setDoctorId(request.getDoctorId());
        appointment.setReason(request.getReason());
        appointment.setAppointmentTime(Instant.now());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setType(com.hms.appointment_service.constants.AppointmentType.WALK_IN);
        
        // Set queue fields
        appointment.setQueueNumber(queueNumber);
        appointment.setPriority(priority);
        appointment.setPriorityReason(request.getPriorityReason());
        
        // Try to fetch patient and doctor names
        try {
            // Note: Patient and doctor name fetching would require client calls
            // For now, we'll leave them as null and they can be populated later
            appointment.setPatientName("Walk-in Patient");
            appointment.setDoctorName("Doctor");
        } catch (Exception e) {
            log.warn("Could not fetch patient/doctor names: {}", e.getMessage());
        }
        
        appointment = appointmentRepository.save(appointment);
        log.info("Walk-in registered: id={}, queueNumber={}, priority={}", 
                appointment.getId(), queueNumber, priority);
        
        return appointmentMapper.entityToResponse(appointment);
    }
    
    /**
     * Get today's queue for a specific doctor.
     *
     * @param doctorId The doctor's ID
     * @return List of appointments in queue order
     */
    public List<AppointmentResponse> getDoctorQueueToday(String doctorId) {
        List<Appointment> queue = queueService.getDoctorQueueToday(doctorId);
        return queue.stream()
                .map(appointmentMapper::entityToResponse)
                .toList();
    }
    
    /**
     * Get next patient in queue for a doctor.
     *
     * @param doctorId The doctor's ID
     * @return Next appointment in queue, or null if empty
     */
    public AppointmentResponse getNextInQueue(String doctorId) {
        Appointment next = queueService.getNextInQueue(doctorId);
        return next != null ? appointmentMapper.entityToResponse(next) : null;
    }
    
    /**
     * Call next patient in queue (mark as IN_PROGRESS).
     *
     * @param doctorId The doctor's ID
     * @return The called appointment, or null if no one in queue
     */
    @Transactional
    public AppointmentResponse callNextPatient(String doctorId) {
        Appointment next = queueService.getNextInQueue(doctorId);
        if (next == null) {
            return null;
        }
        
        // Mark as IN_PROGRESS (being seen by doctor)
        next.setStatus(AppointmentStatus.IN_PROGRESS);
        next = appointmentRepository.save(next);
        
        log.info("Called patient: appointmentId={}, queueNumber={}", 
                next.getId(), next.getQueueNumber());
        
        return appointmentMapper.entityToResponse(next);
    }
}


