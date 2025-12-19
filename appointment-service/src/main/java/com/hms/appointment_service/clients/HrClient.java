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
 * 
 * Note: Using direct URL to bypass Eureka service discovery issues.
 * The URL uses Docker container hostname which works on the same network.
 */
@FeignClient(
        name = "hr-service",
        url = "${feign.client.config.hr-service.url:}",
        configuration = FeignConfig.class
)
public interface HrClient {

    /**
     * Get schedule for a doctor on a specific date.
     * Uses @DateTimeFormat to ensure date is serialized as ISO format (yyyy-MM-dd)
     */
    @GetMapping("/hr/schedules/by-doctor-date")
    ApiResponse<ScheduleInfo> getScheduleByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date
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
     * Uses @JsonIgnoreProperties to ignore fields not needed by this service.
     * Uses @JsonFormat to ensure LocalTime is correctly parsed from "HH:mm:ss" format.
     */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    record ScheduleInfo(
            String id,
            String employeeId,
            java.time.LocalDate workDate,
            @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
            java.time.LocalTime startTime,
            @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm:ss")
            java.time.LocalTime endTime,
            String status
    ) {
        /**
         * Calculate how many 30-minute slots this schedule has.
         */
        public int getTotalSlots() {
            long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
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
