package com.hms.notification_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Feign client for medical-exam-service.
 * Used for follow-up notification queries.
 */
@FeignClient(name = "medical-exam-service")
public interface MedicalExamClient {

    /**
     * Get exams that need follow-up notification.
     * Returns exams where followUpDate = given date and followUpNotificationSent = false.
     */
    @GetMapping("/exams/pending-followup-notifications")
    ApiResponse<List<ExamFollowUpInfo>> getExamsForFollowUpNotification(
            @RequestParam("followUpDate") String followUpDate
    );

    /**
     * Mark an exam's follow-up notification as sent.
     * Using POST instead of PATCH for Feign client compatibility.
     */
    @PostMapping("/exams/{id}/mark-followup-notification-sent")
    ApiResponse<Void> markFollowUpNotificationSent(@PathVariable("id") String id);

    /**
     * Exam record for notification purposes.
     */
    record ExamFollowUpInfo(
            String examId,
            String appointmentId,
            String patientId,
            String patientName,
            String doctorName,
            LocalDate followUpDate,
            String diagnosis
    ) {}
}
