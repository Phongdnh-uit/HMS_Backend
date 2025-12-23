package com.hms.billing_service.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Reference to the source record:
     * - For MEDICINE: prescription_item.id
     * - For CONSULTATION: null (no source)
     * - For TEST: test_order.id (future)
     */
    private String referenceId;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum ItemType {
        CONSULTATION,
        MEDICINE,
        TEST,
        PROCEDURE,
        OTHER
    }

    /**
     * Calculate amount from quantity and unit price
     */
    @PrePersist
    @PreUpdate
    public void calculateAmount() {
        this.amount = unitPrice.multiply(new BigDecimal(quantity));
    }
}
