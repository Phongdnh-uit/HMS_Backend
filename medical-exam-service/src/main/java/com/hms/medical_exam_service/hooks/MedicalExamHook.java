package com.hms.medical_exam_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.medical_exam_service.dtos.exam.MedicalExamRequest;
import com.hms.medical_exam_service.dtos.exam.MedicalExamResponse;
import com.hms.medical_exam_service.dtos.external.AppointmentResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Hook for MedicalExam business logic.
 * 
 * Business Rules:
 * 1. One exam per appointment (UNIQUE constraint validation)
 * 2. Appointment must exist and be COMPLETED (MVP: mock validation)
 * 3. Cannot update exam after 24 hours (audit integrity)
 * 
 * Snapshot Propagation Pattern:
 * - Appointment captures patientName/doctorName at booking time
 * - MedicalExam copies these snapshots at creation (no cross-service calls to patient/employee services)
 * - Data flow: Appointment → MedicalExam → Prescription
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MedicalExamHook implements GenericHook<MedicalExam, String, MedicalExamRequest, MedicalExamResponse> {

    private final MedicalExamRepository medicalExamRepository;
    private final WebClient.Builder webClientBuilder;
    
    // 24-hour rule for exam modification
    private static final Duration MODIFICATION_WINDOW = Duration.ofHours(24);
    
    // MVP flag: set to false to enable real appointment-service calls
    private static final boolean USE_MOCK_APPOINTMENT = true;

    // ============================ VIEW ============================
    
    @Override
    public void enrichFindAll(PageResponse<MedicalExamResponse> response) {
        // No cross-service calls needed - data comes from entity snapshots
        log.debug("FindAll response with {} items (using snapshot data)", response.getContent().size());
    }

    @Override
    public void enrichFindById(MedicalExamResponse response) {
        // No cross-service calls needed - data comes from entity snapshots
        log.debug("FindById response for exam: {} (using snapshot data)", response.getId());
    }

    // ============================ CREATE ============================
    
    @Override
    public void validateCreate(MedicalExamRequest input, Map<String, Object> context) {
        log.debug("Validating create for appointmentId: {}", input.getAppointmentId());
        
        // 1. Check unique constraint: one exam per appointment
        if (medicalExamRepository.existsByAppointmentId(input.getAppointmentId())) {
            log.warn("Exam already exists for appointmentId: {}", input.getAppointmentId());
            throw new ApiException(ErrorCode.EXAM_EXISTS, 
                "Medical exam already exists for appointment: " + input.getAppointmentId());
        }
        
        // 2. Fetch appointment (stores in context for enrichCreate)
        AppointmentResponse appointment = fetchAppointment(input.getAppointmentId());
        
        // 3. Validate appointment exists
        if (appointment == null) {
            log.warn("Appointment not found: {}", input.getAppointmentId());
            throw new ApiException(ErrorCode.APPOINTMENT_NOT_FOUND, 
                "Appointment not found: " + input.getAppointmentId());
        }
        
        // 4. Validate appointment status is COMPLETED
        if (!"COMPLETED".equals(appointment.status())) {
            log.warn("Appointment {} is not COMPLETED, status: {}", input.getAppointmentId(), appointment.status());
            throw new ApiException(ErrorCode.APPOINTMENT_NOT_COMPLETED, 
                "Appointment must be COMPLETED to create medical exam. Current status: " + appointment.status());
        }
        
        // Store appointment in context for enrichCreate (avoid duplicate call)
        context.put("appointment", appointment);
        
        log.debug("Validation passed for appointmentId: {}", input.getAppointmentId());
    }

    @Override
    public void enrichCreate(MedicalExamRequest input, MedicalExam entity, Map<String, Object> context) {
        log.debug("Enriching create for appointmentId: {}", input.getAppointmentId());
        
        // 1. Set exam date to now
        entity.setExamDate(Instant.now());
        
        // 2. Get appointment from context (fetched in validateCreate)
        AppointmentResponse appointment = (AppointmentResponse) context.get("appointment");
        if (appointment == null) {
            // Fallback: fetch again if not in context
            appointment = fetchAppointment(input.getAppointmentId());
        }
        
        // 3. Copy snapshot data from appointment (Snapshot Propagation Pattern)
        // No cross-service calls to patient-service or employee-service needed!
        entity.setPatientId(appointment.patientId());
        entity.setPatientName(appointment.patientName());
        entity.setDoctorId(appointment.doctorId());
        entity.setDoctorName(appointment.doctorName());
        
        log.debug("Enriched exam with examDate: {}, patient: {} ({}), doctor: {} ({})", 
            entity.getExamDate(), entity.getPatientId(), entity.getPatientName(),
            entity.getDoctorId(), entity.getDoctorName());
    }
    
    /**
     * Fetches appointment data from appointment-service.
     * MVP: Uses mock data; set USE_MOCK_APPOINTMENT=false to call real service.
     */
    private AppointmentResponse fetchAppointment(String appointmentId) {
        if (USE_MOCK_APPOINTMENT) {
            log.debug("Using mock appointment for: {}", appointmentId);
            return AppointmentResponse.createMock(appointmentId);
        }
        
        // Real service call via WebClient
        log.debug("Fetching appointment from appointment-service: {}", appointmentId);
        return webClientBuilder.build()
            .get()
            .uri("http://APPOINTMENT-SERVICE/api/v1/appointments/{id}", appointmentId)
            .retrieve()
            .bodyToMono(AppointmentResponse.class)
            .block();
    }

    @Override
    public void afterCreate(MedicalExam entity, MedicalExamResponse response, Map<String, Object> context) {
        log.info("Medical exam created successfully: id={}, appointmentId={}", 
            entity.getId(), entity.getAppointmentId());
        // Response already populated by mapper from entity snapshots
    }

    // ============================ UPDATE ============================
    
    @Override
    public void validateUpdate(String id, MedicalExamRequest input, MedicalExam existingEntity, Map<String, Object> context) {
        log.debug("Validating update for exam: {}", id);
        
        // 1. Check 24-hour rule
        Instant createdAt = existingEntity.getCreatedAt();
        if (createdAt != null) {
            Duration timeSinceCreation = Duration.between(createdAt, Instant.now());
            if (timeSinceCreation.compareTo(MODIFICATION_WINDOW) > 0) {
                log.warn("Exam {} cannot be modified - created {} ago (limit: {})", 
                    id, timeSinceCreation, MODIFICATION_WINDOW);
                throw new ApiException(ErrorCode.EXAM_NOT_MODIFIABLE, 
                    "Medical exam cannot be modified after 24 hours");
            }
        }
        
        // 2. Check ownership: Only creating doctor can update (unless ADMIN)
        // TODO: Get current user from UserContext and validate
        // String currentUserId = UserContext.getCurrentUserId();
        // if (!existingEntity.getCreatedBy().equals(currentUserId) && !UserContext.isAdmin()) {
        //     throw new ApiException(ErrorCode.FORBIDDEN, "Only the creating doctor can modify this exam");
        // }
        
        log.debug("Validation passed for update exam: {}", id);
    }

    @Override
    public void enrichUpdate(MedicalExamRequest input, MedicalExam entity, Map<String, Object> context) {
        // Note: appointmentId cannot be changed during update
        // Keep existing denormalized fields (patientId, doctorId)
        log.debug("Enriching update for exam: {}", entity.getId());
    }

    @Override
    public void afterUpdate(MedicalExam entity, MedicalExamResponse response, Map<String, Object> context) {
        log.info("Medical exam updated successfully: id={}", entity.getId());
        // Response already populated by mapper from entity snapshots
    }

    // ============================ DELETE ============================
    // Medical exams are medical records - should not be hard deleted for audit/legal compliance.
    // Use soft delete if needed (add deletedAt field to entity).
    
    @Override
    public void validateDelete(String id) {
        log.warn("Attempted to delete medical exam {} - operation blocked", id);
        throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
            "Medical exams cannot be deleted (audit/legal requirement). Contact administrator for data correction.");
    }

    @Override
    public void afterDelete(String id) {
        // Should never be called due to validateDelete blocking
    }

    @Override
    public void validateBulkDelete(Iterable<String> ids) {
        log.warn("Attempted to bulk delete medical exams - operation blocked");
        throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
            "Medical exams cannot be deleted (audit/legal requirement).");
    }

    @Override
    public void afterBulkDelete(Iterable<String> ids) {
        // Should never be called due to validateBulkDelete blocking
    }
}
