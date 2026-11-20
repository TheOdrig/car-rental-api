package com.akif.shared.web;

import com.akif.shared.exception.InvalidParameterException;
import com.akif.shared.exception.StoredFileNotFoundException;
import com.akif.shared.infrastructure.FileAccessPolicy;
import com.akif.shared.security.AuthenticatedUser;
import com.akif.shared.security.OwnerOrAdmin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Profile({"dev", "test"})
@Slf4j
public class FileController {

    private final String baseUploadDir;
    private final List<FileAccessPolicy> accessPolicies;

    public FileController(@Value("${file.upload.base-dir:uploads}") String baseUploadDir,
                          List<FileAccessPolicy> accessPolicies) {
        this.baseUploadDir = baseUploadDir;
        this.accessPolicies = accessPolicies;
    }

    @GetMapping("/damage-photos/{filename}")
    public ResponseEntity<Resource> getDamagePhoto(@PathVariable String filename,
                                                   @AuthenticationPrincipal AuthenticatedUser caller) {
        return getFile("damage-photos", filename, caller);
    }

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename,
                                              @AuthenticationPrincipal AuthenticatedUser caller) {
        return getFile("avatars", filename, caller);
    }

    private ResponseEntity<Resource> getFile(String directory, String filename, AuthenticatedUser caller) {
        if (!mayRead(directory, filename, caller)) {
            throw new StoredFileNotFoundException();
        }
        try {
            Path root = Paths.get(baseUploadDir, directory).toAbsolutePath().normalize();
            Path filePath = root.resolve(filename).normalize();
            if (!filePath.startsWith(root)) {
                throw new InvalidParameterException("Invalid file path");
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);
                long contentLength = resource.contentLength();

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(contentLength)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                        .body(resource);
            } else {
                log.warn("File not found or not readable: {}", filePath);
                throw new StoredFileNotFoundException();
            }
        } catch (MalformedURLException e) {
            log.error("Error loading file: {}", filename, e);
            throw new InvalidParameterException("Invalid file path");
        } catch (IOException e) {
            log.warn("File could not be read: {}", filename);
            throw new StoredFileNotFoundException();
        }
    }

    private boolean mayRead(String directory, String filename, AuthenticatedUser caller) {
        if (caller == null) {
            return false;
        }
        if (OwnerOrAdmin.isAdmin(caller)) {
            return true;
        }
        return accessPolicies.stream()
                .filter(policy -> policy.directory().equals(directory))
                .anyMatch(policy -> policy.isOwnedBy(filename, caller.id()));
    }

    private String determineContentType(String filename) {
        if (filename.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.toLowerCase().endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
