package com.akif.rental.api;

import com.akif.car.api.CarSummaryResponse;
import com.akif.payment.api.PaymentStatus;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@lombok.Builder(toBuilder = true)
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

        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate paymentOpensOn,

        PaymentStatus paymentStatus,

        Instant paymentDueBy) {

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
                exchangeRate, rateSource, pickupNotes, returnNotes, null, null, createTime, updateTime, null, null, null);
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
                updateTime, null, null, null);
    }

    public RentalResponse withDisplayPrice(com.akif.currency.api.DisplayPrice price) {
        if (price == null) {
            return this;
        }
        return toBuilder()
                .convertedTotalPrice(price.amount())
                .displayCurrency(price.currency())
                .exchangeRate(price.exchangeRate())
                .rateSource(price.rateSource())
                .build();
    }
}
