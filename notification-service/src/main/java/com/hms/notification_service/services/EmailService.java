package com.hms.notification_service.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Service for sending emails using Spring Mail and Thymeleaf templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@hms.com}")
    private String fromEmail;

    @Value("${app.name:Hospital Management System}")
    private String appName;

    @Value("${app.booking-url:http://localhost:3000/login}")
    private String bookingUrl;

    /**
     * Send follow-up reminder email to patient.
     */
    public void sendFollowUpReminder(String toEmail, String patientName, String doctorName, 
                                      String followUpDate, String appointmentReason) {
        try {
            Context context = new Context();
            context.setVariable("patientName", patientName);
            context.setVariable("doctorName", doctorName);
            context.setVariable("followUpDate", followUpDate);
            context.setVariable("appointmentReason", appointmentReason);
            context.setVariable("appName", appName);
            context.setVariable("bookingUrl", bookingUrl);

            String htmlContent = templateEngine.process("follow-up-reminder", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Follow-up Appointment Reminder - " + appName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Follow-up reminder sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send follow-up reminder to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send simple text email (for testing or simple notifications).
     */
    public void sendSimpleEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
            log.info("Email sent to {}: {}", toEmail, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
