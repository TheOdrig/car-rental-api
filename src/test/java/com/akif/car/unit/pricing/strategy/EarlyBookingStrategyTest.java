package com.akif.car.unit.pricing.strategy;

import com.akif.car.internal.dto.pricing.PriceModifier;
import com.akif.car.internal.dto.pricing.PricingContext;
import com.akif.car.internal.service.pricing.strategy.EarlyBookingStrategy;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("EarlyBookingStrategy Unit Tests")
class EarlyBookingStrategyTest {

    @Mock
    private PricingConfig config;

    @InjectMocks
    private EarlyBookingStrategy strategy;

    private PricingConfig.EarlyBookingConfig earlyBookingConfig;
    private PricingConfig.StrategyConfig strategyConfig;

    @BeforeEach
    void setUp() {
        earlyBookingConfig = new PricingConfig.EarlyBookingConfig();
        strategyConfig = new PricingConfig.StrategyConfig();
        
        lenient().when(config.getEarlyBooking()).thenReturn(earlyBookingConfig);
        lenient().when(config.getStrategy()).thenReturn(strategyConfig);
    }

    @Nested
    @DisplayName("Lead Time Discount Calculations")
    class LeadTimeDiscounts {

        @Test
        @DisplayName("Should apply 15% discount for 30+ days advance booking")
        void shouldApply15PercentDiscountFor30PlusDays() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(35),
                LocalDate.now().plusDays(40),
                LocalDate.now(),
                5,
                35,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.85"));
            assertThat(result.isDiscount()).isTrue();
            assertThat(result.strategyName()).isEqualTo("Early Booking");
            assertThat(result.description()).contains("30+ days");
        }

        @Test
        @DisplayName("Should apply 10% discount for 14-29 days advance booking")
        void shouldApply10PercentDiscountFor14To29Days() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(20),
                LocalDate.now().plusDays(25),
                LocalDate.now(),
                5,
                20,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.90"));
            assertThat(result.isDiscount()).isTrue();
            assertThat(result.description()).contains("14-29 days");
        }

        @Test
        @DisplayName("Should apply 5% discount for 7-13 days advance booking")
        void shouldApply5PercentDiscountFor7To13Days() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(15),
                LocalDate.now(),
                5,
                10,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(result.isDiscount()).isTrue();
            assertThat(result.description()).contains("7-13 days");
        }

        @Test
        @DisplayName("Should apply no discount for less than 7 days advance booking")
        void shouldApplyNoDiscountForLessThan7Days() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(10),
                LocalDate.now(),
                5,
                5,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.isDiscount()).isFalse();
            assertThat(result.description()).contains("No early booking discount");
        }
    }

    @Nested
    @DisplayName("Boundary Conditions")
    class BoundaryConditions {

        @Test
        @DisplayName("Should apply tier 1 discount exactly at 30 days")
        void shouldApplyTier1DiscountExactlyAt30Days() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(35),
                LocalDate.now(),
                5,
                30,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.85"));
        }

        @Test
        @DisplayName("Should apply tier 2 discount exactly at 14 days")
        void shouldApplyTier2DiscountExactlyAt14Days() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(14),
                LocalDate.now().plusDays(19),
                LocalDate.now(),
                5,
                14,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.90"));
        }

        @Test
        @DisplayName("Should apply tier 3 discount exactly at 7 days")
        void shouldApplyTier3DiscountExactlyAt7Days() {
            PricingContext context = new PricingContext(
                1L,
                new BigDecimal("500"),
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(12),
                LocalDate.now(),
                5,
                7,
                "SUV"
            );

            PriceModifier result = strategy.calculate(context);

            assertThat(result.multiplier()).isEqualByComparingTo(new BigDecimal("0.95"));
        }
    }

    @Nested
    @DisplayName("Strategy Metadata")
    class StrategyMetadata {

        @Test
        @DisplayName("Should return correct strategy name")
        void shouldReturnCorrectStrategyName() {
            assertThat(strategy.getStrategyName()).isEqualTo("Early Booking");
        }

        @Test
        @DisplayName("Should return correct order")
        void shouldReturnCorrectOrder() {
            assertThat(strategy.getOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should be enabled when config says enabled")
        void shouldBeEnabledWhenConfigSaysEnabled() {
            strategyConfig.setEarlyBookingEnabled(true);
            assertThat(strategy.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should be disabled when config says disabled")
        void shouldBeDisabledWhenConfigSaysDisabled() {
            strategyConfig.setEarlyBookingEnabled(false);
            assertThat(strategy.isEnabled()).isFalse();
        }
    }
}
