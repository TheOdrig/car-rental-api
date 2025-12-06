package com.akif.service.damage;

import com.akif.dto.damage.request.DamageReportRequestDto;
import com.akif.dto.damage.response.DamagePhotoDto;
import com.akif.dto.damage.response.DamageReportResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDamageReportService {

    DamageReportResponseDto createDamageReport(Long rentalId, DamageReportRequestDto request);

    DamageReportResponseDto getDamageReport(Long damageId);

    List<DamagePhotoDto> uploadDamagePhotos(Long damageId, List<MultipartFile> photos);

    void deleteDamagePhoto(Long damageId, Long photoId);

    String getPhotoUrl(Long photoId);
}
