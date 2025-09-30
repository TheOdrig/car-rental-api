package com.akif.dto.request;

import com.akif.enums.CurrencyType;
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

    private BigDecimal price;
    private CurrencyType currencyType;
    private BigDecimal damagePrice;
    private String reason;
    private String notes;
}
