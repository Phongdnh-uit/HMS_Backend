package com.hms.billing_service.repositories;

import com.hms.billing_service.entities.Payment;
import com.hms.common.repositories.SimpleRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends SimpleRepository<Payment, String> {

    /**
     * Find payment by transaction reference.
     */
    Optional<Payment> findByTxnRef(String txnRef);

    /**
     * Find all payments for a specific invoice.
     */
    List<Payment> findByInvoiceId(String invoiceId);

    /**
     * Calculate total amount paid for an invoice (completed payments only).
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId AND p.status = 'COMPLETED'")
    BigDecimal sumCompletedAmountByInvoiceId(String invoiceId);

    /**
     * Find payments by VNPay transaction number.
     */
    Optional<Payment> findByVnpTransactionNo(String vnpTransactionNo);

    /**
     * Check if transaction reference already exists.
     */
    boolean existsByTxnRef(String txnRef);

    /**
     * Find pending payments for an invoice.
     */
    List<Payment> findByInvoiceIdAndStatus(String invoiceId, Payment.PaymentStatus status);
    
    /**
     * Sum completed payments by gateway within date range for stats.
     */
    @Query("SELECT p.gateway, COALESCE(SUM(p.amount), 0), COUNT(p) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate " +
           "GROUP BY p.gateway")
    List<Object[]> sumByGatewayInDateRange(
        @Param("startDate") Instant startDate, 
        @Param("endDate") Instant endDate
    );
}
