package com.akif.damage.domain;

import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DamageReport lifecycle")
class DamageReportLifecycleTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 18, 10, 0);

    private DamageReport assessedReport() {
        DamageReport report = DamageReport.builder().build();
        report.assess(DamageSeverity.MODERATE, DamageCategory.SCRATCH, new BigDecimal("500.00"),
                new BigDecimal("300.00"), true, new BigDecimal("200.00"), "scratched door", 5L, now);
        return report;
    }

    @Test
    @DisplayName("assess stores the assessment and moves the report to ASSESSED")
    void assessMovesToAssessed() {
        DamageReport report = assessedReport();

        assertThat(report.getStatus()).isEqualTo(DamageStatus.ASSESSED);
        assertThat(report.getSeverity()).isEqualTo(DamageSeverity.MODERATE);
        assertThat(report.getCustomerLiability()).isEqualByComparingTo("300.00");
        assertThat(report.getInsuranceCoverage()).isTrue();
        assertThat(report.getAssessedBy()).isEqualTo(5L);
        assertThat(report.getAssessedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("reviseAssessment changes the figures without changing the status")
    void reviseAssessmentKeepsStatus() {
        DamageReport report = DamageReport.builder().status(DamageStatus.CHARGED).build();

        report.reviseAssessment(DamageSeverity.MINOR, DamageCategory.SCRATCH, new BigDecimal("80.00"),
                new BigDecimal("80.00"), false, BigDecimal.ZERO, "buffed out", 6L, now);

        assertThat(report.getStatus()).isEqualTo(DamageStatus.CHARGED);
        assertThat(report.getCustomerLiability()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("dispute records who disputed and why, then resolveDispute settles the liability")
    void disputeThenResolve() {
        DamageReport report = assessedReport();

        report.dispute("pre-existing", "photos attached", 9L, now.plusDays(1));
        assertThat(report.getStatus()).isEqualTo(DamageStatus.DISPUTED);
        assertThat(report.getDisputedBy()).isEqualTo(9L);
        assertThat(report.getDisputeReason()).isEqualTo("pre-existing");

        BigDecimal charged = report.getCustomerLiability();
        report.resolveDispute(new BigDecimal("100.00"), new BigDecimal("250.00"),
                charged.subtract(new BigDecimal("100.00")), "partially accepted", 5L, now.plusDays(3));
        assertThat(report.getOriginalLiability()).isEqualByComparingTo(charged);
        assertThat(report.getStatus()).isEqualTo(DamageStatus.RESOLVED);
        assertThat(report.getCustomerLiability()).isEqualByComparingTo("100.00");
        assertThat(report.getRepairCostEstimate()).isEqualByComparingTo("250.00");
        assertThat(report.getResolutionNotes()).isEqualTo("partially accepted");
        assertThat(report.getResolvedAt()).isEqualTo(now.plusDays(3));
    }

    @Test
    @DisplayName("payment markers track the charge outcome")
    void paymentMarkersTrackChargeOutcome() {
        DamageReport report = assessedReport();

        report.markPaymentPending();
        assertThat(report.getPaymentStatus()).isEqualTo("PENDING");

        report.markCharged("tx-77");
        assertThat(report.getStatus()).isEqualTo(DamageStatus.CHARGED);
        assertThat(report.getPaymentStatus()).isEqualTo("CAPTURED");
        assertThat(report.getTransactionId()).isEqualTo("tx-77");

        report.markPaymentFailed();
        assertThat(report.getPaymentStatus()).isEqualTo("FAILED");
    }
}
