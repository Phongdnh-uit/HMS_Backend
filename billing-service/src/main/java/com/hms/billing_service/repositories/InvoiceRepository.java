package com.hms.billing_service.repositories;

import com.hms.billing_service.entities.Invoice;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends SimpleRepository<Invoice, String> {

    Optional<Invoice> findByMedicalExamId(String medicalExamId);

    Optional<Invoice> findByAppointmentId(String appointmentId);

    List<Invoice> findByPatientId(String patientId);

    List<Invoice> findByPatientIdAndStatus(String patientId, Invoice.InvoiceStatus status);

    boolean existsByMedicalExamId(String medicalExamId);

    boolean existsByAppointmentId(String appointmentId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    /**
     * Count invoices by status within date range (for stats endpoint).
     */
    @Query("SELECT i.status, COUNT(i) FROM Invoice i WHERE i.createdAt >= :startDate AND i.createdAt <= :endDate GROUP BY i.status")
    List<Object[]> countByStatusInDateRange(
        @Param("startDate") Instant startDate, 
        @Param("endDate") Instant endDate
    );
    
    /**
     * Sum total and paid amounts within date range.
     */
    @Query("SELECT COALESCE(SUM(i.totalAmount), 0), COALESCE(SUM(i.paidAmount), 0), COUNT(i) FROM Invoice i WHERE i.createdAt >= :startDate AND i.createdAt <= :endDate")
    Object[] sumAmountsInDateRange(
        @Param("startDate") Instant startDate, 
        @Param("endDate") Instant endDate
    );
}
