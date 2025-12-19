package com.hms.hr_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.services.CrudService;
import com.hms.hr_service.dtos.schedule.CancelScheduleRequest;
import com.hms.hr_service.dtos.schedule.CancelScheduleResponse;
import com.hms.hr_service.dtos.schedule.ScheduleRequest;
import com.hms.hr_service.dtos.schedule.ScheduleResponse;
import com.hms.hr_service.entities.EmployeeSchedule;
import com.hms.hr_service.enums.ScheduleStatus;
import com.hms.hr_service.services.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for employee schedule management.
 * Extends GenericController for CRUD operations.
 * Adds custom endpoints for schedule-specific queries.
 */
@RequestMapping("/hr/schedules")
@RestController
public class ScheduleController extends GenericController<EmployeeSchedule, String, ScheduleRequest, ScheduleResponse> {

    private final ScheduleService scheduleService;

    public ScheduleController(CrudService<EmployeeSchedule, String, ScheduleRequest, ScheduleResponse> service,
                              ScheduleService scheduleService) {
        super(service);
        this.scheduleService = scheduleService;
    }

    /**
     * Get current user's own schedules (for employees to view their schedule).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getMySchedules(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(value = "status", required = false) ScheduleStatus status) {
        List<ScheduleResponse> schedules = scheduleService.getMySchedules(startDate, endDate, status);
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }

    /**
     * List doctor schedules for appointment booking.
     * Filters only DOCTOR role employees.
     */
    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleResponse>>> getDoctorSchedules(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(value = "status", required = false) ScheduleStatus status,
            @RequestParam(value = "doctorId", required = false) String doctorId,
            @RequestParam(value = "departmentId", required = false) String departmentId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        PageResponse<ScheduleResponse> schedules = scheduleService.getDoctorSchedules(
                startDate, endDate, status, doctorId, departmentId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(schedules));
    }

    /**
     * Get schedule by doctor ID and date.
     * Used by appointment-service to validate and get schedule info.
     */
    @GetMapping("/by-doctor-date")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date) {
        ScheduleResponse schedule = scheduleService.getByDoctorAndDate(doctorId, date);
        return ResponseEntity.ok(ApiResponse.ok(schedule));
    }

    /**
     * Update schedule status.
     * Used by appointment-service to update status to BOOKED/AVAILABLE.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable("id") String id,
            @RequestParam("status") ScheduleStatus status) {
        scheduleService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Cancel a schedule.
     * Implements saga pattern:
     * 1. Set status to PENDING_CANCEL
     * 2. Cancel all appointments via appointment-service
     * 3. If success → set to CANCELLED
     * 4. If failure → rollback to original status
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CancelScheduleResponse>> cancelSchedule(
            @PathVariable("id") String id,
            @Valid @RequestBody CancelScheduleRequest request) {
        CancelScheduleResponse response = scheduleService.cancelSchedule(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.ok("Schedule cancelled successfully", response));
    }
}


