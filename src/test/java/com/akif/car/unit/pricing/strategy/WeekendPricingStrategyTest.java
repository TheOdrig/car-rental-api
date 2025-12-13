package com.akif.car.unit.pricing.strategy;

import com.akif.car.internal.dto.pricing.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.strategy.WeekendPricingStrategy;
import com.akif.rental.internal.config.PricingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeekendPricingStrategy Unit Tests")
class WeekendPricingStrategyTest {

    @Mock
    private PricingConfig config;

    @InjectMocks
    private WeekendPricingStrategy strategy;

    @BeforeEach
    void setUp() {
        PricingConfig.WeekendConfig weekendConfig = new PricingConfig.WeekendConfig();
        weekendConfig.setMultiplier(new BigDecimal("1.15"));
        weekendConfig.setDays(List.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
        
        PricingConfig.StrategyConfig strategyConfig = new PricingConfig.StrategyConfig();
        
        lenient().when(config.getWeekend()).thenReturn(weekendConfig);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
    }

    @Test
    @DisplayName("Should apply surcharge for rental with weekend days")
    void shouldApplySurchargeForRentalWithWeekendDays() {

        LocalDate friday = LocalDate.of(2024, 1, 5);
        LocalDate sunday = LocalDate.of(2024, 1, 7);
        
        PricingContext context = new PricingContext(
            1L,
            new BigDecimal("500"),
            friday,
            sunday,
            LocalDate.now(),
            3,
            10,
            "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.15"));
        assertThat(result.isDiscount()).isFalse();
        assertThat(result.strategyName()).isEqualTo("Weekend Pricing");
    }

    @Test
    @DisplayName("Should apply no surcharge for weekday-only rental")
    void shouldApplyNoSurchargeForWeekdayOnlyRental() {

        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDate wednesday = LocalDate.of(2024, 1, 3);
        
        PricingContext context = new PricingContext(
            1L,
            new BigDecimal("500"),
            monday,
            wednesday,
            LocalDate.now(),
            3,
            10,
            "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.isDiscount()).isFalse();
    }

    @Test
    @DisplayName("Should apply weighted multiplier for mixed weekday/weekend rental")
    void shouldApplyWeightedMultiplierForMixedRental() {

        LocalDate monday = LocalDate.of(2024, 1, 1);
        LocalDate sunday = LocalDate.of(2024, 1, 7);
        
        PricingContext context = new PricingContext(
            1L,
            new BigDecimal("500"),
            monday,
            sunday,
            LocalDate.now(),
            7,
            10,
            "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isGreaterThan(BigDecimal.ONE);
        assertThat(result.multiplier()).isLessThan(new BigDecimal("1.15"));
        assertThat(result.isDiscount()).isFalse();
    }

    @Test
    @DisplayName("Should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertThat(strategy.getStrategyName()).isEqualTo("Weekend Pricing");
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        assertThat(strategy.getOrder()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should be enabled when config says enabled")
    void shouldBeEnabledWhenConfigSaysEnabled() {
        PricingConfig.StrategyConfig strategyConfig = new PricingConfig.StrategyConfig();
        strategyConfig.setWeekendEnabled(true);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
        
        assertThat(strategy.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should be disabled when config says disabled")
    void shouldBeDisabledWhenConfigSaysDisabled() {
        PricingConfig.StrategyConfig strategyConfig = new PricingConfig.StrategyConfig();
        strategyConfig.setWeekendEnabled(false);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
        
        assertThat(strategy.isEnabled()).isFalse();
    }
}