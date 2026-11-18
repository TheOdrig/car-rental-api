package com.akif.damage.integration;

import com.akif.CarRentalApplication;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.dto.damage.response.DamageCurrencyTotals;
import com.akif.damage.internal.dto.damage.response.DamageStatisticsDto;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.DamageHistoryService;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Damage statistics are reported per currency")
class DamageStatisticsCurrencyIntegrationTest {

    @Autowired
    private DamageReportRepository damageReportRepository;

    @Autowired
    private DamageHistoryService damageHistoryService;

    @AfterEach
    void tearDown() {
        damageReportRepository.deleteAll();
    }

    private void save(CurrencyType currency, String repair, String liability, boolean deleted) {
        DamageReport report = DamageReport.builder()
                .rentalId(1L).carId(1L).carBrand("Toyota").carModel("Corolla").carLicensePlate("34ABC123")
                .rentalStartDate(LocalDate.now().minusDays(5)).rentalEndDate(LocalDate.now().minusDays(1))
                .customerEmail("c@example.com").customerFullName("Customer").customerUserId(1L)
                .description("dent").severity(DamageSeverity.MINOR).category(DamageCategory.DENT)
                .status(DamageStatus.ASSESSED).reportedBy(1L).reportedAt(LocalDateTime.now())
                .currency(currency)
                .repairCostEstimate(repair == null ? null : new BigDecimal(repair))
                .customerLiability(liability == null ? null : new BigDecimal(liability))
                .build();
        if (deleted) {
            report.softDelete();
        }
        damageReportRepository.save(report);
    }

    @Test
    @DisplayName("sums and averages each currency separately and skips unassessed reports")
    void groupsByCurrency() {
        save(CurrencyType.TRY, "1000.00", "800.00", false);
        save(CurrencyType.TRY, "2000.00", "1500.00", false);
        save(CurrencyType.EUR, "300.00", "300.00", false);
        save(CurrencyType.USD, "999.00", "999.00", true);
        save(null, null, null, false);

        DamageStatisticsDto statistics = damageHistoryService.getDamageStatistics(null, null);

        assertThat(statistics.totalDamages()).isEqualTo(4);
        assertThat(statistics.byCurrency()).containsExactly(
                new DamageCurrencyTotals(CurrencyType.EUR, 1, new BigDecimal("300.00"), new BigDecimal("300.00"),
                        new BigDecimal("300.00")),
                new DamageCurrencyTotals(CurrencyType.TRY, 2, new BigDecimal("3000.00"), new BigDecimal("2300.00"),
                        new BigDecimal("1500.00")));
    }

    @Test
    @DisplayName("returns an empty breakdown when nothing was assessed")
    void emptyWhenNoCurrency() {
        save(null, null, null, false);

        assertThat(damageHistoryService.getDamageStatistics(null, null).byCurrency()).isEmpty();
    }
}
