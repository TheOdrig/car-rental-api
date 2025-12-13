package com.akif.car.unit.pricing.strategy;

import com.akif.car.internal.dto.pricing.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.strategy.DurationDiscountStrategy;
import com.akif.rental.internal.config.PricingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("DurationDiscountStrategy Unit Tests")
class DurationDiscountStrategyTest {

    @Mock
    private PricingConfig config;

    @InjectMocks
    private DurationDiscountStrategy strategy;

    @BeforeEach
    void setUp() {
        PricingConfig.DurationConfig durationConfig = new PricingConfig.DurationConfig();
        PricingConfig.StrategyConfig strategyConfig = new PricingConfig.StrategyConfig();
        
        lenient().when(config.getDuration()).thenReturn(durationConfig);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
    }

    @Test
    @DisplayName("Should apply 20% discount for 30+ days rental")
    void shouldApply20PercentDiscountFor30PlusDays() {
        PricingContext context = new PricingContext(
            1L, new BigDecimal("500"), LocalDate.now(), LocalDate.now().plusDays(35),
            LocalDate.now(), 35, 0, "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.80"));
        assertThat(result.isDiscount()).isTrue();
    }

    @Test
    @DisplayName("Should apply 15% discount for 14-29 days rental")
    void shouldApply15PercentDiscountFor14To29Days() {
        PricingContext context = new PricingContext(
            1L, new BigDecimal("500"), LocalDate.now(), LocalDate.now().plusDays(20),
            LocalDate.now(), 20, 0, "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(result.isDiscount()).isTrue();
    }

    @Test
    @DisplayName("Should apply 10% discount for 7-13 days rental")
    void shouldApply10PercentDiscountFor7To13Days() {
        PricingContext context = new PricingContext(
            1L, new BigDecimal("500"), LocalDate.now(), LocalDate.now().plusDays(10),
            LocalDate.now(), 10, 0, "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.90"));
        assertThat(result.isDiscount()).isTrue();
    }

    @Test
    @DisplayName("Should apply no discount for less than 7 days rental")
    void shouldApplyNoDiscountForLessThan7Days() {
        PricingContext context = new PricingContext(
            1L, new BigDecimal("500"), LocalDate.now(), LocalDate.now().plusDays(5),
            LocalDate.now(), 5, 0, "SUV"
        );

        PriceModifier result = strategy.calculate(context);

        assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.isDiscount()).isFalse();
    }

    @Test
    @DisplayName("Should return correct strategy name")
    void shouldReturnCorrectStrategyName() {
        assertThat(strategy.getStrategyName()).isEqualTo("Duration Discount");
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        assertThat(strategy.getOrder()).isEqualTo(3);
    }
}
