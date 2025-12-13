package com.akif.damage.internal.service.damage.impl;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarService;
import com.akif.car.api.CarDto;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.api.DamageReportedEvent;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamagePhoto;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.service.damage.DamageReportService;
import com.akif.damage.internal.dto.damage.response.DamageReportResponse;
import com.akif.shared.infrastructure.FileUploadService;
import com.akif.damage.internal.dto.damage.request.DamageReportRequest;
import com.akif.damage.internal.dto.damage.response.DamagePhotoDto;
import com.akif.damage.internal.repository.DamagePhotoRepository;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.exception.DamageReportException;
import com.akif.shared.exception.FileUploadException;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalSummaryDto;
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
@Transactional(readOnly = true)
public class DamageReportServiceImpl implements DamageReportService {

    private final DamageReportRepository damageReportRepository;
    private final DamagePhotoRepository damagePhotoRepository;
    private final FileUploadService fileUploadService;
    private final DamageConfig damageConfig;
    private final ApplicationEventPublisher eventPublisher;

    private final RentalService rentalService;
    private final CarService carService;
    private final AuthService authService;

    @Override
    @Transactional
    public DamageReportResponse createDamageReport(Long rentalId, DamageReportRequest request, String username) {
        RentalSummaryDto rental = rentalService.getRentalSummaryById(rentalId);

        UserDto user = authService.getUserByUsername(username);

        CarDto car = carService.getCarDtoById(rental.carId());

        DamageReport damageReport = DamageReport.builder()
                .rentalId(rental.id())
                .carId(rental.carId())
                .carBrand(car.brand())
                .carModel(car.model())
                .carLicensePlate(car.licensePlate())
                .rentalStartDate(rental.startDate())
                .rentalEndDate(rental.endDate())
                .customerEmail(rental.userEmail())
                .customerFullName(rental.userFullName())
                .customerUserId(rental.userId())
                .description(request.description())
                .damageLocation(request.damageLocation())
                .severity(request.initialSeverity())
                .category(request.category())
                .status(DamageStatus.REPORTED)
                .reportedBy(user.id())
                .reportedAt(LocalDateTime.now())
                .build();

        damageReport = damageReportRepository.save(damageReport);

        rentalService.incrementDamageReportCount(rentalId);

        if (request.initialSeverity() == DamageSeverity.MAJOR) {
            carService.markAsMaintenance(rental.carId());
            log.info("Car {} marked as MAINTENANCE due to MAJOR damage", rental.carId());
        }

        eventPublisher.publishEvent(new DamageReportedEvent(
                this,
                damageReport.getId(),
                damageReport.getRentalId(),
                damageReport.getCarId(),
                damageReport.getCarLicensePlate(),
                damageReport.getCustomerEmail(),
                damageReport.getCustomerFullName(),
                damageReport.getDescription(),
                damageReport.getSeverity(),
                damageReport.getReportedAt()
        ));
        log.info("Damage report created: id={}, rentalId={}", damageReport.getId(), rentalId);

        return mapToResponseDto(damageReport);
    }

    @Override
    public DamageReportResponse getDamageReport(Long damageId) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        return mapToResponseDto(damageReport);
    }

    @Override
    @Transactional
    public List<DamagePhotoDto> uploadDamagePhotos(Long damageId, List<MultipartFile> photos, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        UserDto user = authService.getUserByUsername(username);

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
                    .uploadedBy(user.id())
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
        damageReportRepository.findByIdAndIsDeletedFalse(damageId)
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

    private DamageReportResponse mapToResponseDto(DamageReport damageReport) {
        List<DamagePhotoDto> photos = damagePhotoRepository
                .findByDamageReportIdAndIsDeletedFalse(damageReport.getId())
                .stream()
                .map(this::mapToPhotoDto)
                .toList();

        return new DamageReportResponse(
                damageReport.getId(),
                damageReport.getRentalId(),
                damageReport.getCarId(),
                damageReport.getCarLicensePlate(),
                damageReport.getCustomerFullName(),
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
