package com.akif.mapper;

import com.akif.dto.request.CarRequestDto;
import com.akif.dto.response.CarResponseDto;
import com.akif.dto.response.CarSummaryResponseDto;
import com.akif.model.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CarMapper {

    @Mapping(target = "isAvailable", expression = "java(car.isAvailable())")
    @Mapping(target = "canBeSold", expression = "java(car.canBeSold())")
    @Mapping(target = "canBeReserved", expression = "java(car.canBeReserved())")
    @Mapping(target = "requiresAttention", expression = "java(car.requiresAttention())")
    @Mapping(target = "formattedPrice", expression = "java(car.getFormattedPrice())")
    @Mapping(target = "age", expression = "java(car.getAge())")
    @Mapping(target = "isNew", expression = "java(car.isNew())")
    @Mapping(target = "isOld", expression = "java(car.isOld())")
    @Mapping(target = "fullName", expression = "java(car.getFullName())")
    @Mapping(target = "displayName", expression = "java(car.getDisplayName())")
    @Mapping(target = "hasDamage", expression = "java(car.hasDamage())")
    @Mapping(target = "totalPrice", expression = "java(car.getTotalPrice())")
    @Mapping(target = "needsService", expression = "java(car.needsService())")
    @Mapping(target = "isInsuranceExpired", expression = "java(car.isInsuranceExpired())")
    @Mapping(target = "isInspectionExpired", expression = "java(car.isInspectionExpired())")
    @Mapping(target = "hasExpiredDocuments", expression = "java(car.hasExpiredDocuments())")
    CarResponseDto toDto(Car car);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    Car toEntity(CarRequestDto carRequest);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    void updateEntity(CarRequestDto carRequest, @MappingTarget Car car);


    @Mapping(target = "formattedPrice", expression = "java(car.getFormattedPrice())")
    @Mapping(target = "age", expression = "java(car.getAge())")
    @Mapping(target = "fullName", expression = "java(car.getFullName())")
    @Mapping(target = "displayName", expression = "java(car.getDisplayName())")
    @Mapping(target = "isNew", expression = "java(car.isNew())")
    @Mapping(target = "isAvailable", expression = "java(car.isAvailable())")
    CarSummaryResponseDto toSummaryDto(Car car);


    default java.util.List<CarResponseDto> toDtoList(java.util.List<Car> cars) {
        if (cars == null) {
            return null;
        }
        return cars.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    default java.util.List<CarSummaryResponseDto> toSummaryDtoList(java.util.List<Car> cars) {
        if (cars == null) {
            return null;
        }
        return cars.stream()
                .map(this::toSummaryDto)
                .collect(java.util.stream.Collectors.toList());
    }

}
