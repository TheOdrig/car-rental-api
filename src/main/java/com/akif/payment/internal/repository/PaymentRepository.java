package com.akif.payment.internal.repository;

import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRentalIdAndKindAndIsDeletedFalse(Long rentalId, PaymentKind kind);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.rentalId = :rentalId AND p.kind = com.akif.payment.domain.enums.PaymentKind.PENALTY
            AND p.status = com.akif.payment.api.PaymentStatus.CAPTURED AND p.isDeleted = false
            ORDER BY p.id DESC
            """)
    List<Payment> findCapturedPenaltyPayments(@Param("rentalId") Long rentalId);

    @Query("""
            SELECT COUNT(p) > 0 FROM Payment p
            WHERE p.rentalId = :rentalId AND p.id <> :paymentId
            AND p.kind = com.akif.payment.domain.enums.PaymentKind.PENALTY
            AND p.status = com.akif.payment.api.PaymentStatus.CAPTURED AND p.isDeleted = false
            """)
    boolean existsCapturedPenaltyOtherThan(@Param("rentalId") Long rentalId, @Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Payment p
            WHERE p.rentalId = :rentalId AND p.kind = com.akif.payment.domain.enums.PaymentKind.PENALTY
            AND p.status IN (com.akif.payment.api.PaymentStatus.PENDING, com.akif.payment.api.PaymentStatus.FAILED)
            AND p.closed = false AND p.isDeleted = false
            ORDER BY p.id
            """)
    List<Payment> findOpenPenaltyPaymentsForUpdate(@Param("rentalId") Long rentalId);

    Optional<Payment> findByKindAndReferenceIdAndIsDeletedFalse(PaymentKind kind, Long referenceId);

    Optional<Payment> findFirstByRentalIdAndKindAndIsDeletedFalseOrderByIdDesc(Long rentalId, PaymentKind kind);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.stripePaymentIntentId = :intentId AND p.isDeleted = false")
    Optional<Payment> findByIntentIdForUpdate(@Param("intentId") String intentId);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.updateTime >= :startDate AND p.updateTime < :endDate
            AND p.stripePaymentIntentId IS NOT NULL AND p.isDeleted = false
            """)
    List<Payment> findWithIntentUpdatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p FROM Payment p WHERE p.stripePaymentIntentId IN :intentIds AND p.isDeleted = false")
    List<Payment> findByStripePaymentIntentIdIn(@Param("intentIds") java.util.Collection<String> intentIds);

    @Query("""
            SELECT p.id, p.stripePaymentIntentId FROM Payment p
            WHERE p.status IN (com.akif.payment.api.PaymentStatus.PENDING, com.akif.payment.api.PaymentStatus.AUTHORIZED)
            AND p.closed = false AND p.isDeleted = false
            AND p.stripePaymentIntentId IS NOT NULL
            AND COALESCE(p.lastSyncedAt, p.updateTime) < :cutoff
            ORDER BY p.id
            """)
    List<Object[]> findStaleNonTerminal(@Param("cutoff") LocalDateTime cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT p.id FROM Payment p
            WHERE p.status = com.akif.payment.api.PaymentStatus.PENDING
            AND p.kind = com.akif.payment.domain.enums.PaymentKind.PENALTY
            AND p.closed = false AND p.isDeleted = false
            AND p.stripePaymentIntentId IS NULL
            AND COALESCE(p.lastSyncedAt, p.updateTime) < :cutoff
            ORDER BY p.id
            """)
    List<Long> findStalePenaltiesWithoutIntent(@Param("cutoff") LocalDateTime cutoff, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT p.id FROM Payment p
            WHERE p.status = com.akif.payment.api.PaymentStatus.CAPTURED
            AND p.kind IN (com.akif.payment.domain.enums.PaymentKind.PENALTY, com.akif.payment.domain.enums.PaymentKind.DAMAGE)
            AND p.closed = true AND p.isDeleted = false
            AND p.updateTime < :cutoff
            ORDER BY p.id
            """)
    List<Long> findClosedCapturedForRefund(@Param("cutoff") LocalDateTime cutoff, org.springframework.data.domain.Pageable pageable);

    Optional<Payment> findByStripePaymentIntentIdAndIsDeletedFalse(String stripePaymentIntentId);

    @Query("""
            SELECT p.kind, p.currency, COALESCE(SUM(p.amount - COALESCE(p.refundedAmount, 0)), 0), COUNT(p)
            FROM Payment p
            WHERE p.status = com.akif.payment.api.PaymentStatus.CAPTURED
            AND p.capturedAt >= :start
            AND p.capturedAt < :end
            AND p.isDeleted = false
            GROUP BY p.kind, p.currency
            ORDER BY p.kind, p.currency
            """)
    List<Object[]> sumCollectedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.status = com.akif.payment.api.PaymentStatus.CAPTURED
            AND p.capturedAt >= :start
            AND p.isDeleted = false
            ORDER BY p.capturedAt
            """)
    List<Payment> findCapturedSince(@Param("start") LocalDateTime start);

    @Query("""
            SELECT p.currency, COUNT(DISTINCT p.rentalId), COALESCE(SUM(p.amount - COALESCE(p.refundedAmount, 0)), 0)
            FROM Payment p
            WHERE p.rentalId IN :rentalIds
            AND p.status = com.akif.payment.api.PaymentStatus.CAPTURED
            AND p.isDeleted = false
            GROUP BY p.currency
            ORDER BY p.currency
            """)
    List<Object[]> sumNetPaidByRentalIds(@Param("rentalIds") java.util.Collection<Long> rentalIds);

    int countByStatusAndIsDeletedFalse(PaymentStatus status);
}
