package com.hms.notification_service.services;

import com.hms.notification_service.clients.MedicalExamClient;
import com.hms.notification_service.clients.PatientClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled service for sending follow-up appointment reminders.
 * Runs daily at 8 AM (or configurable for testing) to check for exams with follow-up dates.
 * 
 * The follow-up date is now stored in MedicalExam entity, set by doctors
 * when completing an examination.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FollowUpNotificationScheduler {

    private final EmailService emailService;
    private final MedicalExamClient medicalExamClient;
    private final PatientClient patientClient;

    /**
     * Days offset for follow-up check.
     * Default: 1 (tomorrow) for production.
     * Set to 0 for testing (today).
     */
    @Value("${notification.followup.days-offset:1}")
    private int followUpDaysOffset;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    /**
     * Scheduled job that runs every day at 8 AM.
     * Finds exams with follow-up date = tomorrow and sends reminder emails.
     */
    @Scheduled(cron = "${notification.followup.cron:0 0 8 * * ?}")
    public void sendFollowUpReminders() {
        log.info("Starting follow-up reminder job with days-offset: {}", followUpDaysOffset);
        
        LocalDate targetDate = LocalDate.now().plusDays(followUpDaysOffset);
        
        try {
            // Fetch exams with followUpDate = targetDate and not yet notified
            var response = medicalExamClient.getExamsForFollowUpNotification(targetDate.toString());
            
            if (response == null || response.getData() == null) {
                log.info("No exams found for follow-up notification");
                return;
            }

            List<MedicalExamClient.ExamFollowUpInfo> exams = response.getData();
            log.info("Found {} exams needing follow-up reminders", exams.size());

            int successCount = 0;
            int failCount = 0;

            for (var exam : exams) {
                try {
                    // Fetch patient email
                    var patientResponse = patientClient.getPatientById(exam.patientId());
                    
                    if (patientResponse == null || patientResponse.getData() == null) {
                        log.warn("Patient not found for exam {}", exam.examId());
                        failCount++;
                        continue;
                    }

                    var patient = patientResponse.getData();
                    
                    if (patient.email() == null || patient.email().isEmpty() || "N/A".equals(patient.email())) {
                        log.warn("No valid email for patient {} in exam {}", 
                                exam.patientId(), exam.examId());
                        failCount++;
                        continue;
                    }

                    // Send reminder email
                    emailService.sendFollowUpReminder(
                            patient.email(),
                            exam.patientName(),
                            exam.doctorName(),
                            exam.followUpDate().format(DATE_FORMATTER),
                            exam.diagnosis() != null ? exam.diagnosis() : "Follow-up visit"
                    );

                    // Mark notification as sent
                    medicalExamClient.markFollowUpNotificationSent(exam.examId());
                    successCount++;

                    log.info("Sent follow-up reminder to {} for exam {}", patient.email(), exam.examId());

                } catch (Exception e) {
                    log.error("Failed to send reminder for exam {}: {}", 
                            exam.examId(), e.getMessage());
                    failCount++;
                }
            }

            log.info("Follow-up reminder job completed. Success: {}, Failed: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("Follow-up reminder job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing (can be called via API).
     */
    public void triggerManually() {
        log.info("Manually triggering follow-up reminder job");
        sendFollowUpReminders();
    }
}
