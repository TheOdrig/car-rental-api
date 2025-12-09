package com.akif.dto.availability;

import com.akif.shared.enums.AvailabilityStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DayAvailabilityDto implements Serializable {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private AvailabilityStatus status;

    private Long rentalId;
}
