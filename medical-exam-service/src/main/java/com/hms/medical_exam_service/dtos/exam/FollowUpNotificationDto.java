package com.hms.medical_exam_service.dtos.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO for follow-up notification data.
 * Used by notification service to send follow-up reminders.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class FollowUpNotificationDto {
    
    private String examId;
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String patientEmail;
    private String doctorName;
    private LocalDate followUpDate;
    private String diagnosis;
}
