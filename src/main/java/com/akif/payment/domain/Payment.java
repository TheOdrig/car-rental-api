package com.akif.payment.domain;

import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.api.PaymentStatus;
import com.akif.shared.domain.BaseEntity;
import com.akif.shared.enums.CurrencyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payments_rental", columnList = "rental_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_stripe_session", columnList = "stripe_session_id"),
                @Index(name = "idx_payments_stripe_intent", columnList = "stripe_payment_intent_id")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 20, nullable = false)
    @Builder.Default
    private PaymentKind kind = PaymentKind.RENTAL;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "car_license_plate", length = 20)
    private String carLicensePlate;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10, nullable = false)
    private CurrencyType currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "stripe_session_id", length = 255)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "stripe_payment_method_id", length = 255)
    private String stripePaymentMethodId;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "intent_attempt", nullable = false)
    @Builder.Default
    private int intentAttempt = 1;

    @Column(name = "closed", nullable = false)
    @Builder.Default
    private boolean closed = false;

    @Column(name = "refund_count", nullable = false)
    @Builder.Default
    private int refundCount = 0;

    @Column(name = "refunded_amount", precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public void applyStatus(PaymentStatus newStatus, String newTransactionId, String newFailureReason) {
        this.status = newStatus;
        if (newTransactionId != null) {
            this.transactionId = newTransactionId;
        }
        if (newFailureReason != null) {
            this.failureReason = newFailureReason;
        }
    }

    public void markCaptured(String captureTransactionId, String gatewayMessage) {
        this.status = PaymentStatus.CAPTURED;
        this.transactionId = captureTransactionId;
        this.gatewayResponse = gatewayMessage;
    }

    public void markFailed(String reason, String gatewayMessage) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        if (gatewayMessage != null) {
            this.gatewayResponse = gatewayMessage;
        }
    }

    public void attachIntent(String intentId, String key) {
        this.stripePaymentIntentId = intentId;
        this.transactionId = intentId;
        this.idempotencyKey = key;
    }

    public void rememberCustomer(String customerId) {
        if (customerId != null) {
            this.stripeCustomerId = customerId;
        }
    }

    public void rememberPaymentMethod(String paymentMethodId) {
        if (paymentMethodId != null) {
            this.stripePaymentMethodId = paymentMethodId;
        }
    }

    public boolean authorize() {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.FAILED) {
            this.status = PaymentStatus.AUTHORIZED;
            this.failureReason = null;
            return true;
        }
        return false;
    }

    public boolean capture() {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.AUTHORIZED || status == PaymentStatus.FAILED) {
            this.status = PaymentStatus.CAPTURED;
            this.failureReason = null;
            return true;
        }
        return false;
    }

    public boolean cancel() {
        if (status == PaymentStatus.PENDING || status == PaymentStatus.AUTHORIZED || status == PaymentStatus.FAILED) {
            this.status = PaymentStatus.CANCELLED;
            return true;
        }
        return false;
    }

    public void close() {
        this.closed = true;
    }

    public boolean fail(String reason) {
        if (status == PaymentStatus.PENDING) {
            markFailed(reason, null);
            return true;
        }
        return false;
    }

    public boolean reopen() {
        if (status == PaymentStatus.FAILED) {
            this.status = PaymentStatus.PENDING;
            return true;
        }
        return false;
    }

    public void restart() {
        this.status = PaymentStatus.PENDING;
        this.failureReason = null;
        nextAttempt();
    }

    public void nextAttempt() {
        this.intentAttempt++;
        this.stripePaymentIntentId = null;
    }

    public BigDecimal refundedTotal() {
        return refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
    }

    public BigDecimal refundableBalance() {
        return amount.subtract(refundedTotal());
    }

    public void applyRefundTotal(BigDecimal total) {
        if (total.compareTo(refundedTotal()) > 0) {
            this.refundedAmount = total;
        }
        if (refundedTotal().compareTo(amount) >= 0 && status == PaymentStatus.CAPTURED) {
            this.status = PaymentStatus.REFUNDED;
        }
    }

    public void markSynced(LocalDateTime at) {
        this.lastSyncedAt = at;
    }

    public void reverseRefund(BigDecimal amountToReverse) {
        BigDecimal reduced = refundedTotal().subtract(amountToReverse);
        this.refundedAmount = reduced.signum() > 0 ? reduced : BigDecimal.ZERO;
        if (status == PaymentStatus.REFUNDED && refundedTotal().compareTo(amount) < 0) {
            this.status = PaymentStatus.CAPTURED;
        }
    }

    public void recordRefundIssued(BigDecimal previousTotal, BigDecimal refundAmount) {
        applyRefundTotal(previousTotal.add(refundAmount));
        this.refundCount++;
    }

    public void recordRefund(BigDecimal refundAmount) {
        BigDecimal alreadyRefunded = refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
        this.refundedAmount = alreadyRefunded.add(refundAmount);
        if (this.refundedAmount.compareTo(amount) >= 0) {
            this.status = PaymentStatus.REFUNDED;
        }
    }

    public boolean isSuccessful() {
        return status.isSuccessful();
    }

    public boolean canRefund() {
        return status.canRefund();
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + getId() +
                ", rentalId=" + rentalId +
                ", kind=" + kind +
                ", amount=" + amount +
                ", currency=" + currency +
                ", status=" + status +
                '}';
    }
}
