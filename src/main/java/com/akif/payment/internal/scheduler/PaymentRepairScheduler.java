package com.akif.payment.internal.scheduler;

import com.akif.payment.internal.service.reconciliation.PaymentRepairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "payment.repair.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentRepairScheduler {

    private final PaymentRepairService repairService;

    @Scheduled(
            initialDelayString = "${payment.repair.initial-delay-ms:300000}",
            fixedDelayString = "${payment.repair.interval-ms:1800000}")
    public void repair() {
        try {
            repairService.repairStalePayments();
        } catch (RuntimeException e) {
            log.error("Scheduled payment repair failed: {}", e.getMessage(), e);
        }
    }
}
