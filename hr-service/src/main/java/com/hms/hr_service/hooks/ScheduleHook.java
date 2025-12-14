package com.hms.hr_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.hr_service.clients.AppointmentClient;
import com.hms.hr_service.dtos.schedule.*;
import com.hms.hr_service.entities.Department;
import com.hms.hr_service.entities.Employee;
import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.ScheduleStatus;
import com.hms.hr_service.repositories.DepartmentRepository;
import com.hms.hr_service.repositories.EmployeeRepository;
import com.hms.hr_service.repositories.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hook for EmployeeSchedule business logic.
 * Handles validation, enrichment, and cascade operations.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ScheduleHook implements GenericHook<EmployeeSchedule, String, ScheduleRequest, ScheduleResponse> {

    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentClient appointmentClient;

    // Context keys
    private static final String EMPLOYEE_KEY = "employee";
    private static final String OLD_STATUS_KEY = "oldStatus";
    private static final String CANCEL_REASON_KEY = "cancelReason";
    private static final String DEFAULT_CANCEL_REASON = "Doctor schedule cancelled";

    @Override
    public void enrichFindAll(PageResponse<ScheduleResponse> response) {
        response.getContent().forEach(this::enrichEmployeeInfo);
    }

    @Override
    public void enrichFindById(ScheduleResponse response) {
        enrichEmployeeInfo(response);
    }

    @Override
    public void validateCreate(ScheduleRequest input, Map<String, Object> context) {
        Map<String, String> errors = new HashMap<>();

        // Validate employee exists
        Optional<Employee> employeeOpt = employeeRepository.findById(input.getEmployeeId());
        if (employeeOpt.isEmpty()) {
            errors.put("employeeId", "Employee not found");
        } else {
            context.put(EMPLOYEE_KEY, employeeOpt.get());
        }

        // Validate workDate not in past
        if (input.getWorkDate() != null && input.getWorkDate().isBefore(LocalDate.now())) {
            errors.put("workDate", "Work date cannot be in the past");
        }

        // Validate startTime < endTime
        if (input.getStartTime() != null && input.getEndTime() != null
                && !input.getStartTime().isBefore(input.getEndTime())) {
            errors.put("endTime", "End time must be after start time");
        }

        // Validate unique constraint (employeeId, workDate)
        if (input.getEmployeeId() != null && input.getWorkDate() != null) {
            if (scheduleRepository.existsByEmployeeIdAndWorkDate(input.getEmployeeId(), input.getWorkDate())) {
                errors.put("workDate", "Schedule already exists for this employee on this date");
            }
        }

        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
        }
    }

    @Override
    public void enrichCreate(ScheduleRequest input, EmployeeSchedule entity, Map<String, Object> context) {
        // Default status to AVAILABLE if not provided
        if (entity.getStatus() == null) {
            entity.setStatus(ScheduleStatus.AVAILABLE);
        }
    }

    @Override
    public void afterCreate(EmployeeSchedule entity, ScheduleResponse response, Map<String, Object> context) {
        enrichEmployeeInfo(response);
    }

    @Override
    public void validateUpdate(String id, ScheduleRequest input, EmployeeSchedule existing, Map<String, Object> context) {
        Map<String, String> errors = new HashMap<>();

        // Store old status for cascade check
        context.put(OLD_STATUS_KEY, existing.getStatus());

        // Validate employee if changed
        if (input.getEmployeeId() != null && !input.getEmployeeId().equals(existing.getEmployeeId())) {
            Optional<Employee> employeeOpt = employeeRepository.findById(input.getEmployeeId());
            if (employeeOpt.isEmpty()) {
                errors.put("employeeId", "Employee not found");
            } else {
                context.put(EMPLOYEE_KEY, employeeOpt.get());
            }
        }

        // Validate workDate not in past
        if (input.getWorkDate() != null && input.getWorkDate().isBefore(LocalDate.now())) {
            errors.put("workDate", "Work date cannot be in the past");
        }

        // Validate startTime < endTime
        LocalTime startTime = input.getStartTime() != null ? input.getStartTime() : existing.getStartTime();
        LocalTime endTime = input.getEndTime() != null ? input.getEndTime() : existing.getEndTime();
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            errors.put("endTime", "End time must be after start time");
        }

        // Validate unique constraint if employeeId or workDate changed
        String newEmployeeId = input.getEmployeeId() != null ? input.getEmployeeId() : existing.getEmployeeId();
        LocalDate newWorkDate = input.getWorkDate() != null ? input.getWorkDate() : existing.getWorkDate();
        
        if (!newEmployeeId.equals(existing.getEmployeeId()) || !newWorkDate.equals(existing.getWorkDate())) {
            if (scheduleRepository.existsByEmployeeIdAndWorkDate(newEmployeeId, newWorkDate)) {
                errors.put("workDate", "Schedule already exists for this employee on this date");
            }
        }

        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
        }

        // Block setting status to CANCELLED via PATCH - use POST /{id}/cancel endpoint instead
        if (input.getStatus() == ScheduleStatus.CANCELLED && existing.getStatus() != ScheduleStatus.CANCELLED) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "Cannot set status to CANCELLED via update. Use POST /hr/schedules/{id}/cancel endpoint instead.");
        }
        
        // Block setting status to PENDING_CANCEL - internal use only
        if (input.getStatus() == ScheduleStatus.PENDING_CANCEL) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "PENDING_CANCEL is an internal status and cannot be set directly.");
        }
    }

    @Override
    public void enrichUpdate(ScheduleRequest input, EmployeeSchedule entity, Map<String, Object> context) {
        // No additional enrichment needed
    }

    @Override
    public void afterUpdate(EmployeeSchedule entity, ScheduleResponse response, Map<String, Object> context) {
        enrichEmployeeInfo(response);
        // Saga logic moved to ScheduleService.cancelSchedule() - dedicated cancel endpoint
    }

    @Override
    public void validateDelete(String id) {
        // Block delete if ANY appointments exist - must cancel or set schedule to CANCELLED first
        Optional<EmployeeSchedule> scheduleOpt = scheduleRepository.findById(id);
        if (scheduleOpt.isEmpty()) {
            return; // Let the generic service handle not found
        }
        
        EmployeeSchedule schedule = scheduleOpt.get();
        
        try {
            // Call appointment-service to count active appointments
            var response = appointmentClient.countByDoctorAndDate(
                    schedule.getEmployeeId(), schedule.getWorkDate());
            
            int appointmentCount = response.getData() != null ? response.getData() : 0;
            
            if (appointmentCount > 0) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                        "Cannot delete schedule with " + appointmentCount + " active appointment(s). " +
                        "Cancel all appointments first, or set schedule status to CANCELLED to trigger cascade cancel.");
            }
        } catch (ApiException e) {
            throw e; // Re-throw our validation exception
        } catch (Exception e) {
            log.error("Failed to verify appointments for schedule {}: {}", id, e.getMessage());
            // If we can't verify, fail safe - block the delete
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "Unable to verify if schedule has appointments. Please try again or use cancel operation.");
        }
    }

    @Override
    public void afterDelete(String id) {
        // No cleanup needed
    }

    @Override
    public void validateBulkDelete(Iterable<String> ids) {
        // Validate each schedule - collect all that have appointments
        Map<String, Integer> schedulesWithAppointments = new HashMap<>();
        
        for (String id : ids) {
            Optional<EmployeeSchedule> scheduleOpt = scheduleRepository.findById(id);
            if (scheduleOpt.isEmpty()) {
                continue;
            }
            
            EmployeeSchedule schedule = scheduleOpt.get();
            
            try {
                var response = appointmentClient.countByDoctorAndDate(
                        schedule.getEmployeeId(), schedule.getWorkDate());
                int count = response.getData() != null ? response.getData() : 0;
                
                if (count > 0) {
                    schedulesWithAppointments.put(id, count);
                }
            } catch (Exception e) {
                log.error("Failed to verify appointments for schedule {}: {}", id, e.getMessage());
                // Fail safe - treat as having appointments
                schedulesWithAppointments.put(id, -1); // -1 means unknown but blocked
            }
        }
        
        if (!schedulesWithAppointments.isEmpty()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                    "Cannot delete " + schedulesWithAppointments.size() + " schedule(s) that have active appointments. " +
                    "Cancel appointments first or use status CANCELLED to trigger cascade cancel.");
        }
    }

    @Override
    public void afterBulkDelete(Iterable<String> ids) {
        // No cleanup needed
    }

    // ==================== Helper Methods ====================

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

            // Add department info
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
}

