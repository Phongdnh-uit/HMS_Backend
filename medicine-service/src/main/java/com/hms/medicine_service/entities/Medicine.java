package com.hms.medicine_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Table
@Entity
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String activeIngredient;

    @Column(nullable = false)
    private String unit;

    private String description;

    @Column(nullable = false)
    private Long quantity;

    private String concentration;

    private String packaging;

    @Column(nullable = false)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private Instant expiresAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;
}