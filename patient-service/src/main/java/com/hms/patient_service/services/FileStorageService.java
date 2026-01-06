package com.hms.patient_service.services;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Service for uploading and managing files in MinIO storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:patient-images}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_TYPES = {"image/jpeg", "image/png", "image/webp"};

    /**
     * Upload a profile image.
     * @param file The image file to upload
     * @param patientId The patient ID (used as prefix)
     * @return The public URL of the uploaded image
     */
    public String uploadProfileImage(MultipartFile file, String patientId) {
        // Validate file
        validateFile(file);

        // Ensure bucket exists
        ensureBucketExists();

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String objectName = "profiles/" + patientId + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("Uploaded profile image: {} for patient: {}", objectName, patientId);

            // Return public URL - using external endpoint (localhost:9000 for dev)
            return minioEndpoint.replace("minio-storage", "localhost") + "/" + bucketName + "/" + objectName;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from MinIO.
     * @param fileUrl The full URL of the file to delete
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // Extract object name from URL
            String objectName = extractObjectName(fileUrl);
            if (objectName != null) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
                log.info("Deleted file: {}", objectName);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file: {}", e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (2MB)");
        }

        String contentType = file.getContentType();
        boolean isAllowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            throw new IllegalArgumentException("File type not allowed. Allowed: JPEG, PNG, WebP");
        }
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                // Set bucket policy to public read
                String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [{
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }]
                    }
                    """.formatted(bucketName);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build());
                log.info("Created bucket: {} with public read policy", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".jpg";
    }

    private String extractObjectName(String fileUrl) {
        // URL format: http://localhost:9000/bucket-name/path/to/file.jpg
        try {
            String path = fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
