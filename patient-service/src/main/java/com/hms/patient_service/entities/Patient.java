package com.hms.patient_service.entities;

import com.hms.patient_service.constants.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@Table
@Entity
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String accountId;

    @Column(nullable = false)
    private String fullName;

    private String email;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String phoneNumber;

    private String address;

    private String identificationNumber;

    private String healthInsuranceNumber;

    private String relativeFullName;

    private String relativePhoneNumber;

    private String relativeRelationship;

    private String bloodType;
    
    private String allergies;

    private String profileImageUrl;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

}
