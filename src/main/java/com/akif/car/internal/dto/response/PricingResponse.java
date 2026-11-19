package com.akif.car.internal.dto.response;

import com.akif.car.api.PricingResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PricingResponse(
        BigDecimal basePrice,
        BigDecimal baseTotalPrice,
        BigDecimal finalPrice,
        BigDecimal effectiveDailyPrice,
        BigDecimal totalSavings,
        BigDecimal combinedMultiplier,
        Integer rentalDays,
        List<ModifierDto> appliedModifiers) {

    @Builder
    public record ModifierDto(
            String strategyName,
            BigDecimal multiplier,
            String description,
            Boolean isDiscount,
            String formattedPercentage) {
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
