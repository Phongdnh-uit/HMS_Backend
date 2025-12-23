package com.hms.medical_exam_service.hooks;

import com.hms.common.dtos.PageResponse;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.hooks.GenericHook;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionItemRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionRequest;
import com.hms.medical_exam_service.dtos.prescription.PrescriptionResponse;
import com.hms.medical_exam_service.entities.MedicalExam;
import com.hms.medical_exam_service.entities.Prescription;
import com.hms.medical_exam_service.entities.PrescriptionItem;
import com.hms.medical_exam_service.mappers.PrescriptionItemMapper;
import com.hms.medical_exam_service.repositories.MedicalExamRepository;
import com.hms.medical_exam_service.repositories.PrescriptionRepository;
import com.hms.medical_exam_service.clients.BillingClient;
import com.hms.common.securities.UserContext;
import com.hms.common.helpers.FeignHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hook for Prescription business logic.
 * 
 * Business Rules:
 * 1. One prescription per medical exam
 * 2. Medical exam must exist
 * 3. All medicines must exist and have sufficient stock
 * 4. Capture medicine name and price snapshots
 * 5. Decrement stock after prescription creation (with Saga compensation)
 * 6. Denormalize patientId/doctorId from exam
 * 7. Snapshot patientName/doctorName at creation time for historical accuracy and avoid service calls
 * 8. Immutable prescriptions - no updates allowed
 * 9. Cancellation tracking for prescriptions
 * 
 * Saga Pattern Implementation:
 * - Step 1: Validate prescription data
 * - Step 2: Create prescription in DB
 * - Step 3: Decrement stock for each medicine (with tracking)
 * - Compensation: If any stock decrement fails, rollback all previous decrements with retry logic
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PrescriptionHook implements GenericHook<Prescription, String, PrescriptionRequest, PrescriptionResponse> {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalExamRepository medicalExamRepository;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final WebClient.Builder webClientBuilder;
    private final BillingClient billingClient;
    
    @Value("${app.services.medicine-service.url:http://medicine-service}")
    private String medicineServiceUrl;
    
    // Context keys for passing data between hook phases
    // CONTEXT_EXAM_ID: Set by controller from @PathVariable examId
    // CONTEXT_EXAM: Set by hook after fetching from repository
    public static final String CONTEXT_EXAM_ID = "examId";
    private static final String CONTEXT_EXAM = "medicalExam";
    private static final String CONTEXT_MEDICINE_PREFIX = "medicine_";

    // ============================ VIEW ============================
    
    @Override
    public void enrichFindAll(PageResponse<PrescriptionResponse> response) {
        // No cross-service calls needed - data comes from entity snapshots
        log.debug("FindAll response with {} items (using snapshot data)", response.getContent().size());
    }

    @Override
    public void enrichFindById(PrescriptionResponse response) {
        // No cross-service calls needed - data comes from entity snapshots
        log.debug("FindById response for prescription: {} (using snapshot data)", response.getId());
    }

    // ============================ CREATE ============================
    
    @Override
    public void validateCreate(PrescriptionRequest input, Map<String, Object> context) {
        // examId comes from URL path: POST /exams/{examId}/prescriptions
        // Controller must set context.put(CONTEXT_EXAM_ID, examId) before calling service
        String examId = (String) context.get(CONTEXT_EXAM_ID);
        if (examId == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, 
                "Medical exam ID is required. Controller must set CONTEXT_EXAM_ID from @PathVariable.");
        }
        
        log.debug("Validating prescription create for examId: {}", examId);
        
        // 1. Check medical exam exists
        MedicalExam exam = medicalExamRepository.findById(examId)
            .orElseThrow(() -> {
                log.warn("Medical exam not found: {}", examId);
                return new ApiException(ErrorCode.EXAM_NOT_FOUND, 
                    "Medical exam not found: " + examId);
            });
        context.put(CONTEXT_EXAM, exam);
        
        // 2. Check unique constraint: one prescription per exam
        if (prescriptionRepository.existsByMedicalExamId(examId)) {
            log.warn("Prescription already exists for examId: {}", examId);
            throw new ApiException(ErrorCode.PRESCRIPTION_EXISTS, 
                "Prescription already exists for this medical exam");
        }
        
        // 3. Validate all medicines exist and have sufficient stock
        validateMedicines(input.getItems(), context);
        
        log.debug("Validation passed for prescription create");
    }

    @Override
    public void enrichCreate(PrescriptionRequest input, Prescription entity, Map<String, Object> context) {
        MedicalExam exam = (MedicalExam) context.get(CONTEXT_EXAM);
        
        log.debug("Enriching prescription create for examId: {}", exam.getId());
        
        // 1. Set prescribedAt timestamp
        entity.setPrescribedAt(Instant.now());
        
        // 2. Set medical exam reference
        entity.setMedicalExamId(exam.getId());
        
        // 3. Copy snapshots from exam (patient/doctor info already captured there)
        entity.setPatientId(exam.getPatientId());
        entity.setPatientName(exam.getPatientName());
        entity.setDoctorId(exam.getDoctorId());
        entity.setDoctorName(exam.getDoctorName());
        
        // 4. Process prescription items with medicine snapshots
        List<PrescriptionItem> items = prescriptionItemMapper.requestsToEntities(input.getItems());
        for (int i = 0; i < items.size(); i++) {
            PrescriptionItem item = items.get(i);
            PrescriptionItemRequest itemRequest = input.getItems().get(i);
            
            // Get cached medicine info from validation phase
            MedicineInfo medicineInfo = (MedicineInfo) context.get(CONTEXT_MEDICINE_PREFIX + itemRequest.getMedicineId());
            
            if (medicineInfo != null) {
                // Set snapshots from medicine service
                item.setMedicineName(medicineInfo.name());
                item.setUnitPrice(medicineInfo.sellingPrice());
            } else {
                // Fallback: Should not happen if validation passed
                item.setMedicineName("Unknown Medicine");
                item.setUnitPrice(BigDecimal.ZERO);
            }
            
            // Add item to prescription (sets bidirectional relationship)
            entity.addItem(item);
        }
        
        log.debug("Enriched prescription with {} items, patient: {} ({}), doctor: {} ({})", 
            entity.getItems().size(), entity.getPatientId(), entity.getPatientName(),
            entity.getDoctorId(), entity.getDoctorName());
    }

    @Override
    public void afterCreate(Prescription entity, PrescriptionResponse response, Map<String, Object> context) {
        log.info("Prescription created successfully: id={}, examId={}, itemCount={}", 
            entity.getId(), entity.getMedicalExamId(), entity.getItems().size());
        
        // 1. Set hasPrescription = true on the medical exam
        MedicalExam exam = (MedicalExam) context.get(CONTEXT_EXAM);
        if (exam != null) {
            exam.setHasPrescription(true);
            medicalExamRepository.save(exam);
            log.info("Updated exam hasPrescription=true for examId={}", exam.getId());
        }
        
        // 2. Decrement stock for each medicine
        decrementMedicineStock(entity.getItems());
        // Response already populated by mapper from entity snapshots
    }

    // ============================ UPDATE ============================
    // Prescriptions are IMMUTABLE - no update methods implemented.
    // Service layer should not expose PUT/PATCH endpoints.
    // To "change" a prescription: Cancel original → Create new prescription.

    // ============================ DELETE ============================
    // Hard delete is NOT allowed - no delete methods implemented.
    // Service layer should not expose DELETE endpoint.
    // Use cancelPrescription() method instead.
    
    // ============================ CANCEL OPERATION ============================
    // This is the proper way to "undo" a prescription in healthcare systems.
    // Called by PrescriptionService.cancel() method.
    
    /**
     * Cancels a prescription and restores stock for all items.
     * This is a business operation, not a CRUD delete.
     * 
     * @param prescription The prescription to cancel
     * @param reason The reason for cancellation (required for audit)
     * @param cancelledBy User ID who cancelled (from security context)
     * @throws ApiException if prescription is not in ACTIVE status
     */
    public void cancelPrescription(Prescription prescription, String reason, String cancelledBy) {
        log.info("[CANCEL] Cancelling prescription: id={}, reason={}", prescription.getId(), reason);
        
        // 1. Validate current status
        if (prescription.getStatus() != Prescription.Status.ACTIVE) {
            log.warn("Cannot cancel prescription {} - status is {}", 
                prescription.getId(), prescription.getStatus());
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                String.format("Cannot cancel prescription with status %s. Only ACTIVE prescriptions can be cancelled.",
                    prescription.getStatus()));
        }
        
        // 2. Restore stock for all items (saga with retry)
        restoreStockWithRetry(prescription.getItems());
        
        // 3. Update prescription status
        prescription.setStatus(Prescription.Status.CANCELLED);
        prescription.setCancelledAt(Instant.now());
        prescription.setCancelledBy(cancelledBy);
        prescription.setCancelReason(reason);
        
        // 4. Save is handled by caller (service layer with @Transactional)
        log.info("[CANCEL] Prescription cancelled successfully: id={}", prescription.getId());
    }
    
    /**
     * Dispenses a prescription (pharmacy has given medicines to patient).
     * This is the terminal state - prescription cannot be cancelled after dispense.
     * 
     * @param prescription The prescription to dispense
     * @param dispensedBy User ID who dispensed (pharmacist from security context)
     * @throws ApiException if prescription is not in ACTIVE status
     */
    public void dispensePrescription(Prescription prescription, String dispensedBy) {
        log.info("[DISPENSE] Dispensing prescription: id={}, by={}", prescription.getId(), dispensedBy);
        
        // 1. Validate current status
        if (prescription.getStatus() != Prescription.Status.ACTIVE) {
            log.warn("Cannot dispense prescription {} - status is {}", 
                prescription.getId(), prescription.getStatus());
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                String.format("Cannot dispense prescription with status %s. Only ACTIVE prescriptions can be dispensed.",
                    prescription.getStatus()));
        }
        
        // 2. Update prescription status
        prescription.setStatus(Prescription.Status.DISPENSED);
        prescription.setDispensedAt(Instant.now());
        prescription.setDispensedBy(dispensedBy);
        
        // 3. Get medical exam to find appointmentId for invoice
        MedicalExam exam = medicalExamRepository.findById(prescription.getMedicalExamId())
            .orElse(null);
        
        // 4. Generate invoice via billing-service
        if (exam != null) {
            try {
                log.info("[DISPENSE] Generating invoice for appointmentId: {}", exam.getAppointmentId());
                BillingClient.InvoiceRequest invoiceRequest = new BillingClient.InvoiceRequest(
                    exam.getAppointmentId(),
                    "Auto-generated after prescription dispense"
                );
                FeignHelper.safeCall(() -> billingClient.createInvoice(invoiceRequest));
                log.info("[DISPENSE] Invoice generated successfully for prescription: {}", prescription.getId());
            } catch (Exception e) {
                log.error("[DISPENSE] Failed to generate invoice for prescription {}: {}", 
                    prescription.getId(), e.getMessage());
                // Don't fail dispense if invoice creation fails - log for manual follow-up
            }
        }
        
        // 5. Save is handled by caller (controller with @Transactional)
        log.info("[DISPENSE] Prescription dispensed successfully: id={}", prescription.getId());
    }
    
    /**
     * Restores stock for prescription items with retry logic.
     * Used during cancellation.
     */
    private void restoreStockWithRetry(List<PrescriptionItem> items) {
        WebClient medicineClient = createMedicineClient();
        
        List<StockDecrementRecord> itemsToRestore = items.stream()
            .map(item -> new StockDecrementRecord(item.getMedicineId(), item.getQuantity()))
            .toList();
        
        log.info("[CANCEL-SAGA] Restoring stock for {} items", itemsToRestore.size());
        
        List<StockDecrementRecord> failedRestorations = new ArrayList<>();
        
        for (StockDecrementRecord record : itemsToRestore) {
            boolean restored = false;
            
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS && !restored; attempt++) {
                try {
                    log.debug("[CANCEL-SAGA] Attempt {}/{} - Restoring stock for medicine {}: +{}", 
                        attempt, MAX_RETRY_ATTEMPTS, record.medicineId(), record.quantity());
                    
                    medicineClient.patch()
                        .uri("/medicines/{id}/stock", record.medicineId())
                        .bodyValue(Map.of("delta", record.quantity()))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
                    
                    log.info("[CANCEL-SAGA] Stock restored for medicine {}: +{}", 
                        record.medicineId(), record.quantity());
                    restored = true;
                    
                } catch (Exception e) {
                    log.warn("[CANCEL-SAGA] Attempt {}/{} failed for medicine {}: {}", 
                        attempt, MAX_RETRY_ATTEMPTS, record.medicineId(), e.getMessage());
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * (1L << (attempt - 1)));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            if (!restored) {
                failedRestorations.add(record);
            }
        }
        
        if (!failedRestorations.isEmpty()) {
            logFailedCompensationsForManualRecovery(failedRestorations);
            // Don't throw - allow cancellation to proceed, log for manual fix
            log.error("[CANCEL-SAGA] {} items failed stock restoration - manual intervention required", 
                failedRestorations.size());
        }
    }

    // ============================ MEDICINE SERVICE INTEGRATION ============================
    
    /**
     * Validates all medicines exist and have sufficient stock.
     * Caches medicine info in context for enrichment phase.
     */
    private void validateMedicines(List<PrescriptionItemRequest> items, Map<String, Object> context) {
        WebClient medicineClient = createMedicineClient();
        
        for (PrescriptionItemRequest item : items) {
            String medicineId = item.getMedicineId();
            log.debug("Validating medicine: {}, quantity: {}", medicineId, item.getQuantity());
            
            try {
                // Call medicine-service to get medicine details
                MedicineApiResponse response = medicineClient.get()
                    .uri("/medicines/{id}", medicineId)
                    .retrieve()
                    .bodyToMono(MedicineApiResponse.class)
                    .block();
                
                // Extract medicine data from response wrapper
                MedicineData medicine = response.data();
                
                // Note: If medicine doesn't exist, WebClient throws WebClientResponseException.NotFound
                // before returning null, so null check is not needed here.
                
                // Check stock availability
                if (medicine.quantity() < item.getQuantity()) {
                    log.warn("Insufficient stock for medicine {}: required={}, available={}", 
                        medicineId, item.getQuantity(), medicine.quantity());
                    throw new ApiException(ErrorCode.INSUFFICIENT_STOCK, 
                        String.format("Insufficient stock for medicine %s: required %d, available %d",
                            medicine.name(), item.getQuantity(), medicine.quantity()));
                }
                
                // Cache medicine info for enrichment phase
                context.put(CONTEXT_MEDICINE_PREFIX + medicineId, 
                    new MedicineInfo(medicine.id(), medicine.name(), medicine.sellingPrice(), medicine.quantity().intValue()));
                
                log.debug("Medicine validated: {} (stock: {})", medicine.name(), medicine.quantity());
                
            } catch (WebClientResponseException.NotFound e) {
                log.warn("Medicine not found: {}", medicineId);
                throw new ApiException(ErrorCode.MEDICINE_NOT_FOUND, 
                    "Medicine not found: " + medicineId);
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error calling medicine-service for medicine {}: {}", medicineId, e.getMessage());
                throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Error validating medicine: " + e.getMessage());
            }
        }
    }
    
    
    // Retry configuration for compensation
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second base delay

    /**
     * Decrements stock for each medicine after prescription creation.
     * Implements Saga pattern with compensation (rollback) on failure.
     * 
     * Saga Flow:
     * 1. Attempt to decrement stock for each medicine sequentially
     * 2. Track successful decrements in completedDecrements list
     * 3. If any decrement fails, execute compensation with retry (restore stock for all completed items)
     * 4. Throw exception to signal saga failure
     * 
     * Compensation Reliability:
     * - Retries each compensation up to MAX_RETRY_ATTEMPTS times
     * - Uses exponential backoff between retries
     * - Logs failed compensations for manual recovery if all retries fail
     */
    private void decrementMedicineStock(List<PrescriptionItem> items) {
        WebClient medicineClient = createMedicineClient();
        
        // Track successful decrements for potential compensation
        List<StockDecrementRecord> completedDecrements = new ArrayList<>();
        
        for (PrescriptionItem item : items) {
            String medicineId = item.getMedicineId();
            int quantity = item.getQuantity();
            
            try {
                log.debug("[SAGA] Decrementing stock for medicine {}: -{}", medicineId, quantity);
                
                // Call PATCH /api/medicines/{id}/stock with negative delta
                medicineClient.patch()
                    .uri("/medicines/{id}/stock", medicineId)
                    .bodyValue(Map.of("delta", -quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
                
                // Track successful decrement for potential rollback
                completedDecrements.add(new StockDecrementRecord(medicineId, quantity));
                log.info("[SAGA] Stock decremented for medicine {}: -{}", medicineId, quantity);
                
            } catch (Exception e) {
                log.error("[SAGA] Failed to decrement stock for medicine {}: {}", medicineId, e.getMessage());
                
                // Execute compensation with retry: rollback all successful decrements
                List<StockDecrementRecord> failedCompensations = compensateStockDecrementsWithRetry(completedDecrements, medicineClient);
                
                // Log any permanently failed compensations for manual recovery
                if (!failedCompensations.isEmpty()) {
                    logFailedCompensationsForManualRecovery(failedCompensations);
                }
                
                // Throw exception to signal saga failure
                throw new ApiException(ErrorCode.STOCK_DECREMENT_FAILED, 
                    String.format("Failed to decrement stock for medicine %s. Compensation attempted for %d items, %d failed.", 
                        medicineId, completedDecrements.size(), failedCompensations.size()));
            }
        }
        
        log.info("[SAGA] All stock decrements completed successfully for {} items", completedDecrements.size());
    }
    
    /**
     * Compensation action with retry: Restores stock for all successfully decremented medicines.
     * Implements exponential backoff retry strategy for reliability.
     * 
     * @param completedDecrements List of successful decrements to rollback
     * @param medicineClient WebClient instance for medicine-service calls
     * @return List of compensations that failed after all retries (for manual recovery)
     */
    private List<StockDecrementRecord> compensateStockDecrementsWithRetry(
            List<StockDecrementRecord> completedDecrements, 
            WebClient medicineClient) {
        
        if (completedDecrements.isEmpty()) {
            log.debug("[SAGA-COMPENSATE] No decrements to rollback");
            return List.of();
        }
        
        log.warn("[SAGA-COMPENSATE] Rolling back {} stock decrements with retry", completedDecrements.size());
        
        List<StockDecrementRecord> failedCompensations = new ArrayList<>();
        
        for (StockDecrementRecord record : completedDecrements) {
            boolean compensated = false;
            
            for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS && !compensated; attempt++) {
                try {
                    log.debug("[SAGA-COMPENSATE] Attempt {}/{} - Restoring stock for medicine {}: +{}", 
                        attempt, MAX_RETRY_ATTEMPTS, record.medicineId(), record.quantity());
                    
                    // Call PATCH with positive delta to restore stock
                    medicineClient.patch()
                        .uri("/medicines/{id}/stock", record.medicineId())
                        .bodyValue(Map.of("delta", record.quantity())) // Positive = add back
                        .retrieve()
                        .toBodilessEntity()
                        .block();
                    
                    log.info("[SAGA-COMPENSATE] Stock restored for medicine {}: +{}", 
                        record.medicineId(), record.quantity());
                    compensated = true;
                    
                } catch (Exception e) {
                    log.warn("[SAGA-COMPENSATE] Attempt {}/{} failed for medicine {}: {}", 
                        attempt, MAX_RETRY_ATTEMPTS, record.medicineId(), e.getMessage());
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        // Exponential backoff: 1s, 2s, 4s
                        long delayMs = RETRY_DELAY_MS * (1L << (attempt - 1));
                        log.debug("[SAGA-COMPENSATE] Waiting {}ms before retry...", delayMs);
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("[SAGA-COMPENSATE] Retry interrupted");
                            break;
                        }
                    }
                }
            }
            
            if (!compensated) {
                failedCompensations.add(record);
            }
        }
        
        log.warn("[SAGA-COMPENSATE] Compensation completed: {}/{} successful, {} failed", 
            completedDecrements.size() - failedCompensations.size(),
            completedDecrements.size(),
            failedCompensations.size());
        
        return failedCompensations;
    }
    
    /**
     * Logs failed compensations for manual recovery.
     * In production, this should:
     * 1. Write to a compensation_log table
     * 2. Send alert to operations team
     * 3. Publish to dead letter queue for later processing
     * 
     * @param failedCompensations List of compensations that failed after all retries
     */
    private void logFailedCompensationsForManualRecovery(List<StockDecrementRecord> failedCompensations) {
        log.error("╔══════════════════════════════════════════════════════════════════╗");
        log.error("║  CRITICAL: SAGA COMPENSATION FAILED - MANUAL INTERVENTION NEEDED ║");
        log.error("╠══════════════════════════════════════════════════════════════════╣");
        for (StockDecrementRecord record : failedCompensations) {
            log.error("║  Medicine: {} | Quantity to restore: +{}",
                String.format("%-20s", record.medicineId()), 
                String.format("%-5d", record.quantity()));
        }
        log.error("╠══════════════════════════════════════════════════════════════════╣");
        log.error("║  ACTION REQUIRED:                                                ║");
        log.error("║  1. Check medicine-service health                                ║");
        log.error("║  2. Manually restore stock via admin API or database             ║");
        log.error("║  3. Verify inventory consistency                                 ║");
        log.error("╚══════════════════════════════════════════════════════════════════╝");
        
        // TODO: In production, add these features:
        // 1. Save to compensation_log table for scheduled retry
        // compensationLogRepository.save(new CompensationLog(record, "PENDING"));
        //
        // 2. Send alert to operations team
        // alertService.sendCriticalAlert("Saga compensation failed", failedCompensations);
        //
        // 3. Publish to dead letter queue
        // deadLetterQueue.publish(new CompensationEvent(failedCompensations));
    }
    
    // ============================ INTERNAL RECORDS ============================
    
    /**
     * Internal record for caching medicine info between validation and enrichment phases.
     */
    private record MedicineInfo(String id, String name, BigDecimal sellingPrice, Integer stockQuantity) {}
    
    /**
     * Creates a WebClient for medicine-service with forwarded user context headers.
     * This enables internal service-to-service calls to pass user identity.
     */
    private WebClient createMedicineClient() {
        WebClient.Builder builder = webClientBuilder.baseUrl(medicineServiceUrl);
        
        // Forward user context headers for internal calls
        UserContext.User user = UserContext.getUser();
        if (user != null) {
            builder.defaultHeader("X-User-ID", user.getId() != null ? user.getId() : "system");
            builder.defaultHeader("X-User-Role", user.getRole() != null ? user.getRole() : "SYSTEM");
            builder.defaultHeader("X-User-Email", user.getEmail() != null ? user.getEmail() : "");
        } else {
            // For system/internal calls without user context
            builder.defaultHeader("X-User-ID", "system");
            builder.defaultHeader("X-User-Role", "SYSTEM");
        }
        
        return builder.build();
    }
    
    /**
     * Record for tracking successful stock decrements during saga execution.
     * Used for compensation (rollback) if a subsequent step fails.
     */
    private record StockDecrementRecord(String medicineId, int quantity) {}
    
    /**
     * Record for medicine-service response wrapper.
     * Maps the expected JSON structure from GET /api/medicines/{id}
     * Response format: {"code": 1000, "message": "success", "data": {...}}
     */
    private record MedicineApiResponse(
        Integer code,
        String message,
        MedicineData data
    ) {}
    
    /**
     * Record for medicine data inside API response.
     */
    private record MedicineData(
        String id,
        String name,
        BigDecimal sellingPrice,
        Long quantity
    ) {}
}
