package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Patient activity report response DTO.
 * Contains patient statistics and demographics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientReportResponse implements Serializable {
    
    private int totalPatients;
    private int newPatientsThisMonth;
    private int newPatientsThisYear;
    
    private Map<String, Integer> patientsByGender;
    private Map<String, Integer> patientsByBloodType;
    
    private List<RegistrationTrend> registrationTrend;  // Daily patient registration trend
    private List<TopDiagnosis> topDiagnoses;  // Top diagnoses
    
    private double averageAge;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegistrationTrend implements Serializable {
        private LocalDate date;
        private int newPatients;
        private int visits;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDiagnosis implements Serializable {
        private String diagnosis;
        private String icdCode;
        private int count;
        private double percentage;
    }
}
