package com.akif.dto.currency;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConvertRequest(
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Source currency is required")
        CurrencyType fromCurrency,

        @NotNull(message = "Target currency is required")
        CurrencyType toCurrency
) {
}
