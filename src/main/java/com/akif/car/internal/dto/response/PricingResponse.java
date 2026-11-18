package com.akif.car.internal.dto.response;

import com.akif.car.api.PricingResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PricingResponse {

    private BigDecimal basePrice;
    private BigDecimal baseTotalPrice;
    private BigDecimal finalPrice;
    private BigDecimal effectiveDailyPrice;
    private BigDecimal totalSavings;
    private BigDecimal combinedMultiplier;
    private Integer rentalDays;
    private List<ModifierDto> appliedModifiers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifierDto {
        private String strategyName;
        private BigDecimal multiplier;
        private String description;
        private Boolean isDiscount;
        private String formattedPercentage;
    }

    public static PricingResponse fromPricingResult(PricingResult result) {
        List<ModifierDto> modifiers = result.appliedModifiers().stream()
            .map(modifier -> {
                BigDecimal percentage = modifier.multiplier()
                    .subtract(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100));
                
                String formattedPercentage = String.format("%+.1f%%", percentage);
                
                return ModifierDto.builder()
                    .strategyName(modifier.strategyName())
                    .multiplier(modifier.multiplier())
                    .description(modifier.description())
                    .isDiscount(modifier.isDiscount())
                    .formattedPercentage(formattedPercentage)
                    .build();
            })
            .toList();

        return PricingResponse.builder()
            .basePrice(result.basePrice())
            .baseTotalPrice(result.baseTotalPrice())
            .finalPrice(result.finalPrice())
            .effectiveDailyPrice(result.effectiveDailyPrice())
            .totalSavings(result.totalSavings())
            .combinedMultiplier(result.combinedMultiplier())
            .rentalDays(result.rentalDays())
            .appliedModifiers(modifiers)
            .build();
    }
}
