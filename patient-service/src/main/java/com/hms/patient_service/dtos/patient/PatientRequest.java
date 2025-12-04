package com.hms.common.dtos.patient;

import com.hms.patient_service.constants.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class PatientRequest {

    private String accountId;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

//    @NotBlank(message = "Password is required", groups = {Action.Create.class})
//    private String password = App.PASSWORD_DEFAULT;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Pattern(
            regexp = "^(0|\\+84)(\\d{9})$",
            message = "Invalid phone number format"
    )
    private String phoneNumber;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Pattern(regexp = "^\\d{12}$", message = "Identification number must be 12 digits")
    private String identificationNumber;

    @Size(max = 20, message = "Health insurance number cannot exceed 20 characters")
    private String healthInsuranceNumber;

    @Size(max = 100, message = "Relative name cannot exceed 100 characters")
    private String relativeFullName;

    @Pattern(
            regexp = "^(0|\\+84)(\\d{9})$",
            message = "Invalid relative phone number format"
    )
    private String relativePhoneNumber;

    @Size(max = 100, message = "Relative relationship cannot exceed 100 characters")
    private String relativeRelationship;

    @Size(max = 100, message = "Blood type cannot exceed 100 characters")
    private String bloodType;

    @Size(max = 100, message = "Allergies cannot exceed 100 characters")
    private String allergies;
}
