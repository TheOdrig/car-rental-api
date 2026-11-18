package com.akif.auth.api;

import com.akif.shared.enums.CurrencyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record UserRentalStatisticsDto(
        int totalRentals,
        int completedRentals,
        int cancelledRentals,
        int activeRentals,
        @Schema(description = "Amount spent on returned rentals, grouped by rental currency; amounts of different currencies are never summed together.")
        List<RentalSpendTotals> spentByCurrency,
        double averageRentalDuration,
        int lateReturns) {

    public record RentalSpendTotals(
            CurrencyType currency,
            long rentalCount,
            BigDecimal totalSpent) {
    }
}
