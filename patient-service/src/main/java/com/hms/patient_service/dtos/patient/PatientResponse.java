package com.hms.patient_service.dtos.patient;

import com.hms.patient_service.constants.Gender;

import java.time.Instant;
import java.time.LocalDate;

public record PatientResponse(
        String id,
        String accountId,
        String fullName,
        String email,
        LocalDate dateOfBirth,
        Gender gender,
        String phoneNumber,
        String address,
        String identificationNumber,
        String healthInsuranceNumber,
        String relativeFullName,
        String relativePhoneNumber,
        String relativeRelationship,
        String bloodType,
        String allergies,
        String profileImageUrl,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
