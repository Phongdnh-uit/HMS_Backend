package com.hms.billing_service.hooks;

import com.hms.billing_service.dtos.InvoiceRequest;
import com.hms.billing_service.dtos.InvoiceResponse;
import com.hms.billing_service.entities.Invoice;
import com.hms.billing_service.entities.InvoiceItem;
import com.hms.billing_service.repositories.InvoiceRepository;
import com.hms.billing_service.clients.MedicalExamClient;
import com.hms.billing_service.clients.HrClient;
import com.hms.common.hooks.GenericHook;
import com.hms.common.exceptions.errors.ApiException;
import com.hms.common.exceptions.errors.ErrorCode;
import com.hms.common.helpers.FeignHelper;
import com.hms.common.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final HrClient hrClient;

    // Simple counter for invoice number generation
    private static final AtomicLong invoiceCounter = new AtomicLong(System.currentTimeMillis() % 10000);

    public static final String CONTEXT_EXAM = "exam";
    public static final String CONTEXT_PRESCRIPTION = "prescription";
    public static final String CONTEXT_CONSULTATION_FEE = "consultationFee";

    @Override
    public void validateCreate(InvoiceRequest request, Map<String, Object> context) {
        String appointmentId = request.getAppointmentId();

        // Check if invoice already exists
        if (invoiceRepository.existsByAppointmentId(appointmentId)) {
            throw new ApiException(ErrorCode.INVOICE_EXISTS, 
                "Invoice already exists for appointment: " + appointmentId);
        }

        // Fetch medical exam
        ApiResponse<MedicalExamClient.MedicalExamResponse> examResponse = FeignHelper.safeCall(
            () -> medicalExamClient.getExamByAppointment(appointmentId)
        );
        MedicalExamClient.MedicalExamResponse exam = examResponse.getData();
        if (exam == null) {
            throw new ApiException(ErrorCode.EXAM_NOT_FOUND, "Medical exam not found for appointment: " + appointmentId);
        }
        context.put(CONTEXT_EXAM, exam);

        // Fetch prescription (optional - may not exist for consultation-only invoices)
        try {
            ApiResponse<MedicalExamClient.PrescriptionResponse> prescriptionResponse = FeignHelper.safeCall(
                () -> medicalExamClient.getPrescriptionByExam(exam.id())
            );
            MedicalExamClient.PrescriptionResponse prescription = prescriptionResponse.getData();
            if (prescription != null) {
                context.put(CONTEXT_PRESCRIPTION, prescription);
                log.info("Found prescription for exam: {}, items: {}", exam.id(), 
                    prescription.items() != null ? prescription.items().size() : 0);
            } else {
                log.info("No prescription found for exam: {} - creating consultation-only invoice", exam.id());
            }
        } catch (Exception e) {
            // Prescription not found is OK - consultation-only invoice
            log.info("No prescription for exam: {} - creating consultation-only invoice", exam.id());
        }

        // Fetch consultation fee
        if (exam.doctor() != null) {
            try {
                ApiResponse<HrClient.EmployeeResponse> doctorResponse = FeignHelper.safeCall(
                    () -> hrClient.getEmployeeById(exam.doctor().id())
                );
                HrClient.EmployeeResponse doctor = doctorResponse.getData();
                if (doctor.department() != null && doctor.department().consultationFee() != null) {
                    context.put(CONTEXT_CONSULTATION_FEE, doctor.department().consultationFee());
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
}
