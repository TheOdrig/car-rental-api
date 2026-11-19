package com.akif.car.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DayAvailabilityDto(

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    AvailabilityStatus status
) {}
