package com.akif.service.damage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IFileUploadService {

    String uploadFile(MultipartFile file, String directory);

    void deleteFile(String filePath);

    String generateSecureUrl(String filePath, int expirationMinutes);

    boolean validateFileType(MultipartFile file, List<String> allowedTypes);

    boolean validateFileSize(MultipartFile file, long maxSizeBytes);
}
