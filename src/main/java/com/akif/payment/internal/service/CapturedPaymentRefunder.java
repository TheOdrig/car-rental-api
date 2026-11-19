package com.akif.payment.internal.service;

import com.akif.payment.api.PaymentAttentionEvent;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
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
public class CapturedPaymentRefunder {

    static final String OPERATION_KEY = "auto-refund";

    private final Clock clock;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    public boolean refund(Long paymentId, RefundRequiredEvent.Cause cause) {
        Payment payment = paymentRepository.findById(paymentId).filter(p -> !p.getIsDeleted()).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.CAPTURED) {
            return true;
        }
        BigDecimal balance = payment.refundableBalance();
        if (balance.signum() <= 0) {
            return true;
        }
        PaymentResult result = paymentService.refundPayment(payment.getId(), balance, OPERATION_KEY);
        if (result.success()) {
            log.warn("[AUDIT] Auto-refunded a charge that must not be kept | Payment: {} | Kind: {} | Cause: {} | Amount: {}",
                    payment.getId(), payment.getKind(), cause, balance);
            publish(payment, cause == RefundRequiredEvent.Cause.DUPLICATE_PENALTY
                    ? PaymentAttentionEvent.Reason.DUPLICATE_PENALTY_REFUNDED
                    : PaymentAttentionEvent.Reason.LATE_CAPTURE_REFUNDED);
            return true;
        }
        log.error("[AUDIT] Auto-refund failed, a manual refund may be required | Payment: {} | Reason: {}",
                payment.getId(), result.message());
        publish(payment, PaymentAttentionEvent.Reason.AUTO_REFUND_FAILED);
        return false;
    }

    public RefundRequiredEvent.Cause causeOf(Payment payment) {
        boolean duplicate = payment.getKind() == PaymentKind.PENALTY
                && paymentRepository.existsCapturedPenaltyOtherThan(payment.getRentalId(), payment.getId());
        return duplicate ? RefundRequiredEvent.Cause.DUPLICATE_PENALTY : RefundRequiredEvent.Cause.LATE_CAPTURE;
    }

    private void publish(Payment payment, PaymentAttentionEvent.Reason reason) {
        eventPublisher.publishEvent(new PaymentAttentionEvent(
                payment.getId(),
                payment.getRentalId(),
                reason,
                payment.getAmount(),
                payment.getCurrency(),
                LocalDateTime.now(clock)));
    }
}
