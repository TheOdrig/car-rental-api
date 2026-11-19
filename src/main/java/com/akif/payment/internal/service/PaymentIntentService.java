package com.akif.payment.internal.service;

import java.time.Clock;
import com.akif.payment.api.InvalidPaymentStateException;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentNotFoundException;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.PaymentSummary;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentIntentService {

    private static final Set<PaymentStatus> PAYABLE = EnumSet.of(
            PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.AUTHORIZED, PaymentStatus.CANCELLED);

    private static final int MAX_INTENT_ROUNDS = 4;

    private final Clock clock;
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyGenerator keys;
    private final PaymentStateService stateService;
    private final StripeConfig stripeConfig;
    private final TransactionTemplate transactionTemplate;
    private final PaymentSupport support;

    @Value("${payment.sync-min-interval-seconds:10}")
    private long syncMinIntervalSeconds;

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentIntentResult createOrGetIntentForRental(Long rentalId) {
        Long paymentId = transactionTemplate.execute(status -> paymentRepository
                .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(Payment::getId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for rental: " + rentalId)));
        return createOrGetIntent(paymentId);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<PaymentSummary> syncAndGetSummaryForRental(Long rentalId) {
        Optional<PaymentSnapshot> snapshot = transactionTemplate.execute(status -> paymentRepository
                .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(PaymentSnapshot::of));
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }
        PaymentSnapshot current = snapshot.get();
        if (current.intentId() != null
                && (current.status() == PaymentStatus.PENDING || current.status() == PaymentStatus.FAILED)
                && claimSyncSlot(current.id())) {
            try {
                GatewayIntent intent = paymentGateway.retrieveIntent(current.intentId());
                transactionTemplate.executeWithoutResult(status -> paymentRepository.findByIdForUpdate(current.id())
                        .filter(payment -> current.intentId().equals(payment.getStripePaymentIntentId()))
                        .ifPresent(payment -> stateService.applyIntent(payment, intent)));
            } catch (RuntimeException e) {
                log.warn("Could not refresh payment {} from the gateway: {}", current.id(), e.getMessage());
            }
        }
        return transactionTemplate.execute(status -> paymentRepository
                .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(this::toSummary));
    }

    private boolean claimSyncSlot(Long paymentId) {
        return Boolean.TRUE.equals(support.isolated().execute(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
            if (payment == null) {
                return false;
            }
            Instant now = Instant.now(clock);
            Instant last = payment.getLastSyncedAt();
            if (last != null && last.plusSeconds(syncMinIntervalSeconds).isAfter(now)) {
                return false;
            }
            payment.markSynced(now);
            paymentRepository.save(payment);
            return true;
        }));
    }

    private PaymentIntentResult createOrGetIntent(Long paymentId) {
        for (int round = 0; round < MAX_INTENT_ROUNDS; round++) {
            PaymentSnapshot snapshot = transactionTemplate.execute(status -> PaymentSnapshot.of(support.requirePayment(paymentId)));
            if (snapshot.closed() || !PAYABLE.contains(snapshot.status())) {
                throw notPayable(snapshot.status(), snapshot.closed());
            }

            int attempt = snapshot.attempt();
            GatewayIntent intent;
            if (snapshot.intentId() == null) {
                intent = newIntent(snapshot, attempt);
            } else {
                intent = paymentGateway.retrieveIntent(snapshot.intentId());
                if (intent.status() == GatewayIntentStatus.CANCELED) {
                    Integer restarted = transactionTemplate.execute(status -> {
                        Payment payment = support.requirePaymentForUpdate(paymentId);
                        if (payment.isClosed()) {
                            throw notPayable(payment.getStatus(), true);
                        }
                        if (!Objects.equals(payment.getStripePaymentIntentId(), snapshot.intentId())) {
                            return null;
                        }
                        payment.restart();
                        paymentRepository.save(payment);
                        return payment.getIntentAttempt();
                    });
                    if (restarted == null) {
                        continue;
                    }
                    attempt = restarted;
                    intent = newIntent(snapshot, attempt);
                }
            }

            GatewayIntent resolved = intent;
            String key = keys.forIntent(snapshot.rentalId(), paymentId, attempt);
            IntentStep step;
            try {
                step = transactionTemplate.execute(status -> {
                    Payment payment = support.requirePaymentForUpdate(paymentId);
                    if (payment.isClosed()) {
                        throw notPayable(payment.getStatus(), true);
                    }
                    String stored = payment.getStripePaymentIntentId();
                    if (stored != null && !stored.equals(resolved.id())) {
                        return IntentStep.other(stored);
                    }
                    if (stored == null) {
                        payment.attachIntent(resolved.id(), key);
                    }
                    stateService.applyIntent(payment, resolved);
                    return IntentStep.attached(toIntentResult(payment, resolved));
                });
            } catch (InvalidPaymentStateException closed) {
                cancelOrphan(resolved.id());
                throw closed;
            }
            if (step.result() != null) {
                return step.result();
            }

            cancelOrphan(resolved.id());
            GatewayIntent stored = paymentGateway.retrieveIntent(step.storedIntentId());
            if (stored.status() == GatewayIntentStatus.CANCELED) {
                continue;
            }
            PaymentIntentResult adopted = transactionTemplate.execute(status -> {
                Payment payment = support.requirePaymentForUpdate(paymentId);
                if (!step.storedIntentId().equals(payment.getStripePaymentIntentId())) {
                    return null;
                }
                stateService.applyIntent(payment, stored);
                return toIntentResult(payment, stored);
            });
            if (adopted != null) {
                return adopted;
            }
        }
        throw new InvalidPaymentStateException("Payment intent is being replaced, retry shortly");
    }

    private static InvalidPaymentStateException notPayable(PaymentStatus status, boolean closed) {
        return new InvalidPaymentStateException(closed
                ? "Payment is closed because its rental was cancelled"
                : "Payment cannot be started in status: " + status.name());
    }

    private void cancelOrphan(String intentId) {
        try {
            PaymentResult result = paymentGateway.cancel(intentId, keys.forOrphanCancel(intentId));
            log.warn("Cancelled an untracked payment intent created by a concurrent request: {} (success={})",
                    intentId, result.success());
        } catch (RuntimeException e) {
            log.error("Could not cancel the untracked payment intent {}: {}", intentId, e.getMessage());
        }
    }

    private GatewayIntent newIntent(PaymentSnapshot snapshot, int attempt) {
        IntentCommand command = new IntentCommand(
                snapshot.id(),
                snapshot.rentalId(),
                snapshot.amount(),
                snapshot.currency(),
                snapshot.userEmail(),
                snapshot.customerId(),
                "Car rental #" + snapshot.rentalId(),
                keys.forCustomer(snapshot.rentalId()));
        return paymentGateway.createIntent(command, keys.forIntent(snapshot.rentalId(), snapshot.id(), attempt));
    }

    private record IntentStep(PaymentIntentResult result, String storedIntentId) {

        static IntentStep attached(PaymentIntentResult result) {
            return new IntentStep(result, null);
        }

        static IntentStep other(String storedIntentId) {
            return new IntentStep(null, storedIntentId);
        }
    }

    private PaymentIntentResult toIntentResult(Payment payment, GatewayIntent intent) {
        String publishable = stripeConfig.getPublishableKey();
        return new PaymentIntentResult(
                payment.getId(),
                payment.getRentalId(),
                intent.id(),
                intent.clientSecret(),
                publishable == null || publishable.isBlank() ? null : publishable,
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getCurrency());
    }

    private PaymentSummary toSummary(Payment payment) {
        return new PaymentSummary(
                payment.getId(),
                payment.getRentalId(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRefundedAmount(),
                payment.getFailureReason());
    }

    private record PaymentSnapshot(
            Long id,
            Long rentalId,
            String userEmail,
            BigDecimal amount,
            com.akif.shared.enums.CurrencyType currency,
            PaymentStatus status,
            String intentId,
            String customerId,
            int attempt,
            boolean closed) {

        static PaymentSnapshot of(Payment payment) {
            return new PaymentSnapshot(
                    payment.getId(),
                    payment.getRentalId(),
                    payment.getUserEmail(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getStatus(),
                    payment.getStripePaymentIntentId(),
                    payment.getStripeCustomerId(),
                    payment.getIntentAttempt(),
                    payment.isClosed());
        }
    }
}
