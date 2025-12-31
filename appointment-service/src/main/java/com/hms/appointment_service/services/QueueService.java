package com.hms.appointment_service.services;

import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.constants.AppointmentType;
import com.hms.appointment_service.dtos.WalkInRequest;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.repositories.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Service for queue management in walk-in patient flow.
 */
@Service
@RequiredArgsConstructor
public class QueueService {
    
    private final AppointmentRepository appointmentRepository;
    
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    /**
     * Generate next queue number for today.
     * @return Next queue number (1, 2, 3...)
     */
    public int getNextQueueNumber() {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        Instant startOfDay = today.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        
        Integer maxQueue = appointmentRepository.findMaxQueueNumberForDate(startOfDay, endOfDay);
        return (maxQueue == null ? 0 : maxQueue) + 1;
    }
    
    /**
     * Calculate priority based on type and patient info.
     * Lower number = higher priority.
     * 
     * Priority levels:
     * - 10: Emergency
     * - 30: Pre-booked appointment
     * - 50: Elderly (priority reason)
     * - 60: Pregnant (priority reason)
     * - 80: Other priority reasons
     * - 100: Normal walk-in
     */
    public int calculatePriority(WalkInRequest request, AppointmentType type) {
        // Emergency gets highest priority
        if (type == AppointmentType.EMERGENCY) {
            return 10;
        }
        
        // Check priority reason
        if (request.getPriorityReason() != null) {
            String reason = request.getPriorityReason().toUpperCase();
            return switch (reason) {
                case "EMERGENCY" -> 10;
                case "ELDERLY" -> 50;
                case "PREGNANT" -> 60;
                case "DISABILITY" -> 70;
                default -> 80;
            };
        }
        
        // Normal walk-in
        return 100;
    }
    
    /**
     * Get ordered queue for a specific doctor today.
     * Ordered by: priority ASC, queueNumber ASC
     */
    public List<Appointment> getDoctorQueueToday(String doctorId) {
        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        Instant startOfDay = today.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
        
        return appointmentRepository.findDoctorQueueForDate(doctorId, startOfDay, endOfDay);
    }
    
    /**
     * Get next patient in queue for doctor.
     * Returns the patient with lowest priority number (highest priority)
     * who is still waiting (SCHEDULED status).
     */
    public Appointment getNextInQueue(String doctorId) {
        List<Appointment> queue = getDoctorQueueToday(doctorId);
        return queue.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .filter(a -> a.getQueueNumber() != null)
                .findFirst()
                .orElse(null);
    }
}
