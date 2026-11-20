package com.akif.damage.internal.mapper;

import com.akif.damage.api.DamageReportDto;
import com.akif.damage.domain.model.DamagePhoto;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.api.DamagePhotoDto;
import com.akif.damage.internal.dto.damage.response.DamageReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface DamageMapper {

    @Mapping(target = "customerName", source = "customerFullName")
    @Mapping(target = "carSummary.id", source = "carId")
    @Mapping(target = "carSummary.brand", source = "carBrand")
    @Mapping(target = "carSummary.model", source = "carModel")
    @Mapping(target = "carSummary.licensePlate", source = "carLicensePlate")
    @Mapping(target = "carSummary.thumbnailUrl", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "refundAmount", source = "disputeRefundAmount")
    DamageReportDto toPublicDto(DamageReport damageReport);

    List<DamageReportDto> toPublicDtoList(List<DamageReport> damageReports);

    @Mapping(target = "customerName", source = "customerFullName")
    @Mapping(target = "refundAmount", source = "disputeRefundAmount")
    @Mapping(target = "carSummary.id", source = "carId")
    @Mapping(target = "carSummary.brand", source = "carBrand")
    @Mapping(target = "carSummary.model", source = "carModel")
    @Mapping(target = "carSummary.licensePlate", source = "carLicensePlate")
    @Mapping(target = "carSummary.thumbnailUrl", ignore = true)
    DamageReportResponse toResponseDto(DamageReport damageReport);

    List<DamageReportResponse> toResponseDtoList(List<DamageReport> damageReports);

    @Mapping(target = "secureUrl", ignore = true)
    DamagePhotoDto toPhotoDto(DamagePhoto damagePhoto);

    List<DamagePhotoDto> toPhotoDtoList(List<DamagePhoto> photos);
}
