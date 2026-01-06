package com.hms.report_service.services;

import com.hms.report_service.clients.MedicalExamClient;
import com.hms.report_service.clients.PatientClient;
import com.hms.report_service.dtos.DiagnosisStatsDTO;
import com.hms.report_service.dtos.PatientReportResponse;
import com.hms.report_service.dtos.PatientStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating patient activity reports.
 * Uses Redis caching for performance optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientReportService {

    private final PatientClient patientClient;
    private final MedicalExamClient medicalExamClient;

    /**
     * Generate patient activity report.
     * Calls patient-service /patients/stats endpoint for pre-aggregated data.
     * Calls medical-exam-service /exams/stats for top diagnoses.
     * Results are cached in Redis for 30 minutes.
     */
    @Cacheable(value = "patient-reports")
    public PatientReportResponse generatePatientReport() {
        log.info("Generating patient report (fetching from patient-service and medical-exam-service)");
        
        var statsResponse = patientClient.getPatientStats();
        
        if (statsResponse == null || statsResponse.getData() == null) {
            log.warn("No stats data returned from patient-service");
            return buildEmptyReport();
        }
        
        PatientStatsDTO stats = statsResponse.getData();
        
        // Map registration trend
        List<PatientReportResponse.RegistrationTrend> registrationTrend = new ArrayList<>();
        if (stats.getRegistrationTrend() != null) {
            registrationTrend = stats.getRegistrationTrend().stream()
                .map(t -> PatientReportResponse.RegistrationTrend.builder()
                    .date(t.getDate())
                    .newPatients(t.getNewPatients())
                    .visits(0) // Visits would need separate query
                    .build())
                .collect(Collectors.toList());
        }
        
        // Fetch top diagnoses from medical-exam-service
        List<PatientReportResponse.TopDiagnosis> topDiagnoses = fetchTopDiagnoses();

        return PatientReportResponse.builder()
            .totalPatients(stats.getTotalPatients())
            .newPatientsThisMonth(stats.getNewPatientsThisMonth())
            .newPatientsThisYear(stats.getNewPatientsThisYear())
            .patientsByGender(stats.getPatientsByGender() != null ? stats.getPatientsByGender() : new HashMap<>())
            .patientsByBloodType(stats.getPatientsByBloodType() != null ? stats.getPatientsByBloodType() : new HashMap<>())
            .registrationTrend(registrationTrend)
            .topDiagnoses(topDiagnoses)
            .averageAge(stats.getAverageAge())
            .generatedAt(Instant.now())
            .build();
    }
    
    /**
     * Fetch top diagnoses from medical-exam-service.
     */
    private List<PatientReportResponse.TopDiagnosis> fetchTopDiagnoses() {
        try {
            var diagnosisResponse = medicalExamClient.getDiagnosisStats();
            if (diagnosisResponse != null && diagnosisResponse.getData() != null) {
                DiagnosisStatsDTO diagnosisStats = diagnosisResponse.getData();
                if (diagnosisStats.getTopDiagnoses() != null) {
                    return diagnosisStats.getTopDiagnoses().stream()
                        .map(d -> PatientReportResponse.TopDiagnosis.builder()
                            .diagnosis(d.getDiagnosis())
                            .icdCode("") // ICD code not available in current data model
                            .count(d.getCount())
                            .percentage(d.getPercentage())
                            .build())
                        .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch diagnoses from medical-exam-service: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Clear the patient report cache.
     */
    @CacheEvict(value = "patient-reports", allEntries = true)
    public void clearCache() {
        log.info("Patient report cache cleared");
    }
    
    private PatientReportResponse buildEmptyReport() {
        return PatientReportResponse.builder()
            .totalPatients(0)
            .newPatientsThisMonth(0)
            .newPatientsThisYear(0)
            .patientsByGender(new HashMap<>())
            .patientsByBloodType(new HashMap<>())
            .registrationTrend(new ArrayList<>())
            .topDiagnoses(new ArrayList<>())
            .averageAge(0)
            .generatedAt(Instant.now())
            .build();
    }
}
