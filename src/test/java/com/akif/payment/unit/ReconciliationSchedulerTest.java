package com.akif.payment.unit;

import java.time.LocalDateTime;
import com.akif.payment.domain.enums.DiscrepancyType;
import com.akif.payment.internal.dto.Discrepancy;
import com.akif.payment.internal.dto.ReconciliationReport;
import com.akif.payment.internal.scheduler.ReconciliationScheduler;
import com.akif.payment.internal.service.reconciliation.PaymentReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReconciliationScheduler")
class ReconciliationSchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T02:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 3, 9);

    private final PaymentReconciliationService service = mock(PaymentReconciliationService.class);
    private final ReconciliationScheduler scheduler = new ReconciliationScheduler(CLOCK, service);

    @Test
    @DisplayName("reconciles the previous day")
    void reconcilesPreviousDay() {
        when(service.runDailyReconciliation(YESTERDAY)).thenReturn(report(List.of()));

        scheduler.runDailyReconciliation();

        verify(service).runDailyReconciliation(YESTERDAY);
    }

    @Test
    @DisplayName("reports discrepancies without failing")
    void toleratesDiscrepancies() {
        Discrepancy discrepancy = new Discrepancy(DiscrepancyType.AMOUNT_MISMATCH, "1", "pi_1",
                new BigDecimal("10.00"), new BigDecimal("12.00"), "CAPTURED", "succeeded", "amounts differ");
        when(service.runDailyReconciliation(YESTERDAY)).thenReturn(report(List.of(discrepancy)));

        assertThatCode(scheduler::runDailyReconciliation).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("keeps the job alive when the reconciliation fails")
    void swallowsFailures() {
        when(service.runDailyReconciliation(YESTERDAY)).thenThrow(new IllegalStateException("stripe down"));

        assertThatCode(scheduler::runDailyReconciliation).doesNotThrowAnyException();
    }

    private static ReconciliationReport report(List<Discrepancy> discrepancies) {
        return new ReconciliationReport(YESTERDAY, 1, 1, discrepancies, !discrepancies.isEmpty(),
                LocalDateTime.of(2026, 3, 10, 2, 0).toInstant(ZoneOffset.UTC));
    }
}
