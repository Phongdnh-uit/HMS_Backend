package com.hms.billing_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Feign client for medical-exam-service.
 * Used to fetch exam and prescription data for invoice generation.
 */
@FeignClient(name = "medical-exam-service", path = "/exams")
public interface MedicalExamClient {

    @GetMapping("/{id}")
    ApiResponse<MedicalExamResponse> getExamById(@PathVariable String id);

    @GetMapping("/by-appointment/{appointmentId}")
    ApiResponse<MedicalExamResponse> getExamByAppointment(@PathVariable String appointmentId);

    @GetMapping("/{examId}/prescription")
    ApiResponse<PrescriptionResponse> getPrescriptionByExam(@PathVariable String examId);

    /**
     * Medical exam response DTO matching medical-exam-service contract.
     */
    record MedicalExamResponse(
        String id,
        AppointmentInfo appointment,
        PatientInfo patient,
        DoctorInfo doctor,
        String diagnosis,
        Instant examDate,
        Instant createdAt
    ) {
        public record AppointmentInfo(String id, LocalDateTime appointmentTime) {}
        public record PatientInfo(String id, String fullName) {}
        public record DoctorInfo(String id, String fullName) {}
    }

    /**
     * Prescription response DTO matching medical-exam-service contract.
     */
    record PrescriptionResponse(
        String id,
        MedicalExamInfo medicalExam,
        PatientInfo patient,
        DoctorInfo doctor,
        String status,
        Instant prescribedAt,
        String notes,
        List<PrescriptionItemResponse> items
    ) {
        public record MedicalExamInfo(String id) {}
        public record PatientInfo(String id, String fullName) {}
        public record DoctorInfo(String id, String fullName) {}
    }

    /**
     * Prescription item for invoice generation.
     */
    record PrescriptionItemResponse(
        String id,
        MedicineInfo medicine,
        Integer quantity,
        BigDecimal unitPrice,
        String dosage,
        Integer durationDays,
        String instructions
    ) {
        public record MedicineInfo(String id, String name) {}
    }
}
