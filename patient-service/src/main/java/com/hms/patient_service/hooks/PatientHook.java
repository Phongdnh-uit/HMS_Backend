package com.hms.patient_service.hooks;

import com.hms.common.clients.AccountClient;
import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import com.hms.patient_service.dtos.patient.PatientRequest;
import com.hms.patient_service.dtos.patient.PatientResponse;
import com.hms.patient_service.entities.Patient;
import com.hms.patient_service.helpers.PatientHelper;
import com.hms.patient_service.repositories.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class PatientHook implements GenericHook<Patient, String, PatientRequest, PatientResponse> {
    private final PatientRepository patientRepository;
    private final AccountClient authClient;

    @Override
    public void enrichFindAll(PageResponse<PatientResponse> response) {

    }

    @Override
    public void enrichFindById(PatientResponse response) {

    }

    @Override
    public void validateCreate(PatientRequest input, Map<String, Object> context) {

        //CHECK IF FIELDS ALREADY EXIST IN ANOTHER ACCOUNT
        // EMAIL, IDENTIFICATION NUMBER, HEALTH INSURANCE NUMBER
        if (PatientHelper.isAccountExists(input, patientRepository))
            throw new RuntimeException("Patient already exists");

//        //CREATE ACCOUNT BEFORE CREATE PATIENT RECORD
//        AccountResponse newAccount = Objects.requireNonNull(authClient.create(AccountRequest.builder()
//                .email(input.getEmail())
//                .password(input.getPassword())
//                .role(RoleEnum.PATIENT)
//                .build()).getBody()).getData();

//        input.setAccountId(newAccount.getId());
    }

    @Override
    public void enrichCreate(PatientRequest input, Patient entity, Map<String, Object> context) {
        PatientHelper.enrichDefaultData(entity);
    }

    @Override
    public void afterCreate(Patient entity, PatientResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String id, PatientRequest input, Patient existingEntity, Map<String, Object> context) {
        //CHECK IF FIELDS ALREADY EXIST IN ANOTHER ACCOUNT
        // EMAIL, IDENTIFICATION NUMBER, HEALTH INSURANCE NUMBER
        if (PatientHelper.isAccountExists(input, patientRepository, id))
            throw new RuntimeException("Patient already exists");

        //IGNORE SOME FIELD THAT DON ALLOW TO UPDATE
        this.ignoreFieldBeforeUpdate(input);

    }

    @Override
    public void enrichUpdate(PatientRequest input, Patient entity, Map<String, Object> context) {
        PatientHelper.enrichDefaultData(entity);
    }

    @Override
    public void afterUpdate(Patient entity, PatientResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateDelete(String s) {

    }

    @Override
    public void afterDelete(String s) {

    }

    @Override
    public void validateBulkDelete(Iterable<String> strings) {

    }

    @Override
    public void afterBulkDelete(Iterable<String> strings) {

    }

    void ignoreFieldBeforeUpdate(PatientRequest request) {
        //EMAIL
        request.setEmail(null);
    }
}
