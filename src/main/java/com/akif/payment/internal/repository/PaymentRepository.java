package com.akif.payment.internal.repository;

import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRentalIdAndIsDeletedFalse(Long rentalId);

    @Query("SELECT p FROM Payment p WHERE p.createTime >= :startDate AND p.createTime < :endDate AND p.isDeleted = false")
    List<Payment> findByCreateTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    Optional<Payment> findByStripeSessionIdAndIsDeletedFalse(String stripeSessionId);

    Optional<Payment> findByStripePaymentIntentIdAndIsDeletedFalse(String stripePaymentIntentId);


    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p 
            WHERE p.status = com.akif.payment.api.PaymentStatus.CAPTURED 
            AND p.createTime >= :start 
            AND p.createTime < :end 
            AND p.isDeleted = false
            """)
    BigDecimal sumCapturedPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT p FROM Payment p 
            WHERE p.status = :status 
            AND p.createTime >= :start 
            AND p.isDeleted = false 
            ORDER BY p.createTime DESC
            """)
    List<Payment> findByStatusAndCreateTimeAfter(
            @Param("status") PaymentStatus status, 
            @Param("start") LocalDateTime start);

    int countByStatusAndIsDeletedFalse(PaymentStatus status);
}