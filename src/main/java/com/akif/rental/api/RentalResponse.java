package com.akif.rental.api;

import com.akif.car.api.CarSummaryResponse;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.UserSummaryResponse;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

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

        Instant createTime,

        Instant updateTime,

        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate paymentOpensOn) {

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
            Instant createTime,
            Instant updateTime) {
        this(id, carSummary, userSummary, startDate, endDate, days, dailyPrice, totalPrice, currency, status,
                originalPrice, finalPrice, totalSavings, appliedDiscounts, convertedTotalPrice, displayCurrency,
                exchangeRate, rateSource, pickupNotes, returnNotes, null, null, createTime, updateTime, null);
    }

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
            String approvalNotes,
            String cancellationReason,
            Instant createTime,
            Instant updateTime) {
        this(id, carSummary, userSummary, startDate, endDate, days, dailyPrice, totalPrice, currency, status,
                originalPrice, finalPrice, totalSavings, appliedDiscounts, convertedTotalPrice, displayCurrency,
                exchangeRate, rateSource, pickupNotes, returnNotes, approvalNotes, cancellationReason, createTime,
                updateTime, null);
    }

    public RentalResponse withDisplayPrice(com.akif.currency.api.DisplayPrice price) {
        if (price == null) {
            return this;
        }
        return new RentalResponse(id, carSummary, userSummary, startDate, endDate, days, dailyPrice, totalPrice,
                currency, status, originalPrice, finalPrice, totalSavings, appliedDiscounts, price.amount(),
                price.currency(), price.exchangeRate(), price.rateSource(), pickupNotes, returnNotes,
                approvalNotes, cancellationReason, createTime, updateTime, paymentOpensOn);
    }
}
