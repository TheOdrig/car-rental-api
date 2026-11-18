package com.akif.auth.api;

import com.akif.shared.enums.CurrencyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record UserDamageStatisticsDto(
        int totalDamageReports,
        @Schema(description = "Customer liability grouped by the currency each report was assessed in; amounts of different currencies are never summed together. Reports not yet assessed carry no currency and are not listed.")
        List<CurrencyTotals> byCurrency) {

    public record CurrencyTotals(
            CurrencyType currency,
            long assessedCount,
            BigDecimal totalCustomerLiability) {
    }
}
