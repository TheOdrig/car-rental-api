package com.akif.payment.internal.service;

import com.akif.payment.api.CreatePaymentRequest;
import com.akif.payment.api.PaymentAttentionEvent;
import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.mapper.PaymentMapper;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import com.akif.payment.internal.service.gateway.GatewayIntentStatus;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentLifecycleService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final IdempotencyKeyGenerator keys;
    private final PaymentStateService stateService;
    private final TransactionTemplate transactionTemplate;
    private final PaymentSupport support;

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentDto createPayment(CreatePaymentRequest request) {
        PaymentKind kind = switch (String.valueOf(request.paymentMethod())) {
            case "PENALTY" -> PaymentKind.PENALTY;
            case "DAMAGE" -> PaymentKind.DAMAGE;
            default -> PaymentKind.RENTAL;
        };
        TransactionTemplate template = kind == PaymentKind.RENTAL ? transactionTemplate : support.isolated();
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

    public Optional<PaymentDto> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .filter(p -> !p.getIsDeleted())
                .map(paymentMapper::toDto);
    }

    public Optional<PaymentDto> getPaymentByRentalId(Long rentalId) {
        return paymentRepository.findByRentalIdAndKindAndIsDeletedFalse(rentalId, PaymentKind.RENTAL)
                .map(paymentMapper::toDto);
    }

    public Optional<PaymentDto> getPenaltyPaymentByRentalId(Long rentalId) {
        return paymentRepository.findCapturedPenaltyPayments(rentalId).stream()
                .findFirst()
                .map(paymentMapper::toDto);
    }

    @Transactional
    public PaymentResult capturePayment(Long paymentId) {
        Payment payment = support.requirePaymentForUpdate(paymentId);

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

    @Transactional
    public PaymentResult cancelPayment(Long paymentId) {
        Payment payment = support.requirePaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            payment.close();
            paymentRepository.save(payment);
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
        payment.close();
        paymentRepository.save(payment);
        return PaymentResult.success(payment.getTransactionId(), "Payment cancelled");
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public int closeOutstandingPenaltyPayments(Long rentalId) {
        List<Long> openIds = support.isolated().execute(status -> paymentRepository.findOpenPenaltyPaymentsForUpdate(rentalId)
                .stream().map(Payment::getId).toList());
        int closed = 0;
        for (Long paymentId : openIds) {
            if (closeOutstandingPenaltyPayment(paymentId)) {
                closed++;
            }
        }
        if (closed > 0) {
            log.info("Closed {} outstanding penalty payment(s) of rental {}", closed, rentalId);
        }
        return closed;
    }

    private boolean closeOutstandingPenaltyPayment(Long paymentId) {
        String intentId = paymentRepository.findById(paymentId).map(Payment::getStripePaymentIntentId).orElse(null);
        GatewayIntent resolved = null;
        boolean lookupFailed = false;
        try {
            Optional<GatewayIntent> found = intentId != null
                    ? Optional.of(paymentGateway.retrieveIntent(intentId))
                    : paymentGateway.findIntentByPayment(paymentId);
            if (found.isPresent()) {
                resolved = found.get();
                if (resolved.status() != GatewayIntentStatus.SUCCEEDED
                        && resolved.status() != GatewayIntentStatus.CANCELED) {
                    paymentGateway.cancel(resolved.id(), keys.forOrphanCancel(resolved.id()));
                }
            }
        } catch (RuntimeException e) {
            log.warn("Could not resolve the penalty payment {} with the gateway before closing it: {}",
                    paymentId, e.getMessage());
            lookupFailed = true;
        }
        GatewayIntent outcome = resolved;
        boolean failed = lookupFailed;
        return Boolean.TRUE.equals(support.isolated().execute(status -> {
            Optional<Payment> locked = paymentRepository.findByIdForUpdate(paymentId);
            if (locked.isEmpty()) {
                return false;
            }
            Payment payment = locked.get();
            if (payment.isClosed() || payment.getStatus() == PaymentStatus.CAPTURED
                    || payment.getStatus() == PaymentStatus.REFUNDED) {
                return false;
            }
            if (outcome != null && outcome.status() == GatewayIntentStatus.SUCCEEDED) {
                stateService.applyIntent(payment, outcome);
                if (payment.getStatus() == PaymentStatus.CAPTURED) {
                    return false;
                }
            }
            if (failed) {
                stateService.publishAttention(payment, PaymentAttentionEvent.Reason.CHARGE_OUTCOME_UNKNOWN);
            }
            payment.cancel();
            payment.close();
            paymentRepository.save(payment);
            return true;
        }));
    }
}
