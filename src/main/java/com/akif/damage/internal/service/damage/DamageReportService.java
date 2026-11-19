package com.akif.damage.internal.service.damage;

import com.akif.damage.internal.dto.damage.request.DamageReportRequest;
import com.akif.damage.internal.dto.damage.response.DamagePhotoDto;
import com.akif.damage.internal.dto.damage.response.DamageReportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DamageReportService {

    DamageReportResponse createDamageReport(Long rentalId, DamageReportRequest request, Long userId);

    DamageReportResponse getDamageReport(Long damageId);

    List<DamagePhotoDto> uploadDamagePhotos(Long damageId, List<MultipartFile> photos, Long userId);

    void deleteDamagePhoto(Long damageId, Long photoId);

    String getPhotoUrl(Long photoId, Long userId);
}

