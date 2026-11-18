package com.akif.damage.web;

import com.akif.damage.internal.dto.damage.request.DamageReportRequest;
import com.akif.damage.internal.dto.damage.response.DamagePhotoDto;
import com.akif.damage.internal.dto.damage.response.DamageReportResponse;
import com.akif.damage.internal.service.damage.DamageReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Damage Report", description = "Operations for damage report management")
public class DamageReportController {

    private final DamageReportService damageReportService;

    @PostMapping("/api/admin/damages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create damage report", description = "Create a new damage report for a rental (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Damage report created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Rental not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageReportResponse> createDamageReport(
            @Parameter(description = "Rental ID", required = true)
            @RequestParam Long rentalId,
            @Valid @RequestBody DamageReportRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/admin/damages - rentalId: {}, user: {}", rentalId, username);
        DamageReportResponse response = damageReportService.createDamageReport(rentalId, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/admin/damages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get damage report", description = "Get damage report by ID (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Damage report found"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<DamageReportResponse> getDamageReport(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id) {

        log.info("GET /api/admin/damages/{}", id);
        DamageReportResponse response = damageReportService.getDamageReport(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/admin/damages/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload photos", description = "Upload damage photos (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Photos uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid file or limit exceeded"),
            @ApiResponse(responseCode = "404", description = "Damage report not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<DamagePhotoDto>> uploadPhotos(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Photo files", required = true)
            @RequestParam("photos") List<MultipartFile> photos,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("POST /api/admin/damages/{}/photos - count: {}, user: {}", id, photos.size(), username);
        List<DamagePhotoDto> uploadedPhotos = damageReportService.uploadDamagePhotos(id, photos, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedPhotos);
    }

    @DeleteMapping("/api/admin/damages/{id}/photos/{photoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete photo", description = "Delete a damage photo (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Photo deleted"),
            @ApiResponse(responseCode = "404", description = "Photo not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deletePhoto(
            @Parameter(description = "Damage ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Photo ID", required = true)
            @PathVariable Long photoId) {

        log.info("DELETE /api/admin/damages/{}/photos/{}", id, photoId);
        damageReportService.deleteDamagePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/damages/photos/{photoId}/url")
    @Operation(summary = "Get photo URL", description = "Get secure URL for a damage photo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL generated"),
            @ApiResponse(responseCode = "403", description = "Photo belongs to another customer"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    public ResponseEntity<String> getPhotoUrl(
            @Parameter(description = "Photo ID", required = true)
            @PathVariable Long photoId,
            Authentication authentication) {

        log.debug("GET /api/damages/photos/{}/url", photoId);
        String url = damageReportService.getPhotoUrl(photoId, authentication.getName());
        return ResponseEntity.ok(url);
    }
}
