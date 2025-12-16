package com.hms.appointment_service.hooks;

import com.hms.appointment_service.clients.HrClient;
import com.hms.appointment_service.clients.PatientClient;
import com.hms.appointment_service.constants.AppointmentStatus;
import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.appointment_service.repositories.AppointmentRepository;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AppointmentHook implements GenericHook<Appointment, String, AppointmentRequest, AppointmentResponse> {

    private final HrClient hrClient;
    private final PatientClient patientClient;
    private final AppointmentRepository appointmentRepository;

    // Manual constructor to apply @Lazy
    public AppointmentHook(
            @Lazy HrClient hrClient,
            @Lazy PatientClient patientClient,
            AppointmentRepository appointmentRepository) {
        this.hrClient = hrClient;
        this.patientClient = patientClient;
        this.appointmentRepository = appointmentRepository;
    }

    private static final int APPOINTMENT_DURATION_MINUTES = 30;
    private static final String SCHEDULE_KEY = "schedule";
    private static final String PATIENT_KEY = "patient";
    private static final String DOCTOR_KEY = "doctor";

    @Override
    public void enrichFindAll(PageResponse<AppointmentResponse> response) {
        // Patient and doctor are mapped directly by the mapper
    }

    @Override
    public void enrichFindById(AppointmentResponse response) {
        // Patient and doctor are mapped directly by the mapper
    }

    @Override
    public void validateCreate(AppointmentRequest input, Map<String, Object> context) {
        // 1. Parse and validate appointment time
        Instant appointmentInstant;
        try {
            appointmentInstant = Instant.parse(input.getAppointmentTime());
        } catch (Exception e) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Invalid appointment time format. Use ISO-8601 format.");
        }
        
        if (appointmentInstant.isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Appointment time must be in the future");
        }
        context.put("appointmentInstant", appointmentInstant);

        // 2. Validate patient exists
        PatientClient.PatientInfo patient;
        try {
            var response = patientClient.getPatientById(input.getPatientId());
            patient = response.getData();
        } catch (Exception e) {
            log.error("Failed to validate patient: {}", e.getMessage());
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unable to verify patient");
        }
        if (patient == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Patient not found");
        }
        context.put(PATIENT_KEY, patient);

        // 3. Validate doctor exists and has DOCTOR role
        HrClient.EmployeeInfo doctor;
        try {
            var response = hrClient.getEmployeeById(input.getDoctorId());
            doctor = response.getData();
        } catch (Exception e) {
            log.error("Failed to validate doctor: {}", e.getMessage());
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unable to verify doctor");
        }
        if (doctor == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Doctor not found");
        }
        if (!"DOCTOR".equals(doctor.role())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Employee is not a doctor");
        }
        context.put(DOCTOR_KEY, doctor);

        // 4. Validate doctor has schedule for this date
        LocalDate appointmentDate = appointmentInstant.atZone(ZoneId.systemDefault()).toLocalDate();
        HrClient.ScheduleInfo schedule;
        try {
            var response = hrClient.getScheduleByDoctorAndDate(input.getDoctorId(), appointmentDate);
            schedule = response.getData();
        } catch (Exception e) {
            log.error("Failed to validate doctor schedule: {}", e.getMessage());
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unable to verify doctor schedule");
        }
        if (schedule == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Doctor has no schedule on this date");
        }
        context.put(SCHEDULE_KEY, schedule);
        
        if ("CANCELLED".equals(schedule.status())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Doctor's schedule is cancelled for this date");
        }
        
        // Validate time is within schedule hours
        LocalTime appointmentTime = appointmentInstant.atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime scheduleStart = LocalTime.parse(schedule.startTime());
        LocalTime scheduleEnd = LocalTime.parse(schedule.endTime());
        
        if (appointmentTime.isBefore(scheduleStart) || 
            appointmentTime.plusMinutes(APPOINTMENT_DURATION_MINUTES).isAfter(scheduleEnd)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, 
                    "Appointment time is outside doctor's schedule hours (" + scheduleStart + " - " + scheduleEnd + ")");
        }

        // 5. Check for double booking
        Instant start = appointmentInstant;
        Instant end = start.plus(Duration.ofMinutes(APPOINTMENT_DURATION_MINUTES));
        
        List<Appointment> overlapping = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(input.getDoctorId(), start, end);
        
        if (!overlapping.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This time slot is already booked");
        }
    }

    @Override
    public void enrichCreate(AppointmentRequest input, Appointment entity, Map<String, Object> context) {
        // Set default status to SCHEDULED
        if (entity.getStatus() == null) {
            entity.setStatus(AppointmentStatus.SCHEDULED);
        }
        
        // Snapshot patient name (context populated by validateCreate)
        var patient = (PatientClient.PatientInfo) context.get(PATIENT_KEY);
        entity.setPatientName(patient.fullName());
        
        // Snapshot doctor name and department (context populated by validateCreate)
        var doctor = (HrClient.EmployeeInfo) context.get(DOCTOR_KEY);
        entity.setDoctorName(doctor.fullName());
        entity.setDoctorDepartment(doctor.departmentName());
    }

    @Override
    public void afterCreate(Appointment entity, AppointmentResponse response, Map<String, Object> context) {
        // Check if schedule should be marked as BOOKED
        checkAndUpdateScheduleStatus(entity);
    }

    @Override
    public void validateUpdate(String id, AppointmentRequest input, Appointment existing, Map<String, Object> context) {
        // Can only update SCHEDULED appointments
        if (existing.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "Cannot update appointment with status: " + existing.getStatus());
        }
    }

    @Override
    public void enrichUpdate(AppointmentRequest input, Appointment entity, Map<String, Object> context) {
        // No additional enrichment - patient/doctor cannot be changed on update
    }

    @Override
    public void afterUpdate(Appointment entity, AppointmentResponse response, Map<String, Object> context) {
        // If status changed to CANCELLED, may need to update schedule back to AVAILABLE
        if (entity.getStatus() == AppointmentStatus.CANCELLED) {
            checkAndUpdateScheduleStatus(entity);
        }
    }

    @Override
    public void validateDelete(String id) {
        // Generally don't allow hard delete - use cancel status instead
    }

    @Override
    public void afterDelete(String id) {
        // No cleanup needed
    }

    @Override
    public void validateBulkDelete(Iterable<String> ids) {
        // No bulk delete validation
    }

    @Override
    public void afterBulkDelete(Iterable<String> ids) {
        // No cleanup needed
    }

    // ==================== Helper Methods ====================

    /**
     * Check if all slots are booked and update schedule status accordingly.
     */
    private void checkAndUpdateScheduleStatus(Appointment appointment) {
        try {
            LocalDate appointmentDate = appointment.getAppointmentTime()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            
            var scheduleResponse = hrClient.getScheduleByDoctorAndDate(
                    appointment.getDoctorId(), appointmentDate);
            
            if (scheduleResponse.getData() == null) return;
            
            var schedule = scheduleResponse.getData();
            int totalSlots = schedule.getTotalSlots();
            
            // Count current SCHEDULED appointments for this doctor on this date
            Instant startOfDay = appointmentDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endOfDay = appointmentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            
            List<Appointment> scheduledAppointments = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetweenAndStatus(
                            appointment.getDoctorId(), startOfDay, endOfDay, AppointmentStatus.SCHEDULED);
            
            int bookedSlots = scheduledAppointments.size();
            
            log.info("Schedule {} has {} booked slots out of {} total", 
                    schedule.id(), bookedSlots, totalSlots);
            
            // Detect data inconsistency: more bookings than available slots
            if (bookedSlots > totalSlots) {
                log.warn("ALERT: Schedule {} has {} bookings but only {} slots! Possible data inconsistency.", 
                        schedule.id(), bookedSlots, totalSlots);
            }
            
            // Update schedule status based on slot availability
            String newStatus = bookedSlots >= totalSlots ? "BOOKED" : "AVAILABLE";
            if (!newStatus.equals(schedule.status())) {
                hrClient.updateScheduleStatus(schedule.id(), newStatus);
                log.info("Updated schedule {} status to {}", schedule.id(), newStatus);
            }
        } catch (Exception e) {
            log.error("Failed to update schedule status: {}", e.getMessage());
            // Don't fail the appointment operation - this is a non-critical side effect
        }
    }
}
