package com.hms.auth_service.services;

import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String senderAddress;

    /**
     * @param to : recipient of the email
     * @param subject : subject of the email
     * @param content : content of the email, can be HTML or plain text
     * @param isMultipart : if the email contains attachments
     * @param isHtml : if the email content is HTML
     */
    @Async
    public void sendEmail(
            String to, String subject, String content, boolean isMultipart, boolean isHtml) {

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, isMultipart, "UTF-8");
            helper.setTo(to);
            helper.setFrom(senderAddress);
            helper.setSubject(subject);
            helper.setText(content, isHtml);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {} with subject {}: {}", to, subject, e.getMessage(), e);
            throw new ApiException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "Failed to send email");
        }
    }

    /**
     * @param to : recipient of the email
     * @param subject : subject of the email
     * @param templateName : name of the email template
     * @param model : model data to be used in the template
     */
    @Async
    public void sendEmailFromTemplate(
            String to, String subject, String templateName, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        String content = templateEngine.process(templateName, context);
        sendEmail(to, subject, content, false, true);
    }
}
