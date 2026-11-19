package com.akif.payment.internal.service;

import com.akif.payment.domain.Payment;
import com.akif.payment.domain.PaymentRefund;
import com.akif.payment.domain.enums.RefundStatus;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.internal.repository.PaymentRefundRepository;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.IdempotencyKeyGenerator;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.gateway.StripeAmounts;
import com.akif.shared.enums.CurrencyType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
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
class PaymentRefundService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final IdempotencyKeyGenerator keys;
    private final EntityManager entityManager;
    private final PaymentSupport support;

    @Value("${payment.refund.lock-timeout-ms:5000}")
    private long refundLockTimeoutMs;

    @Value("${payment.refund.statement-timeout-ms:30000}")
    private long refundStatementTimeoutMs;

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult refundPayment(Long paymentId, BigDecimal refundAmount, String operationKey) {
        return refundInternal(() -> paymentRepository.findByIdForUpdate(paymentId), refundAmount, operationKey);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public PaymentResult refund(String transactionId, BigDecimal amount, String operationKey) {
        return refundInternal(() -> paymentRepository.findByIntentIdForUpdate(transactionId), amount, operationKey);
    }

    private PaymentResult refundInternal(java.util.function.Supplier<Optional<Payment>> locator,
                                         BigDecimal refundAmount, String operationKey) {
        try {
            return support.isolated().execute(status -> {
                applyRefundTimeouts();
                Optional<Payment> located = locator.get();
                if (located.isEmpty()) {
                    return PaymentResult.failure("No payment found for transaction");
                }
                Payment payment = located.get();
                Optional<PaymentRefund> already = refundRepository.findByPaymentIdAndOperationKeyAndStatus(
                        payment.getId(), operationKey, RefundStatus.SUCCEEDED);
                if (already.isPresent()) {
                    String refundId = already.get().getStripeRefundId();
                    return PaymentResult.success(
                            refundId != null ? refundId : payment.getTransactionId(), "Refund already processed");
                }
                if (!payment.canRefund()) {
                    return PaymentResult.failure("Payment cannot be refunded, current status: " + payment.getStatus());
                }
                if (payment.getStripePaymentIntentId() == null) {
                    return PaymentResult.failure("Payment has no gateway reference");
                }
                if (refundAmount == null || refundAmount.signum() <= 0) {
                    return PaymentResult.failure("Refund amount must be positive");
                }
                BigDecimal others = refundRepository.sumSucceededExcludingOperation(payment.getId(), operationKey);
                BigDecimal targetTotal = others.add(refundAmount);
                if (targetTotal.compareTo(payment.getAmount()) > 0) {
                    return PaymentResult.failure("Refund amount exceeds the refundable balance");
                }
                long failed = refundRepository.countByPaymentIdAndOperationKeyAndStatus(
                        payment.getId(), operationKey, RefundStatus.FAILED);
                String intentId = payment.getStripePaymentIntentId();
                CurrencyType currency = payment.getCurrency();
                String key = keys.forRefundOperation(operationKey, (int) failed);
    
                long alreadyRefundedMinor;
                try {
                    alreadyRefundedMinor = paymentGateway.refundedMinor(intentId);
                } catch (RuntimeException e) {
                    log.warn("Could not read the refunded amount from the gateway: payment={}, reason={}",
                            payment.getId(), e.getMessage());
                    return PaymentResult.failure("Could not verify the refunded amount with the gateway");
                }
                long missingMinor = StripeAmounts.toMinorUnits(targetTotal, currency) - alreadyRefundedMinor;
    
                String refundId = null;
                if (missingMinor > 0) {
                    BigDecimal toRefund = StripeAmounts.fromMinorUnits(missingMinor, currency);
                    PaymentResult result;
                    try {
                        result = paymentGateway.refund(intentId, toRefund, currency, key);
                    } catch (RuntimeException e) {
                        log.warn("Refund request failed: payment={}, reason={}", payment.getId(), e.getMessage());
                        return PaymentResult.failure("Refund request failed");
                    }
                    if (!result.success()) {
                        log.warn("Refund failed: payment={}, reason={}", payment.getId(), result.message());
                        return result;
                    }
                    refundId = result.transactionId();
                }
    
                boolean duplicate = refundId != null && refundRepository.findByStripeRefundId(refundId).isPresent();
                if (!duplicate) {
                    try {
                        refundRepository.saveAndFlush(PaymentRefund.builder()
                                .paymentId(payment.getId())
                                .operationKey(operationKey)
                                .stripeRefundId(refundId)
                                .amount(refundAmount)
                                .status(RefundStatus.SUCCEEDED)
                                .isDeleted(false)
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        log.info("Refund already recorded: payment={}, operation={}", payment.getId(), operationKey);
                    }
                }
                payment.applyRefundTotal(targetTotal);
                paymentRepository.save(payment);
                log.info("Refunded payment: ID={}, operation={}, amount={}", payment.getId(), operationKey, refundAmount);
                return PaymentResult.success(
                        refundId != null ? refundId : payment.getTransactionId(), "Refund processed");
            });
        } catch (PessimisticLockingFailureException | QueryTimeoutException e) {
            log.warn("Refund timed out waiting for the payment lock or the database: reason={}", e.getMessage());
            return PaymentResult.failure("Refund is busy, retry shortly");
        }
    }

    private void applyRefundTimeouts() {
        entityManager.createNativeQuery("select set_config('lock_timeout', ?1, true), set_config('statement_timeout', ?2, true)")
                .setParameter(1, refundLockTimeoutMs + "ms")
                .setParameter(2, refundStatementTimeoutMs + "ms")
                .getSingleResult();
    }
}
