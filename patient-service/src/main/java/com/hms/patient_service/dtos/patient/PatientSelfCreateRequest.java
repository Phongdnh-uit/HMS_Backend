package com.hms.patient_service.dtos.patient;

import com.hms.patient_service.constants.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for patient self-registration (creating initial profile).
 * Contains required fields for new patient record.
 */
@Data
public class PatientSelfCreateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(0|\\+84)(\\d{9})$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 20, message = "ID number cannot exceed 20 characters")
    private String identificationNumber;
}
