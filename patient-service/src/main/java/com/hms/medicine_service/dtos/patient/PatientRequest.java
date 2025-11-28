package com.hms.patient_service.dtos.patient;

import com.hms.patient_service.constants.Gender;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record PatientRequest(

        @NotNull(message = "Account ID is required")
        Long accountId,

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        Instant dateOfBirth,

        @NotNull(message = "Gender is required")
        Gender gender,

        @Pattern(
                regexp = "^(0|\\+84)(\\d{9})$",
                message = "Invalid phone number format"
        )
        String phoneNumber,

        @Size(max = 255, message = "Address cannot exceed 255 characters")
        String address,

        // CCCD Việt Nam: 12 số
        @Pattern(regexp = "^\\d{12}$", message = "Identification number must be 12 digits")
        String identificationNumber,

        @Size(max = 20, message = "Health insurance number cannot exceed 20 characters")
        String healthInsuranceNumber,

        @Size(max = 100, message = "Relative name cannot exceed 100 characters")
        String relativeFullName,

        @Pattern(
                regexp = "^(0|\\+84)(\\d{9})$",
                message = "Invalid relative phone number format"
        )
        String relativePhoneNumber

) {
}
