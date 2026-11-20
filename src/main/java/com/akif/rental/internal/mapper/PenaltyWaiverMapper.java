package com.akif.rental.internal.mapper;

import com.akif.rental.domain.model.PenaltyWaiver;
import com.akif.rental.internal.dto.penalty.PenaltyWaiverResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PenaltyWaiverMapper {

    @Mapping(target = "rentalId", source = "rental.id")
    PenaltyWaiverResponse toResponse(PenaltyWaiver waiver);

    List<PenaltyWaiverResponse> toResponses(List<PenaltyWaiver> waivers);
}
