package com.akif.car.internal.mapper;

import com.akif.car.api.CarDto;
import com.akif.car.api.CarSummaryResponse;
import com.akif.car.internal.dto.request.CarRequest;
import com.akif.car.api.CarResponse;
import com.akif.car.domain.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = LocalDate.class)
public abstract class CarMapper {

    @Autowired
    protected Clock clock;

    @Mapping(target = "age", expression = "java(car.ageAt(LocalDate.now(clock)))")
    public abstract CarResponse toDto(Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    public abstract Car toEntity(CarRequest carRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    public abstract void updateEntity(CarRequest carRequest, @MappingTarget Car car);

    @Mapping(target = "age", expression = "java(car.ageAt(LocalDate.now(clock)))")
    @Mapping(target = "isNew", ignore = true)
    @Mapping(target = "isAvailable", ignore = true)
    public abstract CarSummaryResponse toSummaryDto(Car car);

    @Mapping(target = "currency", source = "currencyType")
    @Mapping(target = "status", source = "carStatusType")
    @Mapping(target = "available", expression = "java(car.isAvailable())")
    @Mapping(target = "deleted", source = "isDeleted")
    public abstract CarDto toCarDto(Car car);
}
