package com.hms.report_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for receiving diagnosis stats from medical-exam-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisStatsDTO {
    
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
