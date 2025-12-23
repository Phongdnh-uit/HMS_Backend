package com.hms.medical_exam_service.controllers;

import com.hms.common.controllers.GenericController;
import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.securities.UserContext;
import com.hms.common.services.CrudService;
import com.hms.medical_exam_service.clients.PatientClient;
import com.hms.medical_exam_service.dtos.exam.DiagnosisStatsResponse;
import com.hms.medical_exam_service.dtos.exam.FollowUpNotificationDto;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.mappers.MedicalExamMapper;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Medical Exam operations.
 * 
 * Endpoints:
 * - GET /exams/all - List all exams (with filter, pagination)
 * - GET /exams/{id} - Get exam by ID
 * - GET /exams/by-appointment/{appointmentId} - Get exam by appointment
 * - GET /exams/stats - Get diagnosis statistics
 * - POST /exams - Create exam
 * - PUT /exams/{id} - Update exam
 * - DELETE /exams/{id} - Delete exam (blocked in hook)
 */
@RestController
@RequestMapping("/exams")
@Slf4j
public class MedicalExamController extends GenericController<MedicalExam, String, MedicalExamRequest, MedicalExamResponse> {

    private final MedicalExamRepository medicalExamRepository;
    private final MedicalExamMapper medicalExamMapper;
    private final PatientClient patientClient;

    public MedicalExamController(
            CrudService<MedicalExam, String, MedicalExamRequest, MedicalExamResponse> service,
            MedicalExamRepository medicalExamRepository,
            MedicalExamMapper medicalExamMapper,
            PatientClient patientClient) {
        super(service);
        this.medicalExamRepository = medicalExamRepository;
        this.medicalExamMapper = medicalExamMapper;
        this.patientClient = patientClient;
    }

    /**
     * Override findAll to enforce PATIENT role can only see their own medical exams.
     * For PATIENT users, this automatically filters by their patientId.
     */
    @Override
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<MedicalExamResponse>>> findAll(
            Pageable pageable,
            @RequestParam(value = "filter", required = false) @Nullable String filter,
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        
        String effectiveFilter = filter;
        
        // Check if current user is PATIENT role
        UserContext.User currentUser = UserContext.getUser();
        if (currentUser != null && "PATIENT".equals(currentUser.getRole())) {
            try {
                // Fetch patient profile to get patientId
                var patientResponse = patientClient.getMyPatientProfile();
                if (patientResponse != null && patientResponse.getData() != null) {
                    String patientId = patientResponse.getData().id();
                    log.info("PATIENT role detected. Enforcing filter for patientId: {}", patientId);
                    
                    // Prepend patient filter to existing filter
                    String patientFilter = "patientId==" + patientId;
                    if (effectiveFilter != null && !effectiveFilter.isBlank()) {
                        effectiveFilter = patientFilter + ";" + effectiveFilter;
                    } else {
                        effectiveFilter = patientFilter;
                    }
                } else {
                    log.warn("PATIENT role but no patient profile found. Returning empty results.");
                    return ResponseEntity.ok(ApiResponse.ok(PageResponse.empty()));
                }
            } catch (Exception e) {
                log.error("Failed to fetch patient profile for PATIENT role: {}", e.getMessage());
                return ResponseEntity.ok(ApiResponse.ok(PageResponse.empty()));
            }
        }
        
        Specification<MedicalExam> specification = RSQLJPASupport.toSpecification(effectiveFilter);
        if (all) {
            pageable = Pageable.unpaged(pageable.getSort());
        }
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(pageable, specification)));
    }

    /**
     * Get medical exam by appointment ID.
     * Since there's a 1:1 relationship (one exam per appointment), this returns a single exam.
     * 
     * @param appointmentId The appointment ID
     * @return The medical exam for this appointment
     */
    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<MedicalExamResponse>> getByAppointment(
            @PathVariable String appointmentId) {
        
        MedicalExam exam = medicalExamRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ApiException(ErrorCode.EXAM_NOT_FOUND, 
                "No medical exam found for appointment: " + appointmentId));
        
        MedicalExamResponse response = medicalExamMapper.entityToResponse(exam);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    /**
     * Get diagnosis statistics for reporting.
     * Returns top 10 diagnoses with counts and percentages.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DiagnosisStatsResponse>> getStats() {
        long totalWithDiagnosis = medicalExamRepository.countWithDiagnosis();
        
        List<Object[]> diagnosisCounts = medicalExamRepository.countTopDiagnoses();
        List<DiagnosisStatsResponse.TopDiagnosis> topDiagnoses = new ArrayList<>();
        
        int limit = Math.min(diagnosisCounts.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = diagnosisCounts.get(i);
            String diagnosis = row[0] != null ? row[0].toString() : "Unknown";
            int count = ((Number) row[1]).intValue();
            double percentage = totalWithDiagnosis > 0 ? Math.round((count * 1000.0 / totalWithDiagnosis)) / 10.0 : 0;
            
            topDiagnoses.add(DiagnosisStatsResponse.TopDiagnosis.builder()
                    .diagnosis(diagnosis)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }
        
        DiagnosisStatsResponse response = DiagnosisStatsResponse.builder()
                .topDiagnoses(topDiagnoses)
                .totalExamsWithDiagnosis(totalWithDiagnosis)
                .generatedAt(Instant.now())
                .build();
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    /**
     * Get exams that need follow-up notifications.
     * Used by notification service to send reminder emails.
     * 
     * @param followUpDate The target follow-up date to check
     * @return List of exams needing follow-up notification
     */
    @GetMapping("/pending-followup-notifications")
    public ResponseEntity<ApiResponse<List<FollowUpNotificationDto>>> getExamsForFollowUpNotification(
            @RequestParam("followUpDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpDate) {
        
        List<MedicalExam> exams = medicalExamRepository.findByFollowUpDateAndFollowUpNotificationSentFalse(followUpDate);
        
        List<FollowUpNotificationDto> dtos = exams.stream()
                .map(exam -> FollowUpNotificationDto.builder()
                        .examId(exam.getId())
                        .appointmentId(exam.getAppointmentId())
                        .patientId(exam.getPatientId())
                        .patientName(exam.getPatientName())
                        .doctorName(exam.getDoctorName())
                        .followUpDate(exam.getFollowUpDate())
                        .diagnosis(exam.getDiagnosis())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }
    
    /**
     * Mark an exam's follow-up notification as sent.
     * Called by notification service after successfully sending the email.
     * Using POST instead of PATCH for Feign client compatibility.
     * 
     * @param id The exam ID
     */
    @PostMapping("/{id}/mark-followup-notification-sent")
    public ResponseEntity<ApiResponse<Void>> markFollowUpNotificationSent(@PathVariable String id) {
        MedicalExam exam = medicalExamRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.EXAM_NOT_FOUND, "Exam not found: " + id));
        
        exam.setFollowUpNotificationSent(true);
        medicalExamRepository.save(exam);
        
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
