package com.akif.payment.unit.gateway;

import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StripeAmounts")
class StripeAmountsTest {

    @Test
    @DisplayName("converts two-decimal currencies to cents")
    void twoDecimalCurrencies() {
        assertThat(StripeAmounts.toMinorUnits(new BigDecimal("19.99"), CurrencyType.USD)).isEqualTo(1999L);
        assertThat(StripeAmounts.toMinorUnits(new BigDecimal("1250"), CurrencyType.TRY)).isEqualTo(125000L);
    }

    @Test
    @DisplayName("keeps zero-decimal currencies as they are")
    void zeroDecimalCurrency() {
        assertThat(StripeAmounts.toMinorUnits(new BigDecimal("1500"), CurrencyType.JPY)).isEqualTo(1500L);
        assertThat(StripeAmounts.toMinorUnits(new BigDecimal("1500.00"), CurrencyType.JPY)).isEqualTo(1500L);
    }

    @Test
    @DisplayName("rejects fractions a zero-decimal currency cannot carry")
    void rejectsFractionOfYen() {
        assertThatThrownBy(() -> StripeAmounts.toMinorUnits(new BigDecimal("1500.50"), CurrencyType.JPY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects sub-cent precision instead of rounding silently")
    void rejectsSubCentPrecision() {
        assertThatThrownBy(() -> StripeAmounts.toMinorUnits(new BigDecimal("10.005"), CurrencyType.EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects zero, negative and missing amounts")
    void rejectsNonPositive() {
        assertThatThrownBy(() -> StripeAmounts.toMinorUnits(BigDecimal.ZERO, CurrencyType.USD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StripeAmounts.toMinorUnits(new BigDecimal("-1"), CurrencyType.USD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StripeAmounts.toMinorUnits(null, CurrencyType.USD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("converts minor units back using the currency exponent")
    void fromMinorUnits() {
        assertThat(StripeAmounts.fromMinorUnits(1999L, CurrencyType.GBP)).isEqualByComparingTo("19.99");
        assertThat(StripeAmounts.fromMinorUnits(1500L, CurrencyType.JPY)).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("uses the lower-case ISO code Stripe expects")
    void lowerCaseCode() {
        assertThat(StripeAmounts.code(CurrencyType.TRY)).isEqualTo("try");
    }
}
