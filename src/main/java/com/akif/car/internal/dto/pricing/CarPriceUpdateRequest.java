package com.akif.car.internal.dto.pricing;

import com.akif.shared.enums.CurrencyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CarPriceUpdateRequest(

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    BigDecimal price,

    @NotNull(message = "Currency type cannot be null")
    CurrencyType currencyType,

    String reason,
    String notes
) {}
