package com.akif.damage.internal.service.damage.impl;

import com.akif.damage.api.DamagePhotoDto;
import com.akif.damage.domain.model.DamagePhoto;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.internal.repository.DamagePhotoRepository;
import com.akif.shared.infrastructure.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DamagePhotoAssembler {

    private final DamagePhotoRepository damagePhotoRepository;
    private final FileUploadService fileUploadService;
    private final DamageConfig damageConfig;

    public DamagePhotoDto toDto(DamagePhoto photo) {
        String secureUrl = fileUploadService.generateSecureUrl(
                photo.getFilePath(),
                damageConfig.getPhotoUrlExpirationMinutes());
        return new DamagePhotoDto(
                photo.getId(),
                photo.getFileName(),
                secureUrl,
                photo.getFileSize(),
                photo.getUploadedAt());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<DamagePhotoDto>> byReport(Collection<Long> damageReportIds) {
        Map<Long, List<DamagePhotoDto>> grouped = new LinkedHashMap<>();
        if (damageReportIds.isEmpty()) {
            return grouped;
        }
        for (DamagePhoto photo : damagePhotoRepository.findByDamageReportIdInAndIsDeletedFalseOrderByIdAsc(damageReportIds)) {
            grouped.computeIfAbsent(photo.getDamageReport().getId(), key -> new java.util.ArrayList<>()).add(toDto(photo));
        }
        return grouped;
    }
}
