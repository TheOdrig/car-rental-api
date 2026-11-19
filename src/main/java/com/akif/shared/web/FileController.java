package com.akif.shared.web;

import com.akif.shared.infrastructure.FileAccessPolicy;
import com.akif.shared.security.AuthenticatedUser;
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

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@Profile({"dev", "test"})
@Slf4j
public class FileController {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

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
            return ResponseEntity.notFound().build();
        }
        try {
            Path root = Paths.get(baseUploadDir, directory).toAbsolutePath().normalize();
            Path filePath = root.resolve(filename).normalize();
            if (!filePath.startsWith(root)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                        .body(resource);
            } else {
                log.warn("File not found or not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error loading file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean mayRead(String directory, String filename, AuthenticatedUser caller) {
        if (caller == null) {
            return false;
        }
        if (caller.getAuthorities().stream().anyMatch(a -> ADMIN_AUTHORITY.equals(a.getAuthority()))) {
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
