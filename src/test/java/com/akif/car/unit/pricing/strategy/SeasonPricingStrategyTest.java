package com.akif.car.unit.pricing.strategy;

import com.akif.car.internal.dto.pricing.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.strategy.SeasonPricingStrategy;
import com.akif.rental.internal.config.PricingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeasonPricingStrategy Unit Tests")
class SeasonPricingStrategyTest {

    @Mock
    private PricingConfig config;

    @InjectMocks
    private SeasonPricingStrategy strategy;

    private PricingConfig.SeasonConfig seasonConfig;
    private PricingConfig.StrategyConfig strategyConfig;

    @BeforeEach
    void setUp() {
        seasonConfig = new PricingConfig.SeasonConfig();
        strategyConfig = new PricingConfig.StrategyConfig();

        PricingConfig.SeasonPeriod peakPeriod = new PricingConfig.SeasonPeriod();
        peakPeriod.setStart(MonthDay.of(6, 1));
        peakPeriod.setEnd(MonthDay.of(8, 31));
        peakPeriod.setMultiplier(new BigDecimal("1.25"));

        PricingConfig.SeasonPeriod offpeakPeriod = new PricingConfig.SeasonPeriod();
        offpeakPeriod.setStart(MonthDay.of(11, 1));
        offpeakPeriod.setEnd(MonthDay.of(2, 28));
        offpeakPeriod.setMultiplier(new BigDecimal("0.90"));
        
        seasonConfig.setPeak(peakPeriod);
        seasonConfig.setOffpeak(offpeakPeriod);
        
        lenient().when(config.getSeason()).thenReturn(seasonConfig);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
    }

    @Nested
    @DisplayName("Peak Season Pricing")
    class PeakSeasonPricing {

        @Test
        @DisplayName("Should apply peak season surcharge for summer rental")
        void shouldApplyPeakSeasonSurchargeForSummerRental() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 7, 1),
                LocalDate.of(2024, 7, 7),
                LocalDate.now(),
                7,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.25"));
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.strategyName()).isEqualTo("Season Pricing");
            assertThat(result.description()).contains("Peak season");
        }

        @Test
        @DisplayName("Should apply peak season surcharge for partial peak rental")
        void shouldApplyPeakSeasonSurchargeForPartialPeakRental() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 10),
                LocalDate.now(),
                10,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("1.25"));
            assertThat(result.isDiscount()).isFalse();
        }
    }

    @Nested
    @DisplayName("Off-Peak Season Pricing")
    class OffPeakSeasonPricing {

        @Test
        @DisplayName("Should apply off-peak discount for winter rental")
        void shouldApplyOffPeakDiscountForWinterRental() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 20),
                LocalDate.now(),
                11,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.90"));
            assertThat(result.isDiscount()).isTrue();
            assertThat(result.description()).contains("Off-peak season");
        }

        @Test
        @DisplayName("Should apply off-peak discount for November rental")
        void shouldApplyOffPeakDiscountForNovemberRental() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 11, 15),
                LocalDate.of(2024, 11, 25),
                LocalDate.now(),
                11,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.90"));
            assertThat(result.isDiscount()).isTrue();
        }
    }

    @Nested
    @DisplayName("Regular Season Pricing")
    class RegularSeasonPricing {

        @Test
        @DisplayName("Should apply no modifier for regular season rental")
        void shouldApplyNoModifierForRegularSeasonRental() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 4, 10),
                LocalDate.of(2024, 4, 20),
                LocalDate.now(),
                11,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.description()).contains("Regular season");
        }
    }

    @Nested
    @DisplayName("Mixed Season Pricing")
    class MixedSeasonPricing {

        @Test
        @DisplayName("Should apply weighted average for rental spanning peak and regular seasons")
        void shouldApplyWeightedAverageForPeakAndRegularSeasons() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 5, 25),
                LocalDate.of(2024, 6, 5),
                LocalDate.now(),
                12,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);


            assertThat(result.multiplier()).isGreaterThan(BigDecimal.ONE);
            assertThat(result.multiplier()).isLessThan(new BigDecimal("1.25"));
            assertThat(result.description()).contains("Peak season");
            assertThat(result.description()).contains("5/12 days");
        }

        @Test
        @DisplayName("Should apply weighted average for rental spanning off-peak and regular seasons")
        void shouldApplyWeightedAverageForOffPeakAndRegularSeasons() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 2, 25),
                LocalDate.of(2024, 3, 5),
                LocalDate.now(),
                10,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isLessThan(BigDecimal.ONE);
            assertThat(result.multiplier()).isGreaterThan(new BigDecimal("0.90"));
            assertThat(result.description()).contains("Off-peak season");
            assertThat(result.description()).contains("4/10 days");
        }

        @Test
        @DisplayName("Should apply weighted average for rental spanning peak, off-peak and regular seasons")
        void shouldApplyWeightedAverageForAllSeasons() {

            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 7, 15),
                LocalDate.now(),
                183,
                200,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.description()).contains("Mixed season");
            assertThat(result.description()).contains("peak");
            assertThat(result.description()).contains("off-peak");
        }
    }

    @Nested
    @DisplayName("Strategy Metadata")
    class StrategyMetadata {

        @Test
        @DisplayName("Should return correct strategy name")
        void shouldReturnCorrectStrategyName() {
            assertThat(strategy.getStrategyName()).isEqualTo("Season Pricing");
        }

        @Test
        @DisplayName("Should return correct order")
        void shouldReturnCorrectOrder() {
            assertThat(strategy.getOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should be enabled when config says enabled")
        void shouldBeEnabledWhenConfigSaysEnabled() {
            strategyConfig.setSeasonEnabled(true);
            assertThat(strategy.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should be disabled when config says disabled")
        void shouldBeDisabledWhenConfigSaysDisabled() {
            strategyConfig.setSeasonEnabled(false);
            assertThat(strategy.isEnabled()).isFalse();
        }
    }
}
