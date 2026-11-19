package com.akif.dashboard.unit;

import java.time.LocalDateTime;
import com.akif.car.api.CarCatalog;
import com.akif.damage.api.DamageService;
import com.akif.damage.api.RepairCostTotal;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.MonthlyRevenueDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import com.akif.dashboard.api.dto.RevenueBreakdownDto;
import com.akif.dashboard.internal.service.DashboardQueryService;
import com.akif.payment.api.CollectedRevenue;
import com.akif.payment.api.DailyRevenue;
import com.akif.payment.api.MonthlyRevenue;
import com.akif.payment.api.PaymentCategory;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalService;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardQueryService revenue")
class DashboardQueryServiceRevenueTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2031-03-20T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RentalService rentalService;

    @Mock
    private CarCatalog carCatalog;

    @Mock
    private PaymentService paymentService;

    @Mock
    private DamageService damageService;

    private DashboardQueryService service;

    @BeforeEach
    void setUp() {
        service = new DashboardQueryService(CLOCK, rentalService, carCatalog, paymentService, damageService);
        lenient().when(damageService.sumRepairCostsByCurrency(any(), any())).thenReturn(List.of());
        lenient().when(rentalService.getAverageRentalDurationDays(any(), any())).thenReturn(BigDecimal.ZERO);
    }

    private static CollectedRevenue collected(PaymentCategory category, CurrencyType currency, String amount, long count) {
        return new CollectedRevenue(category, currency, new BigDecimal(amount), count);
    }

    private MonthlyMetricsDto metrics() {
        return service.fetchMonthlyMetrics(LocalDate.of(2031, 3, 1), LocalDate.of(2031, 3, 20));
    }

    @Test
    @DisplayName("a rental of 1000 and a captured penalty of 150 add up to 1150, the penalty is not counted twice")
    void penaltyIsNotCountedTwice() {
        when(paymentService.sumCollectedBetween(any(), any())).thenReturn(List.of(
                collected(PaymentCategory.RENTAL, CurrencyType.TRY, "1000.00", 1),
                collected(PaymentCategory.PENALTY, CurrencyType.TRY, "150.00", 1)));

        RevenueBreakdownDto breakdown = metrics().revenueByCurrency().get(0);

        assertThat(breakdown.currency()).isEqualTo(CurrencyType.TRY);
        assertThat(breakdown.rentalRevenue()).isEqualByComparingTo("1000.00");
        assertThat(breakdown.penaltyRevenue()).isEqualByComparingTo("150.00");
        assertThat(breakdown.totalRevenue()).isEqualByComparingTo("1150.00");
    }

    @Test
    @DisplayName("amounts of different currencies are reported separately and never summed")
    void currenciesAreNeverMixed() {
        when(paymentService.sumCollectedBetween(any(), any())).thenReturn(List.of(
                collected(PaymentCategory.RENTAL, CurrencyType.USD, "100.00", 1),
                collected(PaymentCategory.RENTAL, CurrencyType.JPY, "10000", 1)));

        List<RevenueBreakdownDto> byCurrency = metrics().revenueByCurrency();

        assertThat(byCurrency).extracting(RevenueBreakdownDto::currency)
                .containsExactly(CurrencyType.USD, CurrencyType.JPY);
        assertThat(byCurrency.get(0).totalRevenue()).isEqualByComparingTo("100.00");
        assertThat(byCurrency.get(1).totalRevenue()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("damage recovered comes from the collected damage payments, repair costs are kept per currency")
    void damageRecoveredComesFromPayments() {
        when(paymentService.sumCollectedBetween(any(), any())).thenReturn(List.of(
                collected(PaymentCategory.RENTAL, CurrencyType.TRY, "1000.00", 1),
                collected(PaymentCategory.DAMAGE, CurrencyType.TRY, "300.00", 1)));
        when(damageService.sumRepairCostsByCurrency(any(), any())).thenReturn(List.of(
                new RepairCostTotal(CurrencyType.TRY, new BigDecimal("500.00")),
                new RepairCostTotal(CurrencyType.EUR, new BigDecimal("80.00"))));

        List<RevenueBreakdownDto> byCurrency = metrics().revenueByCurrency();

        RevenueBreakdownDto lira = byCurrency.get(0);
        assertThat(lira.currency()).isEqualTo(CurrencyType.TRY);
        assertThat(lira.damageRecovered()).isEqualByComparingTo("300.00");
        assertThat(lira.damageRepairCosts()).isEqualByComparingTo("500.00");
        assertThat(lira.netDamageImpact()).isEqualByComparingTo("-200.00");
        assertThat(lira.totalRevenue()).isEqualByComparingTo("1300.00");
        assertThat(lira.netRevenue()).isEqualByComparingTo("800.00");
        RevenueBreakdownDto euro = byCurrency.get(1);
        assertThat(euro.currency()).isEqualTo(CurrencyType.EUR);
        assertThat(euro.damageRecovered()).isEqualByComparingTo("0");
        assertThat(euro.damageRepairCosts()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("the period is read by the capture timestamp over the whole end day")
    void periodUsesTheWholeEndDay() {
        when(paymentService.sumCollectedBetween(any(), any())).thenReturn(List.of());

        metrics();

        verify(paymentService).sumCollectedBetween(
                LocalDateTime.of(2031, 3, 1, 0, 0).toInstant(ZoneOffset.UTC), LocalDateTime.of(2031, 3, 21, 0, 0).toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("daily and monthly charts are listed per currency and monthly growth is computed within a currency")
    void chartsArePerCurrency() {
        when(paymentService.getDailyRevenue(30)).thenReturn(List.of(
                new DailyRevenue(LocalDate.of(2031, 3, 19), CurrencyType.TRY, new BigDecimal("100.00"), 1),
                new DailyRevenue(LocalDate.of(2031, 3, 19), CurrencyType.USD, new BigDecimal("50.00"), 1)));
        when(paymentService.getMonthlyRevenue(12)).thenReturn(List.of(
                new MonthlyRevenue(YearMonth.of(2031, 1), CurrencyType.TRY, new BigDecimal("1000.00"), 2),
                new MonthlyRevenue(YearMonth.of(2031, 2), CurrencyType.TRY, new BigDecimal("1250.00"), 3),
                new MonthlyRevenue(YearMonth.of(2031, 2), CurrencyType.USD, new BigDecimal("40.00"), 1)));
        when(paymentService.sumCollectedBetween(any(), any())).thenReturn(List.of());

        RevenueAnalyticsDto analytics = service.fetchRevenueAnalytics();

        assertThat(analytics.dailyRevenue()).hasSize(2);
        assertThat(analytics.dailyRevenue().get(1).currency()).isEqualTo(CurrencyType.USD);
        List<MonthlyRevenueDto> monthly = analytics.monthlyRevenue();
        assertThat(monthly.get(0).growthPercentage()).isEqualByComparingTo("0");
        assertThat(monthly.get(1).growthPercentage()).isEqualByComparingTo("25.00");
        assertThat(monthly.get(2).growthPercentage()).isEqualByComparingTo("0");
    }
}
