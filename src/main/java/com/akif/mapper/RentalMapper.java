package com.akif.mapper;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {CarMapper.class}
)
public interface RentalMapper {

    @Mapping(target = "carSummary.id", source = "car.id")
    @Mapping(target = "carSummary.brand", source = "car.brand")
    @Mapping(target = "carSummary.model", source = "car.model")
    @Mapping(target = "carSummary.licensePlate", source = "car.licensePlate")
    @Mapping(target = "userSummary.id", source = "user.id")
    @Mapping(target = "userSummary.username", source = "user.username")
    @Mapping(target = "userSummary.email", source = "user.email")
    RentalResponseDto toDto(Rental rental);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "car", ignore = true)
    @Mapping(target = "days", ignore = true)
    @Mapping(target = "dailyPrice", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "pickupNotes", ignore = true)
    @Mapping(target = "returnNotes", ignore = true)
    @Mapping(target = "pickupReminderSent", ignore = true)
    @Mapping(target = "returnReminderSent", ignore = true)
    Rental toEntity(RentalRequestDto dto);
}