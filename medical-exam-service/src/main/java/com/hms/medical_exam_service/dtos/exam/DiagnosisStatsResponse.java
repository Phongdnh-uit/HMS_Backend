package com.hms.medical_exam_service.dtos.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for diagnosis statistics.
 * Used by report-service for top diagnoses chart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisStatsResponse {
    
    private List<TopDiagnosis> topDiagnoses;
    private long totalExamsWithDiagnosis;
    private Instant generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDiagnosis {
        private String diagnosis;
        private int count;
        private double percentage;
    }
}
