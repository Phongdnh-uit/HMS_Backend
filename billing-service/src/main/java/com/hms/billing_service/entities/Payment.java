package com.hms.billing_service.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_txn_ref", columnList = "txnRef", unique = true),
    @Index(name = "idx_payment_invoice_id", columnList = "invoice_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Transaction reference (unique per payment attempt)
    @Column(nullable = false, unique = true)
    private String txnRef;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentGateway gateway = PaymentGateway.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // VNPay specific fields
    private String vnpTransactionNo;  // VNPay's transaction number
    private String vnpBankCode;       // Bank code used
    private String vnpCardType;       // ATM, QRCODE, etc.
    private String vnpBankTranNo;     // Bank's transaction number
    private String vnpResponseCode;   // VNPay response code
    private String vnpSecureHash;     // For verification

    @Column(length = 500)
    private String orderInfo;         // Order description

    @Column(length = 500)
    private String returnUrl;         // Frontend callback URL

    @Column(length = 500)
    private String notes;

    private Instant paymentDate;      // When payment completed
    private Instant expireAt;         // Payment link expiration

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum PaymentGateway {
        CASH,
        VNPAY,
        MOMO,
        BANK_TRANSFER
    }

    public enum PaymentStatus {
        PENDING,      // Payment created, awaiting user action
        PROCESSING,   // User redirected to gateway
        COMPLETED,    // Payment successful
        FAILED,       // Payment failed
        CANCELLED,    // Payment cancelled by user
        EXPIRED,      // Payment link expired
        REFUNDED      // Payment refunded
    }
}
