package com.hms.patient_service.dtos.patient;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for patient self-service profile updates.
 * Only allows updating non-sensitive fields.
 * Sensitive fields (name, email, DOB, ID numbers, blood type) require staff assistance.
 */
@Data
public class PatientSelfUpdateRequest {

    @Pattern(
            regexp = "^(0|\\+84)(\\d{9})$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 500, message = "Allergies cannot exceed 500 characters")
    private String allergies;

    @Size(max = 100, message = "Relative name cannot exceed 100 characters")
    private String relativeFullName;

    @Pattern(
            regexp = "^(0|\\+84)(\\d{9})$",
            message = "Invalid relative phone number format"
    )
    private String relativePhoneNumber;

    @Size(max = 100, message = "Relative relationship cannot exceed 100 characters")
    private String relativeRelationship;
}
