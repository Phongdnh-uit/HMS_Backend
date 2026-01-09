package com.hms.billing_service.hooks;

import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.dtos.InvoiceResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.InvoiceItem;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.billing_service.clients.MedicalExamClient;
import com.hms.billing_service.clients.AppointmentClient;
import com.hms.billing_service.clients.PatientClient;
import com.hms.billing_service.clients.HrClient;
import com.hms.common.hooks.GenericHook;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.helpers.FeignHelper;
import com.hms.common.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceHook implements GenericHook<Invoice, String, InvoiceRequest, InvoiceResponse> {

    private final InvoiceRepository invoiceRepository;
    private final MedicalExamClient medicalExamClient;
    private final AppointmentClient appointmentClient;
    private final PatientClient patientClient;
    private final HrClient hrClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    // Simple counter for invoice number generation
    private static final AtomicLong invoiceCounter = new AtomicLong(System.currentTimeMillis() % 10000);

    public static final String CONTEXT_EXAM = "exam";
    public static final String CONTEXT_PRESCRIPTION = "prescription";
    public static final String CONTEXT_CONSULTATION_FEE = "consultationFee";
    public static final String CONTEXT_LAB_TESTS = "labTests";

    @Override
    public void validateCreate(InvoiceRequest request, Map<String, Object> context) {
        String appointmentId = request.getAppointmentId();
        String examId = request.getExamId(); // New: examId passed directly

        var medicalExamCircuitBreaker = circuitBreakerFactory.create("billingMedicalExam");
        var hrCircuitBreaker = circuitBreakerFactory.create("billingHr");

        // Check if invoice already exists - if so, we'll UPDATE it instead of creating new
        Invoice existingInvoice = invoiceRepository.findByAppointmentId(appointmentId).orElse(null);
        if (existingInvoice != null) {
            log.info("Invoice already exists for appointment: {}, will update items", appointmentId);
            context.put("EXISTING_INVOICE", existingInvoice);
        }

        // Fetch medical exam - use examId if provided, otherwise by appointment
        // This is CRITICAL - fail fast if medical exam service is unavailable
        MedicalExamClient.MedicalExamResponse exam;
        if (examId != null && !examId.isEmpty()) {
            // Direct exam lookup by ID (more reliable)
            log.info("Fetching exam directly by examId: {}", examId);
            ApiResponse<MedicalExamClient.MedicalExamResponse> examResponse = medicalExamCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> medicalExamClient.getExamById(examId)),
                throwable -> {
                    log.error("[CB-FALLBACK] Medical exam service unavailable: {}", throwable.getMessage());
                    throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                        "Medical exam service unavailable. Cannot generate invoice.");
                }
            );
            exam = examResponse.getData();
        } else {
            // Fallback: lookup by appointment
            log.info("Fetching exam by appointmentId: {}", appointmentId);
            ApiResponse<MedicalExamClient.MedicalExamResponse> examResponse = medicalExamCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> medicalExamClient.getExamByAppointment(appointmentId)),
                throwable -> {
                    log.error("[CB-FALLBACK] Medical exam service unavailable: {}", throwable.getMessage());
                    throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                        "Medical exam service unavailable. Cannot generate invoice.");
                }
            );
            exam = examResponse.getData();
        }
        
        if (exam == null) {
            throw new ApiException(ErrorCode.EXAM_NOT_FOUND, "Medical exam not found for appointment: " + appointmentId);
        }
        context.put(CONTEXT_EXAM, exam);

        // Fetch prescription using the CORRECT exam ID
        // This is OPTIONAL - prescription may not exist for consultation-only visits
        // BUT if service is down (CB open), we should NOT generate incomplete invoice
        String prescriptionExamId = examId != null && !examId.isEmpty() ? examId : exam.id();
        log.info("Fetching prescription by examId: {}", prescriptionExamId);
        ApiResponse<MedicalExamClient.PrescriptionResponse> prescriptionResponse = medicalExamCircuitBreaker.run(
            () -> FeignHelper.safeCall(() -> medicalExamClient.getPrescriptionByExam(prescriptionExamId)),
            throwable -> {
                // CB open = service unavailable. Don't generate incomplete invoice!
                log.error("[CB-FALLBACK] Prescription service unavailable: {}", throwable.getMessage());
                throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                    "Cannot verify prescription data. Please try again later to avoid incorrect invoice.");
            }
        );
        // Service responded - check if prescription exists
        MedicalExamClient.PrescriptionResponse prescription = prescriptionResponse.getData();
        if (prescription != null) {
            context.put(CONTEXT_PRESCRIPTION, prescription);
            log.info("Found prescription for exam: {}, items: {}", prescriptionExamId, 
                prescription.items() != null ? prescription.items().size() : 0);
        } else {
            // Prescription genuinely doesn't exist - OK for consultation-only invoice
            log.info("No prescription found for exam: {} - creating consultation-only invoice", prescriptionExamId);
        }


        // Fetch lab test results (optional but price-affecting)
        // If service is down (CB open), we should NOT generate incomplete invoice
        log.info("Fetching lab tests by examId: {}", exam.id());
        ApiResponse<java.util.List<MedicalExamClient.LabTestResultResponse>> labTestsResponse = medicalExamCircuitBreaker.run(
            () -> FeignHelper.safeCall(() -> medicalExamClient.getLabResultsByExam(exam.id())),
            throwable -> {
                // CB open = service unavailable. Don't generate incomplete invoice!
                log.error("[CB-FALLBACK] Lab test service unavailable: {}", throwable.getMessage());
                throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                    "Cannot verify lab test data. Please try again later to avoid incorrect invoice.");
            }
        );
        // Service responded - check if lab tests exist
        java.util.List<MedicalExamClient.LabTestResultResponse> labTests = labTestsResponse.getData();
        if (labTests != null && !labTests.isEmpty()) {
            context.put(CONTEXT_LAB_TESTS, labTests);
            log.info("Found {} lab tests for exam: {}", labTests.size(), exam.id());
        } else {
            log.info("No lab tests for exam: {}", exam.id());
        }

        // Fetch consultation fee (optional - use default on failure)
        // This is less critical - default fee is acceptable if HR service is down
        if (exam.doctor() != null) {
            try {
                ApiResponse<HrClient.EmployeeResponse> doctorResponse = hrCircuitBreaker.run(
                    () -> FeignHelper.safeCall(() -> hrClient.getEmployeeById(exam.doctor().id())),
                    throwable -> {
                        log.warn("[CB-FALLBACK] Could not fetch doctor info for consultation fee, using default: {}", 
                            throwable.getMessage());
                        return null;
                    }
                );
                if (doctorResponse != null) {
                    HrClient.EmployeeResponse doctor = doctorResponse.getData();
                    if (doctor != null && doctor.department() != null && doctor.department().consultationFee() != null) {
                        context.put(CONTEXT_CONSULTATION_FEE, doctor.department().consultationFee());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch consultation fee, using default: {}", e.getMessage());
            }
        }
    }

    @Override
    public void enrichCreate(InvoiceRequest request, Invoice entity, Map<String, Object> context) {
        MedicalExamClient.MedicalExamResponse exam = 
            (MedicalExamClient.MedicalExamResponse) context.get(CONTEXT_EXAM);
        MedicalExamClient.PrescriptionResponse prescription = 
            (MedicalExamClient.PrescriptionResponse) context.get(CONTEXT_PRESCRIPTION);
        BigDecimal consultationFee = (BigDecimal) context.getOrDefault(
            CONTEXT_CONSULTATION_FEE, new BigDecimal("200000"));

        // Populate basic info (some fields are already mapped by requestToEntity)
        entity.setInvoiceNumber(generateInvoiceNumber());
        entity.setMedicalExamId(exam.id());
        entity.setAppointmentId(request.getAppointmentId());
        entity.setPatientId(exam.patient().id());
        entity.setPatientName(exam.patient().fullName());
        entity.setInvoiceDate(Instant.now());
        entity.setDueDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        entity.setNotes(request.getNotes());

        // Add consultation fee item
        InvoiceItem consultationItem = InvoiceItem.builder()
            .type(InvoiceItem.ItemType.CONSULTATION)
            .description("Consultation Fee")
            .quantity(1)
            .unitPrice(consultationFee)
            .amount(consultationFee)
            .invoice(entity)
            .build();
        entity.addItem(consultationItem);

        // Add medicine items (only if prescription exists)
        if (prescription != null && prescription.items() != null) {
            for (var item : prescription.items()) {
                InvoiceItem medicineItem = InvoiceItem.builder()
                    .type(InvoiceItem.ItemType.MEDICINE)
                    .description(item.medicine().name())
                    .referenceId(item.id())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .amount(item.unitPrice().multiply(new BigDecimal(item.quantity())))
                    .invoice(entity)
                    .build();
                entity.addItem(medicineItem);
            }
        }

        // Add lab test items (if exists)
        @SuppressWarnings("unchecked")
        java.util.List<MedicalExamClient.LabTestResultResponse> labTests = 
            (java.util.List<MedicalExamClient.LabTestResultResponse>) context.get(CONTEXT_LAB_TESTS);
        if (labTests != null) {
            for (var labTest : labTests) {
                if (labTest.labTestPrice() != null) {
                    InvoiceItem labTestItem = InvoiceItem.builder()
                        .type(InvoiceItem.ItemType.TEST)
                        .description("Lab Test: " + labTest.labTestName())
                        .referenceId(labTest.id())
                        .quantity(1)
                        .unitPrice(labTest.labTestPrice())
                        .amount(labTest.labTestPrice())
                        .invoice(entity)
                        .build();
                    entity.addItem(labTestItem);
                }
            }
        }

        recalculateTotals(entity);
    }

    public void recalculateTotals(Invoice invoice) {
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.10"));
        BigDecimal discount = invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO;
        
        invoice.setSubtotal(subtotal);
        invoice.setTax(tax);
        invoice.setTotalAmount(subtotal.add(tax).subtract(discount));
    }

    public void validateCancel(Invoice invoice) {
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Invoice is already cancelled");
        }
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, 
                "Cannot cancel a fully paid invoice. Use refund instead.");
        }
    }

    private String generateInvoiceNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long counter = invoiceCounter.incrementAndGet() % 10000;
        return String.format("INV-%s-%04d", dateStr, counter);
    }
    
    /**
     * Create or update invoice with all items (Consultation + Medicine + Lab Tests).
     * If invoice exists for appointmentId, it will be UPDATED with fresh items.
     * If not, a new invoice will be created.
     * 
     * @param appointmentId The appointment ID
     * @param examId The exam ID for direct lookup
     * @param notes Optional notes
     * @return The created or updated invoice
     */
    @org.springframework.transaction.annotation.Transactional
    public Invoice upsertInvoice(String appointmentId, String examId, String notes) {
        log.info("[UPSERT] Creating/updating invoice for appointmentId: {}, examId: {}", appointmentId, examId);

        var medicalExamCircuitBreaker = circuitBreakerFactory.create("billingMedicalExam");
        var appointmentCircuitBreaker = circuitBreakerFactory.create("billingAppointment");
        var patientCircuitBreaker = circuitBreakerFactory.create("billingPatient");
        var hrCircuitBreaker = circuitBreakerFactory.create("billingHr");
        
        // 1. Find or create invoice
        Invoice invoice = invoiceRepository.findByAppointmentId(appointmentId).orElse(null);
        boolean isUpdate = invoice != null;
        
        if (invoice == null) {
            invoice = new Invoice();
            invoice.setInvoiceNumber(generateInvoiceNumber());
            invoice.setAppointmentId(appointmentId);
            invoice.setInvoiceDate(Instant.now());
            invoice.setDueDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
            invoice.setStatus(Invoice.InvoiceStatus.UNPAID);
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setDiscount(BigDecimal.ZERO);
            log.info("[UPSERT] Creating new invoice");
        } else {
            // Clear existing items for fresh recalculation
            invoice.getItems().clear();
            log.info("[UPSERT] Updating existing invoice: {}", invoice.getInvoiceNumber());
        }
        
        invoice.setNotes(notes);
        
        // 2. Fetch exam data (CRITICAL - fail fast if unavailable)
        MedicalExamClient.MedicalExamResponse exam = null;
        if (examId != null && !examId.isEmpty()) {
            log.info("[UPSERT] Fetching exam by examId: {}", examId);
            var examResponse = medicalExamCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> medicalExamClient.getExamById(examId)),
                throwable -> {
                    log.error("[CB-FALLBACK] Exam service unavailable: {}", throwable.getMessage());
                    throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                        "Medical exam service unavailable. Cannot generate invoice.");
                }
            );
            exam = examResponse.getData();
        }
        if (exam == null) {
            log.info("[UPSERT] Fetching exam by appointmentId: {}", appointmentId);
            var examResponse = medicalExamCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> medicalExamClient.getExamByAppointment(appointmentId)),
                throwable -> {
                    log.error("[CB-FALLBACK] Exam service unavailable: {}", throwable.getMessage());
                    throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, 
                        "Medical exam service unavailable. Cannot generate invoice.");
                }
            );
            exam = examResponse.getData();
        }
        
        if (exam == null) {
            throw new ApiException(ErrorCode.EXAM_NOT_FOUND, "Medical exam not found for invoice generation");
        }
        
        // Make exam effectively final for use in lambdas
        final MedicalExamClient.MedicalExamResponse finalExam = exam;
        
        // 3. Fetch appointment to get type and patient info fallback (optional)
        AppointmentClient.AppointmentResponse appointment = null;
        try {
            var appointmentResponse = appointmentCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> appointmentClient.getAppointmentById(appointmentId)),
                throwable -> {
                    log.warn("[CB-FALLBACK] Appointment service unavailable: {}", throwable.getMessage());
                    return null;
                }
            );
            if (appointmentResponse == null) {
                throw new IllegalStateException("Appointment service unavailable");
            }
            appointment = appointmentResponse.getData();
            log.info("[UPSERT] Fetched appointment: id={}, type={}", appointmentId, appointment != null ? appointment.type() : "null");
        } catch (Exception e) {
            log.warn("[UPSERT] Could not fetch appointment: {}", e.getMessage());
        }
        
        // 4. Set patient info - prefer exam, fallback to appointment, then fetch directly
        invoice.setMedicalExamId(finalExam.id());
        String patientId = null;
        String patientName = null;
        
        // Try from exam first
        if (finalExam.patient() != null) {
            patientId = finalExam.patient().id();
            patientName = finalExam.patient().fullName();
            log.info("[UPSERT] Got patient from exam: id={}, name={}", patientId, patientName);
        }
        
        // Fallback: try from appointment (uses safe getters that check both nested object and direct fields)
        if ((patientId == null || patientName == null) && appointment != null) {
            patientId = patientId != null ? patientId : appointment.getPatientId();
            patientName = patientName != null ? patientName : appointment.getPatientName();
            if (patientId != null || patientName != null) {
                log.info("[UPSERT] Got patient from appointment: id={}, name={}", patientId, patientName);
            }
        }
        
        // Final fallback: fetch patient directly from patient-service
        if (patientName == null && patientId != null) {
            try {
                final String finalPatientId = patientId;
                var patientResponse = patientCircuitBreaker.run(
                    () -> FeignHelper.safeCall(() -> patientClient.getPatientById(finalPatientId)),
                    throwable -> {
                        log.warn("[CB-FALLBACK] Patient service unavailable: {}", throwable.getMessage());
                        return null;
                    }
                );
                if (patientResponse == null) {
                    throw new IllegalStateException("Patient service unavailable");
                }
                var patient = patientResponse.getData();
                if (patient != null) {
                    patientName = patient.fullName();
                    log.info("[UPSERT] Got patient from patient-service: id={}, name={}", patientId, patientName);
                }
            } catch (Exception e) {
                log.warn("[UPSERT] Could not fetch patient: {}", e.getMessage());
            }
        }
        
        // Set on invoice
        invoice.setPatientId(patientId);
        invoice.setPatientName(patientName);
        
        // 5. Get consultation fee (optional - use default on failure)
        BigDecimal consultationFee = new BigDecimal("200000"); // default
        if (finalExam.doctor() != null) {
            try {
                var doctorResponse = hrCircuitBreaker.run(
                    () -> FeignHelper.safeCall(() -> hrClient.getEmployeeById(finalExam.doctor().id())),
                    throwable -> {
                        log.warn("[CB-FALLBACK] HR service unavailable for consultation fee: {}", throwable.getMessage());
                        return null;
                    }
                );
                if (doctorResponse == null) {
                    throw new IllegalStateException("HR service unavailable");
                }
                var doctor = doctorResponse.getData();
                if (doctor.department() != null && doctor.department().consultationFee() != null) {
                    consultationFee = doctor.department().consultationFee();
                }
            } catch (Exception e) {
                log.warn("[UPSERT] Could not fetch consultation fee: {}", e.getMessage());
            }
        }
        
        // 6. Add Consultation item with dynamic description based on appointment type
        String consultationDescription = "Consultation Fee"; // default
        if (appointment != null) {
            consultationDescription = appointment.getTypeLabel() + " Fee";
        }
        
        InvoiceItem consultationItem = InvoiceItem.builder()
            .type(InvoiceItem.ItemType.CONSULTATION)
            .description(consultationDescription)
            .quantity(1)
            .unitPrice(consultationFee)
            .amount(consultationFee)
            .invoice(invoice)
            .build();
        invoice.addItem(consultationItem);
        
        // 6. Fetch and add prescription items (optional)
        try {
            var prescriptionResponse = medicalExamCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> medicalExamClient.getPrescriptionByExam(finalExam.id())),
                throwable -> {
                    log.warn("[CB-FALLBACK] Could not fetch prescription: {}", throwable.getMessage());
                    return null;
                }
            );
            if (prescriptionResponse == null) {
                throw new IllegalStateException("Exam service unavailable");
            }
            var prescription = prescriptionResponse.getData();
            if (prescription != null && prescription.items() != null) {
                for (var item : prescription.items()) {
                    InvoiceItem medicineItem = InvoiceItem.builder()
                        .type(InvoiceItem.ItemType.MEDICINE)
                        .description(item.medicine().name())
                        .referenceId(item.id())
                        .quantity(item.quantity())
                        .unitPrice(item.unitPrice())
                        .amount(item.unitPrice().multiply(new BigDecimal(item.quantity())))
                        .invoice(invoice)
                        .build();
                    invoice.addItem(medicineItem);
                }
                log.info("[UPSERT] Added {} medicine items", prescription.items().size());
            }
        } catch (Exception e) {
            log.info("[UPSERT] No prescription found: {}", e.getMessage());
        }
        
        // 7. Fetch and add lab test items (optional)
        try {
            var labTestsResponse = medicalExamCircuitBreaker.run(
                () -> FeignHelper.safeCall(() -> medicalExamClient.getLabResultsByExam(finalExam.id())),
                throwable -> {
                    log.warn("[CB-FALLBACK] Could not fetch lab tests: {}", throwable.getMessage());
                    return null;
                }
            );
            if (labTestsResponse == null) {
                throw new IllegalStateException("Exam service unavailable");
            }
            var labTests = labTestsResponse.getData();
            if (labTests != null && !labTests.isEmpty()) {
                for (var labTest : labTests) {
                    if (labTest.labTestPrice() != null) {
                        InvoiceItem labTestItem = InvoiceItem.builder()
                            .type(InvoiceItem.ItemType.TEST)
                            .description("Lab Test: " + labTest.labTestName())
                            .referenceId(labTest.id())
                            .quantity(1)
                            .unitPrice(labTest.labTestPrice())
                            .amount(labTest.labTestPrice())
                            .invoice(invoice)
                            .build();
                        invoice.addItem(labTestItem);
                    }
                }
                log.info("[UPSERT] Added {} lab test items", labTests.size());
            }
        } catch (Exception e) {
            log.info("[UPSERT] No lab tests found: {}", e.getMessage());
        }
        
        // 8. Recalculate totals
        recalculateTotals(invoice);
        
        // 9. Save
        Invoice saved = invoiceRepository.save(invoice);
        log.info("[UPSERT] Invoice {} with {} items, total: {}", 
            isUpdate ? "UPDATED" : "CREATED", 
            saved.getItems().size(), 
            saved.getTotalAmount());
        
        return saved;
    }
}
