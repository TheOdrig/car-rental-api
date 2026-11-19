package com.akif.damage.internal.mapper;

import com.akif.damage.api.DamageReportDto;
import com.akif.damage.domain.model.DamagePhoto;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.dto.damage.response.DamagePhotoDto;
import com.akif.damage.internal.dto.damage.response.DamageReportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface DamageMapper {

    @Mapping(target = "customerName", source = "customerFullName")
    DamageReportDto toPublicDto(DamageReport damageReport);

    List<DamageReportDto> toPublicDtoList(List<DamageReport> damageReports);

    @Mapping(target = "customerName", source = "customerFullName")
    @Mapping(target = "refundAmount", source = "disputeRefundAmount")
    DamageReportResponse toResponseDto(DamageReport damageReport);

    List<DamageReportResponse> toResponseDtoList(List<DamageReport> damageReports);

    @Mapping(target = "secureUrl", ignore = true)
    DamagePhotoDto toPhotoDto(DamagePhoto damagePhoto);

    List<DamagePhotoDto> toPhotoDtoList(List<DamagePhoto> photos);
}
