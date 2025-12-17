package com.akif.dashboard.internal.mapper;

import com.akif.dashboard.api.dto.AlertDto;
import com.akif.dashboard.domain.model.Alert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    @Mapping(target = "createdAt", source = "createTime")
    AlertDto toAlertDto(Alert alert);

    List<AlertDto> toAlertDtoList(List<Alert> alerts);
}


