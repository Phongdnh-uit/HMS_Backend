package com.hms.billing_service.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private String medicalExamId;

    @Column(nullable = false)
    private String appointmentId;

    // Snapshot fields - stored at invoice creation time
    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private Instant invoiceDate;

    private Instant dueDate;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(length = 500)
    private String cancelReason;

    private Instant cancelledAt;

    private String cancelledBy;

    @Column(length = 1000)
    private String notes;

    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String updatedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    public enum InvoiceStatus {
        UNPAID,
        PARTIALLY_PAID,
        PAID,
        OVERDUE,
        CANCELLED
    }

    // Helper method to manage bidirectional relationship
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }
}
