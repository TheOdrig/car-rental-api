package com.akif.payment.internal.service;

import java.time.Clock;

import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.InvalidPaymentStateException;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentNotFoundException;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.PaymentSummary;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.PaymentRefund;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.domain.enums.RefundStatus;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.mapper.PaymentMapper;
import com.akif.payment.internal.repository.PaymentRefundRepository;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentServiceImpl implements PaymentService {

    private static final Set<PaymentStatus> PAYABLE = EnumSet.of(
            PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.AUTHORIZED, PaymentStatus.CANCELLED);

    private static final int MAX_INTENT_ROUNDS = 4;

    private final Clock clock;

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentMapper paymentMapper;
    private final IdempotencyKeyGenerator keys;
    private final PaymentStateService stateService;
    private final StripeConfig stripeConfig;
    private final TransactionTemplate transactionTemplate;
    private final PlatformTransactionManager transactionManager;

    @Value("${payment.sync-min-interval-seconds:10}")
    private long syncMinIntervalSeconds;

    @Value("${payment.isolated-transactions:true}")
    private boolean isolatedTransactions;

    private TransactionTemplate isolated() {
        if (!isolatedTransactions) {
            return transactionTemplate;
        }
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentDto createPayment(CreatePaymentRequest request) {
        PaymentKind kind = switch (String.valueOf(request.paymentMethod())) {
            case "PENALTY" -> PaymentKind.PENALTY;
            case "DAMAGE" -> PaymentKind.DAMAGE;
            default -> PaymentKind.RENTAL;
        };
        TransactionTemplate template = kind == PaymentKind.RENTAL ? transactionTemplate : isolated();
        return template.execute(status -> {
            if (kind == PaymentKind.PENALTY) {
                Optional<Payment> reusable = paymentRepository
                        .findFirstByRentalIdAndKindAndIsDeletedFalseOrderByIdDesc(request.rentalId(), kind)
                        .filter(p -> (p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.FAILED)
                                && p.getAmount().compareTo(request.amount()) == 0
                                && p.getCurrency() == request.currency());
                if (reusable.isPresent()) {
                    return paymentMapper.toDto(reusable.get());
                }
            }
            Payment payment = Payment.builder()
                    .rentalId(request.rentalId())
                    .kind(kind)
                    .userEmail(request.userEmail())
                    .carLicensePlate(request.carLicensePlate())
                    .amount(request.amount())
                    .currency(request.currency())
                    .status(PaymentStatus.PENDING)
                    .paymentMethod(request.paymentMethod())
                    .isDeleted(false)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Created payment: ID={}, Rental ID={}, Kind={}", savedPayment.getId(), request.rentalId(), kind);
            return paymentMapper.toDto(savedPayment);
        });
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentIntentResult createOrGetIntentForRental(Long rentalId) {
        Long paymentId = transactionTemplate.execute(status -> paymentRepository
                .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(Payment::getId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for rental: " + rentalId)));
        return createOrGetIntent(paymentId);
    }

    @Override
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
        return Boolean.TRUE.equals(isolated().execute(status -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
            if (payment == null) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime last = payment.getLastSyncedAt();
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
            PaymentSnapshot snapshot = transactionTemplate.execute(status -> PaymentSnapshot.of(requirePayment(paymentId)));
            if (!PAYABLE.contains(snapshot.status())) {
                throw new InvalidPaymentStateException(
                        "Payment cannot be started in status: " + snapshot.status().name());
            }

            int attempt = snapshot.attempt();
            GatewayIntent intent;
            if (snapshot.intentId() == null) {
                intent = newIntent(snapshot, attempt);
            } else {
                intent = paymentGateway.retrieveIntent(snapshot.intentId());
                if (intent.status() == GatewayIntentStatus.CANCELED) {
                    Integer restarted = transactionTemplate.execute(status -> {
                        Payment payment = requirePaymentForUpdate(paymentId);
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
            IntentStep step = transactionTemplate.execute(status -> {
                Payment payment = requirePaymentForUpdate(paymentId);
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
            if (step.result() != null) {
                return step.result();
            }

            cancelOrphan(resolved.id());
            GatewayIntent stored = paymentGateway.retrieveIntent(step.storedIntentId());
            if (stored.status() == GatewayIntentStatus.CANCELED) {
                continue;
            }
            PaymentIntentResult adopted = transactionTemplate.execute(status -> {
                Payment payment = requirePaymentForUpdate(paymentId);
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

    @Override
    public Optional<PaymentDto> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> !p.getIsDeleted())
                .map(paymentMapper::toDto);
    }

    @Override
    public Optional<PaymentDto> getPaymentByRentalId(Long rentalId) {
        return paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(paymentMapper::toDto);
    }

    @Override
    public Optional<PaymentDto> getPenaltyPaymentByRentalId(Long rentalId) {
        return paymentRepository.findCapturedPenaltyPayments(rentalId).stream()
                .findFirst()
                .map(paymentMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentResult capturePayment(Long paymentId) {
        Payment payment = requirePaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return PaymentResult.success(payment.getTransactionId(), "Payment already captured");
        }
        if (payment.getStatus() != PaymentStatus.AUTHORIZED || payment.getStripePaymentIntentId() == null) {
            return PaymentResult.failure("Payment is not authorized, current status: " + payment.getStatus());
        }

        PaymentResult result = paymentGateway.capture(
                payment.getStripePaymentIntentId(),
                keys.forCapture(payment.getRentalId(), payment.getId(), payment.getIntentAttempt()));

        if (result.success()) {
            stateService.markCaptured(payment, result.transactionId());
            log.info("Captured payment: ID={}, Rental={}", payment.getId(), payment.getRentalId());
        } else {
            log.warn("Capture failed: ID={}, Reason: {}", payment.getId(), result.message());
        }
        return result;
    }

    @Override
    @Transactional
    public PaymentResult cancelPayment(Long paymentId) {
        Payment payment = requirePaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return PaymentResult.success(payment.getTransactionId(), "Payment already cancelled");
        }
        if (payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.AUTHORIZED
                && payment.getStatus() != PaymentStatus.FAILED) {
            return PaymentResult.failure("Payment cannot be cancelled, current status: " + payment.getStatus());
        }

        if (payment.getStripePaymentIntentId() != null) {
            PaymentResult result = paymentGateway.cancel(
                    payment.getStripePaymentIntentId(),
                    keys.forCancel(payment.getRentalId(), payment.getId(), payment.getIntentAttempt()));
            if (!result.success()) {
                log.warn("Cancel failed: ID={}, Reason: {}", payment.getId(), result.message());
                return result;
            }
        }
        stateService.applyCancellation(payment);
        return PaymentResult.success(payment.getTransactionId(), "Payment cancelled");
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult chargePayment(Long paymentId) {
        ChargePlan plan = isolated().execute(status -> {
            Payment payment = requirePaymentForUpdate(paymentId);
            Optional<Payment> base = paymentRepository
                    .findByRentalIdAndKindAndIsDeletedFalse(payment.getRentalId(), PaymentKind.RENTAL);
            return planCharge(payment, base.orElse(null),
                    "Penalty for rental #" + payment.getRentalId());
        });
        return executeCharge(plan);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult chargeDamage(Long rentalId, Long damageId, BigDecimal amount, CurrencyType currency) {
        ChargePlan plan = isolated().execute(status -> {
            Optional<Payment> base = paymentRepository
                    .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL);
            if (base.isEmpty()) {
                return ChargePlan.done(PaymentResult.failure("No rental payment on record for rental " + rentalId));
            }
            Payment rentalPayment = base.get();
            CurrencyType damageCurrency = currency != null ? currency : rentalPayment.getCurrency();

            Payment damagePayment = paymentRepository
                    .findByKindAndReferenceIdAndIsDeletedFalse(PaymentKind.DAMAGE, damageId)
                    .orElseGet(() -> paymentRepository.save(Payment.builder()
                            .rentalId(rentalId)
                            .kind(PaymentKind.DAMAGE)
                            .referenceId(damageId)
                            .userEmail(rentalPayment.getUserEmail())
                            .carLicensePlate(rentalPayment.getCarLicensePlate())
                            .amount(amount)
                            .currency(damageCurrency)
                            .status(PaymentStatus.PENDING)
                            .paymentMethod("DAMAGE")
                            .isDeleted(false)
                            .build()));
            Payment locked = requirePaymentForUpdate(damagePayment.getId());

            if (locked.getStatus() == PaymentStatus.CAPTURED) {
                return ChargePlan.done(PaymentResult.success(locked.getTransactionId(), "Damage already charged"));
            }
            if (locked.getAmount().compareTo(amount) != 0) {
                return ChargePlan.done(PaymentResult.failure("Damage amount changed after the charge was created"));
            }
            if (locked.getCurrency() != damageCurrency) {
                return ChargePlan.done(PaymentResult.failure("Damage currency changed after the charge was created"));
            }
            return planCharge(locked, rentalPayment, "Damage report #" + damageId);
        });
        return executeCharge(plan);
    }

    private ChargePlan planCharge(Payment payment, Payment base, String description) {
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return ChargePlan.done(PaymentResult.success(payment.getTransactionId(), "Payment already captured"));
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return ChargePlan.done(PaymentResult.failure("Payment was refunded and cannot be charged again"));
        }
        if (base == null || base.getStripeCustomerId() == null || base.getStripePaymentMethodId() == null) {
            payment.markFailed("No saved payment method for this rental", null);
            paymentRepository.save(payment);
            return ChargePlan.done(PaymentResult.failure("No saved payment method for this rental"));
        }
        if (base.getCurrency() != payment.getCurrency()) {
            payment.markFailed("Currency differs from the rental payment", null);
            paymentRepository.save(payment);
            return ChargePlan.done(PaymentResult.failure("Currency differs from the rental payment"));
        }
        ChargeCommand command = new ChargeCommand(
                payment.getId(),
                payment.getRentalId(),
                payment.getAmount(),
                payment.getCurrency(),
                base.getStripeCustomerId(),
                base.getStripePaymentMethodId(),
                description);
        String key = payment.getKind() == PaymentKind.DAMAGE
                ? keys.forDamageCharge(payment.getReferenceId(), payment.getIntentAttempt())
                : keys.forPenaltyCharge(payment.getRentalId(), payment.getIntentAttempt());
        return new ChargePlan(null, payment.getId(), command, key, base.getStripeCustomerId(),
                base.getStripePaymentMethodId());
    }

    private PaymentResult executeCharge(ChargePlan plan) {
        if (plan.finished() != null) {
            return plan.finished();
        }
        ChargeResult outcome = paymentGateway.chargeSavedMethod(plan.command(), plan.key());
        return isolated().execute(status -> {
            Payment payment = requirePaymentForUpdate(plan.paymentId());
            if (outcome.success()) {
                payment.rememberCustomer(plan.customerId());
                payment.rememberPaymentMethod(plan.paymentMethodId());
                stateService.markCaptured(payment, outcome.result().transactionId());
            } else if (outcome.definitiveDecline()) {
                payment.markFailed(outcome.result().message(), null);
                payment.nextAttempt();
                paymentRepository.save(payment);
            } else {
                log.warn("Charge outcome is unknown, keeping the idempotency key: payment={}, reason={}",
                        payment.getId(), outcome.result().message());
            }
            return outcome.result();
        });
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount, String operationKey) {
        return refundInternal(() -> paymentRepository.findByIdForUpdate(paymentId), refundAmount, operationKey);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult refund(String transactionId, BigDecimal amount, String operationKey) {
        return refundInternal(() -> paymentRepository.findByIntentIdForUpdate(transactionId), amount, operationKey);
    }

    private PaymentResult refundInternal(java.util.function.Supplier<Optional<Payment>> locator,
                                         BigDecimal refundAmount, String operationKey) {
        RefundPlan plan = isolated().execute(status -> {
            Optional<Payment> located = locator.get();
            if (located.isEmpty()) {
                return RefundPlan.done(PaymentResult.failure("No payment found for transaction"));
            }
            Payment payment = located.get();
            Optional<PaymentRefund> already = refundRepository.findByPaymentIdAndOperationKeyAndStatus(
                    payment.getId(), operationKey, RefundStatus.SUCCEEDED);
            if (already.isPresent()) {
                String refundId = already.get().getStripeRefundId();
                return RefundPlan.done(PaymentResult.success(
                        refundId != null ? refundId : payment.getTransactionId(), "Refund already processed"));
            }
            if (!payment.canRefund()) {
                return RefundPlan.done(PaymentResult.failure(
                        "Payment cannot be refunded, current status: " + payment.getStatus()));
            }
            if (payment.getStripePaymentIntentId() == null) {
                return RefundPlan.done(PaymentResult.failure("Payment has no gateway reference"));
            }
            if (refundAmount == null || refundAmount.signum() <= 0) {
                return RefundPlan.done(PaymentResult.failure("Refund amount must be positive"));
            }
            BigDecimal others = refundRepository.sumSucceededExcludingOperation(payment.getId(), operationKey);
            BigDecimal target = others.add(refundAmount);
            if (target.compareTo(payment.getAmount()) > 0) {
                return RefundPlan.done(PaymentResult.failure("Refund amount exceeds the refundable balance"));
            }
            long failed = refundRepository.countByPaymentIdAndOperationKeyAndStatus(
                    payment.getId(), operationKey, RefundStatus.FAILED);
            return new RefundPlan(null, payment.getId(), payment.getStripePaymentIntentId(), payment.getCurrency(),
                    target, refundAmount, keys.forRefundOperation(operationKey, (int) failed), operationKey);
        });
        if (plan.finished() != null) {
            return plan.finished();
        }

        long alreadyRefundedMinor;
        try {
            alreadyRefundedMinor = paymentGateway.refundedMinor(plan.intentId());
        } catch (RuntimeException e) {
            log.warn("Could not read the refunded amount from the gateway: payment={}, reason={}",
                    plan.paymentId(), e.getMessage());
            return PaymentResult.failure("Could not verify the refunded amount with the gateway");
        }
        long missingMinor = StripeAmounts.toMinorUnits(plan.targetTotal(), plan.currency()) - alreadyRefundedMinor;

        String refundId = null;
        if (missingMinor > 0) {
            BigDecimal toRefund = StripeAmounts.fromMinorUnits(missingMinor, plan.currency());
            PaymentResult result = paymentGateway.refund(plan.intentId(), toRefund, plan.currency(), plan.key());
            if (!result.success()) {
                log.warn("Refund failed: payment={}, reason={}", plan.paymentId(), result.message());
                return result;
            }
            refundId = result.transactionId();
        }

        String recordedRefundId = refundId;
        return isolated().execute(status -> {
            Payment payment = requirePaymentForUpdate(plan.paymentId());
            boolean duplicate = recordedRefundId != null
                    && refundRepository.findByStripeRefundId(recordedRefundId).isPresent();
            if (!duplicate) {
                try {
                    refundRepository.saveAndFlush(PaymentRefund.builder()
                            .paymentId(payment.getId())
                            .operationKey(plan.operationKey())
                            .stripeRefundId(recordedRefundId)
                            .amount(plan.operationAmount())
                            .status(RefundStatus.SUCCEEDED)
                            .isDeleted(false)
                            .build());
                } catch (DataIntegrityViolationException e) {
                    log.info("Refund already recorded: payment={}, operation={}", payment.getId(), plan.operationKey());
                }
            }
            payment.applyRefundTotal(plan.targetTotal());
            paymentRepository.save(payment);
            log.info("Refunded payment: ID={}, operation={}, amount={}",
                    payment.getId(), plan.operationKey(), plan.operationAmount());
            return PaymentResult.success(
                    recordedRefundId != null ? recordedRefundId : payment.getTransactionId(), "Refund processed");
        });
    }

    private record ChargePlan(
            PaymentResult finished,
            Long paymentId,
            ChargeCommand command,
            String key,
            String customerId,
            String paymentMethodId) {

        static ChargePlan done(PaymentResult result) {
            return new ChargePlan(result, null, null, null, null, null);
        }
    }

    private record RefundPlan(
            PaymentResult finished,
            Long paymentId,
            String intentId,
            CurrencyType currency,
            BigDecimal targetTotal,
            BigDecimal operationAmount,
            String key,
            String operationKey) {

        static RefundPlan done(PaymentResult result) {
            return new RefundPlan(result, null, null, null, null, null, null, null);
        }
    }

    private record IntentStep(PaymentIntentResult result, String storedIntentId) {

        static IntentStep attached(PaymentIntentResult result) {
            return new IntentStep(result, null);
        }

        static IntentStep other(String storedIntentId) {
            return new IntentStep(null, storedIntentId);
        }
    }

    @Override
    public BigDecimal sumCapturedPaymentsBetween(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.sumCapturedPaymentsBetween(start, end);
    }

    @Override
    public List<DailyRevenueProjection> getDailyRevenue(int days) {
        LocalDateTime startDate = LocalDateTime.now(clock).minusDays(days);
        List<Payment> payments = paymentRepository.findByStatusAndCreateTimeAfter(
                PaymentStatus.CAPTURED, startDate);

        Map<LocalDate, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreateTime().toLocalDate()));

        return grouped.entrySet().stream()
                .map(entry -> new DailyRevenueProjectionImpl(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MonthlyRevenueProjection> getMonthlyRevenue(int months) {
        LocalDateTime startDate = LocalDateTime.now(clock).minusMonths(months);
        List<Payment> payments = paymentRepository.findByStatusAndCreateTimeAfter(
                PaymentStatus.CAPTURED, startDate);

        Map<YearMonth, List<Payment>> grouped = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getCreateTime())));

        return grouped.entrySet().stream()
                .map(entry -> new MonthlyRevenueProjectionImpl(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .sorted((a, b) -> a.getMonth().compareTo(b.getMonth()))
                .collect(Collectors.toList());
    }

    @Override
    public int countFailedPayments() {
        return paymentRepository.countByStatusAndIsDeletedFalse(PaymentStatus.FAILED);
    }

    private Payment requirePayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }

    private Payment requirePaymentForUpdate(Long paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
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
            int attempt) {

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
                    payment.getIntentAttempt());
        }
    }

    private record DailyRevenueProjectionImpl(
            LocalDate date,
            BigDecimal revenue,
            int rentalCount) implements DailyRevenueProjection {
        @Override
        public LocalDate getDate() { return date; }
        @Override
        public BigDecimal getRevenue() { return revenue; }
        @Override
        public int getRentalCount() { return rentalCount; }
    }

    private record MonthlyRevenueProjectionImpl(
            YearMonth month,
            BigDecimal revenue,
            int rentalCount) implements MonthlyRevenueProjection {
        @Override
        public YearMonth getMonth() { return month; }
        @Override
        public BigDecimal getRevenue() { return revenue; }
        @Override
        public int getRentalCount() { return rentalCount; }
    }
}
