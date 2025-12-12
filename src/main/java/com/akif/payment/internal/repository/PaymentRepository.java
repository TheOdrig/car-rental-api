package com.akif.payment.internal.repository;

import com.akif.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRentalIdAndIsDeletedFalse(Long rentalId);

    @Query("SELECT p FROM Payment p WHERE p.createTime >= :startDate AND p.createTime < :endDate AND p.isDeleted = false")
    List<Payment> findByCreateTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}