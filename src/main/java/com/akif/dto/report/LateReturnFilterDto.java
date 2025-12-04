package com.akif.dto.report;

import com.akif.enums.LateReturnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class LateReturnFilterDto implements Serializable {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private LateReturnStatus status;

    @Builder.Default
    private String sortBy = "endDate";

    @Builder.Default
    private String sortDirection = "DESC";
}
