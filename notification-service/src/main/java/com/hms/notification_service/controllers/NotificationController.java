package com.hms.notification_service.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.notification_service.services.EmailService;
import com.hms.notification_service.services.FollowUpNotificationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for notification management and testing.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;
    private final FollowUpNotificationScheduler scheduler;

    /**
     * Manually trigger follow-up notification job (for testing).
     */
    @PostMapping("/trigger-followup-job")
    public ResponseEntity<ApiResponse<String>> triggerFollowUpJob() {
        scheduler.triggerManually();
        return ResponseEntity.ok(ApiResponse.ok("Follow-up notification job triggered"));
    }

    /**
     * Send a test email (for testing SMTP configuration).
     */
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(
            @RequestParam String toEmail,
            @RequestParam(defaultValue = "Test Email") String subject,
            @RequestParam(defaultValue = "This is a test email from HMS.") String content
    ) {
        emailService.sendSimpleEmail(toEmail, subject, content);
        return ResponseEntity.ok(ApiResponse.ok("Test email sent to " + toEmail));
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Notification service is running"));
    }
}
