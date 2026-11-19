package com.akif.damage.web;

import com.akif.shared.security.AuthenticatedUser;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Damage Report", description = "Operations for damage report management")
public class DamageReportController {

    private final DamageReportService damageReportService;

    @PostMapping("/api/v1/admin/damages")
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
            @AuthenticationPrincipal AuthenticatedUser caller) {

        DamageReportResponse response = damageReportService.createDamageReport(rentalId, request, caller.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/admin/damages/{id}")
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

        DamageReportResponse response = damageReportService.getDamageReport(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/api/v1/admin/damages/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
            @AuthenticationPrincipal AuthenticatedUser caller) {

        List<DamagePhotoDto> uploadedPhotos = damageReportService.uploadDamagePhotos(id, photos, caller.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedPhotos);
    }

    @DeleteMapping("/api/v1/admin/damages/{id}/photos/{photoId}")
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

        damageReportService.deleteDamagePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/damages/photos/{photoId}/url")
    @Operation(summary = "Get photo URL", description = "Get secure URL for a damage photo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL generated"),
            @ApiResponse(responseCode = "404", description = "Photo not found or belongs to another customer")
    })
    public ResponseEntity<String> getPhotoUrl(
            @Parameter(description = "Photo ID", required = true)
            @PathVariable Long photoId,
            @AuthenticationPrincipal AuthenticatedUser caller) {

        String url = damageReportService.getPhotoUrl(photoId, caller.id());
        return ResponseEntity.ok(url);
    }
}
