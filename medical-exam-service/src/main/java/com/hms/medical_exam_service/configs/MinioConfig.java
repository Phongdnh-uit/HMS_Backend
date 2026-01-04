package com.hms.medical_exam_service.configs;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MinioConfig {
    
    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;
    
    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;
    
    @Value("${minio.access-key:minioadmin}")
    private String accessKey;
    
    @Value("${minio.secret-key:minioadmin123}")
    private String secretKey;
    
    @Value("${minio.bucket-name:lab-images}")
    private String bucketName;
    
    /**
     * Primary MinioClient for internal operations (upload, delete, etc.)
     * Uses Docker internal hostname (minio-storage:9000)
     */
    @Bean
    @Primary
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
    
    /**
     * MinioClient for generating presigned URLs
     * Uses public endpoint (localhost:9000) so browser can access with valid signature
     */
    @Bean
    @Qualifier("publicMinioClient")
    public MinioClient publicMinioClient() {
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
    
    public String getBucketName() {
        return bucketName;
    }
}
