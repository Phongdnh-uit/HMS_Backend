package com.hms.billing_service.clients;

import com.hms.common.dtos.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;

/**
 * Feign client to fetch appointment data from appointment-service.
 * Used to get appointment type for dynamic consultation description.
 */
@FeignClient(name = "appointment-service", path = "/appointments")
public interface AppointmentClient {

    @GetMapping("/{id}")
    ApiResponse<AppointmentResponse> getAppointmentById(@PathVariable String id);

    /**
     * Appointment response DTO.
     * Includes both nested patient object and direct ID fields for maximum compatibility.
     */
    record AppointmentResponse(
        String id,
        PatientInfo patient,
        String patientId,      // Direct field - appointment-service includes this
        String patientName,    // Direct field - appointment-service includes this  
        String type,   // CONSULTATION, FOLLOW_UP, EMERGENCY, etc.
        String status,
        Instant appointmentTime
    ) {
        public record PatientInfo(String id, String fullName) {}
        
        /** Safe getter for patientId - tries nested object first, then direct field */
        public String getPatientId() {
            if (patient != null && patient.id() != null) {
                return patient.id();
            }
            return patientId;
        }
        
        /** Safe getter for patientName - tries nested object first, then direct field */
        public String getPatientName() {
            if (patient != null && patient.fullName() != null) {
                return patient.fullName();
            }
            return patientName;
        }
        
        /** Safe getter for type with default */
        public String getTypeLabel() {
            if (type == null) return "Consultation";
            return switch (type.toUpperCase()) {
                case "FOLLOW_UP" -> "Follow Up";
                case "EMERGENCY" -> "Emergency";
                case "ROUTINE_CHECK" -> "Routine Check";
                case "SPECIALIST" -> "Specialist";
                default -> "Consultation";
            };
        }
    }
}
