package com.hms.hr_service.clients;

import com.hms.common.configs.FeignConfig;
import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Feign client for appointment-service.
 * Used for:
 * - Cascade cancel when schedule is cancelled
 * - Checking appointment count before schedule delete
 * 
 * Note: Using direct URL to bypass Eureka service discovery issues.
 */
@FeignClient(
        name = "appointment-service",
        url = "${feign.client.config.appointment-service.url:}",
        configuration = FeignConfig.class
)
public interface AppointmentClient {

    /**
     * Cancel all appointments for a doctor on a specific date.
     * Called when a schedule is set to CANCELLED status.
     *
     * @param doctorId    The doctor's employee ID
     * @param date        The date to cancel appointments for
     * @param reason      Cancellation reason
     * @return API response with count of cancelled appointments
     */
    @PostMapping("/appointments/bulk-cancel")
    ApiResponse<Integer> cancelByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date,
            @RequestParam("reason") String reason
    );

    /**
     * Count active (SCHEDULED) appointments for a doctor on a specific date.
     * Used to validate if schedule can be deleted.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to check
     * @return Count of active appointments
     */
    @GetMapping("/appointments/count")
    ApiResponse<Integer> countByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date
    );

    /**
     * COMPENSATION: Restore cancelled appointments back to SCHEDULED.
     * Called when saga rollback is needed after appointments were cancelled
     * but schedule final update failed.
     *
     * @param doctorId The doctor's employee ID
     * @param date     The date to restore appointments for
     * @return Count of restored appointments
     */
    @PostMapping("/appointments/bulk-restore")
    ApiResponse<Integer> restoreByDoctorAndDate(
            @RequestParam("doctorId") String doctorId,
            @RequestParam("date") LocalDate date
    );
}
