package com.hms.medical_exam_service.controllers;

import com.hms.common.dtos.ApiResponse;
import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.medical_exam_service.dtos.prescription.CancelPrescriptionRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionResponse;
import com.hms.medical_exam_service.entities.Prescription;
import com.hms.medical_exam_service.hooks.PrescriptionHook;
import com.hms.medical_exam_service.mappers.PrescriptionMapper;
import com.hms.medical_exam_service.repositories.PrescriptionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Prescription operations.
 * 
 * This controller does NOT extend GenericController because:
 * 1. Prescriptions are immutable (no update endpoint)
 * 2. Prescriptions cannot be deleted (only cancelled)
 * 3. Create is nested under exam: POST /exams/{examId}/prescriptions
 * 4. Custom cancel endpoint: POST /exams/prescriptions/{id}/cancel
 * 
 * Endpoints:
 * - POST /exams/{examId}/prescriptions - Create prescription for exam
 * - GET /exams/prescriptions/{id} - Get prescription by ID
 * - GET /exams/{examId}/prescription - Get prescription by exam (singular - 1:1)
 * - GET /exams/prescriptions/by-patient/{patientId} - List prescriptions by patient
 * - POST /exams/prescriptions/{id}/cancel - Cancel prescription
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/exams")
public class PrescriptionController {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionHook prescriptionHook;

    /**
     * Create a prescription for a medical exam.
     * One prescription per exam (enforced by hook).
     * 
     * @param examId The medical exam ID (from URL path)
     * @param request The prescription data with items
     * @return Created prescription
     */
    @PostMapping("/{examId}/prescriptions")
    @Transactional
    public ResponseEntity<ApiResponse<PrescriptionResponse>> create(
            @PathVariable String examId,
            @Valid @RequestBody PrescriptionRequest request) {
        
        log.info("Creating prescription for examId: {}", examId);
        
        // Build context with examId from path
        Map<String, Object> context = new HashMap<>();
        context.put(PrescriptionHook.CONTEXT_EXAM_ID, examId);
        
        // 1. Validate (checks exam exists, one-per-exam, medicines stock)
        prescriptionHook.validateCreate(request, context);
        
        // 2. Map request to entity
        Prescription entity = prescriptionMapper.requestToEntity(request);
        
        // 3. Enrich (set timestamps, snapshots, process items)
        prescriptionHook.enrichCreate(request, entity, context);
        
        // 4. Save
        Prescription saved = prescriptionRepository.save(entity);
        
        // 5. Map to response
        PrescriptionResponse response = prescriptionMapper.entityToResponse(saved);
        
        // 6. After create (stock decrement via saga)
        prescriptionHook.afterCreate(saved, response, context);
        
        log.info("Prescription created: id={}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * Get prescription by ID.
     * 
     * @param id The prescription ID
     * @return The prescription
     */
    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getById(@PathVariable String id) {
        log.debug("Getting prescription by id: {}", id);
        
        Prescription prescription = prescriptionRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.PRESCRIPTION_NOT_FOUND, 
                "Prescription not found: " + id));
        
        PrescriptionResponse response = prescriptionMapper.entityToResponse(prescription);
        prescriptionHook.enrichFindById(response);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get prescription by exam ID.
     * Since there's a 1:1 relationship (one prescription per exam), this returns a single prescription.
     * 
     * @param examId The medical exam ID
     * @return The prescription for this exam
     */
    @GetMapping("/{examId}/prescription")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getByExam(@PathVariable String examId) {
        log.debug("Getting prescription by examId: {}", examId);
        
        Prescription prescription = prescriptionRepository.findByMedicalExamId(examId)
            .orElseThrow(() -> new ApiException(ErrorCode.PRESCRIPTION_NOT_FOUND, 
                "No prescription found for exam: " + examId));
        
        PrescriptionResponse response = prescriptionMapper.entityToResponse(prescription);
        prescriptionHook.enrichFindById(response);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * List prescriptions by patient ID with pagination.
     * 
     * @param patientId The patient ID
     * @param status Optional status filter (ACTIVE, CANCELLED, DISPENSED)
     * @param pageable Pagination parameters
     * @return Page of prescriptions for this patient
     */
    @GetMapping("/prescriptions/by-patient/{patientId}")
    public ResponseEntity<ApiResponse<PageResponse<PrescriptionResponse>>> getByPatient(
            @PathVariable String patientId,
            @RequestParam(required = false) Prescription.Status status,
            Pageable pageable) {
        
        log.debug("Getting prescriptions for patientId: {}, status: {}", patientId, status);
        
        Page<Prescription> page;
        if (status != null) {
            page = prescriptionRepository.findByPatientIdAndStatus(patientId, status, pageable);
        } else {
            page = prescriptionRepository.findByPatientId(patientId, pageable);
        }
        
        Page<PrescriptionResponse> responsePage = page.map(prescriptionMapper::entityToResponse);
        PageResponse<PrescriptionResponse> response = PageResponse.fromPage(responsePage);
        
        prescriptionHook.enrichFindAll(response);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Cancel a prescription.
     * Restores medicine stock and sets status to CANCELLED.
     * Only ACTIVE prescriptions can be cancelled.
     * 
     * @param id The prescription ID
     * @param request The cancellation reason
     * @return The cancelled prescription
     */
    @PostMapping("/prescriptions/{id}/cancel")
    @Transactional
    public ResponseEntity<ApiResponse<PrescriptionResponse>> cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelPrescriptionRequest request) {
        
        log.info("Cancelling prescription: id={}", id);
        
        // 1. Fetch prescription
        Prescription prescription = prescriptionRepository.findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.PRESCRIPTION_NOT_FOUND, 
                "Prescription not found: " + id));
        
        // 2. Cancel via hook (validates status, restores stock, updates fields)
        // TODO: Get actual user ID from security context
        String cancelledBy = "system"; // Placeholder - will be from SecurityContext
        prescriptionHook.cancelPrescription(prescription, request.getReason(), cancelledBy);
        
        // 3. Save
        Prescription saved = prescriptionRepository.save(prescription);
        
        // 4. Map to response
        PrescriptionResponse response = prescriptionMapper.entityToResponse(saved);
        
        log.info("Prescription cancelled: id={}", saved.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
