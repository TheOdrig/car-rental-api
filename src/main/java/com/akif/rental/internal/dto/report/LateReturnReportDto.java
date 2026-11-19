package com.akif.rental.internal.dto.report;

import com.akif.shared.enums.CurrencyType;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record LateReturnReportDto(
        Long rentalId,
        String customerName,
        String customerEmail,
        String carBrand,
        String carModel,
        String licensePlate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        Instant actualReturnTime,
        Integer lateHours,
        Integer lateDays,
        LateReturnStatus status,
        BigDecimal penaltyAmount,
        CurrencyType currency,
        Boolean penaltyPaid
) {
}
