package com.akif.payment.internal.service;

import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.ChargeCommand;
import com.akif.payment.internal.service.gateway.ChargeResult;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentChargeService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyGenerator keys;
    private final PaymentStateService stateService;
    private final PaymentSupport support;

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult chargePayment(Long paymentId) {
        ChargePlan plan = support.isolated().execute(status -> {
            Payment payment = support.requirePaymentForUpdate(paymentId);
            if (payment.getKind() == PaymentKind.PENALTY
                    && paymentRepository.existsCapturedPenaltyOtherThan(payment.getRentalId(), payment.getId())) {
                payment.cancel();
                payment.close();
                paymentRepository.save(payment);
                return ChargePlan.done(PaymentResult.failure("A penalty of this rental was already collected"));
            }
            Optional<Payment> base = paymentRepository
                    .findByRentalIdAndKindAndIsDeletedFalse(payment.getRentalId(), PaymentKind.RENTAL);
            return planCharge(payment, base.orElse(null),
                    "Penalty for rental #" + payment.getRentalId());
        });
        return executeCharge(plan);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult chargeDamage(Long rentalId, Long damageId, BigDecimal amount, CurrencyType currency) {
        ChargePlan plan = support.isolated().execute(status -> {
            Optional<Payment> base = paymentRepository
                    .findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL);
            if (base.isEmpty()) {
                return ChargePlan.done(PaymentResult.failure("No rental payment on record for rental " + rentalId));
            }
            Payment rentalPayment = base.get();
            CurrencyType damageCurrency = currency != null ? currency : rentalPayment.getCurrency();

            java.util.function.Supplier<Payment> newDamagePayment = () -> paymentRepository.save(Payment.builder()
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
                    .build());
            Payment damagePayment = paymentRepository
                    .findByKindAndReferenceIdAndIsDeletedFalse(PaymentKind.DAMAGE, damageId)
                    .orElseGet(newDamagePayment);
            Payment locked = support.requirePaymentForUpdate(damagePayment.getId());

            if (locked.getStatus() == PaymentStatus.CAPTURED) {
                return ChargePlan.done(PaymentResult.success(locked.getTransactionId(), "Damage already charged"));
            }
            if (locked.getAmount().compareTo(amount) != 0 || locked.getCurrency() != damageCurrency) {
                if (locked.getStatus() != PaymentStatus.FAILED && locked.getStatus() != PaymentStatus.CANCELLED) {
                    return ChargePlan.done(PaymentResult.failure(
                            "Damage amount or currency changed while a charge is in progress"));
                }
                locked.cancel();
                locked.close();
                locked.softDelete();
                paymentRepository.saveAndFlush(locked);
                locked = support.requirePaymentForUpdate(newDamagePayment.get().getId());
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
                : keys.forPenaltyCharge(payment.getRentalId(), payment.getId(), payment.getIntentAttempt());
        return new ChargePlan(null, payment.getId(), command, key, base.getStripeCustomerId(),
                base.getStripePaymentMethodId());
    }

    private PaymentResult executeCharge(ChargePlan plan) {
        if (plan.finished() != null) {
            return plan.finished();
        }
        ChargeResult outcome = paymentGateway.chargeSavedMethod(plan.command(), plan.key());
        return support.isolated().execute(status -> {
            Payment payment = support.requirePaymentForUpdate(plan.paymentId());
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
                stateService.flagUnknownOutcome(payment);
                return PaymentResult.unknown(outcome.result().message());
            }
            return outcome.result();
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
}
