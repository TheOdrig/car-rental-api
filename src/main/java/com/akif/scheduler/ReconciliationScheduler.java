package com.akif.scheduler;

import com.akif.dto.reconciliation.Discrepancy;
import com.akif.dto.reconciliation.ReconciliationReport;
import com.akif.service.reconciliation.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final PaymentReconciliationService reconciliationService;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyReconciliation() {
        LocalDate previousDay = LocalDate.now().minusDays(1);
        log.info("Starting scheduled reconciliation for date: {}", previousDay);
        
        try {
            ReconciliationReport report = reconciliationService.runDailyReconciliation(previousDay);

            log.info("Reconciliation completed for {}: DB payments={}, Stripe payments={}, Discrepancies={}",
                previousDay,
                report.totalDatabasePayments(),
                report.totalStripePayments(),
                report.discrepancies().size());
            
            if (report.hasDiscrepancies()) {
                log.warn("Reconciliation found {} discrepancies for date: {}", 
                    report.discrepancies().size(), previousDay);

                for (Discrepancy discrepancy : report.discrepancies()) {
                    log.warn("Discrepancy found - Type: {}, PaymentId: {}, StripeIntentId: {}, Description: {}",
                        discrepancy.type(),
                        discrepancy.paymentId(),
                        discrepancy.stripePaymentIntentId(),
                        discrepancy.description());
                }
            } else {
                log.info("No discrepancies found for date: {}", previousDay);
            }
            
        } catch (Exception e) {
            log.error("Scheduled reconciliation failed for date: {}", previousDay, e);
        }
    }
}
