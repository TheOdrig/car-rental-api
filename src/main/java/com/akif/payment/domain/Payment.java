package com.akif.payment.domain;

import com.akif.shared.enums.CurrencyType;
import com.akif.payment.api.PaymentStatus;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payments_rental", columnList = "rental_id"),
                @Index(name = "idx_payments_status", columnList = "status"),
                @Index(name = "idx_payments_stripe_session", columnList = "stripe_session_id"),
                @Index(name = "idx_payments_stripe_intent", columnList = "stripe_payment_intent_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

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

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "refunded_amount", precision = 12, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;


    public void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
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
                ", userEmail='" + userEmail + '\'' +
                ", carLicensePlate='" + carLicensePlate + '\'' +
                ", amount=" + amount +
                ", currency=" + currency +
                ", status=" + status +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}