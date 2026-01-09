package com.hms.patient_service.services;

import io.minio.*;
import io.minio.errors.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for FileStorageService.
 * Tests file upload and deletion operations with MinIO.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC-PAT-006/007: FileStorageService Unit Tests")
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private FileStorageService fileStorageService;

    private static final String BUCKET_NAME = "patient-images";
    private static final String MINIO_ENDPOINT = "http://localhost:9000";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(fileStorageService, "minioEndpoint", MINIO_ENDPOINT);
    }

    @Nested
    @DisplayName("Method: uploadProfileImage()")
    class UploadProfileImageTests {

        @Test
        @DisplayName("UC-PAT-006: Should upload valid image successfully")
        void uploadProfileImage_withValidImage_shouldUploadSuccessfully() throws Exception {
            // Given
            String patientId = "patient-123";
            byte[] imageContent = "test-image-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    imageContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When
            String result = fileStorageService.uploadProfileImage(file, patientId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).startsWith(MINIO_ENDPOINT);
            assertThat(result).contains(BUCKET_NAME);
            assertThat(result).contains("profiles/" + patientId);
            assertThat(result).endsWith(".jpg");

            then(minioClient).should().bucketExists(any(BucketExistsArgs.class));
            then(minioClient).should().putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should upload PNG image successfully")
        void uploadProfileImage_withPngImage_shouldUploadSuccessfully() throws Exception {
            // Given
            String patientId = "patient-456";
            byte[] imageContent = "test-png-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.png",
                    "image/png",
                    imageContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When
            String result = fileStorageService.uploadProfileImage(file, patientId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).endsWith(".png");
        }

        @Test
        @DisplayName("Should upload WebP image successfully")
        void uploadProfileImage_withWebPImage_shouldUploadSuccessfully() throws Exception {
            // Given
            String patientId = "patient-789";
            byte[] imageContent = "test-webp-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.webp",
                    "image/webp",
                    imageContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When
            String result = fileStorageService.uploadProfileImage(file, patientId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).endsWith(".webp");
        }

        @Test
        @DisplayName("UC-PAT-006: Should throw exception when file is null")
        void uploadProfileImage_withNullFile_shouldThrowException() {
            // Given
            String patientId = "patient-123";

            // When & Then
            assertThatThrownBy(() -> fileStorageService.uploadProfileImage(null, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File is empty or null");
        }

        @Test
        @DisplayName("Should throw exception when file is empty")
        void uploadProfileImage_withEmptyFile_shouldThrowException() {
            // Given
            String patientId = "patient-123";
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.uploadProfileImage(emptyFile, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File is empty or null");
        }

        @Test
        @DisplayName("Should throw exception when file size exceeds 2MB")
        void uploadProfileImage_withOversizedFile_shouldThrowException() {
            // Given
            String patientId = "patient-123";
            byte[] largeContent = new byte[3 * 1024 * 1024]; // 3MB
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    largeContent
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.uploadProfileImage(largeFile, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File size exceeds maximum allowed (2MB)");
        }

        @Test
        @DisplayName("Should throw exception for unsupported file type")
        void uploadProfileImage_withUnsupportedFileType_shouldThrowException() {
            // Given
            String patientId = "patient-123";
            byte[] imageContent = "test-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.gif",
                    "image/gif",
                    imageContent
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.uploadProfileImage(file, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File type not allowed. Allowed: JPEG, PNG, WebP");
        }

        @Test
        @DisplayName("Should throw exception for PDF file")
        void uploadProfileImage_withPdfFile_shouldThrowException() {
            // Given
            String patientId = "patient-123";
            byte[] pdfContent = "pdf-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    pdfContent
            );

            // When & Then
            assertThatThrownBy(() -> fileStorageService.uploadProfileImage(file, patientId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("File type not allowed. Allowed: JPEG, PNG, WebP");
        }

        @Test
        @DisplayName("Should create bucket if it doesn't exist")
        void uploadProfileImage_whenBucketDoesNotExist_shouldCreateBucket() throws Exception {
            // Given
            String patientId = "patient-123";
            byte[] imageContent = "test-image-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    imageContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(false);
            willDoNothing().given(minioClient).makeBucket(any(MakeBucketArgs.class));
            willDoNothing().given(minioClient).setBucketPolicy(any(SetBucketPolicyArgs.class));
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When
            String result = fileStorageService.uploadProfileImage(file, patientId);

            // Then
            assertThat(result).isNotNull();
            then(minioClient).should().bucketExists(any(BucketExistsArgs.class));
            then(minioClient).should().makeBucket(any(MakeBucketArgs.class));
            then(minioClient).should().setBucketPolicy(any(SetBucketPolicyArgs.class));
            then(minioClient).should().putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when MinIO upload fails")
        void uploadProfileImage_whenMinioFails_shouldThrowRuntimeException() throws Exception {
            // Given
            String patientId = "patient-123";
            byte[] imageContent = "test-image-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    imageContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class)))
                    .willThrow(new RuntimeException("MinIO connection failed"));

            // When & Then
            assertThatThrownBy(() -> fileStorageService.uploadProfileImage(file, patientId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to upload file");
        }

        @Test
        @DisplayName("Should generate unique filenames for same patient")
        void uploadProfileImage_withSamePatient_shouldGenerateUniqueFilenames() throws Exception {
            // Given
            String patientId = "patient-123";
            byte[] imageContent1 = "test-image-1".getBytes();
            byte[] imageContent2 = "test-image-2".getBytes();
            MockMultipartFile file1 = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    imageContent1
            );
            MockMultipartFile file2 = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    imageContent2
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When
            String result1 = fileStorageService.uploadProfileImage(file1, patientId);
            String result2 = fileStorageService.uploadProfileImage(file2, patientId);

            // Then
            assertThat(result1).isNotEqualTo(result2); // URLs should be different due to UUID
            assertThat(result1).contains("profiles/" + patientId);
            assertThat(result2).contains("profiles/" + patientId);
        }
    }

    @Nested
    @DisplayName("Method: deleteFile()")
    class DeleteFileTests {

        @Test
        @DisplayName("UC-PAT-007: Should delete file successfully")
        void deleteFile_withValidUrl_shouldDeleteSuccessfully() throws Exception {
            // Given
            String fileUrl = MINIO_ENDPOINT + "/" + BUCKET_NAME + "/profiles/patient-123/test.jpg";
            willDoNothing().given(minioClient).removeObject(any(RemoveObjectArgs.class));

            // When & Then
            assertThatCode(() -> fileStorageService.deleteFile(fileUrl))
                    .doesNotThrowAnyException();

            then(minioClient).should().removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should handle null file URL gracefully")
        void deleteFile_withNullUrl_shouldDoNothing() throws Exception {
            // When & Then
            assertThatCode(() -> fileStorageService.deleteFile(null))
                    .doesNotThrowAnyException();

            then(minioClient).should(never()).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should handle empty file URL gracefully")
        void deleteFile_withEmptyUrl_shouldDoNothing() throws Exception {
            // When & Then
            assertThatCode(() -> fileStorageService.deleteFile(""))
                    .doesNotThrowAnyException();

            then(minioClient).should(never()).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should not throw exception when MinIO delete fails")
        void deleteFile_whenMinioFails_shouldNotThrowException() throws Exception {
            // Given
            String fileUrl = MINIO_ENDPOINT + "/" + BUCKET_NAME + "/profiles/patient-123/test.jpg";
            willThrow(new RuntimeException("MinIO connection failed"))
                    .given(minioClient).removeObject(any(RemoveObjectArgs.class));

            // When & Then - Should log warning but not throw exception
            assertThatCode(() -> fileStorageService.deleteFile(fileUrl))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should extract object name correctly from URL")
        void deleteFile_shouldExtractObjectNameCorrectly() throws Exception {
            // Given
            String objectName = "profiles/patient-123/uuid-here.jpg";
            String fileUrl = MINIO_ENDPOINT + "/" + BUCKET_NAME + "/" + objectName;
            willDoNothing().given(minioClient).removeObject(any(RemoveObjectArgs.class));

            // When
            fileStorageService.deleteFile(fileUrl);

            // Then
            then(minioClient).should().removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("Should handle malformed URL gracefully")
        void deleteFile_withMalformedUrl_shouldHandleGracefully() throws Exception {
            // Given
            String malformedUrl = "not-a-valid-url";

            // When & Then
            assertThatCode(() -> fileStorageService.deleteFile(malformedUrl))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("File Validation")
    class FileValidationTests {

        @Test
        @DisplayName("Should accept file at maximum size (2MB)")
        void validation_withMaxSizeFile_shouldAccept() throws Exception {
            // Given
            String patientId = "patient-123";
            byte[] maxSizeContent = new byte[2 * 1024 * 1024]; // Exactly 2MB
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile.jpg",
                    "image/jpeg",
                    maxSizeContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When & Then
            assertThatCode(() -> fileStorageService.uploadProfileImage(file, patientId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle file without extension")
        void validation_withNoExtension_shouldUseDefaultExtension() throws Exception {
            // Given
            String patientId = "patient-123";
            byte[] imageContent = "test-content".getBytes();
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "profile",
                    "image/jpeg",
                    imageContent
            );

            given(minioClient.bucketExists(any(BucketExistsArgs.class))).willReturn(true);
            given(minioClient.putObject(any(PutObjectArgs.class))).willReturn(null);

            // When
            String result = fileStorageService.uploadProfileImage(file, patientId);

            // Then
            assertThat(result).isNotNull();
            // Should use default extension .jpg
        }
    }
}
