package com.akif.service.damage.impl;

import com.akif.config.DamageConfig;
import com.akif.dto.damage.request.DamageReportRequestDto;
import com.akif.dto.damage.response.DamagePhotoDto;
import com.akif.dto.damage.response.DamageReportResponseDto;
import com.akif.shared.enums.DamageStatus;
import com.akif.event.DamageReportedEvent;
import com.akif.exception.DamageReportException;
import com.akif.exception.FileUploadException;
import com.akif.exception.RentalNotFoundException;
import com.akif.model.DamagePhoto;
import com.akif.model.DamageReport;
import com.akif.model.Rental;
import com.akif.repository.DamagePhotoRepository;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.RentalRepository;
import com.akif.auth.repository.UserRepository;
import com.akif.auth.domain.User;
import com.akif.service.damage.IDamageReportService;
import com.akif.service.damage.IFileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DamageReportServiceImpl implements IDamageReportService {

    private final DamageReportRepository damageReportRepository;
    private final DamagePhotoRepository damagePhotoRepository;
    private final RentalRepository rentalRepository;
    private final UserRepository userRepository;
    private final IFileUploadService fileUploadService;
    private final DamageConfig damageConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DamageReportResponseDto createDamageReport(Long rentalId, DamageReportRequestDto request, String username) {
        Rental rental = rentalRepository.findByIdAndIsDeletedFalse(rentalId)
                .orElseThrow(() -> new RentalNotFoundException("Rental not found with id: " + rentalId));

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        DamageReport damageReport = DamageReport.builder()
                .rental(rental)
                .car(rental.getCar())
                .description(request.description())
                .damageLocation(request.damageLocation())
                .severity(request.initialSeverity())
                .category(request.category())
                .status(DamageStatus.REPORTED)
                .reportedBy(user.getId())
                .reportedAt(LocalDateTime.now())
                .build();

        damageReport = damageReportRepository.save(damageReport);

        rental.setHasDamageReports(true);
        rental.setDamageReportsCount(rental.getDamageReportsCount() + 1);
        rentalRepository.save(rental);

        eventPublisher.publishEvent(new DamageReportedEvent(this, damageReport));
        log.info("Damage report created: id={}, rentalId={}", damageReport.getId(), rentalId);

        return mapToResponseDto(damageReport);
    }

    @Override
    @Transactional(readOnly = true)
    public DamageReportResponseDto getDamageReport(Long damageId) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        return mapToResponseDto(damageReport);
    }

    @Override
    @Transactional
    public List<DamagePhotoDto> uploadDamagePhotos(Long damageId, List<MultipartFile> photos, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        int currentPhotoCount = damagePhotoRepository.countByDamageReportIdAndIsDeletedFalse(damageId);
        int maxPhotos = damageConfig.getMaxPhotosPerReport();

        if (currentPhotoCount + photos.size() > maxPhotos) {
            throw new DamageReportException(
                    String.format("Maximum %d photos allowed per report. Current: %d, Trying to add: %d",
                            maxPhotos, currentPhotoCount, photos.size())
            );
        }

        List<DamagePhotoDto> uploadedPhotos = new ArrayList<>();
        int displayOrder = currentPhotoCount;

        for (MultipartFile photo : photos) {
            validatePhoto(photo);

            String filePath = fileUploadService.uploadFile(photo, damageConfig.getPhotoStorageDirectory());

            DamagePhoto damagePhoto = DamagePhoto.builder()
                    .damageReport(damageReport)
                    .fileName(photo.getOriginalFilename())
                    .filePath(filePath)
                    .fileSize(photo.getSize())
                    .contentType(photo.getContentType())
                    .uploadedBy(user.getId())
                    .uploadedAt(LocalDateTime.now())
                    .displayOrder(++displayOrder)
                    .build();

            damagePhoto = damagePhotoRepository.save(damagePhoto);
            uploadedPhotos.add(mapToPhotoDto(damagePhoto));
        }

        log.info("Uploaded {} photos for damage report: {}", photos.size(), damageId);
        return uploadedPhotos;
    }

    @Override
    @Transactional
    public void deleteDamagePhoto(Long damageId, Long photoId) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        DamagePhoto photo = damagePhotoRepository.findByIdAndIsDeletedFalse(photoId)
                .orElseThrow(() -> DamageReportException.photoNotFound(photoId));

        if (!photo.getDamageReport().getId().equals(damageId)) {
            throw new DamageReportException("Photo does not belong to the specified damage report");
        }

        fileUploadService.deleteFile(photo.getFilePath());
        photo.setIsDeleted(true);
        damagePhotoRepository.save(photo);

        log.info("Deleted photo: {} from damage report: {}", photoId, damageId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPhotoUrl(Long photoId) {
        DamagePhoto photo = damagePhotoRepository.findByIdAndIsDeletedFalse(photoId)
                .orElseThrow(() -> DamageReportException.photoNotFound(photoId));

        return fileUploadService.generateSecureUrl(
                photo.getFilePath(),
                damageConfig.getPhotoUrlExpirationMinutes()
        );
    }

    private void validatePhoto(MultipartFile photo) {
        if (!fileUploadService.validateFileType(photo, damageConfig.getAllowedPhotoTypes())) {
            throw new FileUploadException(
                    "Invalid file type: " + photo.getContentType() +
                            ". Allowed types: " + damageConfig.getAllowedPhotoTypes()
            );
        }

        if (!fileUploadService.validateFileSize(photo, damageConfig.getMaxPhotoSizeBytes())) {
            throw new FileUploadException(
                    "File size exceeds maximum allowed: " + damageConfig.getMaxPhotoSizeBytes() + " bytes"
            );
        }
    }

    private DamageReportResponseDto mapToResponseDto(DamageReport damageReport) {
        List<DamagePhotoDto> photos = damagePhotoRepository
                .findByDamageReportIdAndIsDeletedFalse(damageReport.getId())
                .stream()
                .map(this::mapToPhotoDto)
                .toList();

        return new DamageReportResponseDto(
                damageReport.getId(),
                damageReport.getRental().getId(),
                damageReport.getCar().getId(),
                damageReport.getCar().getLicensePlate(),
                damageReport.getRental().getUser().getUsername(),
                damageReport.getDescription(),
                damageReport.getDamageLocation(),
                damageReport.getSeverity(),
                damageReport.getCategory(),
                damageReport.getStatus(),
                damageReport.getRepairCostEstimate(),
                damageReport.getCustomerLiability(),
                damageReport.getInsuranceCoverage(),
                damageReport.getReportedAt(),
                damageReport.getAssessedAt(),
                photos
        );
    }

    private DamagePhotoDto mapToPhotoDto(DamagePhoto photo) {
        String secureUrl = fileUploadService.generateSecureUrl(
                photo.getFilePath(),
                damageConfig.getPhotoUrlExpirationMinutes()
        );

        return new DamagePhotoDto(
                photo.getId(),
                photo.getFileName(),
                secureUrl,
                photo.getFileSize(),
                photo.getUploadedAt()
        );
    }
}
