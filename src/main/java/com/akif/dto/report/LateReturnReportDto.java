package com.akif.dto.report;

import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.LateReturnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LateReturnReportDto(
        Long rentalId,
        String customerName,
        String customerEmail,
        String carBrand,
        String carModel,
        String licensePlate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime actualReturnTime,
        Integer lateHours,
        Integer lateDays,
        LateReturnStatus status,
        BigDecimal penaltyAmount,
        CurrencyType currency,
        Boolean penaltyPaid
) implements Serializable {
}
