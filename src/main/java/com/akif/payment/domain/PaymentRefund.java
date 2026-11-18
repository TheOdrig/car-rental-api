package com.akif.payment.domain;

import com.akif.payment.domain.enums.RefundStatus;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_refunds",
        indexes = {
                @Index(name = "idx_payment_refunds_payment", columnList = "payment_id")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentRefund extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "operation_key", length = 100, nullable = false)
    private String operationKey;

    @Column(name = "stripe_refund_id", length = 255)
    private String stripeRefundId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RefundStatus status;

    public boolean markFailed() {
        if (status == RefundStatus.FAILED) {
            return false;
        }
        this.status = RefundStatus.FAILED;
        return true;
    }
}
