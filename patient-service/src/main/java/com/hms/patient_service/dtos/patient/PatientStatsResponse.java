package com.hms.patient_service.dtos.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Patient statistics response DTO.
 * Pre-aggregated stats for report-service consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientStatsResponse {
    
    private int totalPatients;
    private int newPatientsThisMonth;
    private int newPatientsThisYear;
    
    private Map<String, Integer> patientsByGender;
    private Map<String, Integer> patientsByBloodType;
    
    private List<RegistrationTrend> registrationTrend;  // Daily registration counts
    
    private double averageAge;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationTrend {
        private LocalDate date;
        private int newPatients;
    }
}
