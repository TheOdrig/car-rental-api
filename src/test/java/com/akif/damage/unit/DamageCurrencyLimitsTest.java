package com.akif.damage.unit;

import com.akif.damage.api.DamageSeverity;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.internal.service.damage.impl.DamageAssessmentServiceImpl;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Damage thresholds and deductible per currency")
class DamageCurrencyLimitsTest {

    private final DamageConfig config = new DamageConfig(
            new BigDecimal("500"), new BigDecimal("2000"), new BigDecimal("10000"), new BigDecimal("1000.00"),
            Map.of(CurrencyType.JPY, new DamageConfig.CurrencyLimits(
                    new BigDecimal("2000"), new BigDecimal("8000"), new BigDecimal("40000"), new BigDecimal("4000"))),
            10, 1024, List.of("image/jpeg"), "damage-photos", 60, 7);

    private final DamageAssessmentServiceImpl service =
            new DamageAssessmentServiceImpl(Clock.systemUTC(), null, config, null, null, null, null);

    @Test
    @DisplayName("a repair cost that is major in TRY is only minor in JPY")
    void severityDependsOnCurrency() {
        BigDecimal cost = new BigDecimal("1500");

        assertThat(service.determineSeverity(cost, CurrencyType.TRY)).isEqualTo(DamageSeverity.MODERATE);
        assertThat(service.determineSeverity(cost, CurrencyType.JPY)).isEqualTo(DamageSeverity.MINOR);
        assertThat(service.determineSeverity(new BigDecimal("9000"), CurrencyType.TRY))
                .isEqualTo(DamageSeverity.MAJOR);
        assertThat(service.determineSeverity(new BigDecimal("9000"), CurrencyType.JPY))
                .isEqualTo(DamageSeverity.MAJOR);
        assertThat(service.determineSeverity(new BigDecimal("45000"), CurrencyType.JPY))
                .isEqualTo(DamageSeverity.TOTAL_LOSS);
    }

    @Test
    @DisplayName("currencies without an entry use the default thresholds and deductible")
    void fallsBackToDefaults() {
        assertThat(config.minorThresholdFor(CurrencyType.TRY)).isEqualByComparingTo("500");
        assertThat(config.insuranceDeductibleFor(CurrencyType.TRY)).isEqualByComparingTo("1000.00");
        assertThat(config.insuranceDeductibleFor(CurrencyType.JPY)).isEqualByComparingTo("4000");
        assertThat(config.majorThresholdFor(CurrencyType.JPY)).isEqualByComparingTo("40000");
    }
}
