package com.hms.patient_service.helpers;

import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.repositories.PatientRepository;
import org.springframework.data.jpa.domain.Specification;

public class PatientHelper {
    /**
     *
     * @param patient
     * @return SET DEFAULT DATA FOR EMPTY FIELDS:
     * - EMAIL
     * - PHONE NUMBER
     * - ADDRESS
     * - IDENTIFICATION NUMBER
     * - HEALTH INSURANCE NUMBER
     * - RELATIVE FULL NAME
     * - RELATIVE PHONE NUMBER
     */
    public static Patient enrichDefaultData(Patient patient) {
        // EMAIL
        if (patient.getEmail() == null || patient.getEmail().isEmpty()) {
            patient.setEmail("N/A");
        }

        // PHONE NUMBER
        if (patient.getPhoneNumber() == null || patient.getPhoneNumber().isEmpty()) {
            patient.setPhoneNumber("N/A");
        }

        // ADDRESS
        if (patient.getAddress() == null || patient.getAddress().isEmpty()) {
            patient.setAddress("Việt Nam");
        }

        // IDENTIFICATION
        if (patient.getIdentificationNumber() == null || patient.getIdentificationNumber().isEmpty()) {
            patient.setIdentificationNumber("N/A");
        }

        // HEALTH INSURANCE
        if (patient.getHealthInsuranceNumber() == null || patient.getHealthInsuranceNumber().isEmpty()) {
            patient.setHealthInsuranceNumber("N/A");
        }

        //RELATIVE FULL NAME
        if (patient.getRelativeFullName() == null || patient.getRelativeFullName().isEmpty()) {
            patient.setRelativeFullName("N/A");
        }

        // RELATIVE PHONE NUMBER
        if (patient.getRelativePhoneNumber() == null || patient.getRelativePhoneNumber().isEmpty()) {
            patient.setRelativePhoneNumber("N/A");
        }

        return patient;

    }


    //SPECIFICATION
    //EMAIL LIKE
    public static Specification<Patient> emailLike(String email) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("email"), email);
    }

    //IDENTIFICATION NUMBER LIKE
    public static Specification<Patient> identificationNumberLike(String identificationNumber) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("identificationNumber"), identificationNumber);
    }

    //HEALTH INSURANCE LIKE
    public static Specification<Patient> healthInsuranceNumberLike(String healthInsuranceNumber) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("healthInsuranceNumber"), healthInsuranceNumber);
    }

    public static boolean isAccountExists(PatientRequest patientRequest, PatientRepository patientRepository) {
        //CHECK IF FIELDS ALREADY EXIST IN ANOTHER ACCOUNT
        // EMAIL, IDENTIFICATION NUMBER, HEALTH INSURANCE NUMBER

        Specification<Patient> existsPatientSpec = PatientHelper.emailLike(patientRequest.email())
                .or(PatientHelper.healthInsuranceNumberLike(patientRequest.healthInsuranceNumber()))
                .or(PatientHelper.identificationNumberLike(patientRequest.identificationNumber()));

        return patientRepository.exists(existsPatientSpec);
    }
}
