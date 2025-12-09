package com.hms.appointment_service.hooks;

import com.hms.appointment_service.dtos.appointment.AppointmentRequest;
import com.hms.appointment_service.dtos.appointment.AppointmentResponse;
import com.hms.appointment_service.entities.Appointment;
import com.hms.common.dtos.PageResponse;
import com.hms.common.hooks.GenericHook;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class AppointmentHook implements GenericHook<Appointment, String, AppointmentRequest, AppointmentResponse> {

    @Override
    public void enrichFindAll(PageResponse<AppointmentResponse> response) {

    }

    @Override
    public void enrichFindById(AppointmentResponse response) {

    }

    @Override
    public void validateCreate(AppointmentRequest input, Map<String, Object> context) {

    }

    @Override
    public void enrichCreate(AppointmentRequest input, Appointment entity, Map<String, Object> context) {

    }

    @Override
    public void afterCreate(Appointment entity, AppointmentResponse response, Map<String, Object> context) {

    }

    @Override
    public void validateUpdate(String s, AppointmentRequest input, Appointment existingEntity, Map<String, Object> context) {

    }

    @Override
    public void enrichUpdate(AppointmentRequest input, Appointment entity, Map<String, Object> context) {

    }

    @Override
    public void afterUpdate(Appointment entity, AppointmentResponse response, Map<String, Object> context) {

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
}
