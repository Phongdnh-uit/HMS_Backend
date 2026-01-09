package com.hms.report_service.services;

import com.hms.report_service.clients.MedicalExamClient;
import com.hms.report_service.clients.PatientClient;
import com.hms.report_service.dtos.DiagnosisStatsDTO;
import com.hms.report_service.dtos.PatientReportResponse;
import com.hms.report_service.dtos.PatientStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
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
 * 
 * Caching strategy:
 * - Fresh data cached for 30 minutes (TTL)
 * - Degraded responses (dataStatus != null) are NOT cached
 * - When TTL expires + downstream unavailable -> returns UNAVAILABLE (honest failure)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientReportService {

    private final PatientClient patientClient;
    private final MedicalExamClient medicalExamClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /**
     * Generate patient activity report.
     * Calls patient-service /patients/stats endpoint for pre-aggregated data.
     * Calls medical-exam-service /exams/stats for top diagnoses.
     * Results are cached in Redis for 30 minutes.
     * Note: Degraded responses (dataStatus != null) are NOT cached.
     */
    @Cacheable(value = "patient-reports", unless = "#result.dataStatus != null")
    public PatientReportResponse generatePatientReport() {
        log.info("Generating patient report (fetching from patient-service and medical-exam-service)");
        
        var patientCircuitBreaker = circuitBreakerFactory.create("reportPatient");
        var statsResponse = patientCircuitBreaker.run(
            patientClient::getPatientStats,
            throwable -> {
                log.warn("[CB-FALLBACK] Patient service unavailable: {}", throwable.getMessage());
                return null;
            }
        );
        
        if (statsResponse == null || statsResponse.getData() == null) {
            log.warn("No stats data returned from patient-service");
            return buildUnavailableReport("Patient service unavailable");
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
            var examCircuitBreaker = circuitBreakerFactory.create("reportMedicalExam");
            var diagnosisResponse = examCircuitBreaker.run(
                medicalExamClient::getDiagnosisStats,
                throwable -> {
                    log.warn("[CB-FALLBACK] Medical exam service unavailable for diagnoses: {}", 
                        throwable.getMessage());
                    return null;
                }
            );
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
    
    /**
     * Build report indicating data is unavailable.
     * This response is NOT cached (via unless condition).
     */
    private PatientReportResponse buildUnavailableReport(String reason) {
        log.warn("Building unavailable patient report: {}", reason);
        return PatientReportResponse.builder()
            .totalPatients(null)  // null indicates unavailable
            .newPatientsThisMonth(null)
            .newPatientsThisYear(null)
            .patientsByGender(null)
            .patientsByBloodType(null)
            .registrationTrend(new ArrayList<>())
            .topDiagnoses(new ArrayList<>())
            .averageAge(null)
            .generatedAt(Instant.now())
            .dataStatus("UNAVAILABLE: " + reason)
            .build();
    }
}
