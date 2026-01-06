package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for receiving patient stats from patient-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientStatsDTO {
    
    private int totalPatients;
    private int newPatientsThisMonth;
    private int newPatientsThisYear;
    
    private Map<String, Integer> patientsByGender;
    private Map<String, Integer> patientsByBloodType;
    
    private List<RegistrationTrend> registrationTrend;
    
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
