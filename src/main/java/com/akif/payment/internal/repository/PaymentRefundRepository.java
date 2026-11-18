package com.akif.payment.internal.repository;

import com.akif.payment.domain.PaymentRefund;
import com.akif.payment.domain.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    Optional<PaymentRefund> findByPaymentIdAndOperationKeyAndStatus(
            Long paymentId, String operationKey, RefundStatus status);

    Optional<PaymentRefund> findByStripeRefundId(String stripeRefundId);

    long countByPaymentIdAndOperationKeyAndStatus(Long paymentId, String operationKey, RefundStatus status);

    @Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM PaymentRefund r
            WHERE r.paymentId = :paymentId AND r.status = com.akif.payment.domain.enums.RefundStatus.SUCCEEDED
            AND r.operationKey <> :operationKey
            """)
    BigDecimal sumSucceededExcludingOperation(
            @Param("paymentId") Long paymentId, @Param("operationKey") String operationKey);
}
