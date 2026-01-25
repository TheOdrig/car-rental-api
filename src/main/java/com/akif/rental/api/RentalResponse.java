package com.akif.rental.api;

import com.akif.car.api.CarSummaryResponse;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.internal.dto.response.UserSummaryResponse;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RentalResponse(

        Long id,

        CarSummaryResponse carSummary,
        UserSummaryResponse userSummary,

        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,

        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,

        Integer days,
        BigDecimal dailyPrice,
        BigDecimal totalPrice,
        CurrencyType currency,
        RentalStatus status,

        BigDecimal originalPrice,
        BigDecimal finalPrice,
        BigDecimal totalSavings,
        java.util.List<String> appliedDiscounts,

        BigDecimal convertedTotalPrice,
        CurrencyType displayCurrency,
        BigDecimal exchangeRate,
        String rateSource,

        String pickupNotes,
        String returnNotes,
        String approvalNotes,
        String cancellationReason,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC") LocalDateTime createTime,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC") LocalDateTime updateTime) {

    public RentalResponse(
            Long id,
            CarSummaryResponse carSummary,
            UserSummaryResponse userSummary,
            LocalDate startDate,
            LocalDate endDate,
            Integer days,
            BigDecimal dailyPrice,
            BigDecimal totalPrice,
            CurrencyType currency,
            RentalStatus status,
            BigDecimal originalPrice,
            BigDecimal finalPrice,
            BigDecimal totalSavings,
            java.util.List<String> appliedDiscounts,
            BigDecimal convertedTotalPrice,
            CurrencyType displayCurrency,
            BigDecimal exchangeRate,
            String rateSource,
            String pickupNotes,
            String returnNotes,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        this(id, carSummary, userSummary, startDate, endDate, days, dailyPrice, totalPrice, currency, status,
                originalPrice, finalPrice, totalSavings, appliedDiscounts, convertedTotalPrice, displayCurrency,
                exchangeRate, rateSource, pickupNotes, returnNotes, null, null, createTime, updateTime);
    }
}
