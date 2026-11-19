package com.akif.damage.api;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;

public record RepairCostTotal(CurrencyType currency, BigDecimal amount) {
}
