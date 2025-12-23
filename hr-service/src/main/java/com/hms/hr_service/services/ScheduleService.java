package com.hms.hr_service.services;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.securities.UserContext;
import com.hms.hr_service.clients.AppointmentClient;
import com.hms.hr_service.dtos.schedule.CancelScheduleResponse;
import com.hms.hr_service.dtos.schedule.ScheduleDepartmentInfo;
import com.hms.hr_service.dtos.schedule.ScheduleEmployeeInfo;
import com.hms.hr_service.dtos.schedule.ScheduleResponse;
import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.ScheduleStatus;
import com.hms.hr_service.mappers.ScheduleMapper;
import com.hms.hr_service.repositories.DepartmentRepository;
import com.hms.hr_service.repositories.EmployeeRepository;
import com.hms.hr_service.repositories.ScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for custom schedule query operations.
 * The generic CRUD is handled by GenericService, this handles specialized queries.
 */
@Service
@Slf4j
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ScheduleMapper scheduleMapper;
    private final AppointmentClient appointmentClient;

    // Manual constructor to apply @Lazy
    public ScheduleService(
            ScheduleRepository scheduleRepository,
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            ScheduleMapper scheduleMapper,
            @Lazy AppointmentClient appointmentClient) {
        this.scheduleRepository = scheduleRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.scheduleMapper = scheduleMapper;
        this.appointmentClient = appointmentClient;
    }

    /**
     * Get current user's schedules.
     */
    public List<ScheduleResponse> getMySchedules(LocalDate startDate, LocalDate endDate, ScheduleStatus status) {
        // Get current user's employee ID from context
        String employeeId = getCurrentUserEmployeeId();

        List<EmployeeSchedule> schedules;
        if (status != null) {
            schedules = scheduleRepository.findByEmployeeIdAndWorkDateBetweenAndStatus(
                    employeeId, startDate, endDate, status);
        } else {
            schedules = scheduleRepository.findByEmployeeIdAndWorkDateBetween(
                    employeeId, startDate, endDate);
        }

        return schedules.stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get doctor schedules for appointment booking.
     */
    public PageResponse<ScheduleResponse> getDoctorSchedules(
            LocalDate startDate, LocalDate endDate,
            ScheduleStatus status, String doctorId, String departmentId,
            int page, int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        // Default to AVAILABLE if no status specified
        ScheduleStatus queryStatus = status != null ? status : ScheduleStatus.AVAILABLE;

        Page<EmployeeSchedule> schedulePage = scheduleRepository.findDoctorSchedules(
                startDate, endDate, queryStatus, doctorId, departmentId, pageable);

        Page<ScheduleResponse> responsePage = schedulePage.map(this::toEnrichedResponse);
        return PageResponse.fromPage(responsePage);
    }

    /**
     * Convert entity to enriched response with employee info.
     */
    private ScheduleResponse toEnrichedResponse(EmployeeSchedule entity) {
        ScheduleResponse response = scheduleMapper.entityToResponse(entity);
        enrichEmployeeInfo(response);
        return response;
    }

    /**
     * Enrich response with employee and department info.
     */
    private void enrichEmployeeInfo(ScheduleResponse response) {
        if (response.getEmployeeId() == null) return;

        employeeRepository.findById(response.getEmployeeId()).ifPresent(employee -> {
            ScheduleEmployeeInfo info = new ScheduleEmployeeInfo();
            info.setId(employee.getId());
            info.setFullName(employee.getFullName());
            info.setRole(employee.getRole().name());
            info.setSpecialization(employee.getSpecialization());

            if (employee.getDepartmentId() != null) {
                departmentRepository.findById(employee.getDepartmentId()).ifPresent(dept -> {
                    ScheduleDepartmentInfo deptInfo = new ScheduleDepartmentInfo();
                    deptInfo.setId(dept.getId());
                    deptInfo.setName(dept.getName());
                    info.setDepartment(deptInfo);
                });
            }

            response.setEmployee(info);
        });
    }

    /**
     * Get current user's employee ID from security context.
     */
    private String getCurrentUserEmployeeId() {
        // Get from UserContext - this links accountId to employeeId
        var user = UserContext.getUser();
        if (user == null) {
            return null;
        }
        String accountId = user.getId();
        
        // Find employee by account ID
        return employeeRepository.findAll().stream()
                .filter(e -> accountId.equals(e.getAccountId()))
                .findFirst()
                .map(e -> e.getId())
                .orElse(accountId); // Fallback to accountId if no employee found
    }

    /**
     * Get schedule by doctor ID and date.
     * Used by appointment-service for validation.
     */
    public ScheduleResponse getByDoctorAndDate(String doctorId, LocalDate date) {
        return scheduleRepository.findByEmployeeIdAndWorkDate(doctorId, date)
                .map(this::toEnrichedResponse)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "No schedule found for doctor on this date"));
    }

    /**
     * Update schedule status.
     * Used by appointment-service to set BOOKED/AVAILABLE based on slot availability.
     */
    public void updateStatus(String id, ScheduleStatus status) {
        EmployeeSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Schedule not found"));
        
        schedule.setStatus(status);
        scheduleRepository.save(schedule);
    }

    /**
     * Cancel a schedule using saga pattern.
     * 
     * Flow:
     * 1. Validate schedule can be cancelled
     * 2. Set status to PENDING_CANCEL
     * 3. Call appointment-service to cancel all appointments
     * 4. If success → set to CANCELLED
     * 5. If failure → rollback to original status
     */
    @Transactional
    public CancelScheduleResponse cancelSchedule(String id, String reason) {
        log.info("Cancel saga START: Schedule {} with reason: {}", id, reason);
        
        // Step 1: Find and validate schedule
        EmployeeSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Schedule not found"));
        
        ScheduleStatus originalStatus = schedule.getStatus();
        
        // Validate - cannot cancel already cancelled schedule
        if (originalStatus == ScheduleStatus.CANCELLED) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Schedule is already cancelled");
        }
        if (originalStatus == ScheduleStatus.PENDING_CANCEL) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "Schedule cancellation is already in progress");
        }
        
        // Get employee name for response
        String employeeName = employeeRepository.findById(schedule.getEmployeeId())
                .map(e -> e.getFullName())
                .orElse("Unknown");
        
        // Step 2: Set to PENDING_CANCEL
        schedule.setStatus(ScheduleStatus.PENDING_CANCEL);
        schedule.setNotes(reason);
        scheduleRepository.save(schedule);
        log.info("Cancel saga STEP 2: Schedule {} set to PENDING_CANCEL", id);
        
        int cancelledAppointments = 0;
        boolean appointmentsCancelled = false;
        
        try {
            // Step 3: Call appointment-service to cancel appointments
            log.info("Cancel saga STEP 3: Calling appointment-service for schedule {}", id);
            
            var result = appointmentClient.cancelByDoctorAndDate(
                    schedule.getEmployeeId(),
                    schedule.getWorkDate(),
                    reason
            );
            
            cancelledAppointments = result.getData() != null ? result.getData() : 0;
            appointmentsCancelled = true;
            log.info("Cancel saga STEP 3 SUCCESS: Cancelled {} appointments", cancelledAppointments);
            
        } catch (Exception e) {
            // Appointment cancel failed - only need to rollback schedule
            log.error("Cancel saga FAILED at STEP 3 (cancel appointments): {}", e.getMessage());
            
            schedule.setStatus(originalStatus);
            schedule.setNotes(null);
            scheduleRepository.save(schedule);
            log.info("Cancel saga ROLLBACK: Schedule {} restored to {}", id, originalStatus);
            
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to cancel appointments. Schedule restored to " + originalStatus + 
                    ". Error: " + e.getMessage());
        }
        
        // Step 4: Set schedule to CANCELLED
        try {
            schedule.setStatus(ScheduleStatus.CANCELLED);
            scheduleRepository.save(schedule);
            log.info("Cancel saga COMPLETE: Schedule {} is now CANCELLED", id);
            
        } catch (Exception e) {
            // Final save failed AFTER appointments were cancelled - COMPENSATION needed!
            log.error("Cancel saga FAILED at STEP 4 (final save): {}. Triggering COMPENSATION.", e.getMessage());
            
            // Compensate: Restore the appointments that were just cancelled
            try {
                appointmentClient.restoreByDoctorAndDate(schedule.getEmployeeId(), schedule.getWorkDate());
                log.info("COMPENSATION SUCCESS: Restored appointments for doctor {} on {}", 
                        schedule.getEmployeeId(), schedule.getWorkDate());
            } catch (Exception compensateError) {
                log.error("COMPENSATION FAILED: Could not restore appointments: {}", compensateError.getMessage());
                // This is a critical failure - manual intervention may be needed
            }
            
            // Rollback schedule
            schedule.setStatus(originalStatus);
            schedule.setNotes(null);
            scheduleRepository.save(schedule);
            log.info("Cancel saga ROLLBACK: Schedule {} restored to {}", id, originalStatus);
            
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to finalize schedule cancellation. Rolled back. Error: " + e.getMessage());
        }
        
        // Build response
        return CancelScheduleResponse.builder()
                .scheduleId(schedule.getId())
                .employeeId(schedule.getEmployeeId())
                .employeeName(employeeName)
                .workDate(schedule.getWorkDate())
                .status(ScheduleStatus.CANCELLED)
                .cancelReason(reason)
                .cancelledAppointments(cancelledAppointments)
                .cancelledAt(Instant.now())
                .build();
    }
}

