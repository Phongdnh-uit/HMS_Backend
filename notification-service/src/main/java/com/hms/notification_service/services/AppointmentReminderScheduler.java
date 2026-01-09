package com.hms.notification_service.services;

import com.hms.notification_service.clients.AppointmentClient;
import com.hms.notification_service.clients.PatientClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Scheduled service for sending appointment reminder emails.
 * Runs daily (configurable) to check for appointments scheduled for tomorrow
 * and sends reminder emails to patients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final EmailService emailService;
    private final AppointmentClient appointmentClient;
    private final PatientClient patientClient;

    /**
     * Days offset for reminder check.
     * Default: 1 (tomorrow) for production.
     * Set to 0 for testing (today).
     */
    @Value("${notification.reminder.days-offset:1}")
    private int reminderDaysOffset;

    @Value("${notification.reminder.cron:0 0 8 * * ?}")
    private String reminderCron;

    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    @PostConstruct
    public void init() {
        log.info("🔔🔔🔔 AppointmentReminderScheduler INITIALIZED - cron: {}, days-offset: {}", 
                reminderCron, reminderDaysOffset);
    }

    /**
     * Scheduled job that runs based on configured cron expression.
     * Default: every day at 8 AM.
     * For testing: configure notification.reminder.cron=*\/5 * * * * ? (every 5 seconds)
     * 
     * Finds SCHEDULED appointments where appointmentTime = targetDate and reminderSent = false,
     * then sends reminder emails.
     */
    @Scheduled(cron = "${notification.reminder.cron:0 0 8 * * ?}")
    public void sendAppointmentReminders() {
        log.info("🔔 Starting appointment reminder job with days-offset: {}", reminderDaysOffset);
        
        LocalDate targetDate = LocalDate.now().plusDays(reminderDaysOffset);
        
        try {
            // Fetch appointments needing reminders
            var response = appointmentClient.getAppointmentsForReminder(targetDate.toString());
            
            if (response == null || response.getData() == null) {
                log.info("No appointments found for reminder notification on {}", targetDate);
                return;
            }

            List<AppointmentClient.AppointmentReminderInfo> appointments = response.getData();
            log.info("📋 Found {} appointments needing reminders for {}", appointments.size(), targetDate);

            int successCount = 0;
            int failCount = 0;

            for (var appointment : appointments) {
                try {
                    // Fetch patient email
                    var patientResponse = patientClient.getPatientById(appointment.patientId());
                    
                    if (patientResponse == null || patientResponse.getData() == null) {
                        log.warn("Patient not found for appointment {}", appointment.id());
                        failCount++;
                        continue;
                    }

                    var patient = patientResponse.getData();
                    
                    if (patient.email() == null || patient.email().isEmpty() || "N/A".equals(patient.email())) {
                        log.warn("No valid email for patient {} in appointment {}", 
                                appointment.patientId(), appointment.id());
                        failCount++;
                        continue;
                    }

                    // Format date and time for Vietnamese locale
                    String formattedDate = appointment.appointmentTime()
                            .atZone(ZONE_ID)
                            .format(DATE_FORMATTER);
                    String formattedTime = appointment.appointmentTime()
                            .atZone(ZONE_ID)
                            .format(TIME_FORMATTER);

                    // Send reminder email
                    emailService.sendAppointmentReminder(
                            patient.email(),
                            appointment.patientName() != null ? appointment.patientName() : patient.fullName(),
                            appointment.doctorName() != null ? appointment.doctorName() : "Bác sĩ",
                            formattedDate,
                            formattedTime,
                            appointment.doctorDepartment(),
                            appointment.reason()
                    );

                    // Mark reminder as sent
                    appointmentClient.markReminderSent(appointment.id());
                    successCount++;

                    log.info("✅ Sent appointment reminder to {} for appointment {}", 
                            patient.email(), appointment.id());

                } catch (Exception e) {
                    log.error("❌ Failed to send reminder for appointment {}: {}", 
                            appointment.id(), e.getMessage());
                    failCount++;
                }
            }

            log.info("🔔 Appointment reminder job completed. Success: {}, Failed: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("❌ Appointment reminder job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for testing (can be called via API or for debugging).
     */
    public void triggerManually() {
        log.info("🔧 Manually triggering appointment reminder job");
        sendAppointmentReminders();
    }
}
