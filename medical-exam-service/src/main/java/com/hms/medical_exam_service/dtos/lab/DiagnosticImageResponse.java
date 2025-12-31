package com.hms.medical_exam_service.dtos.lab;

import com.hms.medical_exam_service.entities.ImageType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class DiagnosticImageResponse {
    
    private String id;
    private String labTestResultId;
    private String fileName;
    private String storagePath;
    private String contentType;
    private Long fileSize;
    private String thumbnailPath;
    private ImageType imageType;
    private String description;
    private Integer sequenceNumber;
    private String uploadedBy;
    private Instant createdAt;
    
    // Presigned URL for download (generated on request)
    private String downloadUrl;
    private String thumbnailUrl;
}
