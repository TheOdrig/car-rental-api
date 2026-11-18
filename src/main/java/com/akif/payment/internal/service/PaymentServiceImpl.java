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
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.config.StripeConfig;
import com.akif.payment.internal.mapper.PaymentMapper;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.IntentCommand;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.EnumSet;
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

    private final Clock clock;

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final IdempotencyKeyGenerator keys;
    private final PaymentStateService stateService;
    private final StripeConfig stripeConfig;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        PaymentKind kind = switch (String.valueOf(request.paymentMethod())) {
            case "PENALTY" -> PaymentKind.PENALTY;
            case "DAMAGE" -> PaymentKind.DAMAGE;
            default -> PaymentKind.RENTAL;
        };

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
                && (current.status() == PaymentStatus.PENDING || current.status() == PaymentStatus.FAILED)) {
            try {
                GatewayIntent intent = paymentGateway.retrieveIntent(current.intentId());
                transactionTemplate.executeWithoutResult(status -> paymentRepository.findByIdForUpdate(current.id())
                        .ifPresent(payment -> stateService.applyIntent(payment, intent)));
            } catch (RuntimeException e) {
                log.warn("Could not refresh payment {} from the gateway: {}", current.id(), e.getMessage());
            }
        }
        return transactionTemplate.execute(status -> paymentRepository
                .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(this::toSummary));
    }

    private PaymentIntentResult createOrGetIntent(Long paymentId) {
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
                attempt = transactionTemplate.execute(status -> {
                    Payment payment = requirePaymentForUpdate(paymentId);
                    payment.restart();
                    paymentRepository.save(payment);
                    return payment.getIntentAttempt();
                });
                intent = newIntent(snapshot, attempt);
            }
        }

        GatewayIntent resolved = intent;
        String key = keys.forIntent(snapshot.rentalId(), paymentId, attempt);
        return transactionTemplate.execute(status -> {
            Payment payment = requirePaymentForUpdate(paymentId);
            if (payment.getStripePaymentIntentId() == null) {
                payment.attachIntent(resolved.id(), key);
            }
            stateService.applyIntent(payment, resolved);
            return toIntentResult(payment, resolved);
        });
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
    @Transactional
    public PaymentResult chargePayment(Long paymentId) {
        Payment payment = requirePaymentForUpdate(paymentId);
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return PaymentResult.success(payment.getTransactionId(), "Payment already captured");
        }
        Optional<Payment> base = paymentRepository
                .findByRentalIdAndKindAndIsDeletedFalse(payment.getRentalId(), PaymentKind.RENTAL);
        return chargeSaved(payment, base.orElse(null), "Penalty for rental #" + payment.getRentalId());
    }

    @Override
    @Transactional
    public PaymentResult chargeDamage(Long rentalId, Long damageId, BigDecimal amount) {
        Optional<Payment> base = paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL);
        if (base.isEmpty()) {
            return PaymentResult.failure("No rental payment on record for rental " + rentalId);
        }
        Payment rentalPayment = base.get();

        Payment damagePayment = paymentRepository
                .findByKindAndReferenceIdAndIsDeletedFalse(PaymentKind.DAMAGE, damageId)
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .rentalId(rentalId)
                        .kind(PaymentKind.DAMAGE)
                        .referenceId(damageId)
                        .userEmail(rentalPayment.getUserEmail())
                        .carLicensePlate(rentalPayment.getCarLicensePlate())
                        .amount(amount)
                        .currency(rentalPayment.getCurrency())
                        .status(PaymentStatus.PENDING)
                        .paymentMethod("DAMAGE")
                        .isDeleted(false)
                        .build()));

        if (damagePayment.getStatus() == PaymentStatus.CAPTURED) {
            return PaymentResult.success(damagePayment.getTransactionId(), "Damage already charged");
        }
        if (damagePayment.getAmount().compareTo(amount) != 0) {
            return PaymentResult.failure("Damage amount changed after the charge was created");
        }
        return chargeSaved(damagePayment, rentalPayment, "Damage report #" + damageId);
    }

    private PaymentResult chargeSaved(Payment payment, Payment base, String description) {
        if (base == null || base.getStripeCustomerId() == null || base.getStripePaymentMethodId() == null) {
            payment.markFailed("No saved payment method for this rental", null);
            paymentRepository.save(payment);
            return PaymentResult.failure("No saved payment method for this rental");
        }
        if (base.getCurrency() != payment.getCurrency()) {
            payment.markFailed("Currency differs from the rental payment", null);
            paymentRepository.save(payment);
            return PaymentResult.failure("Currency differs from the rental payment");
        }

        ChargeCommand command = new ChargeCommand(
                payment.getId(),
                payment.getRentalId(),
                payment.getAmount(),
                payment.getCurrency(),
                base.getStripeCustomerId(),
                base.getStripePaymentMethodId(),
                description);
        PaymentResult result = paymentGateway.chargeSavedMethod(
                command,
                keys.forOffSessionCharge(payment.getRentalId(), payment.getId(), payment.getIntentAttempt()));

        if (result.success()) {
            payment.rememberCustomer(base.getStripeCustomerId());
            payment.rememberPaymentMethod(base.getStripePaymentMethodId());
            stateService.markCaptured(payment, result.transactionId());
        } else {
            payment.markFailed(result.message(), null);
            payment.nextAttempt();
            paymentRepository.save(payment);
        }
        return result;
    }

    @Override
    @Transactional
    public PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount) {
        Payment payment = requirePaymentForUpdate(paymentId);
        return refundInternal(payment, refundAmount);
    }

    @Override
    @Transactional
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        Optional<Payment> payment = paymentRepository.findByIntentIdForUpdate(transactionId);
        if (payment.isEmpty()) {
            return PaymentResult.failure("No payment found for transaction");
        }
        return refundInternal(payment.get(), amount);
    }

    private PaymentResult refundInternal(Payment payment, BigDecimal refundAmount) {
        if (!payment.canRefund()) {
            return PaymentResult.failure("Payment cannot be refunded, current status: " + payment.getStatus());
        }
        if (payment.getStripePaymentIntentId() == null) {
            return PaymentResult.failure("Payment has no gateway reference");
        }
        if (refundAmount == null || refundAmount.signum() <= 0) {
            return PaymentResult.failure("Refund amount must be positive");
        }
        if (refundAmount.compareTo(payment.refundableBalance()) > 0) {
            return PaymentResult.failure("Refund amount exceeds the refundable balance");
        }

        BigDecimal previousTotal = payment.refundedTotal();
        PaymentResult result = paymentGateway.refund(
                payment.getStripePaymentIntentId(),
                refundAmount,
                payment.getCurrency(),
                keys.forRefund(payment.getRentalId(), payment.getId(), payment.getRefundCount()));

        if (result.success()) {
            payment.recordRefundIssued(previousTotal, refundAmount);
            paymentRepository.save(payment);
            log.info("Refunded payment: ID={}, amount={}", payment.getId(), refundAmount);
        } else {
            log.warn("Refund failed: ID={}, Reason: {}", payment.getId(), result.message());
        }
        return result;
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
