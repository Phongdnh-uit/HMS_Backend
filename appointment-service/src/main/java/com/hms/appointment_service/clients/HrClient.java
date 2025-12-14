package com.hms.appointment_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Feign client for hr-service.
 * Used for:
 * - Validating doctor exists and is available
 * - Updating schedule status when appointments are created/cancelled
 */
@FeignClient(
        name = "hr-service",
        configuration = FeignConfig.class
)
public interface HrClient {

    /**
     * Get schedule for a doctor on a specific date.
     */
    @GetMapping("/hr/schedules/by-doctor-date")
    ApiResponse<ScheduleInfo> getScheduleByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date
    );

    /**
     * Update schedule status.
     * Called when:
     * - All slots filled → BOOKED
     * - Appointment cancelled and slots available → AVAILABLE
     */
    @PatchMapping("/hr/schedules/{id}/status")
    ApiResponse<Void> updateScheduleStatus(
            @PathVariable("id") String scheduleId,
            @RequestParam("status") String status
    );

    /**
     * DTO for schedule info from hr-service.
     */
    record ScheduleInfo(
            String id,
            String employeeId,
            LocalDate workDate,
            String startTime,  // HH:mm format
            String endTime,    // HH:mm format
            String status
    ) {
        /**
         * Calculate how many 30-minute slots this schedule has.
         */
        public int getTotalSlots() {
            var start = java.time.LocalTime.parse(startTime);
            var end = java.time.LocalTime.parse(endTime);
            long minutes = java.time.Duration.between(start, end).toMinutes();
            return (int) (minutes / 30); // 30-minute slots
        }
    }

    /**
     * Get employee by ID.
     * Used to validate doctor exists and fetch name for snapshot.
     */
    @GetMapping("/hr/employees/{id}")
    ApiResponse<EmployeeInfo> getEmployeeById(@PathVariable("id") String employeeId);

    /**
     * DTO for employee info from hr-service.
     */
    record EmployeeInfo(
            String id,
            String fullName,
            String role,
            String departmentId,
            String departmentName
    ) {}
}
