package com.akif.service.damage.impl;

import com.akif.config.R2Config;
import com.akif.exception.FileUploadException;
import com.akif.service.damage.IFileUploadService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class R2FileStorageService implements IFileUploadService {

    private final R2Config r2Config;
    
    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @PostConstruct
    public void init() {
        if (!r2Config.isConfigured()) {
            log.warn("R2 storage is not configured. File uploads will fail in production.");
            return;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2Config.getAccessKeyId(),
                r2Config.getSecretAccessKey()
        );

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(r2Config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(r2Config.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();

        log.info("R2 storage initialized with bucket: {}", r2Config.getBucketName());
    }

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        validateClientInitialized();
        
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String key = directory + "/" + UUID.randomUUID() + extension;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.debug("File uploaded to R2: {}", key);
            return key;
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload file to R2: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String filePath) {
        validateClientInitialized();
        
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(filePath)
                .build();

        s3Client.deleteObject(deleteRequest);
        log.debug("File deleted from R2: {}", filePath);
    }

    @Override
    public String generateSecureUrl(String filePath, int expirationMinutes) {
        validateClientInitialized();
        
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(r2Config.getBucketName())
                .key(filePath)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public boolean validateFileType(MultipartFile file, List<String> allowedTypes) {
        String contentType = file.getContentType();
        return contentType != null && allowedTypes.contains(contentType);
    }

    @Override
    public boolean validateFileSize(MultipartFile file, long maxSizeBytes) {
        return file.getSize() <= maxSizeBytes;
    }

    private void validateClientInitialized() {
        if (s3Client == null) {
            throw new FileUploadException("R2 storage client is not initialized. Check R2 configuration.");
        }
    }
}
