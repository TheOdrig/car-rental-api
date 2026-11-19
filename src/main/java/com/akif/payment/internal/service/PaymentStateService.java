package com.akif.payment.internal.service;

import com.akif.payment.api.PaymentAttentionEvent;
import com.akif.payment.api.PaymentCancelledEvent;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.payment.api.PenaltyPaymentCapturedEvent;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.GatewayIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStateService {

    private static final int MAX_REASON_LENGTH = 500;

    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public boolean applyIntent(Payment payment, GatewayIntent intent) {
        boolean changed = false;
        if (payment.getStripePaymentIntentId() == null) {
            payment.attachIntent(intent.id(), payment.getIdempotencyKey());
            changed = true;
        }
        if (intent.customerId() != null && !intent.customerId().equals(payment.getStripeCustomerId())) {
            payment.rememberCustomer(intent.customerId());
            changed = true;
        }
        switch (intent.status()) {
            case REQUIRES_CAPTURE -> {
                payment.rememberPaymentMethod(intent.paymentMethodId());
                changed |= payment.authorize();
            }
            case SUCCEEDED -> {
                payment.rememberPaymentMethod(intent.paymentMethodId());
                changed |= markCaptured(payment, intent.id());
            }
            case CANCELED -> changed |= payment.cancel();
            case REQUIRES_PAYMENT_METHOD -> changed |= payment.reopen();
            default -> {
            }
        }
        if (changed) {
            paymentRepository.save(payment);
        }
        return changed;
    }

    public boolean markCaptured(Payment payment, String transactionId) {
        boolean transitioned = payment.capture();
        boolean late = false;
        if (!transitioned && isLateCaptureKind(payment)) {
            late = payment.captureAfterClose();
        }
        if (transitioned || late) {
            payment.stampCaptured(LocalDateTime.now(clock));
        }
        if (transactionId != null && payment.getStripePaymentIntentId() == null) {
            payment.attachIntent(transactionId, payment.getIdempotencyKey());
        }
        boolean duplicate = transitioned && payment.getKind() == PaymentKind.PENALTY
                && paymentRepository.existsCapturedPenaltyOtherThan(payment.getRentalId(), payment.getId());
        if (duplicate) {
            payment.close();
        }
        if (!transitioned && !late && payment.getStatus() == PaymentStatus.CANCELLED) {
            log.error("[AUDIT] Funds were captured for a cancelled payment, a manual refund may be required | Payment: {} | Kind: {}",
                    payment.getId(), payment.getKind());
        }
        paymentRepository.save(payment);
        if (late) {
            log.error("[AUDIT] Funds were captured for a closed payment, refunding them | Payment: {} | Kind: {}",
                    payment.getId(), payment.getKind());
            eventPublisher.publishEvent(new RefundRequiredEvent(payment.getId(), RefundRequiredEvent.Cause.LATE_CAPTURE));
        } else if (duplicate) {
            log.error("[AUDIT] A second penalty was captured for the rental, refunding it | Payment: {} | Rental: {}",
                    payment.getId(), payment.getRentalId());
            eventPublisher.publishEvent(new RefundRequiredEvent(payment.getId(), RefundRequiredEvent.Cause.DUPLICATE_PENALTY));
        } else if (transitioned && payment.getKind() == PaymentKind.RENTAL) {
            eventPublisher.publishEvent(new PaymentCapturedEvent(
                    payment.getId(),
                    payment.getRentalId(),
                    payment.getUserEmail(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getTransactionId(),
                    LocalDateTime.now(clock)));
        } else if (transitioned && payment.getKind() == PaymentKind.PENALTY) {
            eventPublisher.publishEvent(new PenaltyPaymentCapturedEvent(
                    payment.getId(),
                    payment.getRentalId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    LocalDateTime.now(clock)));
        }
        return transitioned || late;
    }

    public void flagUnknownOutcome(Payment payment) {
        payment.flagUnknownOutcome();
        paymentRepository.save(payment);
        publishAttention(payment, PaymentAttentionEvent.Reason.CHARGE_OUTCOME_UNKNOWN);
    }

    public void publishAttention(Payment payment, PaymentAttentionEvent.Reason reason) {
        eventPublisher.publishEvent(new PaymentAttentionEvent(
                payment.getId(),
                payment.getRentalId(),
                reason,
                payment.getAmount(),
                payment.getCurrency(),
                LocalDateTime.now(clock)));
    }

    private static boolean isLateCaptureKind(Payment payment) {
        return payment.getKind() == PaymentKind.PENALTY || payment.getKind() == PaymentKind.DAMAGE;
    }

    public boolean applyFailure(Payment payment, String reason) {
        boolean changed = payment.fail(truncate(reason));
        if (changed) {
            paymentRepository.save(payment);
        }
        return changed;
    }

    public boolean applyCancellation(Payment payment) {
        boolean changed = payment.cancel();
        if (changed) {
            paymentRepository.save(payment);
        }
        return changed;
    }

    public boolean applyRemoteCancellation(Payment payment) {
        boolean changed = applyCancellation(payment);
        if (changed && payment.getKind() == PaymentKind.RENTAL) {
            eventPublisher.publishEvent(new PaymentCancelledEvent(
                    payment.getId(),
                    payment.getRentalId(),
                    payment.getUserEmail(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    LocalDateTime.now(clock)));
        }
        return changed;
    }

    public boolean reverseRefund(Payment payment, BigDecimal amount) {
        BigDecimal before = payment.refundedTotal();
        PaymentStatus statusBefore = payment.getStatus();
        payment.reverseRefund(amount);
        boolean changed = payment.refundedTotal().compareTo(before) != 0 || payment.getStatus() != statusBefore;
        if (changed) {
            paymentRepository.save(payment);
        }
        return changed;
    }

    public boolean applyRefundTotal(Payment payment, BigDecimal total) {
        BigDecimal before = payment.refundedTotal();
        PaymentStatus statusBefore = payment.getStatus();
        payment.applyRefundTotal(total);
        boolean changed = payment.refundedTotal().compareTo(before) != 0 || payment.getStatus() != statusBefore;
        if (changed) {
            paymentRepository.save(payment);
        }
        return changed;
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "Payment failed";
        }
        return reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
    }
}
