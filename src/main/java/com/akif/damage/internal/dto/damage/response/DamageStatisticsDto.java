package com.akif.damage.internal.dto.damage.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamageStatisticsDto(

    int totalDamages,
    int minorCount,
    int moderateCount,
    int majorCount,
    int totalLossCount,
    @Schema(description = "Monetary totals grouped by the currency each report was assessed in; amounts of different currencies are never summed together. Reports not yet assessed carry no currency and are not listed.")
    List<DamageCurrencyTotals> byCurrency,
    int disputedCount,
    int resolvedCount
) {}
