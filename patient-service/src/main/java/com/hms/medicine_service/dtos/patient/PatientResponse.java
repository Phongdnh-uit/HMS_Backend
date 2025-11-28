package com.hms.patient_service.dtos.patient;

import com.hms.patient_service.constants.Gender;

import java.time.Instant;

public record PatientResponse(
        Long id,
        Long accountId,
        String fullName,
        String email,
        Instant dateOfBirth,
        Gender gender,
        String phoneNumber,
        String address,
        String identificationNumber,
        String healthInsuranceNumber,
        String relativeFullName,
        String relativePhoneNumber,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {
}
