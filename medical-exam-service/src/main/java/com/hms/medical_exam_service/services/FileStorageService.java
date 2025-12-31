package com.hms.medical_exam_service.services;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FileStorageService {
    
    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;
    
    @Value("${minio.bucket-name:lab-images}")
    private String bucketName;
    
    @Value("${minio.presigned-url-expiry-hours:24}")
    private int presignedUrlExpiryHours;
    
    public FileStorageService(
            MinioClient minioClient,
            @Qualifier("publicMinioClient") MinioClient publicMinioClient) {
        this.minioClient = minioClient;
        this.publicMinioClient = publicMinioClient;
    }
    
    /**
     * Initialize bucket on startup if it doesn't exist
     */
    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("Could not initialize MinIO bucket: {}. Will retry on first upload.", e.getMessage());
        }
    }
    
    /**
     * Upload a file to MinIO
     * @param file The file to upload
     * @param patientId Patient ID for organizing storage
     * @param resultId Lab test result ID
     * @return The storage path of the uploaded file
     */
    public String uploadFile(MultipartFile file, String patientId, String resultId) {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;
            
            // Build storage path: lab-images/{patientId}/{resultId}/{uuid}.ext
            String storagePath = String.format("%s/%s/%s", patientId, resultId, uniqueFilename);
            
            // Upload to MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            log.info("Uploaded file to MinIO: {}", storagePath);
            return storagePath;
            
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get a presigned URL for downloading a file
     * @param storagePath The storage path of the file
     * @return Presigned URL valid for configured hours, using public endpoint for browser access
     */
    public String getPresignedUrl(String storagePath) {
        try {
            // Use publicMinioClient to generate URL with localhost:9000 so browser can access
            return publicMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .method(Method.GET)
                    .expiry(presignedUrlExpiryHours, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", storagePath, e.getMessage());
            return null;
        }
    }
    
    /**
     * Download a file from MinIO
     * @param storagePath The storage path of the file
     * @return InputStream of the file content
     */
    public InputStream downloadFile(String storagePath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Delete a file from MinIO
     * @param storagePath The storage path of the file
     */
    public void deleteFile(String storagePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build());
            log.info("Deleted file from MinIO: {}", storagePath);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a file exists in MinIO
     * @param storagePath The storage path of the file
     * @return true if file exists
     */
    public boolean fileExists(String storagePath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
