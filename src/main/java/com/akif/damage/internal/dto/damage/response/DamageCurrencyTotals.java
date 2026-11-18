package com.akif.damage.internal.dto.damage.response;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record DamageCurrencyTotals(
    CurrencyType currency,
    long assessedCount,
    BigDecimal totalRepairCost,
    BigDecimal totalCustomerLiability,
    BigDecimal averageRepairCost
) {}
