package com.akif.payment.internal.service.reconciliation;

import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.CapturedPaymentRefunder;
import com.akif.payment.internal.service.PaymentStateService;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PaymentRepairService {

    private static final int BATCH_SIZE = 100;

    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentStateService stateService;
    private final CapturedPaymentRefunder refunder;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.repair.min-age-minutes:30}")
    private long minAgeMinutes;

    public PaymentRepairService(
            Clock clock,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentStateService stateService,
            CapturedPaymentRefunder refunder,
            PlatformTransactionManager transactionManager) {
        this.clock = clock;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.stateService = stateService;
        this.refunder = refunder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int repairStalePayments() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(minAgeMinutes);
        List<Object[]> candidates = paymentRepository.findStaleNonTerminal(cutoff, PageRequest.of(0, BATCH_SIZE));
        int repaired = 0;
        for (Object[] candidate : candidates) {
            Long paymentId = (Long) candidate[0];
            String intentId = (String) candidate[1];
            try {
                GatewayIntent intent = paymentGateway.retrieveIntent(intentId);
                Boolean changed = transactionTemplate.execute(status -> apply(paymentId, intentId, intent));
                if (Boolean.TRUE.equals(changed)) {
                    repaired++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not repair payment {}: {}", paymentId, e.getMessage());
            }
        }
        repaired += repairPenaltiesWithoutIntent(cutoff);
        repaired += refundClosedCaptures(cutoff);
        if (repaired > 0) {
            log.warn("[AUDIT] Payment repair: {} payment(s) were out of step with the gateway and have been corrected", repaired);
        }
        return repaired;
    }

    private int repairPenaltiesWithoutIntent(LocalDateTime cutoff) {
        int repaired = 0;
        for (Long paymentId : paymentRepository.findStalePenaltiesWithoutIntent(cutoff, PageRequest.of(0, BATCH_SIZE))) {
            try {
                Optional<GatewayIntent> found = paymentGateway.findIntentByPayment(paymentId);
                GatewayIntent intent = found.filter(i -> i.status() == GatewayIntentStatus.SUCCEEDED).orElse(null);
                Boolean changed = transactionTemplate.execute(status -> apply(paymentId, null, intent));
                if (Boolean.TRUE.equals(changed)) {
                    repaired++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not repair penalty payment {}: {}", paymentId, e.getMessage());
            }
        }
        return repaired;
    }

    private int refundClosedCaptures(LocalDateTime cutoff) {
        int refunded = 0;
        for (Long paymentId : paymentRepository.findClosedCapturedForRefund(cutoff, PageRequest.of(0, BATCH_SIZE))) {
            try {
                Boolean done = transactionTemplate.execute(status -> {
                    Payment payment = paymentRepository.findById(paymentId).orElse(null);
                    return payment != null && refunder.refund(paymentId, refunder.causeOf(payment));
                });
                if (Boolean.TRUE.equals(done)) {
                    refunded++;
                }
            } catch (RuntimeException e) {
                log.warn("Could not refund the closed captured payment {}: {}", paymentId, e.getMessage());
            }
        }
        return refunded;
    }

    private boolean apply(Long paymentId, String intentId, GatewayIntent intent) {
        Optional<Payment> locked = paymentRepository.findByIdForUpdate(paymentId);
        if (locked.isEmpty()) {
            return false;
        }
        Payment payment = locked.get();
        payment.markSynced(LocalDateTime.now(clock));
        boolean sameIntent = intentId == null
                ? payment.getStripePaymentIntentId() == null
                : intentId.equals(payment.getStripePaymentIntentId());
        boolean open = !payment.isClosed()
                && (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.AUTHORIZED)
                && sameIntent && intent != null;
        if (!open) {
            paymentRepository.save(payment);
            return false;
        }
        PaymentStatus before = payment.getStatus();
        boolean changed = intent.status() == GatewayIntentStatus.CANCELED
                ? stateService.applyRemoteCancellation(payment)
                : stateService.applyIntent(payment, intent);
        paymentRepository.save(payment);
        if (changed && payment.getStatus() != before) {
            log.warn("[AUDIT] Payment repaired | Payment: {} | Kind: {} | From: {} | To: {}",
                    payment.getId(), payment.getKind(), before, payment.getStatus());
            return true;
        }
        return false;
    }
}
