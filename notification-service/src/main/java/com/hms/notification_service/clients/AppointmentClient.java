package com.hms.notification_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for appointment-service.
 */
@FeignClient(name = "appointment-service")
public interface AppointmentClient {

    /**
     * Get appointments that need follow-up notification.
     * Returns appointments where followUpDate = given date and followUpNotificationSent = false.
     */
    @GetMapping("/appointments/pending-followup-notifications")
    ApiResponse<List<AppointmentInfo>> getAppointmentsForFollowUpNotification(
            @RequestParam("followUpDate") String followUpDate
    );

    /**
     * Mark an appointment's follow-up notification as sent.
     */
    @PutMapping("/appointments/{id}/mark-followup-notification-sent")
    ApiResponse<Void> markFollowUpNotificationSent(@PathVariable("id") String id);

    /**
     * Appointment record for notification purposes.
     */
    record AppointmentInfo(
            String id,
            String patientId,
            String patientName,
            String doctorName,
            LocalDate followUpDate,
            String reason
    ) {}
}
