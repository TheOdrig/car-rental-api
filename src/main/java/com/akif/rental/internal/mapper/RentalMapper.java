package com.akif.rental.internal.mapper;

import com.akif.rental.api.RentalResponse;
import com.akif.rental.domain.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = com.akif.rental.internal.service.PaymentWindow.class, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RentalMapper {

    @Mapping(target = "carSummary.id", source = "carId")
    @Mapping(target = "carSummary.brand", source = "carBrand")
    @Mapping(target = "carSummary.model", source = "carModel")
    @Mapping(target = "carSummary.licensePlate", source = "carLicensePlate")
    @Mapping(target = "carSummary.thumbnailUrl", source = "carThumbnailUrl")
    @Mapping(target = "userSummary.id", source = "userId")
    @Mapping(target = "userSummary.username", source = "userUsername")
    @Mapping(target = "userSummary.email", source = "userEmail")
    @Mapping(target = "userSummary.fullName", source = "userFullName")
    @Mapping(target = "notes", source = "customerNotes")
    @Mapping(target = "paymentOpensOn", source = "startDate", qualifiedByName = "paymentOpensOn")
    RentalResponse toDto(Rental rental);
}