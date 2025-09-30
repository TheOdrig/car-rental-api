package com.akif.dto.request;

import com.akif.enums.CurrencyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarPriceUpdateRequestDto {

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Currency type cannot be null")
    private CurrencyType currencyType;

    @DecimalMin(value = "0.0", message = "Damage price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Damage price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal damagePrice;

    private String reason;
    private String notes;
}
